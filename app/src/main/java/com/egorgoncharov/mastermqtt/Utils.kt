package com.egorgoncharov.mastermqtt

import android.annotation.SuppressLint
import android.content.Context
import android.provider.OpenableColumns
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.core.net.toUri
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date

class Utils {
    companion object {
        val icons = mapOf(
            "Notifications" to Icons.Default.Notifications,
            "Warning" to Icons.Default.Warning,
            "Info" to Icons.Default.Info,
            "Light" to Icons.Default.Lightbulb,
            "Home" to Icons.Default.Home,
            "Sensors" to Icons.Default.Sensors,
            "Power" to Icons.Default.Power
        )

        inline fun <reified T : Enum<T>> fromEnum(o: T?): String? {
            return o?.name
        }

        inline fun <reified T : Enum<T>> toEnum(o: String?): T? {
            return o?.let { enumValueOf<T>(it) }
        }

        fun parseSoundPath(context: Context, path: String): String {
            val uri = path.toUri()
            val titleParam = uri.getQueryParameter("title")
            if (titleParam != null) return titleParam
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        return cursor.getString(nameIndex)
                    }
                }
            }
            return try {
                uri.path?.let { Paths.get(it).fileName.toString() } ?: "unknown_file"
            } catch (e: Exception) {
                path.substringAfterLast('/')
            }
        }

        fun abbreviateMiddle(text: String, maxLength: Int, delimiter: String = "..."): String {
            if (text.length <= maxLength) return text
            if (maxLength <= delimiter.length) return text.take(maxLength)
            val charsToKeep = maxLength - delimiter.length
            val startChars = charsToKeep / 2 + charsToKeep % 2
            val endChars = charsToKeep / 2
            val head = text.take(startChars)
            val tail = text.takeLast(endChars)
            return "$head$delimiter$tail"
        }

        @SuppressLint("SimpleDateFormat")
        fun formatDate(date: Long, format: String = "HH:mm:ss\ndd/MM"): String = SimpleDateFormat(format).format(Date(date))
    }
}