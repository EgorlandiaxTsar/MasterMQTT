package com.egorgoncharov.mastermqtt.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settingsProfiles")
data class SettingsProfileEntity(
    @PrimaryKey val id: String,
    @ColumnInfo("defaultMessageAge") val defaultMessageAge: Int,
    @ColumnInfo("settingsSafetyButtonEnabled") val settingsSafetyButtonEnabled: Boolean
)
