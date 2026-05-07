package com.egorgoncharov.mastermqtt.manager

import com.egorgoncharov.mastermqtt.R
import com.egorgoncharov.mastermqtt.manager.mqtt.MqttConnection
import com.egorgoncharov.mastermqtt.manager.mqtt.MqttManager
import com.egorgoncharov.mastermqtt.model.types.MqttConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DisconnectAlertManager(
    private val mqttManager: MqttManager,
    private val notificationManager: NotificationManager,
    private val soundManager: SoundManager
) {
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isDiscardedManually = false
    private var previouslyFailingIds = emptySet<String>()
    private var pingJob: Job? = null

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        scope.launch {
            mqttManager.clientsFlow.collectLatest { clientsMap ->
                val failingBrokers = clientsMap.values.filter { (it.state == MqttConnectionState.RECONNECTING || it.state == MqttConnectionState.DISCONNECTED_FAILED) && it.broker.alertWhenDisconnected }
                val currentFailingIds = failingBrokers.map { it.broker.id }.toSet()
                val newFailures = currentFailingIds - previouslyFailingIds
                if (newFailures.isNotEmpty()) isDiscardedManually = false
                if (failingBrokers.isNotEmpty()) {
                    if (!isDiscardedManually) triggerAlert(failingBrokers)
                } else {
                    if (previouslyFailingIds.isNotEmpty()) stopAlert(playSuccessSound = true)
                    isDiscardedManually = false
                }
                previouslyFailingIds = currentFailingIds
            }
        }
    }

    private fun triggerAlert(failing: List<MqttConnection>) {
        val description = failing.joinToString("\n") { "${it.broker.name}@${it.broker.host}:${it.broker.port}" }
        notificationManager.showDisconnectAlert(description)
        if (pingJob == null || !pingJob!!.isActive) {
            pingJob = scope.launch {
                while (isActive) {
                    soundManager.playSound(R.raw.disconnected_sound, highPriority = true, bypassDnd = true)
                    delay(1000)
                }
            }
        }
    }

    fun discardAlert() {
        isDiscardedManually = true
        stopAlert(playSuccessSound = false)
    }

    private fun stopAlert(playSuccessSound: Boolean) {
        pingJob?.cancel()
        pingJob = null
        notificationManager.dismissAlert()
        if (playSuccessSound) {
            scope.launch {
                soundManager.playSound(R.raw.reconnected_sound, highPriority = true, bypassDnd = true)
            }
        }
    }
}