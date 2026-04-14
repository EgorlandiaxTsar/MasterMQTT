package com.egorgoncharov.mastermqtt.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.egorgoncharov.mastermqtt.manager.mqtt.MqttManager

open class MainViewModel(
    val mqttManager: MqttManager
) : ViewModel() {
    companion object {
        fun Factory(
            mqttManager: MqttManager
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    MainViewModel(mqttManager)
                }
            }
    }
}