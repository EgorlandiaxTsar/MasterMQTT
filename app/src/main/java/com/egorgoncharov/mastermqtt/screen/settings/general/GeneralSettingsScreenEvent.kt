package com.egorgoncharov.mastermqtt.screen.settings.general

import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import com.egorgoncharov.mastermqtt.model.types.TTSLanguage
import com.egorgoncharov.mastermqtt.model.types.ThemeOption

sealed interface GeneralSettingsScreenEvent {
    data class SafetyButtonEnabledChanged(val enabled: Boolean) : GeneralSettingsScreenEvent
    data class ShowTopicRouteInStreamChanged(val enabled: Boolean) : GeneralSettingsScreenEvent
    data class ThemeOptionChanged(val theme: ThemeOption) : GeneralSettingsScreenEvent
    data class TTSLanguageChanged(val ttsLanguage: TTSLanguage) : GeneralSettingsScreenEvent
    data class DefaultMessageAgeChanged(val defaultMessageAge: String) : GeneralSettingsScreenEvent

    object ToggleConfigurationExportForm : GeneralSettingsScreenEvent
    data class ToggleExportBundleBroker(val broker: BrokerEntity) : GeneralSettingsScreenEvent
    data class ToggleExportBundleTopic(val topic: TopicEntity) : GeneralSettingsScreenEvent

    object ConfigurationExportStarted : GeneralSettingsScreenEvent
    object ToggleConfigurationImportForm : GeneralSettingsScreenEvent
    data class ConfigurationBundleLoadStarted(val zipPath: String) : GeneralSettingsScreenEvent
    object ConfigurationBundleImportStarted : GeneralSettingsScreenEvent
}
