package com.egorgoncharov.mastermqtt

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsInputComposite
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.egorgoncharov.mastermqtt.manager.mqtt.MQTTManager
import com.egorgoncharov.mastermqtt.model.dao.BrokerDao
import com.egorgoncharov.mastermqtt.model.dao.MessageDao
import com.egorgoncharov.mastermqtt.model.dao.TopicDao
import com.egorgoncharov.mastermqtt.screen.BrokerViewModel
import com.egorgoncharov.mastermqtt.screen.BrokersScreen
import com.egorgoncharov.mastermqtt.screen.GeneralSettingsScreen
import com.egorgoncharov.mastermqtt.screen.SettingsScreen
import com.egorgoncharov.mastermqtt.screen.StreamEvent
import com.egorgoncharov.mastermqtt.screen.StreamScreen
import com.egorgoncharov.mastermqtt.screen.StreamViewModel
import com.egorgoncharov.mastermqtt.screen.TopicViewModel
import com.egorgoncharov.mastermqtt.screen.TopicsScreen
import com.egorgoncharov.mastermqtt.service.MQTTService
import com.egorgoncharov.mastermqtt.ui.theme.AppTheme

sealed class NavRoute(val route: String, val label: String, val icon: ImageVector) {
    data object Topics : NavRoute("topics", "Topics", Icons.Filled.Tag)
    data object Brokers : NavRoute("brokers", "Brokers", Icons.Filled.WifiTethering)
    data object Stream : NavRoute("stream?topicId={topicId}", "Stream", Icons.Filled.Bolt)

    data object Settings : NavRoute("settings", "Settings", Icons.Filled.Settings)

    data object General : NavRoute("general", "General", Icons.Filled.SettingsInputComposite)

    companion object {
        val all = listOf(Topics, Brokers, Stream, Settings)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serviceIntent = Intent(this, MQTTService::class.java)
        startForegroundService(serviceIntent)
        enableEdgeToEdge()
        setContent {
            var mqttService by remember { mutableStateOf<MQTTService?>(null) }
            MqttServiceHandler { mqttService = it }
            val dbManager = mqttService?.binder?.database() ?: return@setContent
            val mqttManager = mqttService?.binder?.manager() ?: return@setContent
            val brokerDao = dbManager.db!!.brokerDao()
            val topicDao = dbManager.db!!.topicDao()
            val messageDao = dbManager.db!!.messageDao()
            AppTheme {
                val navController = rememberNavController()
                var showSettingsList by remember { mutableStateOf(false) }
                Scaffold { innerPadding ->
                    Box {
                        AppNavHost(
                            navController = navController,
                            modifier = Modifier.padding(innerPadding),
                            brokerDao = brokerDao,
                            topicDao = topicDao,
                            messageDao = messageDao,
                            mqttManager = mqttManager
                        )
                        if (showSettingsList) {
                            SettingsScreen(navController) { showSettingsList = false }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MqttServiceHandler(
    context: Context = LocalContext.current,
    onServiceConnected: (MQTTService) -> Unit
) {
    DisposableEffect(context) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as? MQTTService.LocalBinder
                binder?.service()?.let { mqttService ->
                    onServiceConnected(mqttService)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
            }
        }

        val intent = Intent(context, MQTTService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        onDispose {
            context.unbindService(connection)
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier,
    brokerDao: BrokerDao,
    topicDao: TopicDao,
    messageDao: MessageDao,
    mqttManager: MQTTManager
) {
    NavHost(
        navController = navController,
        startDestination = NavRoute.Stream.route,
        modifier = modifier
    ) {

        composable(NavRoute.Brokers.route) {
            Scaffold(Modifier.padding(20.dp)) {
                Column(Modifier.fillMaxSize()) {
                    BrokersScreen(
                        vm = viewModel(
                            factory = BrokerViewModel.Factory(brokerDao, mqttManager)
                        ), navController
                    )
                }
            }
        }

        composable(NavRoute.Topics.route) {
            Scaffold(Modifier.padding(20.dp)) {
                Column(Modifier.fillMaxSize()) {
                    TopicsScreen(
                        vm = viewModel(
                            factory = TopicViewModel.Factory(brokerDao, topicDao)
                        ),
                        navController
                    )
                }
            }
        }

        composable(NavRoute.General.route) {
            Scaffold(Modifier.padding(20.dp)) {
                Column(Modifier.fillMaxSize()) {
                    GeneralSettingsScreen(navController)
                }
            }
        }

        composable(NavRoute.Stream.route, deepLinks = listOf(navDeepLink { uriPattern = "mastermqtt://${NavRoute.Stream.route}" })) { backStackEntry ->
            Scaffold(Modifier.padding(20.dp)) {
                Column(Modifier.fillMaxSize()) {
                    val topicId = backStackEntry.arguments?.getString("topicId")
                    val vm = viewModel<StreamViewModel>(factory = StreamViewModel.Factory(brokerDao, topicDao, messageDao, mqttManager))
                    if (topicId != null) {
                        LaunchedEffect(Unit) {
                            if (!vm.isDeepLinkBound()) {
                                val topic = topicDao.findById(topicId) ?: return@LaunchedEffect
                                vm.onEvent(StreamEvent.SelectedStreamChanged(topic))
                                vm.onEvent(StreamEvent.DeepLinkBoundChanged(true))
                            }
                        }
                    } else vm.onEvent(StreamEvent.DeepLinkBoundChanged(false))
                    StreamScreen(vm = vm, navController = navController)
                }
            }
        }
    }
}