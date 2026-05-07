package com.egorgoncharov.mastermqtt.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.egorgoncharov.mastermqtt.model.types.TTSLanguage
import com.egorgoncharov.mastermqtt.model.types.ThemeOption

@Entity(tableName = "settingsProfiles")
data class SettingsProfileEntity(
    @PrimaryKey val id: String,
    @ColumnInfo("ttsLanguage") val ttsLanguage: TTSLanguage,
    @ColumnInfo("theme") val theme: ThemeOption,
    @ColumnInfo("defaultMessageAge") val defaultMessageAge: Int,
    @ColumnInfo("settingsSafetyButtonEnabled") val settingsSafetyButtonEnabled: Boolean,
    @ColumnInfo("showTopicRouteInStream") val showTopicRouteInStream: Boolean
) {
    companion object {
        val DUMMY: SettingsProfileEntity = SettingsProfileEntity("", TTSLanguage.EN, ThemeOption.SYSTEM, 0, settingsSafetyButtonEnabled = false, showTopicRouteInStream = false)
    }
}
