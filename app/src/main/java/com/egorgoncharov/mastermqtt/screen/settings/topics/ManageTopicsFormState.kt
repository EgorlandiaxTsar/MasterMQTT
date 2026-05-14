package com.egorgoncharov.mastermqtt.screen.settings.topics

import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import com.egorgoncharov.mastermqtt.ui.components.EntityManagingFormState
import com.egorgoncharov.mastermqtt.ui.components.FormFieldState

data class ManageTopicsFormState(
    override val visible: Boolean = false,
    override val reference: TopicEntity? = null,
    val exists: Boolean = false,
    val broker: FormFieldState<BrokerEntity> = FormFieldState(BrokerEntity.DEFAULT),
    val name: FormFieldState<String> = FormFieldState(reference?.name ?: ""),
    val topic: FormFieldState<String> = FormFieldState(reference?.topic ?: ""),
    val qos: FormFieldState<Int?> = FormFieldState(reference?.qos ?: 2),
    val showPayload: FormFieldState<Boolean> = FormFieldState(if (reference == null) false else reference.payloadContent != null),
    val binaryEncoding: FormFieldState<Boolean> = FormFieldState(
        reference?.payloadContent?.startsWith(
            "b@"
        ) ?: false
    ),
    val showJsonKeys: FormFieldState<Boolean> = FormFieldState(reference?.showJsonKeys ?: true),
    val payloadContent: FormFieldState<String> = FormFieldState(reference?.payloadContent ?: ""),
    val highPriority: FormFieldState<Boolean> = FormFieldState(reference?.highPriority ?: false),
    val ignoreBedTime: FormFieldState<Boolean> = FormFieldState(reference?.ignoreBedTime ?: false),
    val ttsText: FormFieldState<String> = FormFieldState(if (reference != null) reference.notificationSoundText ?: "" else "={*}"),
    val notificationSound: FormFieldState<String> = FormFieldState(
        reference?.notificationSoundPath ?: ""
    ),
    val notificationSoundLevel: FormFieldState<Double> = FormFieldState(reference?.notificationSoundLevel ?: 1.0),
    val messageAge: FormFieldState<Int?> = FormFieldState(reference?.messageAge ?: 0)
) : EntityManagingFormState<TopicEntity>() {
    override fun valid(): Boolean = listOf(broker, name, topic, qos, showPayload, binaryEncoding, payloadContent, highPriority, ignoreBedTime, ttsText, notificationSound, notificationSoundLevel).all { it.errorMsg == null }
}
