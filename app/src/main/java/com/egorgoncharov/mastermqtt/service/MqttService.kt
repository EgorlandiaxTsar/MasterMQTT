package com.egorgoncharov.mastermqtt.service

import android.app.NotificationChannel
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.egorgoncharov.mastermqtt.MasterMqttApp

class MqttService : Service() {
    companion object {
        const val NOTIFICATION_ID = 1
        const val EXTRA_DISCONNECT_MESSAGE = "extra_disconnect_message"
        const val NOTIFICATION_CHANNEL_ID = "MasterMQTTServiceChannel"
    }

    private val app get() = application as MasterMqttApp

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        app.notificationManager.addChannel(NotificationChannel(NOTIFICATION_CHANNEL_ID, "Master MQTT Service Channel", android.app.NotificationManager.IMPORTANCE_LOW))
        val disconnectMessage = intent?.getStringExtra(EXTRA_DISCONNECT_MESSAGE)
        val notification = app.notificationManager.buildServiceNotification(disconnectMessage)
        startForeground(NOTIFICATION_ID, notification)
        if (!app.mqttManager.started()) app.mqttManager.start()
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onDestroy() {
        app.mqttManager.shutdown()
        super.onDestroy()
    }
}