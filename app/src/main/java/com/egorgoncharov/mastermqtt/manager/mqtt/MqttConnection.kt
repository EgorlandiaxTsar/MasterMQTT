package com.egorgoncharov.mastermqtt.manager.mqtt

import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import com.egorgoncharov.mastermqtt.model.types.MqttConnectionState
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient

data class MqttConnection(
    val broker: BrokerEntity,
    val client: Mqtt5AsyncClient,
    val state: MqttConnectionState,
    val subscriptions: MutableList<TopicEntity> = mutableListOf()
)