package com.egorgoncharov.mastermqtt.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.egorgoncharov.mastermqtt.Utils
import com.egorgoncharov.mastermqtt.model.dao.BrokerDao
import com.egorgoncharov.mastermqtt.model.dao.MessageDao
import com.egorgoncharov.mastermqtt.model.dao.TopicDao
import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import com.egorgoncharov.mastermqtt.model.types.ConnectionType
import com.egorgoncharov.mastermqtt.ui.components.AudioPicker
import com.egorgoncharov.mastermqtt.ui.components.Empty
import com.egorgoncharov.mastermqtt.ui.components.EntityManagingFormState
import com.egorgoncharov.mastermqtt.ui.components.Error
import com.egorgoncharov.mastermqtt.ui.components.FormFieldState
import com.egorgoncharov.mastermqtt.ui.components.FormIsland
import com.egorgoncharov.mastermqtt.ui.components.ItemAction
import com.egorgoncharov.mastermqtt.ui.components.ItemProperty
import com.egorgoncharov.mastermqtt.ui.components.SettingsTopBar
import com.egorgoncharov.mastermqtt.ui.components.jsonPathRegex
import com.egorgoncharov.mastermqtt.ui.components.nameRegex
import com.egorgoncharov.mastermqtt.ui.components.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class TopicViewModel(
    private val brokerDao: BrokerDao,
    private val topicDao: TopicDao,
    private val messageDao: MessageDao
) :
    ViewModel() {
    companion object {
        fun Factory(brokerDao: BrokerDao, topicDao: TopicDao, messageDao: MessageDao): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { TopicViewModel(brokerDao, topicDao, messageDao) }
            }
    }

    private val _manageTopicState = MutableStateFlow(ManageTopicState())

    val topics = topicDao.streamTopics()
    val brokers = brokerDao.streamBrokers()
    val manageTopicState = _manageTopicState.asStateFlow()

    fun onEvent(event: TopicEvent) {
        when (event) {
            is TopicEvent.BrokerChanged -> handleBrokerChange(event.broker)
            is TopicEvent.NameChanged -> handleNameChange(event.name)
            is TopicEvent.TopicChanged -> handleTopicChange(event.topic)
            is TopicEvent.PayloadSettingChanged -> handlePayloadSettingsChange(event.showPayload, event.binaryDecoding, event.jsonPaths)
            is TopicEvent.HighPriorityChanged -> handleHighPriorityChange(event.highPriority)
            is TopicEvent.TTSTextChanged -> handleTTSTextChanged(event.ttsText)
            is TopicEvent.NotificationSoundChanged -> handleNotificationSoundChanged(event.notificationSound)
            is TopicEvent.TopicSaved -> saveTopic()
            is TopicEvent.ToggleManageForm -> toggleManageForm(event.reference)
            is TopicEvent.ToggleTopic -> toggleTopic(event.topic)
            is TopicEvent.DeleteTopic -> deleteTopic(event.topic)
        }
    }

    private fun handleBrokerChange(broker: BrokerEntity) {
        _manageTopicState.update(
            { it.broker },
            broker,
            { if (broker.id.isNotBlank()) null else "Broker is required" }
        ) { copy(broker = it) }
        checkExists()
    }

    private fun handleNameChange(name: String) = _manageTopicState.update(
        { it.name },
        name,
        { if (name.matches(nameRegex)) null else "Invalid name format" }
    ) { copy(name = it) }

    private fun handleTopicChange(topic: String) {
        _manageTopicState.update(
            { it.topic },
            topic,
            { null /* TODO: Should probably add an MQTT topic check regex */ }
        ) { copy(topic = it) }
        checkExists()
    }

    private fun handlePayloadSettingsChange(
        showPayload: Boolean,
        binaryDecoding: Boolean,
        jsonPaths: String
    ) {
        _manageTopicState.update(
            { it.showPayload },
            showPayload,
            { null }
        ) { copy(showPayload = it) }
        _manageTopicState.update(
            { it.binaryEncoding },
            binaryDecoding,
            { null }
        ) { copy(binaryEncoding = it) }
        _manageTopicState.update(
            { it.jsonPaths },
            jsonPaths,
            {
                if (jsonPaths.isEmpty()) null
                else {
                    if (jsonPaths.split(",")
                            .find { path -> !path.matches(jsonPathRegex) } == null
                    ) null else "Invalid JSON paths sequence"
                }
            }
        ) { copy(jsonPaths = it) }
    }

    private fun handleHighPriorityChange(highPriority: Boolean) = _manageTopicState.update(
        { it.highPriority },
        highPriority,
        { null }
    ) { copy(highPriority = it) }

    private fun handleTTSTextChanged(ttsText: String) = _manageTopicState.update(
        { it.ttsText },
        ttsText,
        { if (ttsText.length <= 32) null else "TTS text too long" }
    ) { copy(ttsText = it) }

    private fun handleNotificationSoundChanged(notificationSound: String) =
        _manageTopicState.update(
            { it.notificationSound },
            notificationSound,
            { null }
        ) { copy(notificationSound = it) }

    private fun saveTopic() {
        if (!manageTopicState.value.valid()) return
        val state = manageTopicState.value
        viewModelScope.launch {
            topicDao.save(
                TopicEntity(
                    id = state.reference?.id ?: UUID.randomUUID().toString(),
                    brokerId = state.broker.value.id,
                    name = state.name.value,
                    topic = state.topic.value,
                    enabled = state.reference?.enabled ?: false,
                    payloadContent = if (state.showPayload.value) "${if (state.binaryEncoding.value) "b@" else ""}${state.jsonPaths.value}" else null,
                    notificationColor = Color.White,
                    notificationIcon = "",
                    notificationSoundPath = state.notificationSound.value,
                    notificationSoundText = state.ttsText.value,
                    highPriority = state.highPriority.value,
                    displayIndex = 0,
                    lastOpened = System.currentTimeMillis(),
                    removed = false,
                )
            )
        }
        toggleManageForm(null)
    }

    private fun toggleManageForm(reference: TopicEntity?) {
        resetManageForm()
        _manageTopicState.update {
            ManageTopicState(
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
        }
    }

    private fun resetManageForm() {
        _manageTopicState.update { ManageTopicState(visible = it.visible) }
    }

    private fun updateManageFormErrors() {
        handleBrokerChange(manageTopicState.value.broker.value)
        handleNameChange(manageTopicState.value.name.value)
        handleTopicChange(manageTopicState.value.topic.value)
        handlePayloadSettingsChange(
            manageTopicState.value.showPayload.value,
            manageTopicState.value.binaryEncoding.value,
            manageTopicState.value.jsonPaths.value
        )
        handleHighPriorityChange(manageTopicState.value.highPriority.value)
        handleTTSTextChanged(manageTopicState.value.ttsText.value)
        handleNotificationSoundChanged(manageTopicState.value.notificationSound.value)
    }

    private fun toggleTopic(topic: TopicEntity) {
        viewModelScope.launch { topicDao.save(topic.copy(enabled = !topic.enabled)) }
    }

    private fun deleteTopic(topic: TopicEntity) {
        viewModelScope.launch {
            topicDao.delete(topic)
            messageDao.deleteByTopic(topic.id)
        }
    }

    private fun checkExists() {
        viewModelScope.launch {
            _manageTopicState.update { it.copy(exists = topicDao.existsByTopic(manageTopicState.value.broker.value.id, manageTopicState.value.topic.value)) }
        }
    }
}

