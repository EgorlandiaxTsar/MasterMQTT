package com.egorgoncharov.mastermqtt.configuration.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TopicConfiguration(
    @SerialName("brokerAddress") val brokerAddress: String,
    @SerialName("name") val name: String,
    @SerialName("topic") val topic: String,
    @SerialName("qos") val qos: Int,
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("payloadContent") val payloadContent: String?,
    @SerialName("highPriority") val highPriority: Boolean,
    @SerialName("ignoreBedTime") val ignoreBedTime: Boolean,
    @SerialName("notificationSoundText") val notificationSoundText: String?,
    @SerialName("notificationSoundPath") val notificationSoundPath: String?,
    @SerialName("notificationSoundLevel") val notificationSoundLevel: Double?,
    @SerialName("messageAge") val messageAge: Int
)
