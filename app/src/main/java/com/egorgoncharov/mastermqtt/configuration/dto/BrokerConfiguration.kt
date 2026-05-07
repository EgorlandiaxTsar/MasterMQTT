package com.egorgoncharov.mastermqtt.configuration.dto

import com.egorgoncharov.mastermqtt.model.types.ConnectionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BrokerConfiguration(
    @SerialName("name") val name: String,
    @SerialName("url") val url: String, // ConnectionType + Host + Port
    @SerialName("authentication") val authentication: String?, // User + Password
    @SerialName("clientId") val clientId: String,
    @SerialName("alertWhenDisconnected") val alertWhenDisconnected: Boolean,
    @SerialName("keepAliveInterval") val keepAliveInterval: Int,
    @SerialName("cleanStart") val cleanStart: Boolean,
    @SerialName("reconnectAttempts") val reconnectAttempts: Int?,
    @SerialName("reconnectInterval") val reconnectInterval: Int,
    @SerialName("sessionExpiryInterval") val sessionExpiryInterval: Int?,
    @SerialName("connected") val connected: Boolean,
    @SerialName("topics") val topics: MutableList<TopicConfiguration>
) {
    fun address(): String = url.substringAfter("://")

    fun host(): String = address().substringBefore(":")

    fun port(): Int = address().substringAfter(":").toIntOrNull() ?: 0

    fun connectionType(): ConnectionType = if (url.startsWith("ssl://")) ConnectionType.SSL else ConnectionType.TCP

    fun authenticationUser(): String? = authentication?.substringBefore("${Char(0x1F)}")

    fun authenticationPassword(): String? = authentication?.substringAfter("${Char(0x1F)}")
}