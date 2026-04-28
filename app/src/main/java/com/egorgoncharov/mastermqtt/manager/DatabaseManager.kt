package com.egorgoncharov.mastermqtt.manager

import android.content.Context
import androidx.room.Room
import com.egorgoncharov.mastermqtt.model.ApplicationDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

open class DatabaseManager(protected val context: Context) {
    @Volatile
    var db: ApplicationDatabase? = null

    private var cleanupJob: Job? = null

    fun connect() {
        db = Room.databaseBuilder(
            context.applicationContext,
            ApplicationDatabase::class.java,
            "MasterMQTT"
        ).fallbackToDestructiveMigration(true).build()
        startCleanupLoop()
    }

    suspend fun deleteDanglingReferences() {
        val database = db ?: return
        val allBrokers = database.brokerDao().findAll()
        val allTopics = database.topicDao().findAll()
        val allMessages = database.messageDao().findAll()
        val existingBrokerIds = allBrokers.map { it.id }.toSet()
        val validTopicIds = allTopics.filter { it.brokerId in existingBrokerIds }.map { it.id }
        database.topicDao().exclusiveDelete(validTopicIds)
        val validTopicIdSet = validTopicIds.toSet()
        val validMessageIds = allMessages.filter { it.topicId in validTopicIdSet }.map { it.id }
        database.messageDao().exclusiveDelete(validMessageIds)
    }

    suspend fun clear() {
        db?.messageDao()?.deleteAll()
        db?.topicDao()?.deleteAll()
        db?.brokerDao()?.deleteAll()
    }

    fun stopCleanup() {
        cleanupJob?.cancel()
    }

    private fun startCleanupLoop(scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        cleanupJob?.cancel()
        cleanupJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    db?.messageDao()?.deleteOldMessages()
                } catch (_: Exception) {
                }
                delay(10000)
            }
        }
    }
}