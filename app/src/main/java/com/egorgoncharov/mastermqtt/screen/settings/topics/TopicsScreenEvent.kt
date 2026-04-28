package com.egorgoncharov.mastermqtt.screen.settings.topics

import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity

sealed interface TopicsScreenEvent {
    data class BrokerChanged(val broker: BrokerEntity) : TopicsScreenEvent
    data class NameChanged(val name: String) : TopicsScreenEvent
    data class TopicChanged(val topic: String) : TopicsScreenEvent
    data class QosChanged(val qos: String) : TopicsScreenEvent
    data class PayloadSettingChanged(
        val showPayload: Boolean,
        val binaryDecoding: Boolean,
        val payloadContent: String
    ) : TopicsScreenEvent

    data class HighPriorityChanged(val highPriority: Boolean) : TopicsScreenEvent
    data class IgnoreBedTimeChanged(val ignoreBedTime: Boolean) : TopicsScreenEvent
    data class TTSTextChanged(val ttsText: String) : TopicsScreenEvent
    data class NotificationSoundChanged(val notificationSound: String) : TopicsScreenEvent
    data class NotificationSoundLevelChanged(val notificationSoundLevel: Double) : TopicsScreenEvent
    object PlayNotificationSound : TopicsScreenEvent
    data class MessageAgeChanged(val messageAge: String) : TopicsScreenEvent
    object TopicSaved : TopicsScreenEvent

    data class ToggleManageForm(val reference: TopicEntity?) : TopicsScreenEvent
    data class ToggleTopic(val topic: TopicEntity) : TopicsScreenEvent
    data class ToggleTopicDeleteConfirmationDialog(val topic: TopicEntity, val visible: Boolean) : TopicsScreenEvent
    data class DeleteTopic(val topic: TopicEntity) : TopicsScreenEvent
}
