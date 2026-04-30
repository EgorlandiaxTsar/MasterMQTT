package com.egorgoncharov.mastermqtt.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.TypeConverter
import com.egorgoncharov.mastermqtt.Utils
import com.egorgoncharov.mastermqtt.model.types.ConnectionType
import com.egorgoncharov.mastermqtt.model.types.TTSLanguage
import com.egorgoncharov.mastermqtt.model.types.ThemeOption
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @TypeConverter
    fun connectionTypeToString(o: ConnectionType): String = Utils.fromEnum<ConnectionType>(o)!!

    @TypeConverter
    fun connectionTypeFromString(o: String): ConnectionType = Utils.toEnum<ConnectionType>(o)!!

    @TypeConverter
    fun stringListToString(o: String?): List<String> {
        return o?.let { json.decodeFromString(it) } ?: emptyList()
    }

    @TypeConverter
    fun fromColor(color: Color): Int = color.toArgb()

    @TypeConverter
    fun toColor(color: Int): Color = Color(color)

    @TypeConverter
    fun stringListFromString(o: List<String>?): String {
        return json.encodeToString(o ?: emptyList())
    }

    @TypeConverter
    fun ulongToLong(o: ULong?): Long {
        return o?.let { o.toLong() }!!
    }

    @TypeConverter
    fun ulongFromLong(o: Long?): ULong {
        return o?.toULong()!!;
    }

    @TypeConverter
    fun ttsLanguageToString(o: TTSLanguage): String = Utils.fromEnum<TTSLanguage>(o)!!

    @TypeConverter
    fun ttsLanguageFromString(o: String): TTSLanguage = Utils.toEnum<TTSLanguage>(o)!!

    @TypeConverter
    fun themeOptionToString(o: ThemeOption): String = Utils.fromEnum<ThemeOption>(o)!!

    @TypeConverter
    fun themeOptionFromString(o: String): ThemeOption = Utils.toEnum<ThemeOption>(o)!!
}