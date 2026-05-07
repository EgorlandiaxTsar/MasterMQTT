package com.egorgoncharov.mastermqtt.screen.settings.brokers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.egorgoncharov.mastermqtt.Utils
import com.egorgoncharov.mastermqtt.manager.mqtt.MqttConnection
import com.egorgoncharov.mastermqtt.manager.mqtt.MqttManager
import com.egorgoncharov.mastermqtt.model.dao.BrokerDao
import com.egorgoncharov.mastermqtt.model.dao.MessageDao
import com.egorgoncharov.mastermqtt.model.dao.TopicDao
import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.types.ConnectionType
import com.egorgoncharov.mastermqtt.model.types.MqttConnectionState
import com.egorgoncharov.mastermqtt.ui.components.ConfirmationWindowState
import com.egorgoncharov.mastermqtt.ui.components.hostRegex
import com.egorgoncharov.mastermqtt.ui.components.ipRegex
import com.egorgoncharov.mastermqtt.ui.components.nameRegex
import com.egorgoncharov.mastermqtt.ui.components.update
import com.egorgoncharov.mastermqtt.ui.components.validateNumericalInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class BrokersScreenViewModel(
    private val brokerDao: BrokerDao,
    private val topicDao: TopicDao,
    private val messageDao: MessageDao,
    private val mqttManager: MqttManager
) :
    ViewModel() {
    companion object {
        fun Factory(brokerDao: BrokerDao, topicDao: TopicDao, messageDao: MessageDao, mqttManager: MqttManager): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { BrokersScreenViewModel(brokerDao, topicDao, messageDao, mqttManager) }
            }
    }

    private val _manageBrokerFormState = MutableStateFlow(ManageBrokerFormState())
    private val _brokerDeleteConfirmationDialogState = MutableStateFlow(ConfirmationWindowState())
    private val _notificationPermissionState = MutableStateFlow(NotificationPermissionState())

    val connections = mqttManager.clientsFlow
    val manageBrokerFormState = _manageBrokerFormState.asStateFlow()
    val brokerDeleteConfirmationDialogState = _brokerDeleteConfirmationDialogState.asStateFlow()
    val notificationPermissionState = _notificationPermissionState.asStateFlow()

    fun onEvent(event: BrokersScreenEvent) {
        when (event) {
            is BrokersScreenEvent.NameChanged -> handleNameChange(event.name)
            is BrokersScreenEvent.HostChanged -> handleHostChange(event.host)
            is BrokersScreenEvent.PortChanged -> handlePortChange(event.port)
            is BrokersScreenEvent.AuthenticationSettingsChanged -> handleAuthenticationSettingsChange(
                event.authenticated,
                event.user,
                event.password
            )

            is BrokersScreenEvent.ConnectionTypeChanged -> handleConnectionTypeChange(event.connectionType)
            is BrokersScreenEvent.AlertWhenDisconnectedChanged -> handleAlertWhenDisconnectedChanged(event.alertWhenDisconnected)
            is BrokersScreenEvent.ClientIdChanged -> handleClientIdChange(event.clientId)
            is BrokersScreenEvent.KeepAliveIntervalChanged -> handleKeepAliveIntervalChange(event.keepAliveInterval)
            is BrokersScreenEvent.CleanStartChanged -> handleCleanStartChange(event.cleanStart)
            is BrokersScreenEvent.InfiniteReconnectsChanged -> handleInfiniteReconnectsChange(event.infiniteReconnects)
            is BrokersScreenEvent.ReconnectAttemptsChanged -> handleReconnectAttemptsChange(event.reconnectAttempts)
            is BrokersScreenEvent.ReconnectIntervalChanged -> handleReconnectIntervalChange(event.reconnectInterval)
            is BrokersScreenEvent.SessionExpiryIntervalChanged -> handleSessionExpiryIntervalChange(event.sessionExpiryInterval)
            is BrokersScreenEvent.BrokerSaved -> saveBroker()
            is BrokersScreenEvent.ToggleManageForm -> toggleManageForm(event.reference)
            is BrokersScreenEvent.NotificationPermissionResult -> notificationPermissionResult(event.isGranted)
            is BrokersScreenEvent.ToggleNotificationPermissionRequestDialog -> toggleNotificationPermissionRequestDialog()
            is BrokersScreenEvent.ToggleConnection -> toggleConnection(event.connection)
            is BrokersScreenEvent.ToggleBrokerDeleteConfirmationDialog -> toggleBrokerDeleteConfirmationDialog(event.broker, event.visible)
            is BrokersScreenEvent.DeleteBroker -> deleteBroker(event.broker)
        }
    }

    private fun handleNameChange(name: String) = _manageBrokerFormState.update(
        { it.name },
        name,
        { if (it.matches(nameRegex)) null else "Invalid name format" }
    ) { copy(name = it) }

    private fun handleHostChange(host: String) {
        _manageBrokerFormState.update(
            { it.host },
            host,
            { if (it.matches(hostRegex) || it.matches(ipRegex)) null else "Invalid domain or IPv4 format" }
        ) { copy(host = it) }
        checkExists()
    }

    private fun handlePortChange(port: String) {
        _manageBrokerFormState.update(
            { it.port },
            if (port.isBlank()) null else port.toIntOrNull(),
            { validateNumericalInput(port, true, 0.0, 65535.0) }
        ) { copy(port = it.copy(value = it.value?.coerceIn(0, 65535))) }
        checkExists()
    }

    private fun handleAuthenticationSettingsChange(authenticated: Boolean, user: String, password: String) {
        _manageBrokerFormState.update(
            { it.authenticated },
            authenticated,
            { null }
        ) { copy(authenticated = it) }
        _manageBrokerFormState.update(
            { it.authUser },
            user,
            { if (!(authenticated && user.isBlank())) null else "User is required" }
        ) { copy(authUser = it) }
        _manageBrokerFormState.update(
            { it.authPassword },
            password,
            { null }
        ) { copy(authPassword = it) }
    }

    private fun handleConnectionTypeChange(connectionType: ConnectionType) = _manageBrokerFormState.update(
        { it.connectionType },
        connectionType,
        { null }
    ) { copy(connectionType = it) }

    private fun handleAlertWhenDisconnectedChanged(alertWhenDisconnected: Boolean) = _manageBrokerFormState.update(
        { it.alertWhenDisconnected },
        alertWhenDisconnected,
        { null }
    ) { copy(alertWhenDisconnected = it) }

    private fun handleClientIdChange(clientId: String) = _manageBrokerFormState.update(
        { it.clientId },
        clientId,
        { if (clientId.isNotBlank()) null else "Client ID is required" }
    ) { copy(clientId = it) }

    private fun handleKeepAliveIntervalChange(keepAliveInterval: String) = _manageBrokerFormState.update(
        { it.keepAliveInterval },
        if (keepAliveInterval.isBlank()) null else keepAliveInterval.toIntOrNull(),
        { validateNumericalInput(keepAliveInterval, true, 1.0, Utils.MAX_INPUT_NUMBER.toDouble()) }
    ) { copy(keepAliveInterval = it.copy(value = it.value?.coerceIn(0, Utils.MAX_INPUT_NUMBER))) }

    private fun handleCleanStartChange(cleanStart: Boolean, updateLinkedFieldsErrors: Boolean = true) {
        _manageBrokerFormState.update(
            { it.cleanStart },
            cleanStart,
            { null }
        ) { copy(cleanStart = it) }
        if (updateLinkedFieldsErrors) updateManageFormErrors()
    }

    private fun handleInfiniteReconnectsChange(infiniteReconnects: Boolean, updateLinkedFieldsErrors: Boolean = true) {
        _manageBrokerFormState.update(
            { it.infiniteReconnects },
            infiniteReconnects,
            { null }
        ) { copy(infiniteReconnects = it) }
        if (updateLinkedFieldsErrors) updateManageFormErrors()
    }

    private fun handleReconnectAttemptsChange(reconnectRetries: String) = _manageBrokerFormState.update(
        { it.reconnectAttempts },
        if (reconnectRetries.isBlank()) null else reconnectRetries.toIntOrNull(),
        { validateNumericalInput(reconnectRetries, !manageBrokerFormState.value.infiniteReconnects.value, 0.0, Utils.MAX_INPUT_NUMBER.toDouble()) }
    ) { copy(reconnectAttempts = it.copy(value = it.value?.coerceIn(0, Utils.MAX_INPUT_NUMBER))) }

    private fun handleReconnectIntervalChange(reconnectInterval: String) = _manageBrokerFormState.update(
        { it.reconnectInterval },
        if (reconnectInterval.isBlank()) null else reconnectInterval.toIntOrNull(),
        { validateNumericalInput(reconnectInterval, true, 1.0, Utils.MAX_INPUT_NUMBER.toDouble()) }
    ) { copy(reconnectInterval = it.copy(value = it.value?.coerceIn(1, Utils.MAX_INPUT_NUMBER))) }

    private fun handleSessionExpiryIntervalChange(sessionExpiryInterval: String) = _manageBrokerFormState.update(
        { it.sessionExpiryInterval },
        if (sessionExpiryInterval.isBlank()) null else sessionExpiryInterval.toIntOrNull(),
        {
            validateNumericalInput(
                sessionExpiryInterval,
                !manageBrokerFormState.value.cleanStart.value,
                1.0,
                Utils.MAX_INPUT_NUMBER.toDouble()
            )
        }
    ) { copy(sessionExpiryInterval = it.copy(value = it.value?.coerceIn(1, Utils.MAX_INPUT_NUMBER))) }

    private fun saveBroker() {
        if (!manageBrokerFormState.value.valid()) return
        val state = manageBrokerFormState.value
        viewModelScope.launch {
            brokerDao.save(
                BrokerEntity(
                    id = state.reference?.id ?: UUID.randomUUID().toString(),
                    name = state.name.value,
                    host = state.host.value,
                    port = state.port.value!!,
                    authUser = if (state.authenticated.value) state.authUser.value else null,
                    authPassword = if (state.authenticated.value) state.authPassword.value else null,
                    connectionType = state.connectionType.value,
                    alertWhenDisconnected = state.alertWhenDisconnected.value,
                    clientId = state.clientId.value,
                    keepAliveInterval = state.keepAliveInterval.value!!,
                    cleanStart = state.cleanStart.value,
                    reconnectAttempts = if (state.infiniteReconnects.value) null else state.reconnectAttempts.value,
                    reconnectInterval = state.reconnectInterval.value!!,
                    sessionExpiryInterval = if (state.cleanStart.value) null else state.sessionExpiryInterval.value,
                    connected = false,
                    displayIndex = 0
                )
            )
        }
        toggleManageForm(null)
    }

    private fun toggleManageForm(reference: BrokerEntity?) {
        resetManageForm()
        _manageBrokerFormState.update {
            ManageBrokerFormState(
                visible = !it.visible,
                reference = reference
            )
        }
        updateManageFormErrors()
    }

    private fun resetManageForm() {
        _manageBrokerFormState.update { ManageBrokerFormState(visible = it.visible) }
    }

    private fun updateManageFormErrors() {
        handleNameChange(manageBrokerFormState.value.name.value)
        handleHostChange(manageBrokerFormState.value.host.value)
        handlePortChange(manageBrokerFormState.value.port.value.toString())
        handleAuthenticationSettingsChange(
            manageBrokerFormState.value.authenticated.value,
            manageBrokerFormState.value.authUser.value,
            manageBrokerFormState.value.authPassword.value
        )
        handleConnectionTypeChange(manageBrokerFormState.value.connectionType.value)
        handleClientIdChange(manageBrokerFormState.value.clientId.value)
        handleKeepAliveIntervalChange(manageBrokerFormState.value.keepAliveInterval.value.toString())
        handleCleanStartChange(manageBrokerFormState.value.cleanStart.value, false)
        handleInfiniteReconnectsChange(manageBrokerFormState.value.infiniteReconnects.value, false)
        handleReconnectAttemptsChange(manageBrokerFormState.value.reconnectAttempts.value.toString())
        handleReconnectIntervalChange(manageBrokerFormState.value.reconnectInterval.value.toString())
        handleSessionExpiryIntervalChange(manageBrokerFormState.value.sessionExpiryInterval.value.toString())
    }

    private fun notificationPermissionResult(isGranted: Boolean) {
        _notificationPermissionState.update { it.copy(granted = isGranted) }
    }

    private fun toggleNotificationPermissionRequestDialog() {
        _notificationPermissionState.update { it.copy(showRequestDialog = !it.showRequestDialog) }
    }

    private fun toggleConnection(connection: MqttConnection) {
        if (connection.state == MqttConnectionState.CONNECTED) mqttManager.disconnect(connection.broker)
        else mqttManager.connect(connection.broker)
    }

    private fun toggleBrokerDeleteConfirmationDialog(broker: BrokerEntity, visible: Boolean): Unit = _brokerDeleteConfirmationDialogState.update {
        ConfirmationWindowState(
            title = "Delete broker ${broker.name}?",
            message = "This will delete this broker and all it's associated topics and messages",
            onConfirm = {
                deleteBroker(broker)
                toggleBrokerDeleteConfirmationDialog(broker, false)
            },
            onDecline = { toggleBrokerDeleteConfirmationDialog(broker, false) },
            visible = visible
        )
    }

    private fun deleteBroker(broker: BrokerEntity) {
        viewModelScope.launch {
            brokerDao.delete(broker)
            topicDao.deleteByBroker(broker.id)
            messageDao.deleteByBroker(broker.id)
        }
    }

    private fun checkExists() {
        val port = manageBrokerFormState.value.port.value ?: return
        viewModelScope.launch {
            _manageBrokerFormState.update { it.copy(exists = brokerDao.existsByAddress(manageBrokerFormState.value.host.value, port)) }
        }
    }
}
