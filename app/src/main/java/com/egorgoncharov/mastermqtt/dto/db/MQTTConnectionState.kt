package com.egorgoncharov.mastermqtt.dto.db

enum class MQTTConnectionState {
    DISCONNECTED,
    DISCONNECTED_FAILED,
    INTERMEDIATE,
    CONNECTED
}