package com.egorgoncharov.mastermqtt.model.entity

import androidx.compose.ui.graphics.Color
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey val id: String,
    @ColumnInfo("brokerId") val brokerId: String,
    @ColumnInfo("name") val name: String,
    @ColumnInfo("topic") val topic: String,
    @ColumnInfo("enabled") val enabled: Boolean,
    @ColumnInfo("payloadContent") val payloadContent: String?, // If null, no payload will be shown, if blank string, full payload will be shown, otherwise specify content path. Append b@ at the start, if binary decoding is necessary. Write multiple paths by separating with a comma ",".
    @ColumnInfo("notificationColor") val notificationColor: Color,
    @ColumnInfo("notificationIcon") val notificationIcon: String,
    @ColumnInfo("notificationSound") val notificationSoundPath: String?,
    @ColumnInfo("notificationSoundText") val notificationSoundText: String?,
    @ColumnInfo("highPriority") val highPriority: Boolean,
    @ColumnInfo("displayIndex") val displayIndex: Int,
    @ColumnInfo("last_opened") val lastOpened: Long,
    @ColumnInfo("removed") val removed: Boolean,
)
