package com.egorgoncharov.mastermqtt

import android.app.Application
import com.egorgoncharov.mastermqtt.configuration.ConfigurationEntityConverter
import com.egorgoncharov.mastermqtt.manager.ConfigurationManager
import com.egorgoncharov.mastermqtt.manager.DatabaseManager
import com.egorgoncharov.mastermqtt.manager.DisconnectAlertManager
import com.egorgoncharov.mastermqtt.manager.NotificationManager
import com.egorgoncharov.mastermqtt.manager.SoundManager
import com.egorgoncharov.mastermqtt.manager.StorageManager
import com.egorgoncharov.mastermqtt.manager.mqtt.MqttManager

class MasterMqttApp : Application() {
    lateinit var databaseManager: DatabaseManager
    lateinit var notificationManager: NotificationManager
    lateinit var mqttManager: MqttManager
    lateinit var soundManager: SoundManager
    lateinit var alertManager: DisconnectAlertManager
    lateinit var storageManager: StorageManager
    lateinit var configurationEntityConverter: ConfigurationEntityConverter
    lateinit var configurationManager: ConfigurationManager

    override fun onCreate() {
        super.onCreate()
        instance = this
        databaseManager = DatabaseManager(this).apply { connect() }
        val db = databaseManager.db!!
        notificationManager = NotificationManager(this)
        soundManager = SoundManager(this, db.settingsProfilesDao())
        mqttManager = MqttManager(
            this,
            db.brokerDao(),
            db.topicDao(),
            db.messageDao(),
            notificationManager,
            soundManager
        )
        alertManager = DisconnectAlertManager(mqttManager, notificationManager, soundManager)
        storageManager = StorageManager(applicationContext)
        configurationEntityConverter = ConfigurationEntityConverter(db.brokerDao(), db.topicDao())
        configurationManager = ConfigurationManager(applicationContext, db.brokerDao(), db.topicDao(), db.messageDao(), storageManager, configurationEntityConverter)
    }

    companion object {
        lateinit var instance: MasterMqttApp
            private set
    }
}