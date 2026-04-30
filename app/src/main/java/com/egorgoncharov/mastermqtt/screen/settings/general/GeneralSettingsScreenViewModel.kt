package com.egorgoncharov.mastermqtt.screen.settings.general

import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.egorgoncharov.mastermqtt.configuration.ConfigurationEntityConverter
import com.egorgoncharov.mastermqtt.manager.ConfigurationManager
import com.egorgoncharov.mastermqtt.model.dao.BrokerDao
import com.egorgoncharov.mastermqtt.model.dao.SettingsProfileDao
import com.egorgoncharov.mastermqtt.model.dao.TopicDao
import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import com.egorgoncharov.mastermqtt.model.types.TTSLanguage
import com.egorgoncharov.mastermqtt.model.types.ThemeOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GeneralSettingsScreenViewModel(
    private val brokerDao: BrokerDao,
    topicDao: TopicDao,
    private val settingsProfileDao: SettingsProfileDao,
    private val configurationManager: ConfigurationManager,
    private val configurationConverter: ConfigurationEntityConverter
) : ViewModel() {
    companion object {
        fun Factory(brokerDao: BrokerDao, topicDao: TopicDao, settingsProfileDao: SettingsProfileDao, configurationManager: ConfigurationManager, configurationEntityConverter: ConfigurationEntityConverter): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { GeneralSettingsScreenViewModel(brokerDao, topicDao, settingsProfileDao, configurationManager, configurationEntityConverter) }
            }
    }

    private val _configurationImportFormState = MutableStateFlow(ConfigurationImportFormState())
    private val _configurationExportFormState = MutableStateFlow(ConfigurationExportFormState())

    val brokers = brokerDao.streamBrokers()
    val topics = topicDao.streamTopics()
    val mainSettingProfile = settingsProfileDao.streamMainSettingsProfile()
    val configurationImportFormState = _configurationImportFormState.asStateFlow()
    val configurationExportFormState = _configurationExportFormState.asStateFlow()

    init {
        viewModelScope.launch {
            val initialBrokers = brokers.first()
            val configurations = initialBrokers.map { entity -> configurationConverter.brokerFromEntityToConfiguration(entity) }
            _configurationExportFormState.update { state ->
                state.copy(
                    configuration = state.configuration.copy(
                        brokers = configurations.toMutableList()
                    )
                )
            }
        }
    }

    fun onEvent(event: GeneralSettingsScreenEvent) {
        when (event) {
            is GeneralSettingsScreenEvent.SafetyButtonEnabledChanged -> handleSafetyButtonEnabledChange(event.enabled)
            is GeneralSettingsScreenEvent.ThemeOptionChanged -> handleThemeOptionChange(event.theme)
            is GeneralSettingsScreenEvent.TTSLanguageChanged -> handleTTSLanguageChange(event.ttsLanguage)
            is GeneralSettingsScreenEvent.DefaultMessageAgeChanged -> handleDefaultMessageAgeChange(event.defaultMessageAge)
            is GeneralSettingsScreenEvent.ToggleConfigurationExportForm -> toggleConfigurationExportForm()
            is GeneralSettingsScreenEvent.ToggleExportBundleBroker -> toggleExportBundleBroker(event.broker)
            is GeneralSettingsScreenEvent.ToggleExportBundleTopic -> toggleExportBundleTopic(event.topic)
            is GeneralSettingsScreenEvent.ConfigurationExportStarted -> exportConfiguration()
            is GeneralSettingsScreenEvent.ToggleConfigurationImportForm -> toggleConfigurationImportForm()
            is GeneralSettingsScreenEvent.ConfigurationBundleLoadStarted -> loadConfigurationBundle(event.zipPath)
            is GeneralSettingsScreenEvent.ConfigurationBundleImportStarted -> importConfiguration()
        }
    }

    private fun handleSafetyButtonEnabledChange(enabled: Boolean) {
        viewModelScope.launch {
            val mainSettingsProfile = settingsProfileDao.getMainSettingsProfile() ?: return@launch
            settingsProfileDao.save(mainSettingsProfile.copy(settingsSafetyButtonEnabled = enabled))
        }
    }

    private fun handleThemeOptionChange(theme: ThemeOption) {
        viewModelScope.launch {
            val mainSettingsProfile = settingsProfileDao.getMainSettingsProfile() ?: return@launch
            settingsProfileDao.save(mainSettingsProfile.copy(theme = theme))
        }
    }

    private fun handleTTSLanguageChange(ttsLanguage: TTSLanguage) {
        viewModelScope.launch {
            val mainSettingsProfile = settingsProfileDao.getMainSettingsProfile() ?: return@launch
            settingsProfileDao.save(mainSettingsProfile.copy(ttsLanguage = ttsLanguage))
        }
    }

    private fun handleDefaultMessageAgeChange(defaultMessageAge: String) {
        if (!defaultMessageAge.isDigitsOnly() || defaultMessageAge.toInt() <= 0) return
        viewModelScope.launch {
            val mainSettingsProfile = settingsProfileDao.getMainSettingsProfile() ?: return@launch
            settingsProfileDao.save(mainSettingsProfile.copy(defaultMessageAge = defaultMessageAge.toInt()))
        }
    }

    private fun toggleConfigurationExportForm() = _configurationExportFormState.update { it.copy(showExportForm = !it.showExportForm) }

    private fun toggleExportBundleBroker(broker: BrokerEntity) {
        viewModelScope.launch {
            val configuration = _configurationExportFormState.value.configuration
            val brokerAddress = broker.address()
            val exists = configuration.brokers.any { it.address() == brokerAddress }
            val updatedBrokers = if (exists) configuration.brokers.filterNot { it.address() == brokerAddress } else configuration.brokers + configurationConverter.brokerFromEntityToConfiguration(broker)
            _configurationExportFormState.update { it.copy(configuration = configuration.copy(brokers = updatedBrokers.toMutableList())) }
        }
    }

    private fun toggleExportBundleTopic(topic: TopicEntity) {
        viewModelScope.launch {
            val broker = brokerDao.findById(topic.brokerId) ?: return@launch
            _configurationExportFormState.update { state ->
                val configuration = state.configuration
                val updatedBrokers = configuration.brokers.map { brokerConfig ->
                    if (brokerConfig.address() == broker.address()) {
                        val isIncluded = brokerConfig.topics.any { it.topic == topic.topic }
                        val newTopics = if (isIncluded) brokerConfig.topics.filterNot { it.topic == topic.topic } else brokerConfig.topics + configurationConverter.topicFromEntityToConfiguration(topic, broker)
                        brokerConfig.copy(topics = newTopics.toMutableList())
                    } else {
                        brokerConfig
                    }
                }
                state.copy(configuration = configuration.copy(brokers = updatedBrokers.toMutableList()))
            }
        }
    }

    private fun exportConfiguration() {
        viewModelScope.launch {
            runCatching {
                _configurationExportFormState.update { it.copy(state = ConfigurationExportFormState.State.EXPORTING) }
                return@runCatching configurationManager.write(configurationExportFormState.value.configuration)
            }.onSuccess { result ->
                _configurationExportFormState.update { it.copy(state = ConfigurationExportFormState.State.EXPORT_SUCCESS) }
                configurationManager.revealInExplorer(result)
                toggleConfigurationExportForm()
            }.onFailure { _configurationExportFormState.update { it.copy(state = ConfigurationExportFormState.State.EXPORT_FAILED) } }
        }
    }

    private fun toggleConfigurationImportForm() = _configurationImportFormState.update { ConfigurationImportFormState(showImportForm = !it.showImportForm) }

    private fun loadConfigurationBundle(zipPath: String) {
        viewModelScope.launch {
            runCatching {
                _configurationImportFormState.update { it.copy(state = ConfigurationImportFormState.State.BUNDLE_LOADING, zipFilePath = zipPath) }
                return@runCatching configurationManager.read(zipPath.toUri())
            }.onSuccess { result ->
                _configurationImportFormState.update { it.copy(state = ConfigurationImportFormState.State.WAITING_IMPORT_CONFIRMATION, configuration = result) }
            }.onFailure { _ -> _configurationImportFormState.update { it.copy(state = ConfigurationImportFormState.State.BUNDLE_LOAD_FAILED) } }
        }
    }

    private fun importConfiguration() {
        viewModelScope.launch {
            runCatching {
                _configurationImportFormState.update { it.copy(state = ConfigurationImportFormState.State.IMPORTING) }
                return@runCatching configurationManager.load(configurationImportFormState.value.zipFilePath!!.toUri(), configurationImportFormState.value.configuration!!, true)
            }.onSuccess {
                toggleConfigurationImportForm()
            }.onFailure { /* Shouldn't do failure normally */ }
        }
    }
}
