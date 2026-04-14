package com.egorgoncharov.mastermqtt.configuration.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date

@Serializable
data class AppConfiguration(
    @SerialName("version") val version: String,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("date") val date: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").format(Date(timestamp)),
    @SerialName("brokers") val brokers: MutableList<BrokerConfiguration> = mutableListOf()
)