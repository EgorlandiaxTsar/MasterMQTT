package com.egorgoncharov.mastermqtt.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.egorgoncharov.mastermqtt.MasterMqttApp
import com.egorgoncharov.mastermqtt.manager.NotificationManager

class DisconnectAlertDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationManager.ACTION_DISCARD_ALERT) {
            MasterMqttApp.instance.alertManager.discardAlert()
        }
    }
}