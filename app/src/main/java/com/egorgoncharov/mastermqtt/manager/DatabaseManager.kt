package com.egorgoncharov.mastermqtt.manager

import android.content.Context
import androidx.room.Room
import com.egorgoncharov.mastermqtt.model.ApplicationDatabase

open class DatabaseManager(protected val context: Context) {
    @Volatile
    var db: ApplicationDatabase? = null

    fun connect() {
        db = Room.databaseBuilder(
            context.applicationContext,
            ApplicationDatabase::class.java,
            "MasterMQTT"
        ).fallbackToDestructiveMigration(true).build()
    }

    suspend fun cleanup() {
        // TODO: Cleanup dangling references
    }

    suspend fun clear() {
        db?.messageDao()?.deleteAll()
        db?.topicDao()?.deleteAll()
        db?.brokerDao()?.deleteAll()
    }
}