package com.egorgoncharov.mastermqtt.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.egorgoncharov.mastermqtt.dto.db.ConnectionType

@Entity(tableName = "brokers")
data class BrokerEntity(
    @PrimaryKey val id: String,
    @ColumnInfo("name") val name: String,
    @ColumnInfo("clientId") val clientId: String,
    @ColumnInfo("connected") val connected: Boolean,
    @ColumnInfo("ip") val ip: String,
    @ColumnInfo("port") val port: Int,
    @ColumnInfo("user") val user: String?,
    @ColumnInfo("password") val password: String?,
    @ColumnInfo("connectionType") val connectionType: ConnectionType,
    @ColumnInfo("keepAliveInterval") val keepAliveInterval: Int,
    @ColumnInfo("reconnectAttempts") val reconnectAttempts: Int?,
    @ColumnInfo("displayIndex") val displayIndex: Int,
    @ColumnInfo("removed") val removed: Boolean
)
