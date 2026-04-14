package com.egorgoncharov.mastermqtt.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.egorgoncharov.mastermqtt.configuration.ConfigurationEntityConverter
import com.egorgoncharov.mastermqtt.configuration.dto.AppConfiguration
import com.egorgoncharov.mastermqtt.model.dao.BrokerDao
import com.egorgoncharov.mastermqtt.model.dao.MessageDao
import com.egorgoncharov.mastermqtt.model.dao.TopicDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ConfigurationManager(
    private val context: Context,
    private val brokerDao: BrokerDao,
    private val topicDao: TopicDao,
    private val messageDao: MessageDao,
    private val configurationConverter: ConfigurationEntityConverter
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    suspend fun read(uri: Uri): AppConfiguration {
        return scope.async {
            val tempDir = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream -> unzip(inputStream, tempDir) } ?: throw FileNotFoundException("Could not open URI stream")
                val configFile = File(tempDir, "config.json")
                if (!configFile.exists()) throw FileNotFoundException("Config file missing in bundle")
                return@async json.decodeFromString<AppConfiguration>(configFile.readText())
            } finally {
                tempDir.deleteRecursively()
            }
        }.await()
    }

    suspend fun load(uri: Uri, configuration: AppConfiguration, overrideOnConflict: Boolean) {
        return scope.async {
            val tempDir = File(context.cacheDir, "load_temp_${System.currentTimeMillis()}")
            val soundsDir = File(context.filesDir, "sounds").apply { mkdirs() }
            tempDir.mkdirs()
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream -> unzip(inputStream, tempDir) }
                configuration.brokers.forEach { broker ->
                    val existingBroker = brokerDao.findByAddress(broker.address())
                    if (existingBroker != null && overrideOnConflict) {
                        messageDao.deleteByBroker(existingBroker.id)
                        topicDao.deleteByBroker(existingBroker.id)
                        brokerDao.delete(existingBroker)
                    }
                    val brokerId = UUID.randomUUID().toString()
                    brokerDao.save(configurationConverter.brokerFromConfigurationToEntity(brokerId, broker))
                    broker.topics.forEach { topic ->
                        var finalSoundPath = topic.notificationSoundPath
                        if (topic.notificationSoundPath?.startsWith("resources/") == true) {
                            val fileName = topic.notificationSoundPath.substringAfter("resources/")
                            val bundledFile = File(tempDir, topic.notificationSoundPath)
                            val destinationFile = File(soundsDir, fileName)
                            if (bundledFile.exists()) {
                                bundledFile.copyTo(destinationFile, overwrite = true)
                                finalSoundPath = destinationFile.absolutePath
                            }
                        }
                        topicDao.save(configurationConverter.topicFromConfigurationToEntity(brokerId = brokerId, topicConfiguration = topic.copy(notificationSoundPath = finalSoundPath)))
                    }
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }.await()
    }

    suspend fun write(configuration: AppConfiguration): File {
        return scope.async {
            val tempDir = File(context.cacheDir, "export_temp_${System.currentTimeMillis()}")
            val resourceDir = File(tempDir, "resources").apply { mkdirs() }
            val updatedBrokers = configuration.brokers.map { broker ->
                broker.copy(topics = broker.topics.map { topic ->
                    val uriString = topic.notificationSoundPath
                    if (!uriString.isNullOrEmpty()) {
                        try {
                            val uri = uriString.toUri()
                            val fileName = getFileName(uri) ?: "sound_${System.currentTimeMillis()}.mp3"
                            val destFile = File(resourceDir, fileName)
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                destFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            topic.copy(notificationSoundPath = "resources/$fileName")
                        } catch (e: Exception) {
                            android.util.Log.e("ConfigManager", "Failed to copy sound: $uriString", e)
                            topic
                        }
                    } else {
                        topic
                    }
                }.toMutableList())
            }.toMutableList()
            val updatedConfig = configuration.copy(brokers = updatedBrokers)
            File(tempDir, "config.json").writeText(json.encodeToString(updatedConfig))
            val internalZipFile = File(context.cacheDir, "configuration.mastermqtt")
            zip(tempDir, internalZipFile)
            tempDir.deleteRecursively()
            val publicDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            val publicZipFile = File(publicDir, "configuration.mastermqtt")
            try {
                internalZipFile.copyTo(publicZipFile, overwrite = true)
                internalZipFile.delete()
                return@async publicZipFile
            } catch (_: Exception) {
                return@async internalZipFile
            }
        }.await()
    }

    fun revealInExplorer(zipFile: File) {
        try {
            val folderToOpen = zipFile.parentFile?.canonicalFile ?: return

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                folderToOpen
            )

            val intent = Intent(Intent.createChooser(Intent(), "Open folder")).apply {
                action = Intent.ACTION_VIEW
                setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        } catch (_: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(fallbackIntent)
        }
    }

    private fun zip(sourceDir: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entry = ZipEntry(file.relativeTo(sourceDir).path)
                    zos.putNextEntry(entry)
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
    }

    private fun unzip(zipFile: File, destDir: File) {
        return unzip(FileInputStream(zipFile), destDir)
    }

    private fun unzip(inputStream: java.io.InputStream, destDir: File) {
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos -> zis.copyTo(fos) }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun getFileName(uri: android.net.Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        return uri.path?.substringAfterLast('/')
    }
}