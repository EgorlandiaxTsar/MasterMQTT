package com.egorgoncharov.mastermqtt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamDateTimeFilter(
    modifier: Modifier = Modifier,
    onStartDateSelected: (LocalDate?) -> Unit = {},
    onEndDateSelected: (LocalDate?) -> Unit = {},
    onTimeRangeChanged: (startQuarter: Int, endQuarter: Int) -> Unit = { _, _ -> }
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var sliderRange by remember { mutableStateOf(0f..96f) }
    val startPickerState = rememberDatePickerState()
    val endPickerState = rememberDatePickerState()
    val labelFmt = remember { DateTimeFormatter.ofPattern("d MMM") }
    val isSliderEnabled = remember(startDate, endDate) {
        when {
            startDate == null && endDate == null -> true
            startDate != null && endDate != null -> startDate == endDate
            else -> false
        }
    }

    LaunchedEffect(isSliderEnabled) {
        if (!isSliderEnabled) {
            sliderRange = 0f..96f
            onTimeRangeChanged(0, 96)
        }
    }
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant),
        shape = RoundedCornerShape(5.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DateSelectorColumn(
                date = startDate,
                label = startDate?.format(labelFmt),
                onIconClick = { showStartPicker = true },
                onClear = {
                    startDate = null
                    onStartDateSelected(null)
                },
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RangeSlider(
                    value = sliderRange,
                    onValueChange = { sliderRange = it },
                    valueRange = 0f..96f,
                    steps = 95,
                    enabled = isSliderEnabled,
                    onValueChangeFinished = {
                        onTimeRangeChanged(
                            sliderRange.start.toInt(),
                            sliderRange.endInclusive.toInt(),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )
                HourTickRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp), // align with slider track
                    tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    sliderStart = sliderRange.start,
                    sliderEnd = sliderRange.endInclusive,
                )
            }
            DateSelectorColumn(
                date = endDate,
                label = endDate?.format(labelFmt),
                onIconClick = { showEndPicker = true },
                onClear = {
                    endDate = null
                    onEndDateSelected(null)
                },
            )
        }
    }
    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = startPickerState.selectedDateMillis?.toLocalDate()
                    onStartDateSelected(startDate)
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = startPickerState)
        }
    }
    if (showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDate = endPickerState.selectedDateMillis?.toLocalDate()
                    onEndDateSelected(endDate)
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = endPickerState)
        }
    }
}

@Composable
private fun DateSelectorColumn(
    date: LocalDate?,
    label: String?,
    onIconClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(IntrinsicSize.Min),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconButton(
            onClick = onIconClick,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Pick date",
                tint = if (date != null)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        if (date != null && label != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 2.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(2.dp))
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear date",
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun HourTickRow(
    modifier: Modifier = Modifier,
    tickColor: Color,
    labelColor: Color,
    sliderStart: Float,
    sliderEnd: Float,
) {
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.75,
        textAlign = TextAlign.Center,
    )
    val keyHours = setOf(0, 6, 12, 18, 24)
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
        ) {
            val w = size.width
            val totalQuarters = 96

            for (q in 0..totalQuarters) {
                val x = w * q / totalQuarters.toFloat()
                val isHourBoundary = q % 4 == 0
                val tickH: Dp = if (isHourBoundary) 8.dp else 4.dp
                val alpha = if (q in sliderStart.toInt()..sliderEnd.toInt()) 1f else 0.35f

                drawLine(
                    color = tickColor.copy(alpha = alpha),
                    start = Offset(x, 0f),
                    end = Offset(x, tickH.toPx()),
                    strokeWidth = if (isHourBoundary) 1.5.dp.toPx() else 1.dp.toPx(),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            keyHours.forEach { h ->
                Text(
                    text = if (h < 10) "0$h" else "$h",
                    style = labelStyle,
                    color = labelColor,
                )
            }
        }
    }
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
