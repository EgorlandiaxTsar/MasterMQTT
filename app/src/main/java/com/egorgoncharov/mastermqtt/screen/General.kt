package com.egorgoncharov.mastermqtt.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.egorgoncharov.mastermqtt.BuildConfig
import com.egorgoncharov.mastermqtt.Utils
import com.egorgoncharov.mastermqtt.configuration.ConfigurationEntityConverter
import com.egorgoncharov.mastermqtt.configuration.dto.AppConfiguration
import com.egorgoncharov.mastermqtt.configuration.dto.BrokerConfiguration
import com.egorgoncharov.mastermqtt.configuration.dto.TopicConfiguration
import com.egorgoncharov.mastermqtt.manager.ConfigurationManager
import com.egorgoncharov.mastermqtt.model.dao.BrokerDao
import com.egorgoncharov.mastermqtt.model.dao.TopicDao
import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import com.egorgoncharov.mastermqtt.model.types.ConnectionType
import com.egorgoncharov.mastermqtt.ui.components.Empty
import com.egorgoncharov.mastermqtt.ui.components.Error
import com.egorgoncharov.mastermqtt.ui.components.FilePicker
import com.egorgoncharov.mastermqtt.ui.components.SettingsTopBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GeneralSettingsViewModel(
    private val brokerDao: BrokerDao,
    topicDao: TopicDao,
    private val configurationManager: ConfigurationManager,
    private val configurationConverter: ConfigurationEntityConverter
) : ViewModel() {
    companion object {
        fun Factory(brokerDao: BrokerDao, topicDao: TopicDao, configurationManager: ConfigurationManager, configurationEntityConverter: ConfigurationEntityConverter): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { GeneralSettingsViewModel(brokerDao, topicDao, configurationManager, configurationEntityConverter) }
            }
    }

    private val _importState = MutableStateFlow(ImportState())
    private val _exportState = MutableStateFlow(ExportState())

    val brokers = brokerDao.streamBrokers()
    val topics = topicDao.streamTopics()
    val importState = _importState.asStateFlow()
    val exportState = _exportState.asStateFlow()

    init {
        viewModelScope.launch {
            val initialBrokers = brokers.first()
            val configurations = initialBrokers.map { entity -> configurationConverter.brokerFromEntityToConfiguration(entity) }
            _exportState.update { state ->
                state.copy(
                    configuration = state.configuration.copy(
                        brokers = configurations.toMutableList()
                    )
                )
            }
        }
    }

    fun onEvent(event: GeneralSettingsEvent) {
        when (event) {
            is GeneralSettingsEvent.ToggleConfigurationExportForm -> toggleConfigurationExportForm()
            is GeneralSettingsEvent.ToggleExportBundleBroker -> toggleExportBundleBroker(event.broker)
            is GeneralSettingsEvent.ToggleExportBundleTopic -> toggleExportBundleTopic(event.topic)
            is GeneralSettingsEvent.ConfigurationExportStarted -> exportConfiguration()
            is GeneralSettingsEvent.ToggleConfigurationImportForm -> toggleConfigurationImportForm()
            is GeneralSettingsEvent.ConfigurationBundleLoadStarted -> loadConfigurationBundle(event.zipPath)
            is GeneralSettingsEvent.ConfigurationBundleImportStarted -> importConfiguration()
        }
    }

    private fun toggleConfigurationExportForm() = _exportState.update { it.copy(showExportForm = !it.showExportForm) }

    private fun toggleExportBundleBroker(broker: BrokerEntity) {
        viewModelScope.launch {
            val configuration = _exportState.value.configuration
            val brokerAddress = broker.address()
            val exists = configuration.brokers.any { it.address() == brokerAddress }
            val updatedBrokers = if (exists) configuration.brokers.filterNot { it.address() == brokerAddress } else configuration.brokers + configurationConverter.brokerFromEntityToConfiguration(broker)
            _exportState.update { it.copy(configuration = configuration.copy(brokers = updatedBrokers.toMutableList())) }
        }
    }

    private fun toggleExportBundleTopic(topic: TopicEntity) {
        viewModelScope.launch {
            val broker = brokerDao.findById(topic.brokerId) ?: return@launch
            _exportState.update { state ->
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
                _exportState.update { it.copy(state = ExportState.State.EXPORTING) }
                return@runCatching configurationManager.write(exportState.value.configuration)

            }.onSuccess { result ->
                _exportState.update { it.copy(state = ExportState.State.EXPORT_SUCCESS) }
                configurationManager.revealInExplorer(result)
                toggleConfigurationExportForm()
            }.onFailure { _exportState.update { it.copy(state = ExportState.State.EXPORT_FAILED) } }
        }
    }

    private fun toggleConfigurationImportForm() = _importState.update { ImportState(showImportForm = !it.showImportForm) }

    private fun loadConfigurationBundle(zipPath: String) {
        viewModelScope.launch {
            runCatching {
                _importState.update { it.copy(state = ImportState.State.BUNDLE_LOADING, zipFilePath = zipPath) }
                return@runCatching configurationManager.read(zipPath.toUri())
            }.onSuccess { result ->
                _importState.update { it.copy(state = ImportState.State.WAITING_IMPORT_CONFIRMATION, configuration = result) }
            }.onFailure { _ -> _importState.update { it.copy(state = ImportState.State.BUNDLE_LOAD_FAILED) } }
        }
    }

    private fun importConfiguration() {
        viewModelScope.launch {
            runCatching {
                _importState.update { it.copy(state = ImportState.State.IMPORTING) }
                return@runCatching configurationManager.load(importState.value.zipFilePath!!.toUri(), importState.value.configuration!!, true)
            }.onSuccess {
                toggleConfigurationImportForm()
            }.onFailure {/* Shouldn't do failure normally */ }
        }
    }
}

