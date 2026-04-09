package com.egorgoncharov.mastermqtt.screen

import android.Manifest
import android.os.Build
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tag
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.egorgoncharov.mastermqtt.Utils
import com.egorgoncharov.mastermqtt.dto.MQTTConnection
import com.egorgoncharov.mastermqtt.dto.db.ConnectionType
import com.egorgoncharov.mastermqtt.dto.db.MQTTConnectionState
import com.egorgoncharov.mastermqtt.manager.PermissionManager
import com.egorgoncharov.mastermqtt.manager.mqtt.MQTTManager
import com.egorgoncharov.mastermqtt.model.dao.BrokerDao
import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.ui.components.Empty
import com.egorgoncharov.mastermqtt.ui.components.EntityManagingFormState
import com.egorgoncharov.mastermqtt.ui.components.FormFieldState
import com.egorgoncharov.mastermqtt.ui.components.FormIsland
import com.egorgoncharov.mastermqtt.ui.components.ItemAction
import com.egorgoncharov.mastermqtt.ui.components.ItemProperty
import com.egorgoncharov.mastermqtt.ui.components.PermissionRequestDialog
import com.egorgoncharov.mastermqtt.ui.components.SettingsTopBar
import com.egorgoncharov.mastermqtt.ui.components.hostRegex
import com.egorgoncharov.mastermqtt.ui.components.ipRegex
import com.egorgoncharov.mastermqtt.ui.components.nameRegex
import com.egorgoncharov.mastermqtt.ui.components.update
import com.egorgoncharov.mastermqtt.ui.components.validateNumericalInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class BrokerViewModel(private val brokerDao: BrokerDao, private val mqttManager: MQTTManager) :
    ViewModel() {
    companion object {
        fun Factory(brokerDao: BrokerDao, mqttManager: MQTTManager): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { BrokerViewModel(brokerDao, mqttManager) }
            }
    }

    private val _manageBrokerState = MutableStateFlow(ManageBrokerState())
    private val _notificationPermissionState = MutableStateFlow(NotificationPermissionState())

    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val connections = mqttManager.clientsFlow
    val manageBrokerState = _manageBrokerState.asStateFlow()
    val notificationPermissionState = _notificationPermissionState.asStateFlow()

    fun onEvent(event: BrokerEvent) {
        when (event) {
            is BrokerEvent.NameChanged -> handleNameChange(event.name)
            is BrokerEvent.HostChanged -> handleHostChange(event.host)
            is BrokerEvent.PortChanged -> handlePortChange(event.port)
            is BrokerEvent.AuthenticationSettingsChanged -> handleAuthenticationSettingsChange(
                event.authenticated,
                event.user,
                event.password
            )

            is BrokerEvent.ConnectionTypeChanged -> handleConnectionTypeChange(event.connectionType)
            is BrokerEvent.ClientIdChanged -> handleClientIdChange(event.clientId)
            is BrokerEvent.KeepAliveChanged -> handleKeepAliveChange(event.keepAlive)
            is BrokerEvent.ReconnectRetriesChanged -> handleReconnectRetriesChange(event.reconnectRetries)
            is BrokerEvent.BrokerSaved -> saveBroker()
            is BrokerEvent.ToggleManageForm -> toggleManageForm(event.reference)
            is BrokerEvent.NotificationPermissionResult -> notificationPermissionResult(event.isGranted)
            is BrokerEvent.ToggleNotificationPermissionRequestDialog -> toggleNotificationPermissionRequestDialog()
            is BrokerEvent.ToggleConnection -> toggleConnection(event.connection)
            is BrokerEvent.DeleteBroker -> deleteBroker(event.broker)
        }
    }

    private fun handleNameChange(name: String) = _manageBrokerState.update(
        { it.name },
        name,
        { if (it.matches(nameRegex)) null else "Invalid name format" }
    ) { copy(name = it) }

    private fun handleHostChange(host: String) = _manageBrokerState.update(
        { it.host },
        host,
        { if (it.matches(hostRegex) || it.matches(ipRegex)) null else "Invalid domain or IPv4 format" }
    ) { copy(host = it) }

    private fun handlePortChange(port: String) = _manageBrokerState.update(
        { it.port },
        port.toIntOrNull() ?: 0,
        { validateNumericalInput(port, true, 0.0, 65535.0) }
    ) { copy(port = it) }

    private fun handleAuthenticationSettingsChange(authenticated: Boolean, user: String, password: String) {
        _manageBrokerState.update(
            { it.authenticated },
            authenticated,
            { null }
        ) { copy(authenticated = it) }
        _manageBrokerState.update(
            { it.authUser },
            user,
            { if (!(authenticated && user.isBlank())) null else "User is required" }
        ) { copy(authUser = it) }
        _manageBrokerState.update(
            { it.authPassword },
            password,
            { null }
        ) { copy(authPassword = it) }
    }

    private fun handleConnectionTypeChange(connectionType: ConnectionType) = _manageBrokerState.update(
        { it.connectionType },
        connectionType,
        { null }
    ) { copy(connectionType = it) }

    private fun handleClientIdChange(clientId: String) = _manageBrokerState.update(
        { it.clientId },
        clientId,
        { if (clientId.isNotBlank()) null else "Client ID is required" }
    ) { copy(clientId = it) }

    private fun handleKeepAliveChange(keepAlive: String) = _manageBrokerState.update(
        { it.keepAlive },
        keepAlive.toIntOrNull() ?: 0,
        { validateNumericalInput(keepAlive, true, 1.0) }
    ) { copy(keepAlive = it) }

    private fun handleReconnectRetriesChange(reconnectRetries: String) = _manageBrokerState.update(
        { it.reconnectRetries },
        reconnectRetries.toIntOrNull() ?: 0,
        { validateNumericalInput(reconnectRetries, true, 0.0) }
    ) { copy(reconnectRetries = it) }

    private fun saveBroker() {
        if (!manageBrokerState.value.valid()) return
        val state = manageBrokerState.value
        scope.launch {
            brokerDao.save(
                BrokerEntity(
                    id = state.reference?.id ?: UUID.randomUUID().toString(),
                    name = state.name.value,
                    clientId = state.clientId.value,
                    connected = false,
                    ip = state.host.value,
                    port = state.port.value,
                    user = if (state.authenticated.value) state.authUser.value else null,
                    password = if (state.authenticated.value) state.authPassword.value else null,
                    connectionType = state.connectionType.value,
                    keepAliveInterval = state.keepAlive.value,
                    reconnectAttempts = state.reconnectRetries.value,
                    displayIndex = 0,
                    removed = false,
                )
            )
        }
        toggleManageForm(null)
    }

    private fun toggleManageForm(reference: BrokerEntity?) {
        resetManageForm()
        _manageBrokerState.update {
            ManageBrokerState(
                visible = !it.visible,
                reference = reference
            )
        }
        updateManageFormErrors()
    }

    private fun resetManageForm() {
        _manageBrokerState.update { ManageBrokerState(visible = it.visible) }
    }

    private fun updateManageFormErrors() {
        handleNameChange(manageBrokerState.value.name.value)
        handleHostChange(manageBrokerState.value.host.value)
        handlePortChange(manageBrokerState.value.port.value.toString())
        handleAuthenticationSettingsChange(
            manageBrokerState.value.authenticated.value,
            manageBrokerState.value.authUser.value,
            manageBrokerState.value.authPassword.value
        )
        handleConnectionTypeChange(manageBrokerState.value.connectionType.value)
        handleClientIdChange(manageBrokerState.value.clientId.value)
        handleKeepAliveChange(manageBrokerState.value.keepAlive.value.toString())
        handleReconnectRetriesChange(manageBrokerState.value.reconnectRetries.value.toString())
    }

    private fun notificationPermissionResult(isGranted: Boolean) {
        _notificationPermissionState.update { it.copy(granted = isGranted) }
    }

    private fun toggleNotificationPermissionRequestDialog() {
        _notificationPermissionState.update { it.copy(showRequestDialog = !it.showRequestDialog) }
    }

    private fun toggleConnection(connection: MQTTConnection) {
        if (!notificationPermissionState.value.granted) {
            _notificationPermissionState.update { it.copy(showRequestDialog = true) }
            return
        }
        if (connection.state == MQTTConnectionState.CONNECTED) mqttManager.disconnect(connection.broker)
        else mqttManager.connect(connection.broker)
    }

    private fun deleteBroker(broker: BrokerEntity) {
        scope.launch { brokerDao.delete(broker) }
    }
}

