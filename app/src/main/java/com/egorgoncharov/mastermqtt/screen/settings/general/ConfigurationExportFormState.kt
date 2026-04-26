package com.egorgoncharov.mastermqtt.screen.settings.general

import com.egorgoncharov.mastermqtt.BuildConfig
import com.egorgoncharov.mastermqtt.configuration.dto.AppConfiguration

data class ConfigurationExportFormState(
    val showExportForm: Boolean = false,
    val state: State = State.WAITING_CONFIRMATION,
    val configuration: AppConfiguration = AppConfiguration(BuildConfig.VERSION_NAME, System.currentTimeMillis())
) {

    enum class State {
        WAITING_CONFIRMATION,
        EXPORTING,
        EXPORT_FAILED,
        EXPORT_SUCCESS
    }
}
