package com.egorgoncharov.mastermqtt.screen.settings.brokers

import com.egorgoncharov.mastermqtt.manager.mqtt.MqttConnection
import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.types.ConnectionType

sealed interface BrokersScreenEvent {
    data class NameChanged(val name: String) : BrokersScreenEvent
    data class HostChanged(val host: String) : BrokersScreenEvent
    data class PortChanged(val port: String) : BrokersScreenEvent
    data class AuthenticationSettingsChanged(
        val authenticated: Boolean,
        val user: String,
        val password: String
    ) : BrokersScreenEvent

    data class ConnectionTypeChanged(val connectionType: ConnectionType) : BrokersScreenEvent
    data class ClientIdChanged(val clientId: String) : BrokersScreenEvent
    data class KeepAliveIntervalChanged(val keepAliveInterval: String) : BrokersScreenEvent
    data class CleanStartChanged(val cleanStart: Boolean) : BrokersScreenEvent
    data class InfiniteReconnectsChanged(val infiniteReconnects: Boolean) : BrokersScreenEvent
    data class ReconnectAttemptsChanged(val reconnectAttempts: String) : BrokersScreenEvent
    data class ReconnectIntervalChanged(val reconnectInterval: String) : BrokersScreenEvent
    data class SessionExpiryIntervalChanged(val sessionExpiryInterval: String) : BrokersScreenEvent
    object BrokerSaved : BrokersScreenEvent
    data class ToggleManageForm(val reference: BrokerEntity?) : BrokersScreenEvent

    data class NotificationPermissionResult(val isGranted: Boolean) : BrokersScreenEvent
    object ToggleNotificationPermissionRequestDialog : BrokersScreenEvent

    data class ToggleConnection(val connection: MqttConnection) : BrokersScreenEvent
    data class ToggleBrokerDeleteConfirmationDialog(val broker: BrokerEntity, val visible: Boolean) : BrokersScreenEvent
    data class DeleteBroker(val broker: BrokerEntity) : BrokersScreenEvent
}
