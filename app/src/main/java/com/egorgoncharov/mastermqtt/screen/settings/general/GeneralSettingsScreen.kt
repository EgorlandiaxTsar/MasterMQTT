package com.egorgoncharov.mastermqtt.screen.settings.general

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Upload
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.egorgoncharov.mastermqtt.Utils
import com.egorgoncharov.mastermqtt.configuration.dto.BrokerConfiguration
import com.egorgoncharov.mastermqtt.configuration.dto.TopicConfiguration
import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.entity.SettingsProfileEntity
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import com.egorgoncharov.mastermqtt.model.types.ConnectionType
import com.egorgoncharov.mastermqtt.model.types.TTSLanguage
import com.egorgoncharov.mastermqtt.model.types.ThemeOption
import com.egorgoncharov.mastermqtt.ui.components.Empty
import com.egorgoncharov.mastermqtt.ui.components.Error
import com.egorgoncharov.mastermqtt.ui.components.FilePicker
import com.egorgoncharov.mastermqtt.ui.components.ItemProperty
import com.egorgoncharov.mastermqtt.ui.components.SettingsTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(vm: GeneralSettingsScreenViewModel, navController: NavHostController) {
    val settingsProfile by vm.mainSettingProfile.collectAsStateWithLifecycle(SettingsProfileEntity.DUMMY)
    val importFormState by vm.configurationImportFormState.collectAsStateWithLifecycle()
    val exportFormState by vm.configurationExportFormState.collectAsStateWithLifecycle()

    if (exportFormState.showExportForm || importFormState.showImportForm) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxSize(),
            sheetState = rememberModalBottomSheetState(confirmValueChange = { it != SheetValue.Hidden }, skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            onDismissRequest = {}
        ) {
            if (importFormState.showImportForm) ImportForm(vm) else ExportForm(vm)
        }
    }
    Column(Modifier.fillMaxWidth()) {
        SettingsTopBar(navController, "General Settings")
        Spacer(Modifier.height(20.dp))
        UiSettingsSection(settingsProfile!!) { vm.onEvent(it) }
        TTSSettingsSection(settingsProfile!!) { vm.onEvent(it) }
        StorageSettingsSection(settingsProfile!!) { vm.onEvent(it) }
        ConfigurationImportExportSettingsSection { vm.onEvent(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UiSettingsSection(settingsProfile: SettingsProfileEntity, onEvent: (GeneralSettingsScreenEvent) -> Unit) {
    var expandedThemeOptionSelector by remember { mutableStateOf(false) }

    Text("UI", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
    ExposedDropdownMenuBox(
        expanded = expandedThemeOptionSelector,
        onExpandedChange = { expandedThemeOptionSelector = it }
    ) {
        OutlinedTextField(
            value = settingsProfile.theme.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Application Theme") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedThemeOptionSelector) },
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expandedThemeOptionSelector,
            onDismissRequest = { expandedThemeOptionSelector = false }
        ) {
            ThemeOption.entries.forEach {
                DropdownMenuItem(
                    text = { Text(it.label) },
                    onClick = {
                        onEvent(GeneralSettingsScreenEvent.ThemeOptionChanged(it))
                        expandedThemeOptionSelector = false
                    }
                )
            }
        }
    }
    Spacer(Modifier.height(5.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = settingsProfile.settingsSafetyButtonEnabled, onCheckedChange = { onEvent(GeneralSettingsScreenEvent.SafetyButtonEnabledChanged(it)) })
        Spacer(Modifier.width(10.dp))
        Text("Enable Top Bar Safety Buttons")
    }
    Spacer(Modifier.height(15.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TTSSettingsSection(settingsProfile: SettingsProfileEntity, onEvent: (GeneralSettingsScreenEvent) -> Unit) {
    var expandedTTSLanguageSelector by remember { mutableStateOf(false) }

    Text("TTS", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
    ExposedDropdownMenuBox(
        expanded = expandedTTSLanguageSelector,
        onExpandedChange = { expandedTTSLanguageSelector = it }
    ) {
        OutlinedTextField(
            value = settingsProfile.ttsLanguage.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("TTS Speaking Language") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTTSLanguageSelector) },
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expandedTTSLanguageSelector,
            onDismissRequest = { expandedTTSLanguageSelector = false }
        ) {
            TTSLanguage.entries.forEach {
                DropdownMenuItem(
                    text = { Text(it.label) },
                    onClick = {
                        onEvent(GeneralSettingsScreenEvent.TTSLanguageChanged(it))
                        expandedTTSLanguageSelector = false
                    }
                )
            }
        }
    }
    Spacer(Modifier.height(15.dp))
}

@Composable
fun StorageSettingsSection(settingsProfile: SettingsProfileEntity, onEvent: (GeneralSettingsScreenEvent) -> Unit) {
    var defaultMessageAgeValue by remember { mutableStateOf("${settingsProfile.defaultMessageAge}") }

    LaunchedEffect(settingsProfile.defaultMessageAge) { defaultMessageAgeValue = "${settingsProfile.defaultMessageAge}" }
    Text("Storage", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = defaultMessageAgeValue,
        onValueChange = {
            defaultMessageAgeValue = it
            if (it.isNotBlank()) onEvent(GeneralSettingsScreenEvent.DefaultMessageAgeChanged(it))
        },
        label = { Text("Default Message Age (s)") },
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(15.dp))
}

@Composable
fun ConfigurationImportExportSettingsSection(onEvent: (GeneralSettingsScreenEvent) -> Unit) {
    Text("Configuration Import/Export", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
    SettingActionCard("Import", Icons.Filled.Download, onClick = { onEvent(GeneralSettingsScreenEvent.ToggleConfigurationImportForm) }) { }
    Spacer(Modifier.height(5.dp))
    SettingActionCard("Export", Icons.Filled.Upload, onClick = { onEvent(GeneralSettingsScreenEvent.ToggleConfigurationExportForm) }) { }
}

@Composable
fun ExportForm(vm: GeneralSettingsScreenViewModel) {
    val exportFormState by vm.configurationExportFormState.collectAsStateWithLifecycle()
    val brokers by vm.brokers.collectAsStateWithLifecycle(emptyList())
    val topics by vm.topics.collectAsStateWithLifecycle(emptyList())

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(20.dp),
        ) {
            item {
                Text(
                    "Configuration Export",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            items(brokers, key = { it.id }) { broker ->
                ExportBrokerContainer(exportFormState, broker, topics.filter { it.brokerId == broker.id }) { vm.onEvent(it) }
                Spacer(Modifier.height(10.dp))
            }
            if (brokers.isEmpty()) item { Empty() }
        }
        if (exportFormState.state == ConfigurationExportFormState.State.EXPORT_FAILED) Error("Failed to create bundle")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { vm.onEvent(GeneralSettingsScreenEvent.ToggleConfigurationExportForm) }) { Text("Cancel") }
            Button(
                modifier = Modifier.weight(1f),
                enabled = brokers.isNotEmpty(),
                onClick = { vm.onEvent(GeneralSettingsScreenEvent.ConfigurationExportStarted) }
            ) {
                Text("Export")
            }
        }
    }
}

@Composable
fun ImportForm(vm: GeneralSettingsScreenViewModel) {
    val importFormState by vm.configurationImportFormState.collectAsStateWithLifecycle()
    var bundlePath by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Configuration Import",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 20.dp, bottom = 16.dp, end = 20.dp)
        )
        if (importFormState.state in listOf(ConfigurationImportFormState.State.WAITING_BUNDLE_LOAD, ConfigurationImportFormState.State.BUNDLE_LOAD_FAILED, ConfigurationImportFormState.State.BUNDLE_LOADING)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FilePicker(label = "Load Bundle", selectedFile = bundlePath, mimeType = "application/octet-stream") {
                    bundlePath = it
                    if (it.isNotBlank()) vm.onEvent(GeneralSettingsScreenEvent.ConfigurationBundleLoadStarted(it))
                }
                if (importFormState.state == ConfigurationImportFormState.State.BUNDLE_LOAD_FAILED) {
                    Spacer(Modifier.height(5.dp))
                    Error("Failed to parse bundle")
                }
            }
        } else {
            val brokers = importFormState.configuration!!.brokers
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(20.dp),
            ) {
                items(brokers, key = { it.url }) { broker ->
                    ImportBrokerContainer(broker)
                    Spacer(Modifier.height(10.dp))
                }
                if (brokers.isEmpty()) item { Empty() }
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
                onClick = { vm.onEvent(GeneralSettingsScreenEvent.ToggleConfigurationImportForm) }) { Text("Cancel") }
            Button(
                modifier = Modifier.weight(1f),
                enabled = importFormState.state == ConfigurationImportFormState.State.WAITING_IMPORT_CONFIRMATION,
                onClick = { vm.onEvent(GeneralSettingsScreenEvent.ConfigurationBundleImportStarted) }
            ) {
                Text("Import")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportBrokerContainer(broker: BrokerConfiguration) {
    val expanded = remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(24.dp),
        onClick = { expanded.value = !expanded.value }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(broker.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(5.dp))
            Text(Utils.abbreviateMiddle(broker.url, 48), style = MaterialTheme.typography.labelSmall)
            Text(if (broker.authenticationUser() == null) "Unauthenticated" else "${broker.authenticationUser()}/${broker.authenticationPassword()}", style = MaterialTheme.typography.labelSmall)
            Text("${broker.topics.size} topics", style = MaterialTheme.typography.labelSmall)
            BrokerInfoContainer(
                broker.clientId,
                broker.keepAliveInterval,
                broker.cleanStart,
                broker.reconnectAttempts,
                broker.reconnectInterval,
                broker.sessionExpiryInterval
            )
            if (expanded.value) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 15.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) { broker.topics.forEach { ImportTopicContainer(it) } }
            }
            UnwrappableMark(expanded)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportBrokerContainer(
    exportFormState: ConfigurationExportFormState,
    broker: BrokerEntity,
    topics: List<TopicEntity>,
    onEvent: (GeneralSettingsScreenEvent) -> Unit
) {
    val filteredTopics = topics.filter { it.brokerId == broker.id }
    val included = exportFormState.configuration.brokers.find { it.address() == broker.address() } != null
    val expanded = remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(24.dp),
        onClick = { expanded.value = if (!included) false else !expanded.value }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(broker.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        Utils.abbreviateMiddle(
                            "${if (broker.connectionType == ConnectionType.TCP) "tcp://" else "ssl://"}${broker.host}:${broker.port}",
                            32
                        ), style = MaterialTheme.typography.labelSmall
                    )
                    Text(if (broker.authUser == null) "Unauthenticated" else "${broker.authUser}/${broker.authPassword}", style = MaterialTheme.typography.labelSmall)
                    Text("${filteredTopics.size} topics", style = MaterialTheme.typography.labelSmall)
                }
                Switch(
                    checked = included,
                    onCheckedChange = {
                        if (!it) expanded.value = false
                        onEvent(GeneralSettingsScreenEvent.ToggleExportBundleBroker(broker))
                    }
                )
            }
            BrokerInfoContainer(
                broker.clientId,
                broker.keepAliveInterval,
                broker.cleanStart,
                broker.reconnectAttempts,
                broker.reconnectInterval,
                broker.sessionExpiryInterval
            )
            if (expanded.value) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 15.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) { topics.forEach { ExportTopicContainer(exportFormState, it, broker, onEvent) } }
            }
            UnwrappableMark(expanded)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportTopicContainer(topic: TopicConfiguration) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(topic.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(5.dp))
            Text(topic.topic, style = MaterialTheme.typography.labelSmall)
            Text(if (topic.notificationSoundPath.isNullOrBlank()) "Muted" else Utils.parseSoundPath(LocalContext.current, topic.notificationSoundPath), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(15.dp))
            TopicInfoContainer(
                topic.qos,
                topic.highPriority,
                topic.ignoreBedTime,
                topic.notificationSoundLevel,
                topic.notificationSoundText,
                topic.payloadContent,
                topic.messageAge
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportTopicContainer(
    exportFormState: ConfigurationExportFormState,
    topic: TopicEntity,
    broker: BrokerEntity,
    onEvent: (GeneralSettingsScreenEvent) -> Unit
) {
    val configurationBroker = exportFormState.configuration.brokers.find { it.address() == broker.address() } ?: return
    val included = configurationBroker.topics.find { it.topic == topic.topic } != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (included) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (included) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(topic.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(5.dp))
                    Text(topic.topic, style = MaterialTheme.typography.labelSmall)
                    Text(if (topic.notificationSoundPath.isNullOrBlank()) "Muted" else Utils.parseSoundPath(LocalContext.current, topic.notificationSoundPath), style = MaterialTheme.typography.labelSmall)
                }
                Switch(
                    checked = included,
                    onCheckedChange = { onEvent(GeneralSettingsScreenEvent.ToggleExportBundleTopic(topic)) }
                )
            }
            Spacer(Modifier.height(15.dp))
            TopicInfoContainer(
                topic.qos,
                topic.highPriority,
                topic.ignoreBedTime,
                topic.notificationSoundLevel,
                topic.notificationSoundText,
                topic.payloadContent,
                topic.messageAge
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrokerInfoContainer(
    clientId: String,
    keepAliveInterval: Int,
    cleanStart: Boolean,
    reconnectAttempts: Int?,
    reconnectInterval: Int,
    sessionExpiryInterval: Int?
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 15.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        ItemProperty("Client ID", clientId, Icons.Filled.Tag)
        ItemProperty(
            "Keep Alive Interval",
            "${keepAliveInterval}s",
            Icons.Filled.MonitorHeart
        )
        ItemProperty(
            "Clean Start",
            if (cleanStart) "Yes" else "No",
            Icons.Filled.CleaningServices
        )
        ItemProperty(
            "Reconnect Attempts",
            "${reconnectAttempts ?: "Infinite"}",
            Icons.Filled.Refresh
        )
        ItemProperty(
            "Reconnect Interval",
            "${reconnectInterval}s",
            Icons.Filled.Timer
        )
        ItemProperty(
            "Session Expiry Interval",
            "${sessionExpiryInterval ?: "None"}",
            Icons.Filled.AutoAwesomeMotion
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopicInfoContainer(
    qos: Int,
    highPriority: Boolean,
    ignoreBedTime: Boolean,
    notificationSoundLevel: Double?,
    notificationSoundText: String?,
    payloadContent: String?,
    messageAge: Int
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        ItemProperty(
            "QoS",
            "$qos",
            Icons.Filled.SignalCellularAlt
        )
        ItemProperty(
            "High Priority",
            if (highPriority) "Yes" else "No",
            Icons.Filled.Warning
        )
        ItemProperty(
            "Ignore Bed Time",
            if (ignoreBedTime) "Yes" else "No",
            Icons.Filled.Nightlight
        )
        ItemProperty(
            "Notification Sound Level",
            "${notificationSoundLevel?.times(100)?.toInt()}%",
            Icons.Filled.VolumeUp
        )
        ItemProperty(
            "NotificationSoundText", notificationSoundText ?: "None",
            Icons.Filled.SpatialAudio
        )
        ItemProperty(
            "Notification Body",
            payloadContent?.replaceFirst("b@", "")?.ifBlank { "Full Payload" } ?: "Empty",
            Icons.Filled.FilterAlt
        )
        ItemProperty(
            "Message Age",
            "${messageAge}s",
            Icons.Filled.FolderCopy
        )
    }
}

@Composable
fun UnwrappableMark(unwrapped: MutableState<Boolean>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary),
        shape = RoundedCornerShape(24.dp),
        onClick = { unwrapped.value = !unwrapped.value }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(modifier = Modifier.size(20.dp), imageVector = if (!unwrapped.value) Icons.Filled.Add else Icons.Filled.Remove, contentDescription = null)
            Spacer(Modifier.width(5.dp))
            Text(if (!unwrapped.value) "More" else "Less", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun SettingActionCard(title: String, icon: ImageVector, onClick: () -> Unit, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 15.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                IconButton(
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                    onClick = { onClick() }
                ) {
                    Icon(modifier = Modifier.size(16.dp), imageVector = icon, contentDescription = null)
                }
            }
            content()
        }
    }
}
