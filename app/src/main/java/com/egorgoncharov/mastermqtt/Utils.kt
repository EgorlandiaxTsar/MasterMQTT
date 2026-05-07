package com.egorgoncharov.mastermqtt

import android.annotation.SuppressLint
import android.content.Context
import android.provider.OpenableColumns
import androidx.core.net.toUri
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date

class Utils {
    companion object {
        const val MAX_INPUT_NUMBER: Int = 1_000_000

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
            if (uri.toString().startsWith("content://") ?: false) {
                var name = URLDecoder.decode(uri.toString()).toUri().getQueryParameter("title")
                if (name == null) {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            name = cursor.getString(nameIndex)
                        }
                    }
                }
                return name ?: "unknown_file"
            }
            return try {
                uri.path?.let { Paths.get(it).fileName.toString() } ?: "unknown_file"
            } catch (_: Exception) {
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

        fun resolveJsonTemplates(text: String, jsonPayload: String): String {
            if (text == "={*}") return jsonPayload
            val regex = "=\\{(.*?)\\}".toRegex()
            return regex.replace(text) { matchResult ->
                val path = matchResult.groupValues[1]
                extractJsonValue(jsonPayload, path) ?: ""
            }
        }

        fun extractJsonValue(jsonString: String, path: String): String? {
            return try {
                val cleanPath = path.removePrefix("$").removePrefix(".")
                val keys = cleanPath.split(".")
                var currentElement: Any = JSONObject(jsonString)
                for (key in keys) {
                    currentElement = when (currentElement) {
                        is JSONObject -> currentElement.get(key)
                        is JSONArray -> currentElement.get(key.toInt())
                        else -> return null
                    }
                }
                currentElement.toString()
            } catch (_: Exception) {
                null
            }
        }
    }
}