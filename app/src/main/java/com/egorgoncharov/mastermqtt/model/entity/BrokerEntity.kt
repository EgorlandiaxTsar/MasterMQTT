package com.egorgoncharov.mastermqtt.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.egorgoncharov.mastermqtt.model.types.ConnectionType

@Entity(tableName = "brokers")
data class BrokerEntity(
    @PrimaryKey val id: String,
    @ColumnInfo("name") val name: String,
    @ColumnInfo("host") val host: String,
    @ColumnInfo("port") val port: Int,
    @ColumnInfo("authUser") val authUser: String?,
    @ColumnInfo("authPassword") val authPassword: String?,
    @ColumnInfo("connectionType") val connectionType: ConnectionType,
    @ColumnInfo("alertWhenDisconnected") val alertWhenDisconnected: Boolean,
    @ColumnInfo("alertDisconnectsThreshold") val alertDisconnectsThreshold: Int?,
    @ColumnInfo("clientId") val clientId: String,
    @ColumnInfo("keepAliveInterval") val keepAliveInterval: Int,
    @ColumnInfo("cleanStart") val cleanStart: Boolean,
    @ColumnInfo("reconnectAttempts") val reconnectAttempts: Int?,
    @ColumnInfo("reconnectInterval") val reconnectInterval: Int,
    @ColumnInfo("sessionExpiryInterval") val sessionExpiryInterval: Int?,
    @ColumnInfo("connected") val connected: Boolean,
    @ColumnInfo("displayIndex") val displayIndex: Int
) {
    companion object {
        val DEFAULT = BrokerEntity(
            "",
            "",
            "0.0.0.0",
            1883,
            null,
            null,
            ConnectionType.TCP,
            false,
            null,
            "",
            0,
            true,
            null,
            0,
            null,
            false,
            0
        )
    }

    fun address(includeProtocol: Boolean = false) = (if (includeProtocol) "${connectionType.name.lowercase()}://" else "") + "$host:$port"
}
