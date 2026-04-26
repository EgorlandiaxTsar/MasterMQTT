package com.egorgoncharov.mastermqtt.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

open class DialogWindowState(
    open val title: String = "",
    open val message: String? = null,
    open val optionAText: String = "",
    open val optionBText: String = "",
    open val onActionA: () -> Unit = {},
    open val onActionB: () -> Unit = {},
    open val onDismiss: () -> Unit = {},
    open val visible: Boolean = false
)

class ConfirmationWindowState(
    override val title: String = "Confirm action",
    override val message: String? = null,
    override val optionAText: String = "Cancel",
    override val optionBText: String = "Proceed",
    onConfirm: () -> Unit = {},
    onDecline: () -> Unit = {},
    override val visible: Boolean = false
) : DialogWindowState(
    title = title,
    message = message,
    onActionA = onDecline,
    onActionB = onConfirm,
    onDismiss = onDecline,
    visible = visible
)

@Composable
fun DialogWindow(state: DialogWindowState) {
    if (state.visible) {
        AlertDialog(
            onDismissRequest = { state.onDismiss() },
            title = { Text(state.title) },
            text = { state.message?.let { Text(it) } },
            confirmButton = {
                TextButton(onClick = { state.onActionB() }) { Text(state.optionBText) }
            },
            dismissButton = {
                TextButton(onClick = { state.onActionA() }) { Text(state.optionAText) }
            }
        )
    }
}
