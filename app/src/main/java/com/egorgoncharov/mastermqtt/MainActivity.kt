package com.egorgoncharov.mastermqtt

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.egorgoncharov.mastermqtt.configuration.ConfigurationEntityConverter
import com.egorgoncharov.mastermqtt.manager.ConfigurationManager
import com.egorgoncharov.mastermqtt.manager.SoundManager
import com.egorgoncharov.mastermqtt.manager.StorageManager
import com.egorgoncharov.mastermqtt.manager.mqtt.MqttManager
import com.egorgoncharov.mastermqtt.model.dao.BrokerDao
import com.egorgoncharov.mastermqtt.model.dao.MessageDao
import com.egorgoncharov.mastermqtt.model.dao.SettingsProfileDao
import com.egorgoncharov.mastermqtt.model.dao.TopicDao
import com.egorgoncharov.mastermqtt.model.entity.SettingsProfileEntity
import com.egorgoncharov.mastermqtt.model.types.ThemeOption
import com.egorgoncharov.mastermqtt.screen.settings.SettingsScreen
import com.egorgoncharov.mastermqtt.screen.settings.brokers.BrokersScreen
import com.egorgoncharov.mastermqtt.screen.settings.brokers.BrokersScreenViewModel
import com.egorgoncharov.mastermqtt.screen.settings.general.GeneralSettingsScreen
import com.egorgoncharov.mastermqtt.screen.settings.general.GeneralSettingsScreenViewModel
import com.egorgoncharov.mastermqtt.screen.settings.topics.TopicsScreen
import com.egorgoncharov.mastermqtt.screen.settings.topics.TopicsScreenViewModel
import com.egorgoncharov.mastermqtt.screen.stream.StreamScreen
import com.egorgoncharov.mastermqtt.screen.stream.StreamScreenEvent
import com.egorgoncharov.mastermqtt.screen.stream.StreamScreenViewModel
import com.egorgoncharov.mastermqtt.service.MqttService
import com.egorgoncharov.mastermqtt.ui.theme.AppTheme

sealed class NavRoute(val route: String, val label: String, val icon: ImageVector) {
    data object Topics : NavRoute("topics", "Topics", Icons.Filled.Tag)
    data object Brokers : NavRoute("brokers", "Brokers", Icons.Filled.WifiTethering)
    data object Stream : NavRoute("stream?topicId={topicId}&showBrokersView={showBrokersView}", "Stream", Icons.Filled.Bolt)

    data object Settings : NavRoute("settings", "Settings", Icons.Filled.Settings)

    data object General : NavRoute("general", "General", Icons.Filled.SettingsInputComposite)

    companion object {
        val all = listOf(Topics, Brokers, Stream, Settings)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleNotificationIntent(intent)
        startForegroundService(Intent(this, MqttService::class.java))
        enableEdgeToEdge()
        setContent {
            val app = application as MasterMqttApp
            val db = app.databaseManager.db!!
            val settingsProfile by db.settingsProfilesDao().streamMainSettingsProfile().collectAsStateWithLifecycle(initialValue = SettingsProfileEntity.DUMMY)
            LaunchedEffect(Unit) { db.settingsProfilesDao().createMainSettingsProfileIfNotExists() }
            AppTheme(themeOption = settingsProfile?.theme ?: ThemeOption.SYSTEM) {
                val navController = rememberNavController()
                var showSettingsList by remember { mutableStateOf(false) }
                Scaffold { innerPadding ->
                    Box {
                        AppNavHost(
                            navController = navController,
                            modifier = Modifier.padding(innerPadding),
                            brokerDao = db.brokerDao(),
                            topicDao = db.topicDao(),
                            messageDao = db.messageDao(),
                            settingsProfileDao = db.settingsProfilesDao(),
                            mqttManager = app.mqttManager,
                            storageManager = app.storageManager,
                            soundManager = app.soundManager,
                            configurationManager = app.configurationManager,
                            configurationEntityConverter = app.configurationEntityConverter
                        )
                        if (showSettingsList) {
                            SettingsScreen(navController) { showSettingsList = false }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("ACTION_STOP_PINGING", false) == true) {
            (application as MasterMqttApp).alertManager.discardAlert()
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
    settingsProfileDao: SettingsProfileDao,
    mqttManager: MqttManager,
    storageManager: StorageManager,
    soundManager: SoundManager,
    configurationManager: ConfigurationManager,
    configurationEntityConverter: ConfigurationEntityConverter
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
                            factory = BrokersScreenViewModel.Factory(brokerDao, topicDao, messageDao, mqttManager)
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
                            factory = TopicsScreenViewModel.Factory(brokerDao, topicDao, messageDao, settingsProfileDao, storageManager, soundManager)
                        ),
                        navController
                    )
                }
            }
        }

        composable(NavRoute.General.route) {
            Scaffold(Modifier.padding(20.dp)) {
                Column(Modifier.fillMaxSize()) {
                    GeneralSettingsScreen(
                        vm = viewModel(
                            factory = GeneralSettingsScreenViewModel.Factory(brokerDao, topicDao, settingsProfileDao, configurationManager, configurationEntityConverter)
                        ),
                        navController = navController
                    )
                }
            }
        }

        composable(NavRoute.Stream.route, deepLinks = listOf(navDeepLink { uriPattern = "mastermqtt://${NavRoute.Stream.route}" })) { backStackEntry ->
            val context = LocalContext.current
            BackHandler(enabled = true) {
                (context as? Activity)?.finish()
            }
            Scaffold(Modifier.padding(20.dp)) {
                Column(Modifier.fillMaxSize()) {
                    val topicId = backStackEntry.arguments?.getString("topicId")
                    val showBrokersView = backStackEntry.arguments?.getString("showBrokersView") == "true"
                    val vm = viewModel<StreamScreenViewModel>(factory = StreamScreenViewModel.Factory(brokerDao, topicDao, messageDao, settingsProfileDao, mqttManager))
                    if (topicId != null) {
                        LaunchedEffect(Unit) {
                            if (!vm.isDeepLinkBound()) {
                                val topic = topicDao.findById(topicId) ?: return@LaunchedEffect
                                vm.onEvent(StreamScreenEvent.SelectedStreamChanged(topic))
                                vm.onEvent(StreamScreenEvent.DeepLinkBoundChanged(true))
                            }
                        }
                    } else vm.onEvent(StreamScreenEvent.DeepLinkBoundChanged(false))
                    if (showBrokersView && !vm.isBrokersViewBound()) vm.onEvent(StreamScreenEvent.BrokersViewBoundChanged(true))
                    else vm.onEvent(StreamScreenEvent.BrokersViewBoundChanged(false))
                    StreamScreen(vm = vm, navController = navController)
                }
            }
        }
    }
}
