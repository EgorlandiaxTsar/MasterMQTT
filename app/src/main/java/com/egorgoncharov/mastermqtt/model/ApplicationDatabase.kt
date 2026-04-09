package com.egorgoncharov.mastermqtt.model

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.egorgoncharov.mastermqtt.model.dao.BrokerDao
import com.egorgoncharov.mastermqtt.model.dao.MessageDao
import com.egorgoncharov.mastermqtt.model.dao.TopicDao
import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.entity.MessageEntity
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity

@Database(
    entities = [
        BrokerEntity::class,
        TopicEntity::class,
        MessageEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ApplicationDatabase : RoomDatabase() {
    abstract fun brokerDao(): BrokerDao

    abstract fun topicDao(): TopicDao

    abstract fun messageDao(): MessageDao
}