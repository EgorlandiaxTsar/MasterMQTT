package com.egorgoncharov.mastermqtt.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.egorgoncharov.mastermqtt.manager.mqtt.MQTTManager

open class MainViewModel(
    val mqttManager: MQTTManager
) : ViewModel() {
    companion object {
        fun Factory(
            mqttManager: MQTTManager
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    MainViewModel(mqttManager)
                }
            }
    }
}