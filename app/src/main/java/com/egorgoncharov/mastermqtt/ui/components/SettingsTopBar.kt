package com.egorgoncharov.mastermqtt.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.egorgoncharov.mastermqtt.NavRoute

class SettingsTopBarScope {
    private val actionItems = mutableListOf<@Composable () -> Unit>()

    fun button(onClick: () -> Unit, content: @Composable () -> Unit) {
        actionItems.add { Box(Modifier.clickable(onClick = { onClick() }, interactionSource = null, indication = null)) { content() } }
    }

    internal fun getItems(): List<@Composable () -> Unit> = actionItems
}

@Composable
fun SettingsTopBar(
    navHostController: NavHostController,
    title: String,
    actions: SettingsTopBarScope.() -> Unit = {}
) {
    BackHandler {
        if (!navHostController.popBackStack()) {
            navHostController.navigate(NavRoute.Stream.route) {
                popUpTo(0)
            }
        }
    }
    val scope = SettingsTopBarScope()
    scope.actions()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(modifier = Modifier
            .size(20.dp)
            .clickable(interactionSource = null, indication = null, onClick = { navHostController.navigate(NavRoute.Stream.route) }), imageVector = Icons.Filled.ArrowBack, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) { items(scope.getItems()) { it() } }
    }
}
