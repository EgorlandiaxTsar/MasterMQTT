package com.egorgoncharov.mastermqtt.screen.stream

import com.egorgoncharov.mastermqtt.model.entity.TopicEntity
import java.time.LocalDate

sealed interface StreamScreenEvent {
    data class MinDatetimeFilterChanged(val min: Long?) : StreamScreenEvent
    data class MaxDatetimeFilterChanged(val max: Long?) : StreamScreenEvent
    data class TextSearchFilterChanged(val query: String?) : StreamScreenEvent
    data class TimeRangeMinDateChanged(val minDate: LocalDate?) : StreamScreenEvent
    data class TimeRangeMaxDateChanged(val maxDate: LocalDate?) : StreamScreenEvent
    data class TimeRangeStartQuarterChanged(val startQuarter: Int) : StreamScreenEvent
    data class TimeRangeEndQuarterChanged(val endQuarter: Int) : StreamScreenEvent

    data class SelectedStreamChanged(val streamSource: TopicEntity?) : StreamScreenEvent

    object ToggleStreamDisplayOriginalMessageOption : StreamScreenEvent

    object ToggleStreamClearDialog : StreamScreenEvent
    object StreamCleared : StreamScreenEvent

    data class DeepLinkBoundChanged(val deepLinkBound: Boolean) : StreamScreenEvent
    data class BrokersViewBoundChanged(val brokersViewBound: Boolean) : StreamScreenEvent
}
