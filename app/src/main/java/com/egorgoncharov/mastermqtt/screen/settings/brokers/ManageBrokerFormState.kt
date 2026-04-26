package com.egorgoncharov.mastermqtt.screen.settings.brokers

import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.types.ConnectionType
import com.egorgoncharov.mastermqtt.ui.components.EntityManagingFormState
import com.egorgoncharov.mastermqtt.ui.components.FormFieldState
import java.util.UUID

data class ManageBrokerFormState(
    override val visible: Boolean = false,
    override val reference: BrokerEntity? = null,
    val exists: Boolean = false,
    val name: FormFieldState<String> = FormFieldState(reference?.name ?: ""),
    val host: FormFieldState<String> = FormFieldState(reference?.host ?: ""),
    val port: FormFieldState<Int?> = FormFieldState(reference?.port ?: 1883),
    val authenticated: FormFieldState<Boolean> = FormFieldState(if (reference == null) false else reference.authUser != null),
    val authUser: FormFieldState<String> = FormFieldState(reference?.authUser ?: ""),
    val authPassword: FormFieldState<String> = FormFieldState(reference?.authPassword ?: ""),
    val connectionType: FormFieldState<ConnectionType> = FormFieldState(
        reference?.connectionType ?: ConnectionType.TCP
    ),
    val clientId: FormFieldState<String> = FormFieldState(
        reference?.clientId ?: UUID.randomUUID().toString().take(6)
    ),
    val keepAliveInterval: FormFieldState<Int?> = FormFieldState(reference?.keepAliveInterval ?: 60),
    val cleanStart: FormFieldState<Boolean> = FormFieldState(reference?.cleanStart ?: false),
    val infiniteReconnects: FormFieldState<Boolean> = FormFieldState(reference?.reconnectAttempts == null),
    val reconnectAttempts: FormFieldState<Int?> = FormFieldState(reference?.reconnectAttempts ?: 3),
    val reconnectInterval: FormFieldState<Int?> = FormFieldState(reference?.reconnectInterval ?: 10),
    val sessionExpiryInterval: FormFieldState<Int?> = FormFieldState(reference?.sessionExpiryInterval ?: 86400)
) : EntityManagingFormState<BrokerEntity>() {
    override fun valid(): Boolean = listOf(name, host, port, authenticated, authUser, authPassword, connectionType, clientId, keepAliveInterval, cleanStart, infiniteReconnects, reconnectAttempts, sessionExpiryInterval).all { it.errorMsg == null }
}
