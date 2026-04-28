package com.egorgoncharov.mastermqtt.screen.settings.topics

import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import com.egorgoncharov.mastermqtt.model.types.ConnectionType
import com.egorgoncharov.mastermqtt.ui.components.EntityManagingFormState
import com.egorgoncharov.mastermqtt.ui.components.FormFieldState

data class ManageTopicsFormState(
    override val visible: Boolean = false,
    override val reference: TopicEntity? = null,
    val exists: Boolean = false,
    val broker: FormFieldState<BrokerEntity> = FormFieldState(
        BrokerEntity(
            id = reference?.brokerId ?: "",
            name = "Choose topic's broker",
            host = "0.0.0.0",
            port = 0,
            authUser = null,
            authPassword = null,
            connectionType = ConnectionType.TCP,
            clientId = "",
            keepAliveInterval = 0,
            cleanStart = true,
            reconnectAttempts = null,
            reconnectInterval = 0,
            sessionExpiryInterval = 0,
            connected = false,
            displayIndex = 0
        )
    ),
    val name: FormFieldState<String> = FormFieldState(reference?.name ?: ""),
    val topic: FormFieldState<String> = FormFieldState(reference?.topic ?: ""),
    val qos: FormFieldState<Int?> = FormFieldState(reference?.qos ?: 2),
    val showPayload: FormFieldState<Boolean> = FormFieldState(if (reference == null) false else reference.payloadContent != null),
    val binaryEncoding: FormFieldState<Boolean> = FormFieldState(
        reference?.payloadContent?.startsWith(
            "b@"
        ) ?: false
    ),
    val payloadContent: FormFieldState<String> = FormFieldState(reference?.payloadContent ?: ""),
    val highPriority: FormFieldState<Boolean> = FormFieldState(reference?.highPriority ?: false),
    val ignoreBedTime: FormFieldState<Boolean> = FormFieldState(reference?.ignoreBedTime ?: false),
    val ttsText: FormFieldState<String> = FormFieldState(reference?.notificationSoundText ?: ""),
    val notificationSound: FormFieldState<String> = FormFieldState(
        reference?.notificationSoundPath ?: ""
    ),
    val notificationSoundLevel: FormFieldState<Double> = FormFieldState(reference?.notificationSoundLevel ?: 1.0),
    val messageAge: FormFieldState<Int?> = FormFieldState(reference?.messageAge ?: 0)
) : EntityManagingFormState<TopicEntity>() {
    override fun valid(): Boolean = listOf(broker, name, topic, qos, showPayload, binaryEncoding, payloadContent, highPriority, ignoreBedTime, ttsText, notificationSound, notificationSoundLevel).all { it.errorMsg == null }
}
