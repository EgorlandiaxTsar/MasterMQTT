package com.egorgoncharov.mastermqtt.ui

import androidx.compose.ui.graphics.vector.ImageVector

data class ToolbarButton(
    val text: String? = null,
    val icon: ImageVector? = null,
    val primary: Boolean = false,
    val onClick: () -> Unit
)
