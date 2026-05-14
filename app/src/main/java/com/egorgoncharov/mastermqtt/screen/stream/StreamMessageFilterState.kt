package com.egorgoncharov.mastermqtt.screen.stream

import com.egorgoncharov.mastermqtt.ui.components.FormFieldState
import com.egorgoncharov.mastermqtt.ui.components.FormState
import java.time.LocalDate

data class StreamMessagesFilterState(
    val minDatetime: FormFieldState<Long?> = FormFieldState(null),
    val maxDatetime: FormFieldState<Long?> = FormFieldState(null),
    val query: FormFieldState<String?> = FormFieldState(null),
    val timeRangeMinDate: FormFieldState<LocalDate?> = FormFieldState(null),
    val timeRangeMaxDate: FormFieldState<LocalDate?> = FormFieldState(null),
    val timeRangeStartQuarter: FormFieldState<Int> = FormFieldState(0),
    val timeRangeEndQuarter: FormFieldState<Int> = FormFieldState(96)
) : FormState {
    override fun valid(): Boolean = listOf(minDatetime, maxDatetime, query).all { it.errorMsg == null }
}
