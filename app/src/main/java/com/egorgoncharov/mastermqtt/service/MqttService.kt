package com.egorgoncharov.mastermqtt.service

import android.app.NotificationChannel
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.egorgoncharov.mastermqtt.MainActivity
import com.egorgoncharov.mastermqtt.R
import com.egorgoncharov.mastermqtt.manager.DatabaseManager
import com.egorgoncharov.mastermqtt.manager.NotificationManager
import com.egorgoncharov.mastermqtt.manager.SoundManager
import com.egorgoncharov.mastermqtt.manager.mqtt.MqttManager

class MqttService : Service() {
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "MasterMQTTServiceChannel"
    }

    private lateinit var databaseManager: DatabaseManager
    private lateinit var mqttManager: MqttManager
    private lateinit var notificationManager: NotificationManager
    val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun service(): MqttService = this@MqttService

        fun manager(): MqttManager = mqttManager
        fun database(): DatabaseManager = databaseManager
    }

    override fun onCreate() {
        super.onCreate()
        databaseManager = DatabaseManager(applicationContext).apply { connect() }
        notificationManager = NotificationManager(applicationContext)
        mqttManager = MqttManager(
            applicationContext,
            databaseManager.db!!.brokerDao(),
            databaseManager.db!!.topicDao(),
            databaseManager.db!!.messageDao(),
            notificationManager,
            SoundManager(applicationContext, databaseManager.db!!.settingsProfilesDao())
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        notificationManager.addChannel(NotificationChannel(NOTIFICATION_CHANNEL_ID, "Master MQTT Service Channel", android.app.NotificationManager.IMPORTANCE_LOW))
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Monitoring brokers events")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
        if (!mqttManager.started()) mqttManager.start()
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder = binder

    override fun onDestroy() {
        mqttManager.shutdown()
        super.onDestroy()
    }
}