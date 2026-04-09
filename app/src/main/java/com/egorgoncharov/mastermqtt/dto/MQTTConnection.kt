package com.egorgoncharov.mastermqtt.dto

import com.egorgoncharov.mastermqtt.dto.db.MQTTConnectionState
import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient

data class MQTTConnection(
    val broker: BrokerEntity,
    val client: Mqtt5AsyncClient,
    val state: MQTTConnectionState,
    val subscriptions: MutableList<TopicEntity> = mutableListOf()
)