sealed interface TopicEvent {
    data class BrokerChanged(val broker: BrokerEntity) : TopicEvent
    data class NameChanged(val name: String) : TopicEvent
    data class TopicChanged(val topic: String) : TopicEvent
    data class PayloadSettingChanged(
        val showPayload: Boolean,
        val binaryDecoding: Boolean,
        val jsonPaths: String
    ) : TopicEvent

    data class HighPriorityChanged(val highPriority: Boolean) : TopicEvent
    data class TTSTextChanged(val ttsText: String) : TopicEvent
    data class NotificationSoundChanged(val notificationSound: String) : TopicEvent
    object TopicSaved : TopicEvent
    data class ToggleManageForm(val reference: TopicEntity?) : TopicEvent

    data class ToggleTopic(val topic: TopicEntity) : TopicEvent
    data class DeleteTopic(val topic: TopicEntity) : TopicEvent
}

data class ManageTopicState(
    override val visible: Boolean = false,
    override val reference: TopicEntity? = null,
    val exists: Boolean = false,
    val broker: FormFieldState<BrokerEntity> = FormFieldState(
        BrokerEntity(
            id = reference?.brokerId ?: "",
            name = "Choose topic's broker",
            clientId = "",
            connected = false,
            ip = "0.0.0.0",
            port = 0,
            user = null,
            password = null,
            connectionType = ConnectionType.TCP,
            keepAliveInterval = 0,
            reconnectAttempts = 0,
            displayIndex = 0,
            removed = true
        )
    ),
    val name: FormFieldState<String> = FormFieldState(reference?.name ?: ""),
    val topic: FormFieldState<String> = FormFieldState(reference?.topic ?: ""),
    val showPayload: FormFieldState<Boolean> = FormFieldState(if (reference == null) false else reference.payloadContent != null),
    val binaryEncoding: FormFieldState<Boolean> = FormFieldState(
        reference?.payloadContent?.startsWith(
            "b@"
        ) ?: false
    ),
    val jsonPaths: FormFieldState<String> = FormFieldState(reference?.payloadContent ?: ""),
    val highPriority: FormFieldState<Boolean> = FormFieldState(reference?.highPriority ?: false),
    val ttsText: FormFieldState<String> = FormFieldState(reference?.notificationSoundText ?: ""),
    val notificationSound: FormFieldState<String> = FormFieldState(
        reference?.notificationSoundPath ?: ""
    )
) : EntityManagingFormState<TopicEntity>() {
    override fun valid(): Boolean = listOf(broker, name, topic, showPayload, binaryEncoding, jsonPaths, highPriority, ttsText, notificationSound).all { it.errorMsg == null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsScreen(vm: TopicViewModel, navController: NavHostController) {
    val manageTopicState by vm.manageTopicState.collectAsStateWithLifecycle()
    val brokers by vm.brokers.collectAsStateWithLifecycle(emptyList())
    val topics by vm.topics.collectAsStateWithLifecycle(emptyList())

    if (manageTopicState.visible) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxWidth(),
            sheetState = rememberModalBottomSheetState(confirmValueChange = { it != SheetValue.Hidden }, skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            onDismissRequest = { vm.onEvent(TopicEvent.ToggleManageForm(null)) }
        ) {
            TopicManage(manageTopicState, brokers) { vm.onEvent(it) }
        }
    }
    Column(Modifier.fillMaxWidth()) {
        SettingsTopBar(navController, "Topics Management") {
            button(onClick = { vm.onEvent(TopicEvent.ToggleManageForm(null)) }) {
                Icon(modifier = Modifier.size(24.dp), imageVector = Icons.Default.AddCircle, contentDescription = null)
            }
        }
        Spacer(Modifier.height(20.dp))
        LazyColumn(Modifier.fillMaxWidth()) {
            brokers.forEach { broker ->
                item {
                    Text(broker.name, style = MaterialTheme.typography.titleMedium)
                    Text(Utils.abbreviateMiddle("${broker.connectionType.toString().lowercase()}://${broker.ip}:${broker.port}", 32), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(10.dp))
                }
                val brokerTopics = topics.filter { it.brokerId == broker.id }
                items(brokerTopics, key = { it.id }) { topic ->
                    TopicContainer(vm, topic)
                    Spacer(Modifier.height(5.dp))
                }
                if (brokerTopics.isEmpty()) item { Empty() }
                item { Spacer(Modifier.height(20.dp)) }
            }
            if (brokers.isEmpty()) item { Empty() }
        }
    }
}

@Composable
fun TopicContainer(vm: TopicViewModel, topic: TopicEntity) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                vm.onEvent(TopicEvent.DeleteTopic(topic))
                true
            } else false
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.85f }
    )
    var expanded by remember { mutableStateOf(false) }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {}
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            onClick = { expanded = !expanded }
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .animateContentSize()
            ) {
                TopicHead(topic) { vm.onEvent(it) }
                if (expanded) TopicBody(topic)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopicHead(topic: TopicEntity, onEvent: (TopicEvent) -> Unit) {
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(topic.name, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(3.dp))
            FlowRow(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                ItemProperty(null, topic.topic, Icons.Filled.CellTower)
                val notificationSoundPath = topic.notificationSoundPath?.ifBlank { null }
                ItemProperty(null, if (notificationSoundPath == null) "Muted" else Utils.parseSoundPath(LocalContext.current, notificationSoundPath), Icons.Filled.Audiotrack)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Spacer(Modifier.width(5.dp))
            ItemAction(Icons.Filled.Edit, onClick = { onEvent(TopicEvent.ToggleManageForm(topic)) })
            Switch(
                checked = topic.enabled,
                onCheckedChange = { onEvent(TopicEvent.ToggleTopic(topic)) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopicBody(topic: TopicEntity) {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (topic.highPriority) ItemProperty("HighPriority", "", Icons.Filled.Warning)
        ItemProperty("NotificationBody", topic.payloadContent?.replaceFirst("b@", "")?.ifBlank { "Full Payload" } ?: "Empty", Icons.Filled.FilterAlt)
        if (!topic.notificationSoundText.isNullOrBlank()) ItemProperty(
            "NotificationSoundText", topic.notificationSoundText,
            Icons.Filled.SpatialAudio
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicManage(state: ManageTopicState, brokers: List<BrokerEntity>, onEvent: (TopicEvent) -> Unit) {
    var expandedBrokerSelector by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    if (state.reference == null) "New Topic" else "Edit Topic",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            item {
                FormIsland(title = "Broker & Topic") {
                    ExposedDropdownMenuBox(
                        expanded = expandedBrokerSelector,
                        onExpandedChange = { expandedBrokerSelector = it }
                    ) {
                        OutlinedTextField(
                            value = state.broker.value.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Broker") },
                            isError = state.broker.errorMsg != null,
                            supportingText = { if (state.broker.errorMsg != null) Text(state.broker.errorMsg) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBrokerSelector) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedBrokerSelector,
                            onDismissRequest = { expandedBrokerSelector = false }
                        ) {
                            brokers.forEach { broker ->
                                DropdownMenuItem(
                                    text = { Text(broker.name) },
                                    onClick = {
                                        onEvent(TopicEvent.BrokerChanged(broker))
                                        expandedBrokerSelector = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = state.name.value,
                        onValueChange = { onEvent(TopicEvent.NameChanged(it)) },
                        label = { Text("Name") },
                        isError = state.name.errorMsg != null,
                        supportingText = { if (state.name.errorMsg != null) Text(state.name.errorMsg) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.topic.value,
                        onValueChange = { onEvent(TopicEvent.TopicChanged(it)) },
                        label = { Text("Topic") },
                        isError = state.topic.errorMsg != null,
                        supportingText = { if (state.topic.errorMsg != null) Text(state.topic.errorMsg) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                FormIsland(title = "Payload") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = state.showPayload.value,
                            onCheckedChange = {
                                onEvent(
                                    TopicEvent.PayloadSettingChanged(
                                        it,
                                        state.binaryEncoding.value,
                                        state.jsonPaths.value
                                    )
                                )
                            }
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Show payload in notification body")
                    }
                    if (state.showPayload.value) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = state.binaryEncoding.value,
                                onCheckedChange = {
                                    onEvent(
                                        TopicEvent.PayloadSettingChanged(
                                            state.showPayload.value,
                                            it,
                                            state.jsonPaths.value
                                        )
                                    )
                                }
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Binary Decoding", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Interpret payload in binary format",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedTextField(
                            value = state.jsonPaths.value,
                            onValueChange = {
                                onEvent(
                                    TopicEvent.PayloadSettingChanged(
                                        state.showPayload.value,
                                        state.binaryEncoding.value,
                                        it
                                    )
                                )
                            },
                            label = { Text("Paths") },
                            isError = state.jsonPaths.errorMsg != null,
                            supportingText = {
                                if (state.jsonPaths.errorMsg != null) Text(state.jsonPaths.errorMsg)
                                else Text("Comma-separated JSON paths. Leave blank for full message.")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item {
                FormIsland(title = "Alerts") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = state.highPriority.value, onCheckedChange = { onEvent(TopicEvent.HighPriorityChanged(it)) })
                        Spacer(Modifier.width(12.dp))
                        Text("High Priority")
                    }
                    OutlinedTextField(
                        value = state.ttsText.value,
                        onValueChange = { onEvent(TopicEvent.TTSTextChanged(it)) },
                        label = { Text("Notification Text (TTS)") },
                        isError = state.ttsText.errorMsg != null,
                        supportingText = {
                            if (state.ttsText.errorMsg != null) Text(state.ttsText.errorMsg)
                            else Text("${state.ttsText.value.length}/32 characters")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    AudioPicker(state.reference?.notificationSoundPath ?: "") { onEvent(TopicEvent.NotificationSoundChanged(it)) }
                }
            }
        }
        if (state.exists && state.reference == null) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Error("This topic already exists")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { onEvent(TopicEvent.ToggleManageForm(null)) }
            ) {
                Text("Cancel")
            }
            Button(
                modifier = Modifier.weight(1f),
                enabled = state.valid() && (state.reference != null || !state.exists),
                onClick = { onEvent(TopicEvent.TopicSaved) }
            ) {
                Text("Save")
            }
        }
    }
}
