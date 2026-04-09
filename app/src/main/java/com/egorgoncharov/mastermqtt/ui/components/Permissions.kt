package com.egorgoncharov.mastermqtt.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.egorgoncharov.mastermqtt.manager.PermissionManager

@Composable
fun PermissionRequestDialog(
    permissionName: String,
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permission Required") },
        text = { Text("The $permissionName permission is required for this feature to work properly. Please enable it in the app settings.") },
        confirmButton = {
            TextButton(onClick = onGoToSettings) { Text("Open Settings") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun rememberPermissionAction(
    permission: String,
    onResult: (Boolean) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val manager = remember { PermissionManager(context) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onResult(isGranted)
    }
    return {
        if (manager.isPermissionGranted(permission)) {
            onResult(true)
        } else {
            launcher.launch(permission)
        }
    }
}
