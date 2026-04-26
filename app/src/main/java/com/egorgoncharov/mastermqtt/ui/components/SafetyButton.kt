package com.egorgoncharov.mastermqtt.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SafetyButton(
    onConfirmedClick: () -> Unit,
    interval: Int = 750,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val completed = try {
                            withTimeout(interval.toLong()) {
                                tryAwaitRelease()
                                false
                            }
                        } catch (_: TimeoutCancellationException) {
                            true
                        }
                        if (completed) {
                            onConfirmedClick()
                        }
                        isPressed = false
                    }
                )
            },
        colors = CardDefaults.cardColors(containerColor = if (isPressed) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, contentColor = if (isPressed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant),
        shape = RoundedCornerShape(100)
    ) {
        Box(Modifier.padding(8.dp)) { content() }
    }
}
