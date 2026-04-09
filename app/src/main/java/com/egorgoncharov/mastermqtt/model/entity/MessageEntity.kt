package com.egorgoncharov.mastermqtt.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class MessageEntity(
    @PrimaryKey val id: String,
    @ColumnInfo("topicId") val topicId: String,
    @ColumnInfo("title") val title: String,
    @ColumnInfo("content") val content: String,
    @ColumnInfo("original_content") val originalContent: String,
    @ColumnInfo("date") val date: Long
)