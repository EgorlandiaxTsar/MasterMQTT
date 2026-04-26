package com.egorgoncharov.mastermqtt.screen.settings.general

import com.egorgoncharov.mastermqtt.configuration.dto.AppConfiguration

data class ConfigurationImportFormState(
    val showImportForm: Boolean = false,
    val state: State = State.WAITING_BUNDLE_LOAD,
    val configuration: AppConfiguration? = null,
    val zipFilePath: String? = null
) {
    enum class State {
        WAITING_BUNDLE_LOAD,
        BUNDLE_LOADING,
        BUNDLE_LOAD_FAILED,
        WAITING_IMPORT_CONFIRMATION,
        IMPORTING
    }
}
