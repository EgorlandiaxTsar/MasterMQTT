package com.egorgoncharov.mastermqtt.configuration

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
            url = broker.address(true),
            authentication = if (broker.authUser == null) null else "${broker.authUser}${Char(0x1F)}${broker.authPassword}",
            clientId = UUID.randomUUID().toString().take(6), /* broker.clientId */
            keepAliveInterval = broker.keepAliveInterval,
            alertWhenDisconnected = broker.alertWhenDisconnected,
            cleanStart = broker.cleanStart,
            reconnectAttempts = broker.reconnectAttempts,
            reconnectInterval = broker.reconnectInterval,
            sessionExpiryInterval = broker.sessionExpiryInterval,
            connected = broker.connected,
            topics = topicDao.findByBroker(broker.id).map { topicFromEntityToConfiguration(it, broker) }.toMutableList()
        )
    }

    fun brokerFromConfigurationToEntity(id: String = UUID.randomUUID().toString(), brokerConfiguration: BrokerConfiguration): BrokerEntity {
        return BrokerEntity(
            id = id,
            name = brokerConfiguration.name,
            host = brokerConfiguration.host(),
            port = brokerConfiguration.port(),
            authUser = brokerConfiguration.authenticationUser(),
            authPassword = brokerConfiguration.authenticationPassword(),
            connectionType = brokerConfiguration.connectionType(),
            alertWhenDisconnected = brokerConfiguration.alertWhenDisconnected,
            clientId = brokerConfiguration.clientId,
            keepAliveInterval = brokerConfiguration.keepAliveInterval,
            cleanStart = brokerConfiguration.cleanStart,
            reconnectAttempts = brokerConfiguration.reconnectAttempts,
            reconnectInterval = brokerConfiguration.reconnectInterval,
            sessionExpiryInterval = brokerConfiguration.sessionExpiryInterval,
            connected = brokerConfiguration.connected,
            displayIndex = 0
        )
    }

    fun topicFromEntityToConfiguration(topic: TopicEntity, broker: BrokerEntity): TopicConfiguration {
        if (topic.brokerId != broker.id) throw IllegalArgumentException("Topic's broker ID and passed broker ID must be equal. Received ${topic.brokerId} as topic's broker ID and ${broker.id} as broker's id")
        return TopicConfiguration(
            brokerAddress = broker.address(true),
            name = topic.name,
            topic = topic.topic,
            qos = topic.qos,
            enabled = topic.enabled,
            payloadContent = topic.payloadContent,
            highPriority = topic.highPriority,
            ignoreBedTime = topic.ignoreBedTime,
            notificationSoundText = topic.notificationSoundText,
            notificationSoundPath = topic.notificationSoundPath,
            notificationSoundLevel = topic.notificationSoundLevel,
            messageAge = topic.messageAge
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
            qos = topicConfiguration.qos,
            enabled = topicConfiguration.enabled,
            payloadContent = topicConfiguration.payloadContent,
            highPriority = topicConfiguration.highPriority,
            ignoreBedTime = topicConfiguration.ignoreBedTime,
            notificationSoundText = topicConfiguration.notificationSoundText,
            notificationSoundPath = topicConfiguration.notificationSoundPath,
            notificationSoundLevel = topicConfiguration.notificationSoundLevel,
            messageAge = topicConfiguration.messageAge,
            displayIndex = 0,
            lastOpened = System.currentTimeMillis()
        )
    }
}