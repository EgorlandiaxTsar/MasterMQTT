package com.egorgoncharov.mastermqtt.screen.settings.topics

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
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.egorgoncharov.mastermqtt.Utils
import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import com.egorgoncharov.mastermqtt.ui.components.AudioPicker
import com.egorgoncharov.mastermqtt.ui.components.DialogWindow
import com.egorgoncharov.mastermqtt.ui.components.Empty
import com.egorgoncharov.mastermqtt.ui.components.Error
import com.egorgoncharov.mastermqtt.ui.components.FormIsland
import com.egorgoncharov.mastermqtt.ui.components.ItemAction
import com.egorgoncharov.mastermqtt.ui.components.ItemProperty
import com.egorgoncharov.mastermqtt.ui.components.SettingsTopBar
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsScreen(vm: TopicsScreenViewModel, navController: NavHostController) {
    val manageTopicState by vm.manageTopicsFormState.collectAsStateWithLifecycle()
    val brokers by vm.brokers.collectAsStateWithLifecycle(emptyList())
    val topics by vm.topics.collectAsStateWithLifecycle(emptyList())

    if (manageTopicState.visible) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxWidth(),
            sheetState = rememberModalBottomSheetState(confirmValueChange = { it != SheetValue.Hidden }, skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            onDismissRequest = { vm.onEvent(TopicsScreenEvent.ToggleManageForm(null)) }
        ) {
            TopicManage(manageTopicState, brokers) { vm.onEvent(it) }
        }
    }
    Column(Modifier.fillMaxWidth()) {
        SettingsTopBar(navController, "Topics Management") {
            button(onClick = { vm.onEvent(TopicsScreenEvent.ToggleManageForm(null)) }) {
                Icon(modifier = Modifier.size(24.dp), imageVector = Icons.Default.AddCircle, contentDescription = null)
            }
        }
        Spacer(Modifier.height(20.dp))
        LazyColumn(Modifier.fillMaxWidth()) {
            brokers.forEach { broker ->
                item {
                    Text(broker.name, style = MaterialTheme.typography.titleMedium)
                    Text(Utils.abbreviateMiddle("${broker.connectionType.toString().lowercase()}://${broker.host}:${broker.port}", 32), style = MaterialTheme.typography.labelSmall)
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
fun TopicContainer(vm: TopicsScreenViewModel, topic: TopicEntity) {
    val topicDeleteConfirmationDialogState by vm.topicDeleteConfirmationDialogState.collectAsStateWithLifecycle()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                vm.onEvent(TopicsScreenEvent.ToggleTopicDeleteConfirmationDialog(topic, true))
                false
            } else false
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.85f }
    )
    var expanded by remember { mutableStateOf(false) }

    DialogWindow(topicDeleteConfirmationDialogState)
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
fun TopicHead(topic: TopicEntity, onEvent: (TopicsScreenEvent) -> Unit) {
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
            ItemAction(Icons.Filled.Edit, onClick = { onEvent(TopicsScreenEvent.ToggleManageForm(topic)) })
            Switch(
                checked = topic.enabled,
                onCheckedChange = { onEvent(TopicsScreenEvent.ToggleTopic(topic)) }
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
        ItemProperty(
            "QoS",
            "${topic.qos}",
            Icons.Filled.SignalCellularAlt
        )
        ItemProperty(
            "High Priority",
            if (topic.highPriority) "Yes" else "No",
            Icons.Filled.Warning
        )
        ItemProperty(
            "Ignore Bed Time",
            if (topic.ignoreBedTime) "Yes" else "No",
            Icons.Filled.Nightlight
        )
        ItemProperty(
            "Notification Sound Level",
            "${topic.notificationSoundLevel?.times(100)?.toInt()}%",
            Icons.Filled.VolumeUp
        )
        ItemProperty(
            "Notification TTS Text", topic.notificationSoundText ?: "None",
            Icons.Filled.SpatialAudio
        )
        ItemProperty(
            "Notification Body",
            topic.payloadContent?.replaceFirst("b@", "")?.ifBlank { "Full Payload" } ?: "Empty",
            Icons.Filled.FilterAlt
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicManage(state: ManageTopicsFormState, brokers: List<BrokerEntity>, onEvent: (TopicsScreenEvent) -> Unit) {
    var expandedQosSelector by remember { mutableStateOf(false) }
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
                FormIsland(title = "General") {
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
                                        onEvent(TopicsScreenEvent.BrokerChanged(broker))
                                        expandedBrokerSelector = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = state.name.value,
                        onValueChange = { onEvent(TopicsScreenEvent.NameChanged(it)) },
                        label = { Text("Name") },
                        isError = state.name.errorMsg != null,
                        supportingText = { if (state.name.errorMsg != null) Text(state.name.errorMsg) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.topic.value,
                        onValueChange = { onEvent(TopicsScreenEvent.TopicChanged(it)) },
                        label = { Text("Topic") },
                        isError = state.topic.errorMsg != null,
                        supportingText = { if (state.topic.errorMsg != null) Text(state.topic.errorMsg) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedQosSelector,
                        onExpandedChange = { expandedQosSelector = it }
                    ) {
                        OutlinedTextField(
                            value = state.qos.value.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("QoS") },
                            isError = state.qos.errorMsg != null,
                            supportingText = { if (state.qos.errorMsg != null) Text(state.qos.errorMsg) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedQosSelector) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedQosSelector,
                            onDismissRequest = { expandedQosSelector = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("0 - At most once") },
                                onClick = {
                                    onEvent(TopicsScreenEvent.QosChanged("0"))
                                    expandedQosSelector = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("1 - At least once") },
                                onClick = {
                                    onEvent(TopicsScreenEvent.QosChanged("1"))
                                    expandedQosSelector = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("2 - Exactly once") },
                                onClick = {
                                    onEvent(TopicsScreenEvent.QosChanged("2"))
                                    expandedQosSelector = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                FormIsland(title = "Payload") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = state.showPayload.value,
                            onCheckedChange = {
                                onEvent(
                                    TopicsScreenEvent.PayloadSettingChanged(
                                        it,
                                        state.binaryEncoding.value,
                                        state.payloadContent.value
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
                                        TopicsScreenEvent.PayloadSettingChanged(
                                            state.showPayload.value,
                                            it,
                                            state.payloadContent.value
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
                            value = state.payloadContent.value,
                            onValueChange = {
                                onEvent(
                                    TopicsScreenEvent.PayloadSettingChanged(
                                        state.showPayload.value,
                                        state.binaryEncoding.value,
                                        it
                                    )
                                )
                            },
                            label = { Text("Paths") },
                            isError = state.payloadContent.errorMsg != null,
                            supportingText = {
                                if (state.payloadContent.errorMsg != null) Text(state.payloadContent.errorMsg)
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
                        Switch(
                            checked = state.highPriority.value,
                            enabled = !state.ignoreBedTime.value,
                            onCheckedChange = { onEvent(TopicsScreenEvent.HighPriorityChanged(it)) }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("High Priority")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = state.ignoreBedTime.value, onCheckedChange = {
                            onEvent(TopicsScreenEvent.IgnoreBedTimeChanged(it))
                            if (it) onEvent(TopicsScreenEvent.HighPriorityChanged(true))
                        })
                        Spacer(Modifier.width(12.dp))
                        Text("Ignore Bed Time Mode")
                    }
                    OutlinedTextField(
                        value = state.ttsText.value,
                        onValueChange = { onEvent(TopicsScreenEvent.TTSTextChanged(it)) },
                        label = { Text("Notification Text (TTS)") },
                        isError = state.ttsText.errorMsg != null,
                        supportingText = {
                            if (state.ttsText.errorMsg != null) Text(state.ttsText.errorMsg)
                            else Text("${state.ttsText.value.length}/32 characters")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    AudioPicker(state.reference?.notificationSoundPath ?: "") { onEvent(TopicsScreenEvent.NotificationSoundChanged(it)) }
                    if (state.notificationSound.value.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Slider(
                                value = state.notificationSoundLevel.value.toFloat(),
                                onValueChange = { onEvent(TopicsScreenEvent.NotificationSoundLevelChanged(it.toDouble())) },
                                onValueChangeFinished = { onEvent(TopicsScreenEvent.PlayNotificationSound) },
                                valueRange = 0f..1.0f,
                                steps = 19,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Volume ${round(state.notificationSoundLevel.value * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
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
                onClick = { onEvent(TopicsScreenEvent.ToggleManageForm(null)) }
            ) {
                Text("Cancel")
            }
            Button(
                modifier = Modifier.weight(1f),
                enabled = state.valid() && (state.reference != null || !state.exists),
                onClick = { onEvent(TopicsScreenEvent.TopicSaved) }
            ) {
                Text("Save")
            }
        }
    }
}
