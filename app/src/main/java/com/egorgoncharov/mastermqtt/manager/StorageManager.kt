package com.egorgoncharov.mastermqtt.manager

import android.content.Context
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class StorageManager(private val context: Context) {
    private val soundsDir = File(context.filesDir, "sounds").apply { if (!exists()) mkdirs() }

    fun saveSound(path: String, contentProviderPath: Boolean = false): String? {
        val fileName = path.substringAfterLast("/")
        val destFile = File(soundsDir, fileName)
        try {
            val inputStream = if (contentProviderPath) {
                context.contentResolver.openInputStream(path.toUri())
            } else {
                File(path).inputStream()
            } ?: throw IOException("Failed to open input stream for path: $path")
            inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            val authority = "${context.packageName}.provider"
            return FileProvider.getUriForFile(context, authority, destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun deleteSound(path: String) {
        try {
            val uri = path.toUri()
            val fileToDelete = if (uri.scheme == "content") {
                File(soundsDir, uri.lastPathSegment ?: return)
            } else {
                File(path)
            }
            if (fileToDelete.exists() && fileToDelete.parentFile == soundsDir) {
                fileToDelete.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}