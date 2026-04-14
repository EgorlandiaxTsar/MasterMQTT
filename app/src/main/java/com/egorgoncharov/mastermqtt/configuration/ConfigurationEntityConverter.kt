package com.egorgoncharov.mastermqtt.configuration

import androidx.compose.ui.graphics.Color
import com.egorgoncharov.mastermqtt.configuration.dto.BrokerConfiguration
import com.egorgoncharov.mastermqtt.configuration.dto.TopicConfiguration
import com.egorgoncharov.mastermqtt.model.dao.BrokerDao
import com.egorgoncharov.mastermqtt.model.dao.TopicDao
import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import java.util.UUID

class ConfigurationEntityConverter(private val brokerDao: BrokerDao, private val topicDao: TopicDao) {
    suspend fun brokerFromEntityToConfiguration(broker: BrokerEntity): BrokerConfiguration {
        return BrokerConfiguration(
            name = broker.name,
            clientId = broker.clientId,
            url = broker.address(true),
            authentication = if (broker.user == null) null else "${broker.user}<mastermqtt_splitpoint>${broker.password}",
            connected = broker.connected,
            keepAliveInterval = broker.keepAliveInterval,
            reconnectAttempts = broker.reconnectAttempts ?: 0,
            topics = topicDao.findByBroker(broker.id).map { topicFromEntityToConfiguration(it, broker) }.toMutableList()
        )
    }

    fun brokerFromConfigurationToEntity(id: String = UUID.randomUUID().toString(), brokerConfiguration: BrokerConfiguration): BrokerEntity {
        return BrokerEntity(
            id = id,
            name = brokerConfiguration.name,
            clientId = brokerConfiguration.clientId,
            connected = brokerConfiguration.connected,
            ip = brokerConfiguration.host(),
            port = brokerConfiguration.port(),
            user = brokerConfiguration.authenticationUser(),
            password = brokerConfiguration.authenticationPassword(),
            connectionType = brokerConfiguration.connectionType(),
            keepAliveInterval = brokerConfiguration.keepAliveInterval,
            reconnectAttempts = brokerConfiguration.reconnectAttempts,
            displayIndex = 0,
            removed = false
        )
    }

    fun topicFromEntityToConfiguration(topic: TopicEntity, broker: BrokerEntity): TopicConfiguration {
        if (topic.brokerId != broker.id) throw IllegalArgumentException("Topic's broker ID and passed broker ID must be equal. Received ${topic.brokerId} as topic's broker ID and ${broker.id} as broker's id")
        return TopicConfiguration(
            name = topic.name,
            brokerAddress = broker.address(true),
            topic = topic.topic,
            enabled = topic.enabled,
            notificationDisplaySettings = topic.payloadContent,
            notificationSoundPath = topic.notificationSoundPath,
            notificationSoundText = topic.notificationSoundText,
            highPriority = topic.highPriority
        )
    }

    suspend fun topicFromEntityToConfiguration(topic: TopicEntity): TopicConfiguration {
        val broker = brokerDao.findById(topic.brokerId) ?: throw IllegalArgumentException("Broker ID not found")
        return topicFromEntityToConfiguration(topic, broker)
    }

    fun topicFromConfigurationToEntity(id: String = UUID.randomUUID().toString(), brokerId: String, topicConfiguration: TopicConfiguration): TopicEntity {
        return TopicEntity(
            id = id,
            brokerId = brokerId,
            name = topicConfiguration.name,
            topic = topicConfiguration.topic,
            enabled = topicConfiguration.enabled,
            payloadContent = topicConfiguration.notificationDisplaySettings,
            notificationColor = Color.White,
            notificationIcon = "",
            notificationSoundPath = topicConfiguration.notificationSoundPath,
            notificationSoundText = topicConfiguration.notificationSoundText,
            highPriority = topicConfiguration.highPriority,
            displayIndex = 0,
            lastOpened = System.currentTimeMillis(),
            removed = false
        )
    }
}