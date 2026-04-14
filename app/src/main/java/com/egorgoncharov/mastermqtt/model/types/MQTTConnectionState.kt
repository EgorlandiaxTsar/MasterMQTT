package com.egorgoncharov.mastermqtt.model.types

enum class MQTTConnectionState {
    DISCONNECTED,
    DISCONNECTED_FAILED,
    INTERMEDIATE,
    CONNECTED
}