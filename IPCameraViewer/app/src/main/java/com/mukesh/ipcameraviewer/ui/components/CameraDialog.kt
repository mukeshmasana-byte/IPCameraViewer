package com.mukesh.ipcameraviewer.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mukesh.ipcameraviewer.Camera

@Composable
fun CameraDialog(
    existing: Camera?,
    onSave: (Camera) -> Unit,
    onDelete: (Camera) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var rtspUrl by remember { mutableStateOf(existing?.rtspUrl ?: "rtsp://192.168.1.100:554/stream") }
    var username by remember { mutableStateOf(existing?.username ?: "") }
    var password by remember { mutableStateOf(existing?.password ?: "") }
    var onvifUrl by remember { mutableStateOf(existing?.onvifUrl ?: "") }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Camera" else "Edit Camera") },
        text = {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Camera name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = rtspUrl,
                    onValueChange = { rtspUrl = it },
                    label = { Text("RTSP URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = onvifUrl,
                    onValueChange = { onvifUrl = it },
                    label = { Text("ONVIF XAddr (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val n = name.trim()
                val u = rtspUrl.trim()
                if (n.isBlank() || u.isBlank()) {
                    Toast.makeText(context, "Name and RTSP URL are required", Toast.LENGTH_SHORT).show()
                } else {
                    val camera = existing?.copy(
                        name = n, rtspUrl = u, onvifUrl = onvifUrl.trim(),
                        username = username.trim(), password = password.trim()
                    ) ?: Camera(
                        id = 0, name = n, rtspUrl = u,
                        onvifUrl = onvifUrl.trim(), username = username.trim(), password = password.trim()
                    )
                    onSave(camera)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(onClick = { onDelete(existing) }) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
