package com.egorgoncharov.mastermqtt.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHorizontalCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.WifiTetheringError
import androidx.compose.material.icons.filled.WifiTetheringOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.egorgoncharov.mastermqtt.Utils
import com.egorgoncharov.mastermqtt.dto.MQTTConnection
import com.egorgoncharov.mastermqtt.dto.db.ConnectionType
import com.egorgoncharov.mastermqtt.dto.db.MQTTConnectionState
import com.egorgoncharov.mastermqtt.manager.mqtt.MQTTManager
import com.egorgoncharov.mastermqtt.model.dao.BrokerDao
import com.egorgoncharov.mastermqtt.model.dao.MessageDao
import com.egorgoncharov.mastermqtt.model.dao.TopicDao
import com.egorgoncharov.mastermqtt.model.entity.MessageEntity
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import com.egorgoncharov.mastermqtt.ui.components.Empty
import com.egorgoncharov.mastermqtt.ui.components.FormFieldState
import com.egorgoncharov.mastermqtt.ui.components.FormState
import com.egorgoncharov.mastermqtt.ui.components.SafetyButton
import com.egorgoncharov.mastermqtt.ui.components.StreamDateTimeFilter
import com.egorgoncharov.mastermqtt.ui.components.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class StreamViewModel(
    brokerDao: BrokerDao,
    private val topicDao: TopicDao,
    private val messageDao: MessageDao,
    mqttManager: MQTTManager
) : ViewModel() {
    companion object {
        fun Factory(
            brokerDao: BrokerDao,
            topicDao: TopicDao,
            messageDao: MessageDao,
            mqttManager: MQTTManager
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { StreamViewModel(brokerDao, topicDao, messageDao, mqttManager) }
            }
    }

    private val _streamFilterState = MutableStateFlow(StreamFilterState())
    private val _streamClearState = MutableStateFlow(StreamClearState())
    private val _streamChatState = MutableStateFlow(StreamChatState())
    private var readTrackingJob: Job? = null
    private var deepLinkBound = false

    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val brokers = brokerDao.streamBrokers()
    val topics = topicDao.streamTopics()
    val messages = messageDao.streamNotifications()
    val streamFilterState = _streamFilterState.asStateFlow()
    val streamClearState = _streamClearState.asStateFlow()
    val streamChatState = _streamChatState.asStateFlow()
    val connections = mqttManager.clientsFlow

    fun onEvent(event: StreamEvent) {
        when (event) {
            is StreamEvent.MinDatetimeFilterChanged -> handleMinDatetimeFilterChange(event.min)
            is StreamEvent.MaxDatetimeFilterChanged -> handleMaxDatetimeFilterChange(event.max)
            is StreamEvent.TextSearchFilterChanged -> handleTextSearchFilterChange(event.query)
            is StreamEvent.SelectedStreamChanged -> selectStream(event.streamSource)
            is StreamEvent.ToggleStreamDisplayOriginalMessageOption -> toggleStreamDisplayOriginalMessageOption()
            is StreamEvent.ToggleStreamClearDialog -> toggleStreamClearDialog()
            is StreamEvent.StreamCleared -> clearStream()
            is StreamEvent.DeepLinkBoundChanged -> handleDeepLinkBoundChange(event.deepLinkBound)
        }
    }

    fun isDeepLinkBound() = deepLinkBound

    private fun handleMinDatetimeFilterChange(min: Long?) = _streamFilterState.update(
        { it.minDatetime },
        min,
        { null }
    ) { copy(minDatetime = it) }

    private fun handleMaxDatetimeFilterChange(max: Long?) = _streamFilterState.update(
        { it.maxDatetime },
        max,
        { null }
    ) { copy(maxDatetime = it) }

    private fun handleTextSearchFilterChange(query: String?) = _streamFilterState.update(
        { it.query },
        query,
        { null }
    ) { copy(query = it) }

    private fun selectStream(streamSource: TopicEntity?) {
        _streamChatState.update { it.copy(selected = streamSource) }
        readTrackingJob?.cancel()
        if (streamSource != null) {
            readTrackingJob = scope.launch {
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
            _streamFilterState.update { StreamFilterState() }
        }
    }

    private fun toggleStreamDisplayOriginalMessageOption() = _streamChatState.update { it.copy(showProcessedContent = !it.showProcessedContent) }

    private fun toggleStreamClearDialog() {
        _streamClearState.update { it.copy(showConfirmationDialog = !it.showConfirmationDialog) }
    }

    private fun clearStream() {
        scope.launch { messageDao.delete(messageDao.findAll().filter { it.topicId == streamChatState.value.selected?.id }) }
        toggleStreamClearDialog()
    }

    private fun handleDeepLinkBoundChange(bounded: Boolean) {
        deepLinkBound = bounded
    }
}

sealed interface StreamEvent {
    data class MinDatetimeFilterChanged(val min: Long?) : StreamEvent
    data class MaxDatetimeFilterChanged(val max: Long?) : StreamEvent
    data class TextSearchFilterChanged(val query: String?) : StreamEvent

    data class
    SelectedStreamChanged(val streamSource: TopicEntity?) : StreamEvent

    object ToggleStreamDisplayOriginalMessageOption : StreamEvent

    object ToggleStreamClearDialog : StreamEvent
    object StreamCleared : StreamEvent

    data class DeepLinkBoundChanged(val deepLinkBound: Boolean) : StreamEvent
}

data class StreamFilterState(
    val minDatetime: FormFieldState<Long?> = FormFieldState(null),
    val maxDatetime: FormFieldState<Long?> = FormFieldState(null),
    val query: FormFieldState<String?> = FormFieldState(null)
) : FormState {
    override fun valid(): Boolean = listOf(minDatetime, maxDatetime, query).all { it.errorMsg == null }
}

data class StreamClearState(
    val showConfirmationDialog: Boolean = false
)

data class StreamChatState(
    val selected: TopicEntity? = null,
    val showProcessedContent: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamScreen(vm: StreamViewModel, navController: NavHostController) {
    val connections by vm.connections.collectAsStateWithLifecycle()
    val streamSourceState by vm.streamChatState.collectAsStateWithLifecycle()
    val streamSources by vm.topics.collectAsStateWithLifecycle(emptyList())
    val brokers by vm.brokers.collectAsStateWithLifecycle(emptyList())
    val messages by vm.messages.collectAsStateWithLifecycle(emptyList())

    DisposableEffect(Unit) { onDispose { vm.onEvent(StreamEvent.SelectedStreamChanged(null)) } }
    if (streamSourceState.selected != null) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxWidth(),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { value -> value != SheetValue.Hidden }),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            onDismissRequest = {
                vm.onEvent(StreamEvent.SelectedStreamChanged(null))
            }
        ) {
            StreamSourceChat(vm)
        }
    }
    StatusTopBar(navController, connections)
    Spacer(Modifier.height(20.dp))
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        brokers.forEach { connection ->
            item {
                Text(connection.name, style = MaterialTheme.typography.titleMedium)
                Text(Utils.abbreviateMiddle("${connection.connectionType.toString().lowercase()}://${connection.ip}:${connection.port}", 32), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(10.dp))
            }
            val brokerStreamSources = streamSources.filter { it.brokerId == connection.id }
            items(brokerStreamSources.sortedByDescending { topic ->
                messages.filter { message -> message.topicId == topic.id }.maxByOrNull { it.date }?.date ?: topic.lastOpened
            }, key = { it.id }) { source ->
                StreamSourceContainer(vm, source)
                Spacer(Modifier.height(5.dp))
            }
            if (brokerStreamSources.isEmpty()) item { Empty() }
            item { Spacer(Modifier.height(20.dp)) }
        }
        if (brokers.isEmpty()) item { Empty() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusTopBar(navController: NavHostController, connectionStates: Map<String, MQTTConnection>) {
    val connectedCount = connectionStates.values.filter { it.state == MQTTConnectionState.CONNECTED }.size
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showConnectionsQuickView by remember { mutableStateOf(false) }
    val containerColor = if (connectedCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val dotColor = if (connectedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val contentColor = if (connectedCount > 0) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.onSurfaceVariant

    if (showSettingsScreen) SettingsScreen(navController) { showSettingsScreen = false }
    if (showConnectionsQuickView) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxWidth(),
            sheetState = rememberModalBottomSheetState(),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            onDismissRequest = { showConnectionsQuickView = false }
        ) {
            ConnectionsQuickView(connectionStates)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Master MQTT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SafetyButton(onConfirmedClick = { showSettingsScreen = true }) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
            Spacer(Modifier.width(5.dp))
            SafetyButton(onConfirmedClick = { showConnectionsQuickView = true }) {
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .background(color = containerColor, shape = RoundedCornerShape(24.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(22.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        ) {
                            Box(Modifier.padding(horizontal = if (connectedCount > 0) 5.dp else 0.dp)) {
                                Text(
                                    text = "${connectedCount}/${connectionStates.size}",
                                    color = contentColor,
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (connectedCount > 0) "Connected" else "Disconnected",
                            color = contentColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SimpleDateFormat")
@Composable
fun StreamSourceContainer(vm: StreamViewModel, streamSource: TopicEntity) {
    val messages by vm.messages.collectAsStateWithLifecycle(emptyList())
    val streamMessages = messages.filter { it.topicId == streamSource.id }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = { vm.onEvent(StreamEvent.SelectedStreamChanged(streamSource)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val newMessagesCount = messages.filter { it.topicId == streamSource.id && it.date > streamSource.lastOpened }.size
                Box(
                    Modifier
                        .size(20.dp)
                        .background(
                            color = if (newMessagesCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = RoundedCornerShape(100)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${if (newMessagesCount > 0) newMessagesCount else ""}", style = MaterialTheme.typography.labelSmall, color = if (newMessagesCount > 0) MaterialTheme.colorScheme.onTertiary else Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(streamSource.name, style = MaterialTheme.typography.titleSmall)
                    Text(streamSource.topic, style = MaterialTheme.typography.labelMedium)
                }
                if (streamMessages.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(50.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(Utils.formatDate(streamMessages[0].date), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (streamMessages.isNotEmpty()) {
                Spacer(Modifier.height(5.dp))
                val trimmedMessage = streamMessages[0].content.take(128) + if (streamMessages[0].content.length > 128) "..." else ""
                Text(trimmedMessage, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun StreamSourceChat(vm: StreamViewModel) {
    val connections by vm.connections.collectAsStateWithLifecycle()
    val streamChatState by vm.streamChatState.collectAsStateWithLifecycle()
    val streamClearState by vm.streamClearState.collectAsStateWithLifecycle()
    val streamFilterState by vm.streamFilterState.collectAsStateWithLifecycle()
    val messages by vm.messages.collectAsStateWithLifecycle(emptyList())
    val streamMessages = messages.filter {
        val queryValid = if (streamFilterState.query.value != null) it.content.contains(streamFilterState.query.value!!, true) || it.originalContent.contains(streamFilterState.query.value!!) else true
        val minDatetimeValid = if (streamFilterState.minDatetime.value != null) it.date >= streamFilterState.minDatetime.value!! else true
        val maxDatetimeValid = if (streamFilterState.maxDatetime.value != null) it.date <= streamFilterState.maxDatetime.value!! else true
        it.topicId == streamChatState.selected?.id && queryValid && minDatetimeValid && maxDatetimeValid
    }
    if (streamClearState.showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { vm.onEvent(StreamEvent.ToggleStreamClearDialog) },
            title = { Text("Clear stream history?") },
            text = { Text("You will completely wipe out this stream history. This action is irreversible.") },
            confirmButton = {
                TextButton(onClick = { vm.onEvent(StreamEvent.StreamCleared) }) { Text("Proceed") }
            },
            dismissButton = {
                TextButton(onClick = { vm.onEvent(StreamEvent.ToggleStreamClearDialog) }) { Text("Cancel") }
            }
        )
    }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            StreamSourceChatHeader(connections, streamChatState.selected!!, streamChatState.showProcessedContent) { vm.onEvent(it) }
            StreamSourceChatFilters(streamFilterState) { vm.onEvent(it) }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(streamMessages, key = { it.id }) { StreamSourceChatMessage(it, streamChatState.showProcessedContent) }
                if (streamMessages.isEmpty()) item { Empty() }
            }
        }
    }
}

@Composable
fun StreamSourceChatHeader(
    connections: Map<String, MQTTConnection>,
    streamSourceState: TopicEntity,
    displayProcessedMessage: Boolean,
    onEvent: (StreamEvent) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(Modifier.weight(1f)) {
            val connection = connections.values.find { it.broker.id == streamSourceState.brokerId }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = { onEvent(StreamEvent.SelectedStreamChanged(null)) }, interactionSource = null, indication = null),
                    imageVector = Icons.Default.Close,
                    contentDescription = null
                )
                Text(
                    streamSourceState.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(color = if (connection?.state == MQTTConnectionState.CONNECTED) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(100))
                ) {}
                Text(connection?.broker?.name ?: "...", style = MaterialTheme.typography.labelLarge)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            IconButton(
                modifier = Modifier.size(30.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = if (displayProcessedMessage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary, contentColor = if (displayProcessedMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary),
                onClick = { onEvent(StreamEvent.ToggleStreamDisplayOriginalMessageOption) }
            ) {
                Icon(modifier = Modifier.size(18.dp), imageVector = Icons.Filled.SwapHorizontalCircle, contentDescription = null)
            }
            IconButton(
                modifier = Modifier.size(30.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary),
                onClick = { onEvent(StreamEvent.ToggleStreamClearDialog) }
            ) {
                Icon(modifier = Modifier.size(18.dp), imageVector = Icons.Filled.DeleteSweep, contentDescription = null)
            }
        }
    }
}

@Composable
fun StreamSourceChatFilters(streamFilterState: StreamFilterState, onEvent: (StreamEvent) -> Unit) {
    var selectedStartDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedEndDate by remember { mutableStateOf<LocalDate?>(null) }
    var startQuarter by remember { mutableIntStateOf(0) }
    var endQuarter by remember { mutableIntStateOf(96) }

    OutlinedTextField(
        value = streamFilterState.query.value ?: "",
        onValueChange = { onEvent(StreamEvent.TextSearchFilterChanged(it)) },
        label = { Text("Search text") },
        placeholder = { Text("Query") },
        trailingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(5.dp))
    StreamDateTimeFilter(
        onStartDateSelected = { date ->
            selectedStartDate = date
            val timestamp = combineDateAndTime(date, startQuarter)
            onEvent(StreamEvent.MinDatetimeFilterChanged(timestamp))
        },
        onEndDateSelected = { date ->
            selectedEndDate = date
            val timestamp = combineDateAndTime(date, endQuarter)
            onEvent(StreamEvent.MaxDatetimeFilterChanged(timestamp))
        },
        onTimeRangeChanged = { start, end ->
            startQuarter = start
            endQuarter = end
            val minTs = combineDateAndTime(selectedStartDate, start)
            val maxTs = combineDateAndTime(selectedEndDate, end, selectedStartDate)
            onEvent(StreamEvent.MinDatetimeFilterChanged(minTs))
            onEvent(StreamEvent.MaxDatetimeFilterChanged(maxTs))
        }
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
fun StreamSourceChatMessage(message: MessageEntity, displayProcessedMessage: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (true /* TODO: Check if message was received (start) or sent (end) */) Alignment.Start else Alignment.End
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.75f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val text = if (displayProcessedMessage) message.content else message.originalContent
                SelectionContainer {
                    Text(text.ifBlank { "<empty>" }, style = MaterialTheme.typography.labelSmall.copy(fontStyle = if (text.isEmpty()) FontStyle.Italic else FontStyle.Normal))
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(Utils.formatDate(message.date, "HH:mm:ss dd/MM"), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun ConnectionsQuickView(connectionStates: Map<String, MQTTConnection>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items(connectionStates.values.toList(), key = { it.broker.id }) { connection ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(connection.broker.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            Utils.abbreviateMiddle(
                                "${if (connection.broker.connectionType == ConnectionType.TCP) "tcp://" else "ssl://"}${connection.broker.ip}:${connection.broker.port}",
                                32
                            ), style = MaterialTheme.typography.labelSmall
                        )
                    }
                    val icon = when (connection.state) {
                        MQTTConnectionState.CONNECTED -> Icons.Filled.WifiTethering
                        MQTTConnectionState.DISCONNECTED -> Icons.Filled.WifiTetheringOff
                        MQTTConnectionState.INTERMEDIATE -> Icons.Filled.Timer
                        MQTTConnectionState.DISCONNECTED_FAILED -> Icons.Filled.WifiTetheringError
                    }
                    val backgroundColor = when (connection.state) {
                        MQTTConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                        MQTTConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
                        MQTTConnectionState.INTERMEDIATE -> MaterialTheme.colorScheme.secondary
                        MQTTConnectionState.DISCONNECTED_FAILED -> MaterialTheme.colorScheme.error
                    }
                    val contentColor = when (connection.state) {
                        MQTTConnectionState.CONNECTED -> MaterialTheme.colorScheme.onPrimary
                        MQTTConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                        MQTTConnectionState.INTERMEDIATE -> MaterialTheme.colorScheme.onSecondary
                        MQTTConnectionState.DISCONNECTED_FAILED -> MaterialTheme.colorScheme.onError
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(color = backgroundColor, shape = RoundedCornerShape(100)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(modifier = Modifier.size(16.dp), imageVector = icon, contentDescription = null, tint = contentColor)
                    }
                }
            }
        }
        if (connectionStates.isEmpty()) item { Empty() }
    }
}

private fun combineDateAndTime(
    date: LocalDate?,
    quarters: Int,
    fallbackDate: LocalDate? = null
): Long {
    val baseDate = date ?: fallbackDate ?: LocalDate.now()
    return baseDate.atStartOfDay(ZoneId.systemDefault())
        .plusMinutes(quarters * 15L)
        .toInstant()
        .toEpochMilli()
}
