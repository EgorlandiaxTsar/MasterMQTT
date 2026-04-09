package com.egorgoncharov.mastermqtt.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.egorgoncharov.mastermqtt.ui.components.SettingsTopBar

@Composable
fun GeneralSettingsScreen(navController: NavHostController) {
    Column(Modifier.fillMaxWidth()) {
        SettingsTopBar(navController, "General Settings")
    }
}
