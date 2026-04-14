package com.egorgoncharov.mastermqtt.configuration.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TopicConfiguration(
    @SerialName("name") val name: String,
    @SerialName("brokerAddress") val brokerAddress: String,
    @SerialName("topic") val topic: String,
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("notificationDisplaySettings") val notificationDisplaySettings: String?,
    @SerialName("notificationSoundPath") val notificationSoundPath: String?,
    @SerialName("notificationSoundText") val notificationSoundText: String?,
    @SerialName("highPriority") val highPriority: Boolean,
)
