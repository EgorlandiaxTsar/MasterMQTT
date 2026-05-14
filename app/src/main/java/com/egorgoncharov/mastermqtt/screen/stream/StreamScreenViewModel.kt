package com.egorgoncharov.mastermqtt.screen.stream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.egorgoncharov.mastermqtt.manager.NotificationManager
import com.egorgoncharov.mastermqtt.manager.mqtt.MqttManager
import com.egorgoncharov.mastermqtt.model.dao.BrokerDao
import com.egorgoncharov.mastermqtt.model.dao.MessageDao
import com.egorgoncharov.mastermqtt.model.dao.SettingsProfileDao
import com.egorgoncharov.mastermqtt.model.dao.TopicDao
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import com.egorgoncharov.mastermqtt.ui.components.ConfirmationWindowState
import com.egorgoncharov.mastermqtt.ui.components.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class StreamScreenViewModel(
    brokerDao: BrokerDao,
    private val topicDao: TopicDao,
    private val messageDao: MessageDao,
    settingsProfileDao: SettingsProfileDao,
    mqttManager: MqttManager,
    private val notificationManager: NotificationManager
) : ViewModel() {
    companion object {
        fun Factory(
            brokerDao: BrokerDao,
            topicDao: TopicDao,
            messageDao: MessageDao,
            settingsProfileDao: SettingsProfileDao,
            mqttManager: MqttManager,
            notificationManager: NotificationManager
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { StreamScreenViewModel(brokerDao, topicDao, messageDao, settingsProfileDao, mqttManager, notificationManager) }
            }
    }

    private val _streamMessagesFilterState = MutableStateFlow(StreamMessagesFilterState())
    private val _streamClearDialogState = MutableStateFlow(ConfirmationWindowState())
    private val _streamChatState = MutableStateFlow(StreamChatState())
    private var readTrackingJob: Job? = null
    private var deepLinkBound = false
    private var brokersViewBound = false

    val brokers = brokerDao.streamBrokers()
    val topics = topicDao.streamTopics()
    val messages = messageDao.streamMessages()
    val streamFilterState = _streamMessagesFilterState.asStateFlow()
    val streamClearDialogState = _streamClearDialogState.asStateFlow()
    val streamChatState = _streamChatState.asStateFlow()
    val mainSettingProfile = settingsProfileDao.streamMainSettingsProfile()
    val connections = mqttManager.clientsFlow

    fun onEvent(event: StreamScreenEvent) {
        when (event) {
            is StreamScreenEvent.MinDatetimeFilterChanged -> handleMinDatetimeFilterChange(event.min)
            is StreamScreenEvent.MaxDatetimeFilterChanged -> handleMaxDatetimeFilterChange(event.max)
            is StreamScreenEvent.TextSearchFilterChanged -> handleTextSearchFilterChange(event.query)
            is StreamScreenEvent.TimeRangeMinDateChanged -> handleTimeRangeMinDateChange(event.minDate)
            is StreamScreenEvent.TimeRangeMaxDateChanged -> handleTimeRangeMaxDateChange(event.maxDate)
            is StreamScreenEvent.TimeRangeStartQuarterChanged -> handleTimeRangeStartQuarterChange(event.startQuarter)
            is StreamScreenEvent.TimeRangeEndQuarterChanged -> handleTimeRangeEndQuarterChange(event.endQuarter)
            is StreamScreenEvent.SelectedStreamChanged -> selectStream(event.streamSource)
            is StreamScreenEvent.ToggleStreamDisplayOriginalMessageOption -> toggleStreamDisplayOriginalMessageOption()
            is StreamScreenEvent.ToggleStreamClearDialog -> toggleStreamClearDialog()
            is StreamScreenEvent.StreamCleared -> clearStream()
            is StreamScreenEvent.DeepLinkBoundChanged -> handleDeepLinkBoundChange(event.deepLinkBound)
            is StreamScreenEvent.BrokersViewBoundChanged -> handleBrokersViewBoundChange(event.brokersViewBound)
        }
    }

    fun isDeepLinkBound() = deepLinkBound

    fun isBrokersViewBound() = brokersViewBound

    private fun handleMinDatetimeFilterChange(min: Long?) = _streamMessagesFilterState.update(
        { it.minDatetime },
        min,
        { null }
    ) { copy(minDatetime = it) }

    private fun handleMaxDatetimeFilterChange(max: Long?) = _streamMessagesFilterState.update(
        { it.maxDatetime },
        max,
        { null }
    ) { copy(maxDatetime = it) }

    private fun handleTextSearchFilterChange(query: String?) = _streamMessagesFilterState.update(
        { it.query },
        query,
        { null }
    ) { copy(query = it) }

    private fun handleTimeRangeMinDateChange(minDate: LocalDate?) = _streamMessagesFilterState.update(
        { it.timeRangeMinDate },
        minDate,
        { null }
    ) { copy(timeRangeMinDate = it) }

    private fun handleTimeRangeMaxDateChange(maxDate: LocalDate?) = _streamMessagesFilterState.update(
        { it.timeRangeMaxDate },
        maxDate,
        { null }
    ) { copy(timeRangeMaxDate = it) }

    private fun handleTimeRangeStartQuarterChange(startQuarter: Int) = _streamMessagesFilterState.update(
        { it.timeRangeStartQuarter },
        startQuarter,
        { null }
    ) { copy(timeRangeStartQuarter = it) }

    private fun handleTimeRangeEndQuarterChange(endQuarter: Int) = _streamMessagesFilterState.update(
        { it.timeRangeEndQuarter },
        endQuarter,
        { null }
    ) { copy(timeRangeEndQuarter = it) }

    private fun selectStream(streamSource: TopicEntity?) {
        _streamChatState.update { it.copy(selected = streamSource) }
        readTrackingJob?.cancel()
        if (streamSource != null) {
            notificationManager.dismissTopicNotifications(streamSource.id)
            readTrackingJob = viewModelScope.launch {
                messages.collect { allMessages ->
                    val latestMessage = allMessages
                        .filter { it.topicId == streamSource.id }
                        .maxByOrNull { it.date }
                    latestMessage?.let {
                        delay(1000)
                        streamChatState.value.selected?.let { source ->
                            topicDao.save(source.copy(lastOpened = it.date + 1))
                        }
                    }
                }
            }
        } else {
            _streamMessagesFilterState.update { StreamMessagesFilterState() }
        }
    }

    private fun toggleStreamDisplayOriginalMessageOption() = _streamChatState.update { it.copy(showProcessedContent = !it.showProcessedContent) }

    private fun toggleStreamClearDialog(): Unit = _streamClearDialogState.update {
        ConfirmationWindowState(
            title = "Are you sure?",
            message = "This action will erase completely this topic messages. Deleting is irreversible",
            onConfirm = {
                clearStream()
                toggleStreamClearDialog()
            },
            onDecline = { toggleStreamClearDialog() },
            visible = !it.visible
        )
    }

    private fun clearStream() {
        viewModelScope.launch { messageDao.delete(messageDao.findAll().filter { it.topicId == streamChatState.value.selected?.id }) }
    }

    private fun handleDeepLinkBoundChange(bounded: Boolean) {
        deepLinkBound = bounded
    }

    private fun handleBrokersViewBoundChange(bounded: Boolean) {
        brokersViewBound = bounded
    }
}
