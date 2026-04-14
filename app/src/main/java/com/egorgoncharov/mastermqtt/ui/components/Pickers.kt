package com.egorgoncharov.mastermqtt.ui.components

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.egorgoncharov.mastermqtt.Utils

@Composable
fun AudioPicker(selectedSound: String = "", onSelect: (path: String) -> Unit) {
    var soundPath by remember { mutableStateOf(selectedSound) }
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            uri?.let { onSelect(it.toString()); soundPath = it.toString() }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            onSelect(it.toString())
            soundPath = it.toString()
        }
    }

    Column {
        Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (soundPath.isBlank()) "Select Notification Sound" else "Change Sound")
        }
        if (soundPath.isNotBlank()) ChosenFileCard(soundPath, Icons.Filled.Audiotrack) { onSelect(""); soundPath = "" }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Choose Sound Type") },
                text = { Text("Where would you like to pick your sound from?") },
                confirmButton = {
                    TextButton(onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alert")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, soundPath.toUri().takeIf { soundPath.isNotEmpty() })
                        }
                        ringtonePickerLauncher.launch(intent)
                        showDialog = false
                    }) { Text("System Sounds") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        filePickerLauncher.launch(arrayOf("audio/*"))
                        showDialog = false
                    }) { Text("Local File") }
                }
            )
        }
    }
}

@Composable
fun FilePicker(
    label: String = "Select File",
    mimeType: String = "*/*",
    selectedFile: String = "",
    onSelect: (path: String) -> Unit
) {
    val context = LocalContext.current
    var filePath by remember { mutableStateOf(selectedFile) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val uriString = it.toString()
            filePath = uriString
            onSelect(uriString)
        }
    }
    Column {
        Button(
            onClick = { launcher.launch(arrayOf(mimeType)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (filePath.isBlank()) "Select File" else "Change File")
        }
        if (filePath.isNotBlank()) ChosenFileCard(filePath, Icons.Filled.InsertDriveFile) { onSelect(""); filePath = "" }
    }
}

@Composable
fun ChosenFileCard(path: String, icon: ImageVector? = null, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(100)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier
                .fillMaxHeight()
                .width(50.dp),
            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
            onClick = { onRemove() }
        ) {
            Icon(Icons.Filled.Cancel, contentDescription = null)
        }
        Spacer(Modifier.width(5.dp))
        if (icon != null) Icon(
            modifier = Modifier.size(18.dp),
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondary
        )
        Spacer(Modifier.width(3.dp))
        Text(if (path.isBlank()) "Muted" else Utils.parseSoundPath(LocalContext.current, path), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondary)
    }
}
