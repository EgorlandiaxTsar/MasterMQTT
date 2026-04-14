package com.egorgoncharov.mastermqtt.configuration.dto

import com.egorgoncharov.mastermqtt.model.types.ConnectionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BrokerConfiguration(
    @SerialName("name") val name: String,
    @SerialName("clientId") val clientId: String,
    @SerialName("url") val url: String, // ConnectionType + Host + Port
    @SerialName("authentication") val authentication: String?,
    @SerialName("connected") val connected: Boolean,
    @SerialName("keepAliveInterval") val keepAliveInterval: Int,
    @SerialName("reconnectAttempts") val reconnectAttempts: Int,
    @SerialName("topics") val topics: MutableList<TopicConfiguration>
) {
    fun address(): String = url.substringAfter("://")

    fun host(): String = address().substringBefore(":")

    fun port(): Int = address().substringAfter(":").toIntOrNull() ?: 0

    fun connectionType(): ConnectionType = if (url.startsWith("ssl://")) ConnectionType.SSL else ConnectionType.TCP

    fun authenticationUser(): String? = authentication?.substringBefore("<mastermqtt_splitpoint>")

    fun authenticationPassword(): String? = authentication?.substringAfter("<mastermqtt_splitpoint>")
}