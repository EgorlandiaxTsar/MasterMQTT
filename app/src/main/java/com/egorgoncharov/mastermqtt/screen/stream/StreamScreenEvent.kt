package com.egorgoncharov.mastermqtt.screen.stream

import com.egorgoncharov.mastermqtt.model.entity.TopicEntity

sealed interface StreamScreenEvent {
    data class MinDatetimeFilterChanged(val min: Long?) : StreamScreenEvent
    data class MaxDatetimeFilterChanged(val max: Long?) : StreamScreenEvent
    data class TextSearchFilterChanged(val query: String?) : StreamScreenEvent

    data class
    SelectedStreamChanged(val streamSource: TopicEntity?) : StreamScreenEvent

    object ToggleStreamDisplayOriginalMessageOption : StreamScreenEvent

    object ToggleStreamClearDialog : StreamScreenEvent
    object StreamCleared : StreamScreenEvent

    data class DeepLinkBoundChanged(val deepLinkBound: Boolean) : StreamScreenEvent
    data class BrokersViewBoundChanged(val brokersViewBound: Boolean) : StreamScreenEvent
}