sealed interface BrokerEvent {
    data class NameChanged(val name: String) : BrokerEvent
    data class HostChanged(val host: String) : BrokerEvent
    data class PortChanged(val port: String) : BrokerEvent
    data class AuthenticationSettingsChanged(
        val authenticated: Boolean,
        val user: String,
        val password: String
    ) : BrokerEvent

    data class ConnectionTypeChanged(val connectionType: ConnectionType) : BrokerEvent
    data class ClientIdChanged(val clientId: String) : BrokerEvent
    data class KeepAliveChanged(val keepAlive: String) : BrokerEvent
    data class ReconnectRetriesChanged(val reconnectRetries: String) : BrokerEvent
    object BrokerSaved : BrokerEvent
    data class ToggleManageForm(val reference: BrokerEntity?) : BrokerEvent

    data class NotificationPermissionResult(val isGranted: Boolean) : BrokerEvent
    object ToggleNotificationPermissionRequestDialog : BrokerEvent

    data class ToggleConnection(val connection: MQTTConnection) : BrokerEvent
    data class DeleteBroker(val broker: BrokerEntity) : BrokerEvent
}

data class ManageBrokerState(
    override val visible: Boolean = false,
    override val reference: BrokerEntity? = null,
    val name: FormFieldState<String> = FormFieldState(reference?.name ?: ""),
    val host: FormFieldState<String> = FormFieldState(reference?.ip ?: ""),
    val port: FormFieldState<Int> = FormFieldState(reference?.port ?: 1883),
    val authenticated: FormFieldState<Boolean> = FormFieldState(if (reference == null) false else reference.user != null),
    val authUser: FormFieldState<String> = FormFieldState(reference?.user ?: ""),
    val authPassword: FormFieldState<String> = FormFieldState(reference?.password ?: ""),
    val connectionType: FormFieldState<ConnectionType> = FormFieldState(
        reference?.connectionType ?: ConnectionType.TCP
    ),
    val clientId: FormFieldState<String> = FormFieldState(
        reference?.clientId ?: UUID.randomUUID().toString().take(6)
    ),
    val keepAlive: FormFieldState<Int> = FormFieldState(reference?.keepAliveInterval ?: 60),
    val reconnectRetries: FormFieldState<Int> = FormFieldState(reference?.reconnectAttempts ?: 3)
) : EntityManagingFormState<BrokerEntity>() {
    override fun valid(): Boolean = listOf(name, host, port, authenticated, authUser, authPassword, connectionType, clientId, keepAlive, reconnectRetries).all { it.errorMsg == null }
}

