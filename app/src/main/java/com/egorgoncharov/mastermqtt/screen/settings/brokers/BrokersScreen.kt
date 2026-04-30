package com.egorgoncharov.mastermqtt.screen.settings.brokers

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.WifiTetheringError
import androidx.compose.material.icons.filled.WifiTetheringOff
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.egorgoncharov.mastermqtt.Utils
import com.egorgoncharov.mastermqtt.manager.PermissionManager
import com.egorgoncharov.mastermqtt.manager.mqtt.MqttConnection
import com.egorgoncharov.mastermqtt.model.types.ConnectionType
import com.egorgoncharov.mastermqtt.model.types.MqttConnectionState
import com.egorgoncharov.mastermqtt.ui.components.DialogWindow
import com.egorgoncharov.mastermqtt.ui.components.Empty
import com.egorgoncharov.mastermqtt.ui.components.Error
import com.egorgoncharov.mastermqtt.ui.components.FormIsland
import com.egorgoncharov.mastermqtt.ui.components.ItemAction
import com.egorgoncharov.mastermqtt.ui.components.ItemProperty
import com.egorgoncharov.mastermqtt.ui.components.PermissionRequestDialog
import com.egorgoncharov.mastermqtt.ui.components.SettingsTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrokersScreen(vm: BrokersScreenViewModel, navController: NavHostController) {
    val manageBrokerState by vm.manageBrokerFormState.collectAsStateWithLifecycle()
    val connections by vm.connections.collectAsStateWithLifecycle()

    DisposableEffect(Unit) { onDispose { if (manageBrokerState.visible) vm.onEvent(BrokersScreenEvent.ToggleManageForm(null)) } }
    if (manageBrokerState.visible) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxWidth(),
            sheetState = rememberModalBottomSheetState(confirmValueChange = { it != SheetValue.Hidden }, skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            onDismissRequest = { vm.onEvent(BrokersScreenEvent.ToggleManageForm(null)) }
        ) {
            BrokerManage(manageBrokerState) { vm.onEvent(it) }
        }
    }
    Column(Modifier.fillMaxWidth()) {
        SettingsTopBar(navController, "Brokers Management") {
            button(onClick = { vm.onEvent(BrokersScreenEvent.ToggleManageForm(null)) }) {
                Icon(modifier = Modifier.size(24.dp), imageVector = Icons.Default.AddCircle, contentDescription = null)
            }
        }
        Spacer(Modifier.height(20.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(connections.values.toList(), key = { it.broker.id }) { connection -> BrokerContainer(vm, connection) }
            if (connections.isEmpty()) item { Empty() }
        }
    }
}

@Composable
fun BrokerContainer(vm: BrokersScreenViewModel, connection: MqttConnection) {
    val context = LocalContext.current
    val permissionManager = remember { PermissionManager(context) }
    val notificationPermissionState by vm.notificationPermissionState.collectAsStateWithLifecycle()
    val brokerDeleteConfirmationDialogState by vm.brokerDeleteConfirmationDialogState.collectAsStateWithLifecycle()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                vm.onEvent(BrokersScreenEvent.ToggleBrokerDeleteConfirmationDialog(connection.broker, true))
                false
            } else false
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.85f }
    )
    val expanded = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val isGranted = permissionManager.isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
                    vm.onEvent(BrokersScreenEvent.NotificationPermissionResult(isGranted))
                } else {
                    vm.onEvent(BrokersScreenEvent.NotificationPermissionResult(true))
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (notificationPermissionState.showRequestDialog) {
        PermissionRequestDialog(
            "Notifications",
            onDismiss = { vm.onEvent(BrokersScreenEvent.ToggleNotificationPermissionRequestDialog) },
            onGoToSettings = {
                vm.onEvent(BrokersScreenEvent.ToggleNotificationPermissionRequestDialog)
                permissionManager.openAppSettings()
            }
        )
    }
    DialogWindow(brokerDeleteConfirmationDialogState)
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {}
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .animateContentSize(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                BrokerHead(connection, expanded, permissionManager) { vm.onEvent(it) }
                if (expanded.value) BrokerBody(connection)
            }
        }
    }
}

