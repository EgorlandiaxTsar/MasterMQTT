package com.egorgoncharov.mastermqtt.screen.settings.brokers

data class NotificationPermissionState(
    val granted: Boolean = false,
    val showRequestDialog: Boolean = false
)
