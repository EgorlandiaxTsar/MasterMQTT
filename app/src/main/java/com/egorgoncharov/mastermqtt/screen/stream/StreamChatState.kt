package com.egorgoncharov.mastermqtt.screen.stream

import com.egorgoncharov.mastermqtt.model.entity.TopicEntity

data class StreamChatState(
    val selected: TopicEntity? = null,
    val showProcessedContent: Boolean = true
)
