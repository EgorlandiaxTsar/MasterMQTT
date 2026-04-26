package com.egorgoncharov.mastermqtt.screen.stream

import com.egorgoncharov.mastermqtt.ui.components.FormFieldState
import com.egorgoncharov.mastermqtt.ui.components.FormState

data class StreamMessagesFilterState(
    val minDatetime: FormFieldState<Long?> = FormFieldState(null),
    val maxDatetime: FormFieldState<Long?> = FormFieldState(null),
    val query: FormFieldState<String?> = FormFieldState(null)
) : FormState {
    override fun valid(): Boolean = listOf(minDatetime, maxDatetime, query).all { it.errorMsg == null }
}
