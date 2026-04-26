package com.egorgoncharov.mastermqtt.manager

import android.Manifest
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
import com.egorgoncharov.mastermqtt.R
import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity

open class NotificationManager(protected val context: Context) {
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

    fun clearAllChannels() {
        notificationManager.notificationChannels.forEach { channel -> notificationManager.deleteNotificationChannel(channel.id) }
    }

    fun resetChannels() {
        clearAllChannels()
        initializeChannels()
    }
}