sealed interface GeneralSettingsEvent {
    object ToggleConfigurationExportForm : GeneralSettingsEvent
    data class ToggleExportBundleBroker(val broker: BrokerEntity) : GeneralSettingsEvent
    data class ToggleExportBundleTopic(val topic: TopicEntity) : GeneralSettingsEvent
    object ConfigurationExportStarted : GeneralSettingsEvent

    object ToggleConfigurationImportForm : GeneralSettingsEvent
    data class ConfigurationBundleLoadStarted(val zipPath: String) : GeneralSettingsEvent
    object ConfigurationBundleImportStarted : GeneralSettingsEvent
}

data class ImportState(
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

data class ExportState(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(vm: GeneralSettingsViewModel, navController: NavHostController) {
    val importFormState by vm.importState.collectAsStateWithLifecycle()
    val exportFormState by vm.exportState.collectAsStateWithLifecycle()

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
        Text("Configuration Import/Export", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        SettingActionCard("Import", Icons.Filled.Download, onClick = { vm.onEvent(GeneralSettingsEvent.ToggleConfigurationImportForm) }) { }
        Spacer(Modifier.height(5.dp))
        SettingActionCard("Export", Icons.Filled.Upload, onClick = { vm.onEvent(GeneralSettingsEvent.ToggleConfigurationExportForm) }) { }
    }
}

@Composable
fun ExportForm(vm: GeneralSettingsViewModel) {
    val exportFormState by vm.exportState.collectAsStateWithLifecycle()
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
        if (exportFormState.state == ExportState.State.EXPORT_FAILED) Error("Failed to create bundle")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { vm.onEvent(GeneralSettingsEvent.ToggleConfigurationExportForm) }) { Text("Cancel") }
            Button(
                modifier = Modifier.weight(1f),
                enabled = brokers.isNotEmpty(),
                onClick = { vm.onEvent(GeneralSettingsEvent.ConfigurationExportStarted) }
            ) {
                Text("Export")
            }
        }
    }
}

@Composable
fun ImportForm(vm: GeneralSettingsViewModel) {
    val importFormState by vm.importState.collectAsStateWithLifecycle()
    var bundlePath by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Configuration Import",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 20.dp, bottom = 16.dp, end = 20.dp)
        )
        if (importFormState.state in listOf(ImportState.State.WAITING_BUNDLE_LOAD, ImportState.State.BUNDLE_LOAD_FAILED, ImportState.State.BUNDLE_LOADING)) {
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
                    if (it.isNotBlank()) vm.onEvent(GeneralSettingsEvent.ConfigurationBundleLoadStarted(it))
                }
                if (importFormState.state == ImportState.State.BUNDLE_LOAD_FAILED) {
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
                    ImportBrokerContainer(broker) { vm.onEvent(it) }
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
                onClick = { vm.onEvent(GeneralSettingsEvent.ToggleConfigurationImportForm) }) { Text("Cancel") }
            Button(
                modifier = Modifier.weight(1f),
                enabled = importFormState.state == ImportState.State.WAITING_IMPORT_CONFIRMATION,
                onClick = { vm.onEvent(GeneralSettingsEvent.ConfigurationBundleImportStarted) }
            ) {
                Text("Import")
            }
        }
    }
}

