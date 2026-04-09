package com.egorgoncharov.mastermqtt.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

val nameRegex = """^[A-Za-z0-9\u0400-\u04FF_\-,./*+\[\]{}()?!@#&]{3,32}$""".toRegex()
val ipRegex = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$".toRegex()
val hostRegex = """^([a-z0-9]+(-[a-z0-9]+)*\.)+[a-z]{2,}$""".toRegex(RegexOption.IGNORE_CASE)
val jsonPathRegex = """([a-zA-Z_]\w*(?:\[\d+])?(?:\.[a-zA-Z_]\w*(?:\[\d+])?)*)""".toRegex(RegexOption.IGNORE_CASE)

interface FormState {
    fun valid(): Boolean
}

interface ReferencableFormState<T> {
    val reference: T?
}

interface DynamicFormState {
    val visible: Boolean
}

abstract class EntityManagingFormState<T> : FormState, DynamicFormState, ReferencableFormState<T>

data class FormFieldState<T>(
    val value: T,
    val errorMsg: String? = null,
)

@Composable
fun FormIsland(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

fun validateNumericalInput(
    num: String,
    required: Boolean,
    min: Double = Double.MIN_VALUE,
    max: Double = Double.MAX_VALUE
): String? {
    var errorMsg: String? = ""
    if (num.isBlank() && required) {
        errorMsg += "Required field"
    } else if (num.isBlank() && !required) return null
    else if (!num.isDigitsOnly()) errorMsg += "Digits only field"
    else if (num.toInt()
            .toDouble() !in min..max
    ) errorMsg += "Not in required range from $min to $max"
    else errorMsg = null
    return errorMsg
}

fun <T, E : FormState> MutableStateFlow<E>.update(
    selector: (E) -> FormFieldState<T>,
    new: T,
    errorFn: (T) -> String?,
    reducer: E.(FormFieldState<T>) -> E
) {
    this.update { state -> state.reducer(selector(state).copy(value = new, errorMsg = errorFn(new))) }
}
