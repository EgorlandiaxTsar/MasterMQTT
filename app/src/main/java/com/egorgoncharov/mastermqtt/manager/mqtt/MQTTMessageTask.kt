package com.egorgoncharov.mastermqtt.manager.mqtt

import com.egorgoncharov.mastermqtt.model.entity.BrokerEntity
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish

data class MQTTMessageTask(
    val broker: BrokerEntity,
    val topicId: String,
    val publish: Mqtt5Publish
)