@Composable
fun ImportBrokerContainer(broker: BrokerConfiguration, onEvent: (GeneralSettingsEvent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
        shape = RoundedCornerShape(24.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(broker.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(5.dp))
            Text(Utils.abbreviateMiddle(broker.url, 32), style = MaterialTheme.typography.labelSmall)
            Text(if (broker.authenticationUser() == null) "Unauthenticated" else "${broker.authenticationUser()}/${broker.authenticationPassword()}", style = MaterialTheme.typography.labelSmall)
            Text("${broker.topics.size} topics", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(15.dp))
            Text("ClientId: ${broker.clientId}", style = MaterialTheme.typography.labelSmall)
            Text("KeepAlive: ${broker.keepAliveInterval}s", style = MaterialTheme.typography.labelSmall)
            Text("MaxReconnects: ${broker.reconnectAttempts}", style = MaterialTheme.typography.labelSmall)
            if (expanded) {
                Spacer(Modifier.height(20.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) { broker.topics.forEach { ImportTopicContainer(it) } }
            }
        }
    }
}

@Composable
fun ExportBrokerContainer(
    exportFormState: ExportState,
    broker: BrokerEntity,
    topics: List<TopicEntity>,
    onEvent: (GeneralSettingsEvent) -> Unit
) {
    val filteredTopics = topics.filter { it.brokerId == broker.id }
    val included = exportFormState.configuration.brokers.find { it.address() == broker.address() } != null
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
        shape = RoundedCornerShape(24.dp),
        onClick = { expanded = if (!included) false else !expanded }
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
                            "${if (broker.connectionType == ConnectionType.TCP) "tcp://" else "ssl://"}${broker.ip}:${broker.port}",
                            32
                        ), style = MaterialTheme.typography.labelSmall
                    )
                    Text(if (broker.user == null) "Unauthenticated" else "${broker.user}/${broker.password}", style = MaterialTheme.typography.labelSmall)
                    Text("${filteredTopics.size} topics", style = MaterialTheme.typography.labelSmall)
                }
                Switch(
                    checked = included,
                    onCheckedChange = {
                        if (!it) expanded = false
                        onEvent(GeneralSettingsEvent.ToggleExportBundleBroker(broker))
                    }
                )
            }
            Spacer(Modifier.height(15.dp))
            Text("ClientId: ${broker.clientId}", style = MaterialTheme.typography.labelSmall)
            Text("KeepAlive: ${broker.keepAliveInterval}s", style = MaterialTheme.typography.labelSmall)
            Text("MaxReconnects: ${broker.reconnectAttempts}", style = MaterialTheme.typography.labelSmall)
            if (expanded) {
                Spacer(Modifier.height(20.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredTopics.forEach { topic -> ExportTopicContainer(exportFormState, topic, broker, onEvent) }
                }
            }
        }
    }
}

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
            Text("HighPriority: ${topic.highPriority}", style = MaterialTheme.typography.labelSmall)
            Text("NotificationBody: " + (topic.notificationDisplaySettings?.replaceFirst("b@", "")?.ifBlank { "Full Payload" } ?: "Empty"), style = MaterialTheme.typography.labelSmall)
            Text("NotificationSoundText: ${topic.notificationSoundText?.ifBlank { "-" } ?: "-"}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ExportTopicContainer(
    exportFormState: ExportState,
    topic: TopicEntity,
    broker: BrokerEntity,
    onEvent: (GeneralSettingsEvent) -> Unit
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
                    onCheckedChange = { onEvent(GeneralSettingsEvent.ToggleExportBundleTopic(topic)) }
                )
            }
            Spacer(Modifier.height(15.dp))
            Text("HighPriority: ${topic.highPriority}", style = MaterialTheme.typography.labelSmall)
            Text("NotificationBody: " + (topic.payloadContent?.replaceFirst("b@", "")?.ifBlank { "Full Payload" } ?: "Empty"), style = MaterialTheme.typography.labelSmall)
            Text("NotificationSoundText: ${topic.notificationSoundText?.ifBlank { "-" } ?: "-"}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun SettingActionCard(title: String, icon: ImageVector, onClick: () -> Unit, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                    onClick = { onClick() }
                ) {
                    Icon(modifier = Modifier.size(16.dp), imageVector = icon, contentDescription = null)
                }
            }
            content()
        }
    }
}
