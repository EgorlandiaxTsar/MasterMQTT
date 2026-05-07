package com.egorgoncharov.mastermqtt.screen.settings.topics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.egorgoncharov.mastermqtt.Utils
import com.egorgoncharov.mastermqtt.manager.SoundManager
import com.egorgoncharov.mastermqtt.manager.StorageManager
import com.egorgoncharov.mastermqtt.model.dao.BrokerDao
import com.egorgoncharov.mastermqtt.model.dao.MessageDao
import com.egorgoncharov.mastermqtt.model.dao.SettingsProfileDao
import com.egorgoncharov.mastermqtt.model.dao.TopicDao
import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import com.egorgoncharov.mastermqtt.ui.components.ConfirmationWindowState
import com.egorgoncharov.mastermqtt.ui.components.isValidMqttTopic
import com.egorgoncharov.mastermqtt.ui.components.jsonPathRegex
import com.egorgoncharov.mastermqtt.ui.components.nameRegex
import com.egorgoncharov.mastermqtt.ui.components.update
import com.egorgoncharov.mastermqtt.ui.components.validateNumericalInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class TopicsScreenViewModel(
    private val brokerDao: BrokerDao,
    private val topicDao: TopicDao,
    private val messageDao: MessageDao,
    private val settingsProfileDao: SettingsProfileDao,
    private val storageManager: StorageManager,
    private val soundManager: SoundManager
) :
    ViewModel() {
    companion object {
        fun Factory(
            brokerDao: BrokerDao,
            topicDao: TopicDao,
            messageDao: MessageDao,
            settingsProfileDao: SettingsProfileDao,
            storageManager: StorageManager,
            soundManager: SoundManager
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { TopicsScreenViewModel(brokerDao, topicDao, messageDao, settingsProfileDao, storageManager, soundManager) }
            }
    }

    private val _manageTopicsFormState = MutableStateFlow(ManageTopicsFormState())
    private val _topicDeleteConfirmationDialogState = MutableStateFlow(ConfirmationWindowState())

    val topics = topicDao.streamTopics()
    val brokers = brokerDao.streamBrokers()
    val manageTopicsFormState = _manageTopicsFormState.asStateFlow()
    val topicDeleteConfirmationDialogState = _topicDeleteConfirmationDialogState.asStateFlow()

    fun onEvent(event: TopicsScreenEvent) {
        when (event) {
            is TopicsScreenEvent.BrokerChanged -> handleBrokerChange(event.broker)
            is TopicsScreenEvent.NameChanged -> handleNameChange(event.name)
            is TopicsScreenEvent.TopicChanged -> handleTopicChange(event.topic)
            is TopicsScreenEvent.QosChanged -> handleQosChange(event.qos)
            is TopicsScreenEvent.PayloadSettingChanged -> handlePayloadSettingsChange(event.showPayload, event.binaryDecoding, event.payloadContent)
            is TopicsScreenEvent.HighPriorityChanged -> handleHighPriorityChange(event.highPriority)
            is TopicsScreenEvent.IgnoreBedTimeChanged -> handleIgnoreBedTimeChange(event.ignoreBedTime)
            is TopicsScreenEvent.TTSTextChanged -> handleTTSTextChange(event.ttsText)
            is TopicsScreenEvent.NotificationSoundChanged -> handleNotificationSoundChange(event.notificationSound)
            is TopicsScreenEvent.NotificationSoundLevelChanged -> handleNotificationSoundLevelChange(event.notificationSoundLevel)
            is TopicsScreenEvent.PlayNotificationSound -> playNotificationSound()
            is TopicsScreenEvent.MessageAgeChanged -> handleMessageAgeChange(event.messageAge)
            is TopicsScreenEvent.TopicSaved -> saveTopic()
            is TopicsScreenEvent.ToggleManageForm -> toggleManageForm(event.reference)
            is TopicsScreenEvent.ToggleTopic -> toggleTopic(event.topic)
            is TopicsScreenEvent.ToggleTopicDeleteConfirmationDialog -> toggleTopicDeleteConfirmationDialog(event.topic, event.visible)
            is TopicsScreenEvent.DeleteTopic -> deleteTopic(event.topic)
        }
    }

    private fun handleBrokerChange(broker: BrokerEntity) {
        _manageTopicsFormState.update(
            { it.broker },
            broker,
            { if (broker.id.isNotBlank()) null else "Broker is required" }
        ) { copy(broker = it) }
        checkExists()
    }

    private fun handleNameChange(name: String) = _manageTopicsFormState.update(
        { it.name },
        name,
        { if (name.matches(nameRegex)) null else "Invalid name format" }
    ) { copy(name = it) }

    private fun handleTopicChange(topic: String) {
        _manageTopicsFormState.update(
            { it.topic },
            topic,
            { if (isValidMqttTopic(topic)) null else "Invalid topic format" }
        ) { copy(topic = it) }
        checkExists()
    }

    private fun handleQosChange(qos: String) = _manageTopicsFormState.update(
        { it.qos },
        if (qos.isBlank()) null else qos.toIntOrNull(),
        { validateNumericalInput(qos, true, 0.0, 2.0) }
    ) { copy(qos = it.copy(value = it.value?.coerceIn(0, 2))) }

    private fun handlePayloadSettingsChange(
        showPayload: Boolean,
        binaryDecoding: Boolean,
        payloadContent: String
    ) {
        _manageTopicsFormState.update(
            { it.showPayload },
            showPayload,
            { null }
        ) { copy(showPayload = it) }
        _manageTopicsFormState.update(
            { it.binaryEncoding },
            binaryDecoding,
            { null }
        ) { copy(binaryEncoding = it) }
        _manageTopicsFormState.update(
            { it.payloadContent },
            payloadContent,
            {
                if (payloadContent.isEmpty()) null
                else {
                    if (payloadContent.split(",")
                            .find { path -> !path.matches(jsonPathRegex) } == null
                    ) null else "Invalid JSON paths sequence"
                }
            }
        ) { copy(payloadContent = it) }
    }

    private fun handleHighPriorityChange(highPriority: Boolean) = _manageTopicsFormState.update(
        { it.highPriority },
        highPriority,
        { null }
    ) { copy(highPriority = it) }

    private fun handleIgnoreBedTimeChange(ignoreBedTime: Boolean) = _manageTopicsFormState.update(
        { it.ignoreBedTime },
        ignoreBedTime,
        { null }
    ) { copy(ignoreBedTime = it) }

    private fun handleTTSTextChange(ttsText: String) = _manageTopicsFormState.update(
        { it.ttsText },
        ttsText,
        { if (ttsText.length <= 512) null else "TTS text too long" }
    ) { copy(ttsText = it) }

    private fun handleNotificationSoundChange(notificationSound: String) = _manageTopicsFormState.update(
            { it.notificationSound },
            notificationSound,
            { null }
        ) { copy(notificationSound = it) }

    private fun handleNotificationSoundLevelChange(notificationSoundLevel: Double) = _manageTopicsFormState.update(
        { it.notificationSoundLevel },
        notificationSoundLevel,
        { null }
    ) { copy(notificationSoundLevel = it) }

    private fun playNotificationSound() {
        try {
            if (manageTopicsFormState.value.notificationSound.value.isNotBlank()) {
                viewModelScope.launch {
                    soundManager.playSound(manageTopicsFormState.value.notificationSound.value, manageTopicsFormState.value.notificationSoundLevel.value, highPriority = true, bypassDnd = true)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun handleMessageAgeChange(messageAge: String) = _manageTopicsFormState.update(
        { it.messageAge },
        if (messageAge.isBlank()) null else messageAge.toIntOrNull(),
        { validateNumericalInput(messageAge, true, 0.0, Utils.MAX_INPUT_NUMBER.toDouble()) }
    ) { copy(messageAge = it.copy(value = it.value?.coerceIn(0, Utils.MAX_INPUT_NUMBER))) }

    private fun saveTopic() {
        if (!manageTopicsFormState.value.valid()) return
        val state = manageTopicsFormState.value
        viewModelScope.launch {
            var savedSoundPath: String? = null
            val soundPathChanged = state.reference?.notificationSoundPath != state.notificationSound.value
            if (soundPathChanged) {
                if (state.reference?.notificationSoundPath != null) storageManager.deleteSound(state.reference.notificationSoundPath)
                if (state.notificationSound.value.isNotBlank()) savedSoundPath = storageManager.saveSound(state.notificationSound.value, true)
            }
            if (soundPathChanged && (savedSoundPath == null && state.notificationSound.value.isNotBlank())) return@launch
            topicDao.save(
                TopicEntity(
                    id = state.reference?.id ?: UUID.randomUUID().toString(),
                    brokerId = state.broker.value.id,
                    name = state.name.value,
                    topic = state.topic.value,
                    qos = state.qos.value ?: 2,
                    enabled = state.reference?.enabled ?: false,
                    payloadContent = if (state.showPayload.value) "${if (state.binaryEncoding.value) "b@" else ""}${state.payloadContent.value}" else null,
                    highPriority = state.highPriority.value,
                    ignoreBedTime = state.ignoreBedTime.value,
                    notificationSoundText = state.ttsText.value.ifBlank { null },
                    notificationSoundPath = state.notificationSound.value.ifBlank { null },
                    notificationSoundLevel = if (state.notificationSound.value.isBlank()) null else state.notificationSoundLevel.value,
                    messageAge = state.messageAge.value!!,
                    displayIndex = 0,
                    lastOpened = System.currentTimeMillis()
                )
            )
        }
        toggleManageForm(null)
    }

    private fun toggleManageForm(reference: TopicEntity?) {
        resetManageForm()
        _manageTopicsFormState.update {
            ManageTopicsFormState(
                visible = !it.visible,
                reference = reference
            )
        }
        updateManageFormErrors()
        if (reference != null) {
            viewModelScope.launch {
                handleBrokerChange(
                    brokerDao.findById(reference.brokerId) ?: return@launch
                )
            }
        } else {
            viewModelScope.launch {
                handleMessageAgeChange(
                    settingsProfileDao.getMainSettingsProfile()?.defaultMessageAge?.toString() ?: ""
                )
            }
        }
    }

    private fun resetManageForm() {
        _manageTopicsFormState.update { ManageTopicsFormState(visible = it.visible) }
    }

    private fun updateManageFormErrors() {
        handleBrokerChange(manageTopicsFormState.value.broker.value)
        handleNameChange(manageTopicsFormState.value.name.value)
        handleTopicChange(manageTopicsFormState.value.topic.value)
        handleQosChange(manageTopicsFormState.value.qos.value.toString())
        handlePayloadSettingsChange(
            manageTopicsFormState.value.showPayload.value,
            manageTopicsFormState.value.binaryEncoding.value,
            manageTopicsFormState.value.payloadContent.value
        )
        handleHighPriorityChange(manageTopicsFormState.value.highPriority.value)
        handleIgnoreBedTimeChange(manageTopicsFormState.value.ignoreBedTime.value)
        handleTTSTextChange(manageTopicsFormState.value.ttsText.value)
        handleNotificationSoundChange(manageTopicsFormState.value.notificationSound.value)
        handleNotificationSoundLevelChange(manageTopicsFormState.value.notificationSoundLevel.value)
        handleMessageAgeChange(manageTopicsFormState.value.messageAge.value.toString())
    }

    private fun toggleTopic(topic: TopicEntity) {
        viewModelScope.launch { topicDao.save(topic.copy(enabled = !topic.enabled)) }
    }

    private fun toggleTopicDeleteConfirmationDialog(topic: TopicEntity, visible: Boolean): Unit = _topicDeleteConfirmationDialogState.update {
        ConfirmationWindowState(
            title = "Delete topic ${topic.name}?",
            message = "This will delete this topic and all it's associated messages",
            onConfirm = {
                deleteTopic(topic)
                toggleTopicDeleteConfirmationDialog(topic, false)
            },
            onDecline = { toggleTopicDeleteConfirmationDialog(topic, false) },
            visible = visible
        )
    }

    private fun deleteTopic(topic: TopicEntity) {
        viewModelScope.launch {
            if (topic.notificationSoundPath != null) storageManager.deleteSound(topic.notificationSoundPath)
            topicDao.delete(topic)
            messageDao.deleteByTopic(topic.id)
        }
    }

    private fun checkExists() {
        viewModelScope.launch {
            _manageTopicsFormState.update { it.copy(exists = topicDao.existsByTopic(manageTopicsFormState.value.broker.value.id, manageTopicsFormState.value.topic.value)) }
        }
    }
}
