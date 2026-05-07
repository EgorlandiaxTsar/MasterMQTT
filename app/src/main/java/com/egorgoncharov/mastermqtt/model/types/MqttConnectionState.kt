package com.egorgoncharov.mastermqtt.model.types

enum class MqttConnectionState {
    DISCONNECTED,
    DISCONNECTED_FAILED,
    INTERMEDIATE,
    RECONNECTING,
    CONNECTED
}