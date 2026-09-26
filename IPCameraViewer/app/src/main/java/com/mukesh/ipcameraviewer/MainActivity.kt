package com.mukesh.ipcameraviewer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.mukesh.ipcameraviewer.data.AppDatabase
import com.mukesh.ipcameraviewer.repository.CameraRepository
import com.mukesh.ipcameraviewer.ui.components.CameraDialog
import com.mukesh.ipcameraviewer.ui.components.CameraGrid
import com.mukesh.ipcameraviewer.ui.components.TopBar
import com.mukesh.ipcameraviewer.ui.theme.IPCameraViewerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var repository: CameraRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(this)
        repository = CameraRepository(database.cameraDao())

        setContent {
            IPCameraViewerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraViewerApp(repository = repository)
                }
            }
        }
    }
}

@Composable
fun CameraViewerApp(repository: CameraRepository) {
    val coroutineScope = rememberCoroutineScope()
    val cameras by repository.allCameras.collectAsState(initial = emptyList())

    var columns by remember { mutableStateOf(2) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCamera by remember { mutableStateOf<Camera?>(null) }
    var selectedCameraId by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        TopBar(
            onLayoutChange = { cols -> columns = cols },
            onAddCamera = { editingCamera = null; showAddDialog = true },
            onDiscoverOnvif = {
                Toast.makeText(context, "Searching local network for ONVIF cameras…", Toast.LENGTH_SHORT).show()
                coroutineScope.launch(Dispatchers.IO) {
                    val devices = try { OnvifDiscovery.discover() } catch (_: Exception) { emptyList() }
                    withContext(Dispatchers.Main) {
                        if (devices.isEmpty()) {
                            Toast.makeText(context, "No ONVIF devices found", Toast.LENGTH_LONG).show()
                        } else {
                            val firstDevice = devices.firstOrNull()
                            if (firstDevice != null) {
                                editingCamera = Camera(0, firstDevice.address, "", firstDevice.xAddrs)
                                showAddDialog = true
                            }
                        }
                    }
                }
            }
        )

        CameraGrid(
            columns = columns,
            cameras = cameras,
            selectedCameraId = selectedCameraId,
            onCameraClick = { camera -> selectedCameraId = camera.id },
            onCameraEdit = { camera -> editingCamera = camera; showAddDialog = true }
        )
    }

    if (showAddDialog) {
        CameraDialog(
            existing = editingCamera,
            onSave = { camera ->
                coroutineScope.launch(Dispatchers.IO) {
                    if (editingCamera == null || camera.id == 0L) {
                        repository.insert(camera)
                    } else {
                        repository.update(camera)
                    }
                }
                showAddDialog = false
            },
            onDelete = { camera ->
                coroutineScope.launch(Dispatchers.IO) {
                    repository.delete(camera)
                }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}
