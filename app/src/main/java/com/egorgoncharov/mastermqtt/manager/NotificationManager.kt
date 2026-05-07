package com.egorgoncharov.mastermqtt.manager

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.egorgoncharov.mastermqtt.MainActivity
import com.egorgoncharov.mastermqtt.R
import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import com.egorgoncharov.mastermqtt.receiver.DisconnectAlertDismissReceiver
import com.egorgoncharov.mastermqtt.service.MqttService

open class NotificationManager(protected val context: Context) {
    companion object {
        const val ACTION_DISCARD_ALERT = "com.egorgoncharov.mastermqtt.DISCARD_ALERT"
    }

    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    private val channelMap = mapOf(
        "max" to "MasterMQTTAlertMax",
        "high" to "MasterMQTTAlertHigh",
        "low" to "MasterMQTTAlertLow"
    )

    init {
        initializeChannels()
    }

    fun addChannel(channel: NotificationChannel) {
        notificationManager.createNotificationChannel(channel)
    }

    fun removeChannel(id: String) {
        notificationManager.deleteNotificationChannel(id)
    }

    private fun initializeChannels() {
        val maxChannel = NotificationChannel(
            channelMap["max"],
            "Max Priority Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setBypassDnd(true)
        }
        val highChannel = NotificationChannel(
            channelMap["high"],
            "High Priority Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }
        val lowChannel = NotificationChannel(
            channelMap["low"],
            "Low Priority Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannels(listOf(maxChannel, highChannel, lowChannel))
    }

    fun show(broker: BrokerEntity, topic: TopicEntity, description: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val targetChannelId = if (topic.ignoreBedTime) channelMap["max"] else if (topic.highPriority) channelMap["high"] else channelMap["low"]
        val notificationId = System.currentTimeMillis().toInt()
        val intent = Intent(Intent.ACTION_VIEW, "mastermqtt://stream?topicId=${topic.id}".toUri()).apply { `package` = context.packageName }
        val pendingIntent = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(notificationId, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val builder = NotificationCompat.Builder(context, targetChannelId!!)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${if (topic.highPriority) "[!] " else ""}${broker.name}/${topic.name}")
            .setContentText(description)
            .setStyle(NotificationCompat.BigTextStyle())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        notificationManager.notify(notificationId, builder.build())
    }

    fun showDisconnectAlert(description: String) {
        val intent = Intent(context, MqttService::class.java).apply {
            putExtra(MqttService.EXTRA_DISCONNECT_MESSAGE, description)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun dismissAlert() {
        val intent = Intent(context, MqttService::class.java)
        context.startService(intent)
    }

    fun clearAllChannels() {
        notificationManager.notificationChannels.forEach { channel -> notificationManager.deleteNotificationChannel(channel.id) }
    }

    fun resetChannels() {
        clearAllChannels()
        initializeChannels()
    }

    fun buildServiceNotification(description: String? = null): Notification {
        return if (description == null) {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Builder(context, MqttService.NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Monitoring brokers events")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        } else {
            val discardIntent = Intent(context, DisconnectAlertDismissReceiver::class.java).apply { action = ACTION_DISCARD_ALERT }
            val discardPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                discardIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val clickIntent = Intent(Intent.ACTION_VIEW, "mastermqtt://stream?showBrokersView=true".toUri()).apply {
                `package` = context.packageName
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("ACTION_STOP_PINGING", true)
            }
            val clickPendingIntent = PendingIntent.getActivity(
                context,
                2,
                clickIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            NotificationCompat.Builder(context, channelMap["max"]!!)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Some brokers are disconnected")
                .setContentText(description)
                .setStyle(NotificationCompat.BigTextStyle().bigText(description))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(R.drawable.outline_music_off_24, "Discard", discardPendingIntent)
                .setContentIntent(clickPendingIntent)
                .setDeleteIntent(discardPendingIntent)
                .build()
        }
    }
}