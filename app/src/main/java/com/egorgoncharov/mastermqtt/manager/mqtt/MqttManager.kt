package com.egorgoncharov.mastermqtt.manager.mqtt

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.egorgoncharov.mastermqtt.manager.NotificationManager
import com.egorgoncharov.mastermqtt.manager.SoundManager
import com.egorgoncharov.mastermqtt.model.dao.BrokerDao
import com.egorgoncharov.mastermqtt.model.dao.MessageDao
import com.egorgoncharov.mastermqtt.model.dao.TopicDao
import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.entity.MessageEntity
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import com.egorgoncharov.mastermqtt.model.types.ConnectionType
import com.egorgoncharov.mastermqtt.model.types.MqttConnectionState
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.lifecycle.MqttDisconnectSource
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID
import java.util.concurrent.TimeUnit

open class MqttManager(
    protected val context: Context,
    protected val brokerDao: BrokerDao,
    protected val topicDao: TopicDao,
    protected val messageDao: MessageDao,
    protected val notificationManager: NotificationManager,
    protected val soundManager: SoundManager
) {
    protected val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    protected val clients = MutableStateFlow(mutableMapOf<String, MqttConnection>())
    protected val subscriptionsSyncJobs = mutableMapOf<String, Job>()
    protected val messageChannel = Channel<MqttMessageTask>(1000)
    protected var brokersSyncJob: Job? = null

    val clientsFlow = clients.asStateFlow()

    fun start() {
        scope.launch { for (task in messageChannel) processMessage(task.broker, task.topicId, task.publish) }
        brokersSyncJob = syncBrokers()
    }

    fun shutdown() {
        messageChannel.cancel()
        brokersSyncJob?.cancel()
        subscriptionsSyncJobs.values.forEach { it.cancel() }
        clients.value.values.forEach { deregister(it.broker) }
    }

    fun started() = brokersSyncJob != null

    fun register(broker: BrokerEntity, connectInstantly: Boolean = false, then: (() -> Unit)? = null) {
        val registerFn = {
            val client = buildConnection(broker)
            clients.update { current ->
                (current + (broker.id to MqttConnection(
                    broker,
                    client,
                    MqttConnectionState.DISCONNECTED
                ))).toMutableMap()
            }
            if (connectInstantly) connect(broker)
        }
        if (clients.value.containsKey(broker.id)) {
            deregister(broker) { registerFn(); then?.invoke() }
        } else {
            registerFn()
            then?.invoke()
        }
    }

    fun deregister(broker: BrokerEntity, then: (() -> Unit)? = null) {
        if (!clients.value.containsKey(broker.id)) return
        val remove = {
            clients.update { (it - broker.id).toMutableMap() }
            subscriptionsSyncJobs.remove(broker.id)?.cancel()
        }
        if (clients.value[broker.id]?.state in listOf(MqttConnectionState.RECONNECTING, MqttConnectionState.CONNECTED)) {
            disconnect(broker, updateState = false) { remove(); then?.invoke() }
        } else {
            remove()
            then?.invoke()
        }
    }

    fun connect(broker: BrokerEntity, then: (() -> Unit)? = null) {
        if (!clients.value.containsKey(broker.id)) return
        if (clients.value[broker.id]?.state in listOf(
                MqttConnectionState.CONNECTED,
                MqttConnectionState.INTERMEDIATE,
                MqttConnectionState.RECONNECTING
            )
        ) return
        updateClient(broker.id) { it.copy(state = MqttConnectionState.INTERMEDIATE) }
        clients.value[broker.id]!!.client.connectWith()
            .keepAlive(1.coerceAtLeast(broker.keepAliveInterval))
            .restrictions()
            .receiveMaximum(Short.MAX_VALUE - 1)
            .applyRestrictions()
            .sessionExpiryInterval(0.coerceAtLeast(broker.sessionExpiryInterval ?: 0).toLong())
            .cleanStart(broker.cleanStart)
            .send()
            .handle { _, throwable ->
                updateClient(broker.id) { it.copy(state = if (throwable == null) MqttConnectionState.CONNECTED else MqttConnectionState.DISCONNECTED_FAILED) }
                if (throwable == null && !subscriptionsSyncJobs.containsKey(broker.id)) subscriptionsSyncJobs[broker.id] =
                    syncTopics(clients.value[broker.id]!!)
                if (then != null) then()
            }
        if (!broker.connected) scope.launch { brokerDao.save(broker.copy(connected = true)) }
    }

    fun disconnect(broker: BrokerEntity, updateState: Boolean = true, then: (() -> Unit)? = null) {
        if (!clients.value.containsKey(broker.id)) return
        val connection = clients.value[broker.id]!!
        if (connection.state !in listOf(MqttConnectionState.CONNECTED, MqttConnectionState.RECONNECTING)) return
        scope.launch {
            try {
                withContext(NonCancellable) {
                    connection.client.disconnect().whenComplete { _, _ -> then?.invoke() }
                    if (broker.connected && updateState) brokerDao.save(broker.copy(connected = false))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("CheckResult")
    protected fun buildConnection(broker: BrokerEntity): Mqtt5AsyncClient {
        var retryCount = 0
        return Mqtt5Client.builder()
            .identifier(broker.clientId)
            .serverHost(broker.host)
            .serverPort(broker.port)
            .transportConfig()
            .socketConnectTimeout(3, TimeUnit.SECONDS)
            .mqttConnectTimeout(3, TimeUnit.SECONDS)
            .applyTransportConfig()
            .automaticReconnect()
            .initialDelay(1.coerceAtLeast(broker.reconnectInterval).toLong(), TimeUnit.SECONDS)
            .maxDelay(1.coerceAtLeast(broker.reconnectInterval).toLong(), TimeUnit.SECONDS)
            .applyAutomaticReconnect()
            .addDisconnectedListener { context ->
                when {
                    context.source == MqttDisconnectSource.USER -> {
                        updateClient(broker.id) { it.copy(state = MqttConnectionState.DISCONNECTED) }
                        context.reconnector.reconnect(false)
                    }
                    else -> {
                        updateClient(broker.id) { it.copy(state = MqttConnectionState.RECONNECTING) }
                        retryCount++
                        if (broker.reconnectAttempts == null || retryCount <= broker.reconnectAttempts) {
                            context.reconnector.reconnect(true)
                        } else {
                            updateClient(broker.id) { it.copy(state = MqttConnectionState.DISCONNECTED_FAILED) }
                            context.reconnector.reconnect(false)
                            retryCount = 0
                        }
                    }
                }
            }
            .addConnectedListener {
                if (clients.value[broker.id]?.state != MqttConnectionState.CONNECTED) {
                    updateClient(broker.id) { it.copy(state = MqttConnectionState.CONNECTED) }
                }
                retryCount = 0
            }
            .apply {
                if (broker.connectionType == ConnectionType.SSL) sslWithDefaultConfig()
                if (!broker.authUser.isNullOrBlank()) {
                    simpleAuth()
                        .username(broker.authUser)
                        .password(broker.authPassword?.toByteArray() ?: arrayOf<Byte>().toByteArray())
                        .applySimpleAuth()
                }
            }
            .buildAsync()
    }

    protected fun syncBrokers(): Job {
        val sync = { brokers: List<BrokerEntity> ->
            val ids = brokers.map { it.id }
            clients.value.values.filter { it.broker.id !in ids }.forEach { connection -> deregister(connection.broker) }
            brokers.forEach { broker ->
                val existing = clients.value[broker.id]
                if (existing == null) {
                    register(broker, broker.connected)
                } else {
                    val configChanged = existing.broker.copy(connected = broker.connected) != broker
                    val statusChanged = existing.broker.connected != broker.connected
                    if (configChanged) {
                        val wasActive = existing.state == MqttConnectionState.CONNECTED
                        register(broker, broker.connected || wasActive)
                    } else if (statusChanged) {
                        if (broker.connected) connect(broker) else disconnect(broker)
                        updateClient(broker.id) { it.copy(broker = broker) }
                    } else {
                        updateClient(broker.id) { it.copy(broker = broker) }
                    }
                }
            }
        }
        return scope.launch { brokerDao.streamBrokers().collect { sync(it) } }
    }

    protected fun syncTopics(connection: MqttConnection): Job {
        val sync = { topics: List<TopicEntity> ->
            val currentIds = topics.map { it.id }.toSet()
            val toRemove = connection.subscriptions.filter { it.id !in currentIds }
            toRemove.forEach { topic ->
                connection.client.unsubscribeWith().topicFilter(topic.topic).send()
                connection.subscriptions.remove(topic)
            }
            topics.forEach { topic ->
                val cachedIdx = connection.subscriptions.indexOfFirst { it.id == topic.id }
                if (cachedIdx == -1) {
                    connection.subscriptions.add(topic)
                    if (topic.enabled) subscribeToTopic(connection, topic)
                } else {
                    val cached = connection.subscriptions[cachedIdx]
                    val statusChanged = cached.enabled != topic.enabled
                    val topicChanged = cached.topic != topic.topic
                    if (topicChanged && cached.enabled) connection.client.unsubscribeWith().topicFilter(cached.topic).send()
                    if (topic.enabled && (statusChanged || topicChanged)) subscribeToTopic(connection, topic)
                    else if (!topic.enabled && statusChanged) connection.client.unsubscribeWith().topicFilter(topic.topic).send()
                    connection.subscriptions[cachedIdx] = topic
                }
            }
            updateClient(connection.broker.id) {
                it.copy(subscriptions = connection.subscriptions.toMutableList())
            }
        }
        return scope.launch {
            topicDao.streamTopicsByBroker(connection.broker.id).collect { sync(it) }
        }
    }

    private fun subscribeToTopic(connection: MqttConnection, topic: TopicEntity) {
        connection.client.subscribeWith()
            .topicFilter(topic.topic)
            .qos(MqttQos.fromCode(topic.qos) ?: MqttQos.EXACTLY_ONCE)
            .callback { publish -> handleIncomingMessage(connection.broker, topic.id, publish) }
            .send()
    }

    protected fun handleIncomingMessage(
        broker: BrokerEntity,
        id: String,
        publish: Mqtt5Publish
    ) {
        val result = messageChannel.trySend(MqttMessageTask(broker, id, publish))
        if (result.isFailure) Log.e("MQTTManager", "Failed to process incoming message")
    }

    protected suspend fun processMessage(broker: BrokerEntity, id: String, publish: Mqtt5Publish) {
        val topic = topicDao.findById(id) ?: return
        val payload = publish.payloadAsBytes
        val processed = processPayload(payload, topic.payloadContent)
        var processedDescription = ""
        processed.forEach { (path, value) -> processedDescription += "${if (path.isNotEmpty()) "$path : " else ""}$value\n" }
        if (topic.payloadContent == null) processedDescription = ""
        if (processedDescription.endsWith("\n")) processedDescription = processedDescription.take(processedDescription.length - 1)
        val notification = MessageEntity(
            UUID.randomUUID().toString(),
            topic.id,
            topic.name,
            processedDescription,
            String(payload),
            System.currentTimeMillis()
        )
        messageDao.save(notification)
        notificationManager.show(broker, topic, processedDescription)
        soundManager.alert(topic, String(payload))
    }

    protected fun processPayload(payload: ByteArray, pattern: String?): Map<String, String?> {
        var payloadStr = String(payload)
        if (pattern?.startsWith("b@") == true) {
            payloadStr = payload.joinToString("") { "%02x".format(it) }
            pattern.removeRange(0, 2)
        }
        val root = try {
            json.parseToJsonElement(payloadStr)
        } catch (_: Exception) {
            null
        }
        if (pattern.isNullOrBlank() || root == null) return mapOf("" to payloadStr)
        val extractedValues = mutableMapOf<String, String?>()
        pattern.split(",").forEach { path ->
            val trimmed = path.trim()
            val segments = trimmed.split(Regex("[.\\[\\]]")).filter { it.isNotBlank() }
            if (segments.isEmpty()) return@forEach
            var current: JsonElement? = root
            for (segment in segments) {
                current = when (current) {
                    is JsonObject -> current[segment]
                    is JsonArray -> segment.toIntOrNull()?.let { current.getOrNull(it) }
                    else -> null
                }
                if (current == null) break
            }
            val key =
                if (segments.size - 1 > 0) "...<${segments.size - 1}>${segments.last()}" else segments.last()
            extractedValues[key] = when (current) {
                is JsonPrimitive -> current.content
                null -> null
                else -> current.toString()
            }
        }
        return extractedValues.ifEmpty { mapOf("" to payloadStr) }
    }

    protected fun updateClient(id: String, transform: (MqttConnection) -> MqttConnection) {
        clients.update { current ->
            val connection = current[id] ?: return@update current
            (current + (id to transform(connection))).toMutableMap()
        }
    }
}