data class NotificationPermissionState(
    val granted: Boolean = false,
    val showRequestDialog: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrokersScreen(vm: BrokerViewModel, navController: NavHostController) {
    val manageBrokerState by vm.manageBrokerState.collectAsStateWithLifecycle()
    val connections by vm.connections.collectAsStateWithLifecycle()

    DisposableEffect(Unit) { onDispose { if (manageBrokerState.visible) vm.onEvent(BrokerEvent.ToggleManageForm(null)) } }
    if (manageBrokerState.visible) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxWidth(),
            sheetState = rememberModalBottomSheetState(confirmValueChange = { it != SheetValue.Hidden }, skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            onDismissRequest = { vm.onEvent(BrokerEvent.ToggleManageForm(null)) }
        ) {
            BrokerManage(manageBrokerState) { vm.onEvent(it) }
        }
    }
    Column(Modifier.fillMaxWidth()) {
        SettingsTopBar(navController, "Brokers Management") {
            button(onClick = { /* TODO: Import/Export */ }) {
                Icon(modifier = Modifier.size(24.dp), imageVector = Icons.Default.ImportExport, contentDescription = null)
            }
            button(onClick = { vm.onEvent(BrokerEvent.ToggleManageForm(null)) }) {
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
fun BrokerContainer(vm: BrokerViewModel, connection: MQTTConnection) {
    val context = LocalContext.current
    val permissionManager by remember { mutableStateOf(PermissionManager(context)) }
    val notificationPermissionState by vm.notificationPermissionState.collectAsStateWithLifecycle()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                vm.onEvent(BrokerEvent.DeleteBroker(connection.broker))
                true
            } else false
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.85f }
    )
    val expanded = remember { mutableStateOf(false) }

    if (notificationPermissionState.showRequestDialog) {
        PermissionRequestDialog(
            "Notifications",
            onDismiss = { vm.onEvent(BrokerEvent.ToggleNotificationPermissionRequestDialog) },
            onGoToSettings = {
                vm.onEvent(BrokerEvent.ToggleNotificationPermissionRequestDialog)
                permissionManager.openAppSettings()
            }
        )
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vm.onEvent(BrokerEvent.NotificationPermissionResult(permissionManager.isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)))
        }
    }
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
                BrokerHead(connection, expanded) { vm.onEvent(it) }
                if (expanded.value) BrokerBody(connection)
            }
        }
    }
}

