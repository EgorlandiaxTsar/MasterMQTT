package com.egorgoncharov.mastermqtt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ItemProperty(name: String? = null, value: String, icon: ImageVector, content: (@Composable () -> Unit)? = null) {
    Card(
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 2.dp, horizontal = 10.dp)
                .heightIn(min = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (content == null) {
                Icon(modifier = Modifier.size(12.dp), imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                if (name != null) Text(
                    name,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Light),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
            } else {
                content()
            }
        }
    }
}

@Composable
fun ItemAction(icon: ImageVector, onClick: () -> Unit, content: (@Composable () -> Unit)? = null) {
    IconButton(
        modifier = Modifier.size(32.dp),
        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondary),
        onClick = { onClick() }
    ) {
        if (content == null) {
            Icon(modifier = Modifier.size(16.dp), imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
        } else {
            content()
        }
    }
}

@Composable
fun Empty(message: String = "Nothing found") {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Filled.Air,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(Modifier.width(10.dp))
            Text(
                message,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
fun Error(message: String = "Error occurred") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .background(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(100)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(message, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