@Composable
fun BrokerHead(
    connection: MqttConnection,
    expanded: MutableState<Boolean>,
    permissionManager: PermissionManager,
    onEvent: (BrokersScreenEvent) -> Unit
) {
    val activity = LocalContext.current as? ComponentActivity
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onEvent(BrokersScreenEvent.NotificationPermissionResult(isGranted))
        if (isGranted) {
            onEvent(BrokersScreenEvent.ToggleConnection(connection))
        } else {
            val blocked = activity?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
                } else false
            } ?: false
            if (blocked) onEvent(BrokersScreenEvent.ToggleNotificationPermissionRequestDialog)
        }
    }
    var passwordRevealed by remember { mutableStateOf(false) }
    val displayedPassword = if (connection.broker.authUser == null) "Unauthenticated" else {
        val pass = if (passwordRevealed) connection.broker.authPassword!! else "*".repeat(connection.broker.authPassword!!.length)
        "${connection.broker.authUser}/$pass"
    }
    val isConnected = connection.state == MqttConnectionState.CONNECTED

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(end = 10.dp)
        ) {
            val name = Utils.abbreviateMiddle(connection.broker.name, 32)
            val address = Utils.abbreviateMiddle(
                "${if (connection.broker.connectionType == ConnectionType.TCP) "tcp://" else "ssl://"}${connection.broker.host}:${connection.broker.port}",
                32
            )
            Text(name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(5.dp))
            ItemProperty(null, address, Icons.Filled.WifiTethering)
            Spacer(Modifier.height(3.dp))
            Box(Modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(); passwordRevealed = true; waitForUpOrCancellation(); passwordRevealed = false
                    }
                }
            }) {
                ItemProperty(
                    null,
                    displayedPassword,
                    Icons.Filled.Lock
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            ItemAction(
                Icons.Filled.WifiTethering,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val isGranted = permissionManager.isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
                        if (isGranted) {
                            onEvent(BrokersScreenEvent.ToggleConnection(connection))
                        } else {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        onEvent(BrokersScreenEvent.ToggleConnection(connection))
                    }
                }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = if (isConnected) MaterialTheme.colorScheme.primary else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (connection.state) {
                        MqttConnectionState.DISCONNECTED -> Icons.Filled.WifiTetheringOff
                        MqttConnectionState.DISCONNECTED_FAILED -> Icons.Filled.WifiTetheringError
                        MqttConnectionState.CONNECTED -> Icons.Filled.WifiTethering
                        else -> null
                    }
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
            ItemAction(
                Icons.Filled.Edit,
                onClick = { onEvent(BrokersScreenEvent.ToggleManageForm(connection.broker)) })
            ItemAction(
                if (expanded.value) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                onClick = { expanded.value = !expanded.value })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrokerBody(connection: MqttConnection) {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        ItemProperty("Client ID", connection.broker.clientId, Icons.Filled.Tag)
        ItemProperty(
            "Keep Alive Interval",
            "${connection.broker.keepAliveInterval}s",
            Icons.Filled.MonitorHeart
        )
        ItemProperty(
            "Clean Start",
            if (connection.broker.cleanStart) "Yes" else "No",
            Icons.Filled.CleaningServices
        )
        ItemProperty(
            "Reconnect Attempts",
            "${connection.broker.reconnectAttempts ?: "Infinite"}",
            Icons.Filled.Refresh
        )
        ItemProperty(
            "Reconnect Interval",
            "${connection.broker.reconnectInterval}s",
            Icons.Filled.Timer
        )
        ItemProperty(
            "Session Expiry Interval",
            "${connection.broker.sessionExpiryInterval ?: "None"}",
            Icons.Filled.AutoAwesomeMotion
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrokerManage(state: ManageBrokerFormState, onEvent: (BrokersScreenEvent) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    if (state.reference == null) "New Broker" else "Edit Broker",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            item {
                FormIsland(title = "Address") {
                    OutlinedTextField(
                        value = state.name.value,
                        onValueChange = { onEvent(BrokersScreenEvent.NameChanged(it)) },
                        label = { Text("Name") },
                        isError = state.name.errorMsg != null,
                        supportingText = { if (state.name.errorMsg != null) Text(state.name.errorMsg) },
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.host.value,
                            onValueChange = { onEvent(BrokersScreenEvent.HostChanged(it)) },
                            label = { Text("IP Address/Domain") },
                            isError = state.host.errorMsg != null,
                            supportingText = { if (state.host.errorMsg != null) Text(state.host.errorMsg) },
                            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = if (state.port.value == null) "" else state.port.value.toString(),
                            onValueChange = { onEvent(BrokersScreenEvent.PortChanged(it)) },
                            label = { Text("Port") },
                            isError = state.port.errorMsg != null,
                            supportingText = { if (state.port.errorMsg != null) Text(state.port.errorMsg) },
                            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }
            }
            item {
                FormIsland(title = "Authentication") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = state.authenticated.value,
                            onCheckedChange = {
                                onEvent(
                                    BrokersScreenEvent.AuthenticationSettingsChanged(
                                        authenticated = it,
                                        user = state.authUser.value,
                                        password = state.authPassword.value
                                    )
                                )
                            })
                        Spacer(Modifier.width(10.dp))
                        Text("Use Credentials")
                    }
                    if (state.authenticated.value) {
                        OutlinedTextField(
                            value = state.authUser.value,
                            onValueChange = {
                                onEvent(
                                    BrokersScreenEvent.AuthenticationSettingsChanged(
                                        authenticated = state.authenticated.value,
                                        user = it,
                                        password = state.authPassword.value
                                    )
                                )
                            },
                            label = { Text("Username") },
                            isError = state.authUser.errorMsg != null,
                            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = state.authPassword.value,
                            onValueChange = {
                                onEvent(
                                    BrokersScreenEvent.AuthenticationSettingsChanged(
                                        authenticated = state.authenticated.value,
                                        user = state.authUser.value,
                                        password = it
                                    )
                                )
                            },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item {
                FormIsland(title = "Connection") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = state.connectionType.value == ConnectionType.TCP,
                            onClick = { onEvent(BrokersScreenEvent.ConnectionTypeChanged(ConnectionType.TCP)) }
                        )
                        Text("TCP")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(
                            selected = state.connectionType.value == ConnectionType.SSL,
                            onClick = { onEvent(BrokersScreenEvent.ConnectionTypeChanged(ConnectionType.SSL)) }
                        )
                        Text("SSL/TLS")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = state.infiniteReconnects.value,
                            onCheckedChange = { onEvent(BrokersScreenEvent.InfiniteReconnectsChanged(it)) }
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Infinite Reconnects")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = if (state.reconnectInterval.value == null) "" else state.reconnectInterval.value.toString(),
                            onValueChange = { onEvent(BrokersScreenEvent.ReconnectIntervalChanged(it)) },
                            label = { Text("Reconnection Interval (s)") },
                            isError = state.reconnectInterval.errorMsg != null,
                            supportingText = {
                                if (state.reconnectInterval.errorMsg != null) Text(
                                    state.reconnectInterval.errorMsg
                                )
                            },
                            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        if (!state.infiniteReconnects.value) {
                            OutlinedTextField(
                                value = state.reconnectAttempts.value.toString(),
                                onValueChange = { onEvent(BrokersScreenEvent.ReconnectAttemptsChanged(it)) },
                                label = { Text("Retries") },
                                isError = state.reconnectAttempts.errorMsg != null,
                                supportingText = {
                                    if (state.reconnectAttempts.errorMsg != null) Text(
                                        state.reconnectAttempts.errorMsg
                                    )
                                },
                                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = if (state.keepAliveInterval.value == null) "" else state.keepAliveInterval.value.toString(),
                        onValueChange = { onEvent(BrokersScreenEvent.KeepAliveIntervalChanged(it)) },
                        label = { Text("Keep Alive (s)") },
                        isError = state.keepAliveInterval.errorMsg != null,
                        supportingText = { if (state.keepAliveInterval.errorMsg != null) Text(state.keepAliveInterval.errorMsg) },
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                }
            }
            item {
                FormIsland(title = "Session") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = state.cleanStart.value,
                            onCheckedChange = { onEvent(BrokersScreenEvent.CleanStartChanged(it)) }
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Clean Start")
                    }
                    OutlinedTextField(
                        value = state.clientId.value,
                        onValueChange = { onEvent(BrokersScreenEvent.ClientIdChanged(it)) },
                        label = { Text("Client ID") },
                        isError = state.clientId.errorMsg != null,
                        supportingText = { if (state.clientId.errorMsg != null) Text(state.clientId.errorMsg) },
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!state.cleanStart.value) {
                        OutlinedTextField(
                            value = if (state.sessionExpiryInterval.value == null) "" else state.sessionExpiryInterval.value.toString(),
                            onValueChange = { onEvent(BrokersScreenEvent.SessionExpiryIntervalChanged(it)) },
                            label = { Text("Messages Expiry Interval (s)") },
                            isError = state.sessionExpiryInterval.errorMsg != null,
                            supportingText = { if (state.sessionExpiryInterval.errorMsg != null) Text(state.sessionExpiryInterval.errorMsg) },
                            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        if (state.exists && state.reference == null) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Error("${state.host.value}:${state.port.value} already exists")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { onEvent(BrokersScreenEvent.ToggleManageForm(null)) }) { Text("Cancel") }
            Button(
                modifier = Modifier.weight(1f),
                enabled = state.valid() && (state.reference != null || !state.exists),
                onClick = { onEvent(BrokersScreenEvent.BrokerSaved) }
            ) {
                Text("Save")
            }
        }
    }
}