@Composable
fun BrokerHead(
    connection: MQTTConnection,
    expanded: MutableState<Boolean>,
    onEvent: (BrokerEvent) -> Unit
) {
    var passwordRevealed by remember { mutableStateOf(false) }
    val displayedPassword = if (connection.broker.user == null) "Unauthenticated" else {
        val pass = if (passwordRevealed) connection.broker.password!! else "*".repeat(connection.broker.password!!.length)
        "${connection.broker.user}/$pass"
    }
    val isConnected = connection.state == MQTTConnectionState.CONNECTED

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
                "${if (connection.broker.connectionType == ConnectionType.TCP) "tcp://" else "ssl://"}${connection.broker.ip}:${connection.broker.port}",
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
                onClick = { onEvent(BrokerEvent.ToggleConnection(connection)) }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = if (isConnected) MaterialTheme.colorScheme.primary else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (connection.state) {
                        MQTTConnectionState.DISCONNECTED -> Icons.Filled.WifiTetheringOff
                        MQTTConnectionState.DISCONNECTED_FAILED -> Icons.Filled.WifiTetheringError
                        MQTTConnectionState.CONNECTED -> Icons.Filled.WifiTethering
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
                onClick = { onEvent(BrokerEvent.ToggleManageForm(connection.broker)) })
            ItemAction(
                if (expanded.value) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                onClick = { expanded.value = !expanded.value })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrokerBody(connection: MQTTConnection) {
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
        ItemProperty("ClientId", connection.broker.clientId, Icons.Filled.Tag)
        ItemProperty(
            "KeepAlive",
            "${connection.broker.keepAliveInterval}s",
            Icons.Filled.MonitorHeart
        )
        ItemProperty(
            "MaxReconnects",
            "${connection.broker.reconnectAttempts}",
            Icons.Filled.Refresh
        )
    }
}

@Composable
fun BrokerManage(state: ManageBrokerState, onEvent: (BrokerEvent) -> Unit) {
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
                        onValueChange = { onEvent(BrokerEvent.NameChanged(it)) },
                        label = { Text("Name") },
                        isError = state.name.errorMsg != null,
                        supportingText = { if (state.name.errorMsg != null) Text(state.name.errorMsg) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.host.value,
                            onValueChange = { onEvent(BrokerEvent.HostChanged(it)) },
                            label = { Text("IP Address/Domain") },
                            isError = state.host.errorMsg != null,
                            supportingText = { if (state.host.errorMsg != null) Text(state.host.errorMsg) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.port.value.toString(),
                            onValueChange = { onEvent(BrokerEvent.PortChanged(it)) },
                            label = { Text("Port") },
                            isError = state.port.errorMsg != null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                    BrokerEvent.AuthenticationSettingsChanged(
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
                                    BrokerEvent.AuthenticationSettingsChanged(
                                        authenticated = state.authenticated.value,
                                        user = it,
                                        password = state.authPassword.value
                                    )
                                )
                            },
                            label = { Text("Username") },
                            isError = state.authUser.errorMsg != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = state.authPassword.value,
                            onValueChange = {
                                onEvent(
                                    BrokerEvent.AuthenticationSettingsChanged(
                                        authenticated = state.authenticated.value,
                                        user = state.authUser.value,
                                        password = it
                                    )
                                )
                            },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item {
                FormIsland(title = "Connection Settings") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = state.connectionType.value == ConnectionType.TCP,
                            onClick = { onEvent(BrokerEvent.ConnectionTypeChanged(ConnectionType.TCP)) }
                        )
                        Text("TCP")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(
                            selected = state.connectionType.value == ConnectionType.SSL,
                            onClick = { onEvent(BrokerEvent.ConnectionTypeChanged(ConnectionType.SSL)) }
                        )
                        Text("SSL/TLS")
                    }
                    OutlinedTextField(
                        value = state.clientId.value,
                        onValueChange = { onEvent(BrokerEvent.ClientIdChanged(it)) },
                        label = { Text("Client ID") },
                        isError = state.clientId.errorMsg != null,
                        supportingText = { if (state.clientId.errorMsg != null) Text(state.clientId.errorMsg) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.keepAlive.value.toString(),
                            onValueChange = { onEvent(BrokerEvent.KeepAliveChanged(it)) },
                            label = { Text("Keep Alive") },
                            isError = state.keepAlive.errorMsg != null,
                            supportingText = { if (state.keepAlive.errorMsg != null) Text(state.keepAlive.errorMsg) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.reconnectRetries.value.toString(),
                            onValueChange = { onEvent(BrokerEvent.ReconnectRetriesChanged(it)) },
                            label = { Text("Retries") },
                            isError = state.reconnectRetries.errorMsg != null,
                            supportingText = {
                                if (state.reconnectRetries.errorMsg != null) Text(
                                    state.reconnectRetries.errorMsg
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
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
                onClick = { onEvent(BrokerEvent.ToggleManageForm(null)) }) { Text("Cancel") }
            Button(
                modifier = Modifier.weight(1f),
                enabled = state.valid(),
                onClick = { onEvent(BrokerEvent.BrokerSaved) }
            ) {
                Text("Save")
            }
        }
    }
}
