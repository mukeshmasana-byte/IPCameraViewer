package com.example.ipcameraviewer.ui

import androidx.activity.compose.BackHandler
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.example.ipcameraviewer.model.Camera
import com.example.ipcameraviewer.model.DashboardLayout
import com.example.ipcameraviewer.model.Nvr
import com.example.ipcameraviewer.model.StreamProfile
import com.example.ipcameraviewer.onvif.DiscoveredOnvifDevice
import com.example.ipcameraviewer.onvif.OnvifDiscoveryState
import com.example.ipcameraviewer.onvif.OnvifProfile
import com.example.ipcameraviewer.player.Media3VideoPlayerFactory
import com.example.ipcameraviewer.player.VideoPlayer
import com.example.ipcameraviewer.player.MjpegFrameSource
import com.example.ipcameraviewer.player.MjpegPlaybackState
import com.example.ipcameraviewer.player.playbackErrorMessage
import com.example.ipcameraviewer.util.RtspUrlValidator
import com.example.ipcameraviewer.util.RtspCredentials
import com.example.ipcameraviewer.util.HttpUrlValidator
import kotlinx.coroutines.flow.MutableStateFlow

private val Ink = Color(0xFF0B1116)
private val Panel = Color(0xFF121B22)
private val Tile = Color(0xFF17232B)
private val Mint = Color(0xFF31D6A5)
private val Muted = Color(0xFF8B9AA4)

@Composable
fun IPCameraApp(viewModel: CameraViewModel) {
    val cameras by viewModel.cameras.collectAsState()
    val nvrs by viewModel.nvrs.collectAsState()
    val nvrAdd by viewModel.nvrAdd.collectAsState()
    val layout by viewModel.layout.collectAsState()
    val selectedId by viewModel.selectedId.collectAsState()
    val discovery by viewModel.discovery.collectAsState()
    var settingsOpen by remember { mutableStateOf(false) }
    var addOpen by remember { mutableStateOf(false) }
    var nvrOpen by remember { mutableStateOf(false) }
    var mjpegOpen by remember { mutableStateOf(false) }
    var editingCamera by remember { mutableStateOf<Camera?>(null) }
    var credentialDevice by remember { mutableStateOf<DiscoveredOnvifDevice?>(null) }
    BackHandler(enabled = editingCamera != null || credentialDevice != null || nvrOpen || mjpegOpen || addOpen || settingsOpen) {
        when {
            editingCamera != null -> editingCamera = null
            credentialDevice != null -> credentialDevice = null
            nvrOpen -> { nvrOpen = false; viewModel.clearNvrAddState() }
            mjpegOpen -> mjpegOpen = false
            addOpen -> addOpen = false
            settingsOpen -> { settingsOpen = false; viewModel.resetDiscovery() }
        }
    }
    LaunchedEffect(discovery.importedCameraId) {
        if (discovery.importedCameraId != null) {
            settingsOpen = false
            viewModel.resetDiscovery()
        }
    }
    LaunchedEffect(nvrAdd.addedNvrId) {
        if (nvrAdd.addedNvrId != null) {
            nvrOpen = false
            settingsOpen = false
            viewModel.clearNvrAddState()
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Mint, onPrimary = Ink, background = Ink, surface = Panel,
        onBackground = Color(0xFFE9F1F4), onSurface = Color(0xFFE9F1F4),
        surfaceVariant = Tile, onSurfaceVariant = Muted,
    )) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
                Header(cameras.size, settingsOpen = { settingsOpen = !settingsOpen })
                Spacer(Modifier.height(16.dp))
                if (settingsOpen) {
                    ManagementPanel(
                        cameras = cameras,
                        onAdd = { addOpen = true },
                        onRemove = viewModel::remove,
                        onClose = { settingsOpen = false },
                        discovery = discovery,
                        onDiscover = viewModel::scanOnvif,
                        onSelectDevice = { credentialDevice = it },
                        onImportProfile = viewModel::importOnvif,
                        nvrs = nvrs,
                        onAddNvr = { nvrOpen = true },
                        onRemoveNvr = viewModel::removeNvr,
                        onToggleCamera = viewModel::setCameraEnabled,
                        onMoveCamera = viewModel::moveCamera,
                        onSetProfile = viewModel::setCameraProfile,
                        onTestCamera = { camera -> viewModel.select(camera.id); viewModel.setLayout(DashboardLayout.SINGLE); settingsOpen = false },
                        onEditCamera = { editingCamera = it },
                        onAddMjpeg = { mjpegOpen = true },
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("LIVE VIEW", style = MaterialTheme.typography.labelLarge, color = Muted, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        LayoutSelector(layout, viewModel::setLayout)
                    }
                    Spacer(Modifier.height(12.dp))
                    val count = minOf(layout.cameraCount, 9)
                    val ordered = cameras.sortedBy { it.displayOrder }
                    val tiles = if (layout == DashboardLayout.SINGLE) {
                        val active = ordered.firstOrNull { it.id == selectedId } ?: ordered.firstOrNull()
                        listOf(active)
                    } else (0 until count).map { index -> ordered.getOrNull(index) }
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                      val singleTileHeight = maxHeight
                      LazyVerticalGrid(
                        columns = GridCells.Fixed(layout.columns),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                      ) {
                        items(tiles.size) { index ->
                            val camera = tiles[index]
                            val isSelected = camera != null && camera.id == selectedId
                            StreamTile(
                                camera = camera,
                                selected = isSelected,
                                audioOwner = isSelected && (layout == DashboardLayout.SINGLE || selectedId == camera.id),
                                allowFullscreen = layout != DashboardLayout.SINGLE,
                                useSubStream = layout != DashboardLayout.SINGLE,
                                audioController = viewModel.audioController,
                                fillHeight = layout == DashboardLayout.SINGLE,
                                modifier = if (layout == DashboardLayout.SINGLE) Modifier.height(singleTileHeight) else Modifier,
                                onSelect = {
                                    if (camera == null) addOpen = true
                                    else viewModel.select(camera.id)
                                },
                                onFullscreen = {
                                    camera?.let { viewModel.select(it.id); viewModel.setLayout(DashboardLayout.SINGLE) }
                                },
                            )
                        }
                      }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                        StatusDot()
                        Spacer(Modifier.width(8.dp))
                        Text(if (cameras.isEmpty()) "No cameras configured" else "${cameras.count { it.enabled }} camera${if (cameras.count { it.enabled } == 1) "" else "s"} configured", color = Muted, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.weight(1f))
                        Text("Local network only", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (addOpen) AddCameraDialog(
            onDismiss = { addOpen = false },
            onAdd = { name, url, subUrl -> viewModel.addRtsp(name, url, subUrl); addOpen = false },
        )
        credentialDevice?.let { device ->
            OnvifCredentialDialog(
                device = device,
                onDismiss = { credentialDevice = null },
                onConnect = { username, password ->
                    viewModel.inspectOnvif(device, username, password)
                    credentialDevice = null
                },
            )
        }
        if (nvrOpen) ManualNvrDialog(
            saving = nvrAdd.saving,
            error = nvrAdd.error,
            onDismiss = { nvrOpen = false; viewModel.clearNvrAddState() },
            onSave = viewModel::addManualNvr,
        )
        if (mjpegOpen) AddMjpegDialog(
            onDismiss = { mjpegOpen = false },
            onAdd = { name, url, username, password -> viewModel.addMjpeg(name, url, username, password); mjpegOpen = false },
        )
        editingCamera?.let { camera ->
            EditCameraDialog(
                camera = camera,
                onDismiss = { editingCamera = null },
                onSave = { name, url, subUrl, username, password ->
                    viewModel.updateCamera(camera.id, name, url, subUrl, username, password)
                    editingCamera = null
                },
            )
        }
    }
}

@Composable
private fun Header(cameraCount: Int, settingsOpen: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Mint.copy(alpha = .13f)), Alignment.Center) {
            Icon(Icons.Default.Videocam, null, tint = Mint)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("IP Camera Viewer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("PRIVATE VIDEO MONITOR", style = MaterialTheme.typography.labelSmall, color = Muted, letterSpacing = 1.4.sp)
        }
        Surface(shape = CircleShape, color = Panel) {
            Text("$cameraCount / 9", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Muted, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = settingsOpen, modifier = Modifier.focusable()) {
            Icon(Icons.Default.Settings, contentDescription = "Camera management", tint = if (cameraCount > 0) Color.White else Mint)
        }
    }
}

@Composable
private fun LayoutSelector(selected: DashboardLayout, onSelect: (DashboardLayout) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DashboardLayout.entries.forEach { layout ->
            val active = layout == selected
            Surface(
                color = if (active) Mint else Panel,
                contentColor = if (active) Ink else Muted,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onSelect(layout) }.focusable(),
            ) {
                Text(layout.label, Modifier.padding(horizontal = 13.dp, vertical = 9.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StreamTile(camera: Camera?, selected: Boolean, audioOwner: Boolean, allowFullscreen: Boolean, useSubStream: Boolean, audioController: com.example.ipcameraviewer.player.AudioController, fillHeight: Boolean, modifier: Modifier = Modifier, onSelect: () -> Unit, onFullscreen: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    val streamUrl = if (useSubStream || (fillHeight && camera?.profile == StreamProfile.SUB)) camera?.subStreamUrl ?: camera?.streamUrl else camera?.streamUrl
    val video = remember(camera?.id, streamUrl) {
        if (camera?.enabled == true && streamUrl?.startsWith("rtsp://", ignoreCase = true) == true) {
            runCatching { Media3VideoPlayerFactory().create(context, RtspCredentials.playbackUrl(streamUrl, camera.username, camera.password)) }.getOrNull()
        } else null
    }
    val mjpeg = remember(camera?.id, camera?.streamUrl, camera?.sourceType) {
        if (camera?.enabled == true && camera.sourceType == com.example.ipcameraviewer.model.CameraSourceType.MJPEG) runCatching { MjpegFrameSource(camera) }.getOrNull() else null
    }
    val noMjpegState = remember(mjpeg) { MutableStateFlow(MjpegPlaybackState()) }
    val mjpegState by (mjpeg?.state ?: noMjpegState).collectAsState()
    val noAudioState = remember(video) { MutableStateFlow<Boolean?>(null) }
    val audioTrackAvailable by (video?.audioTrackAvailable ?: noAudioState).collectAsState()
    var playbackError by remember(video) { mutableStateOf<String?>(null) }
    var isPlaying by remember(video) { mutableStateOf(false) }
    DisposableEffect(video, camera?.id, camera?.audioEnabled, camera?.supportsAudio) {
        if (video != null && camera != null) audioController.attach(camera.id, video, camera.audioEnabled && camera.supportsAudio != false)
        onDispose { if (camera != null) audioController.detach(camera.id) }
    }
    DisposableEffect(video, lifecycle) {
        val playerListener = if (video != null) object : Player.Listener {
            override fun onPlayerError(exception: androidx.media3.common.PlaybackException) {
                playbackError = playbackErrorMessage(exception)
                isPlaying = false
            }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_IDLE) playbackError = null
            }
        } else null
        playerListener?.let(video!!.player::addListener)
        val observer = if (video != null) LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> video.player.playWhenReady = false
                Lifecycle.Event.ON_START -> video.player.playWhenReady = true
                else -> Unit
            }
        } else null
        observer?.let(lifecycle::addObserver)
        onDispose {
            playerListener?.let(video!!.player::removeListener)
            observer?.let(lifecycle::removeObserver)
            video?.release()
        }
    }
    DisposableEffect(mjpeg, lifecycle) {
        val observer = if (mjpeg != null) LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mjpeg.start()
                Lifecycle.Event.ON_STOP -> mjpeg.stop()
                else -> Unit
            }
        } else null
        mjpeg?.start()
        observer?.let(lifecycle::addObserver)
        onDispose { observer?.let(lifecycle::removeObserver); mjpeg?.release() }
    }

    val borderColor = if (selected || focused) Mint else Color(0xFF26343D)
    Column(
        modifier.fillMaxWidth().then(if (fillHeight) Modifier else Modifier.aspectRatio(16f / 10f))
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(if (selected || focused) 2.dp else 1.dp, borderColor), RoundedCornerShape(14.dp))
            .background(Tile)
            .clickable(onClick = onSelect)
            .focusable()
            .onFocusChanged { focused = it.isFocused },
    ) {
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF080D11))) {
            if (mjpeg != null) {
                mjpegState.frame?.let { frame ->
                    Image(frame.asImageBitmap(), "${camera?.name ?: "Camera"} live video", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                } ?: Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null, tint = Muted, modifier = Modifier.size(25.dp))
                    Text("Connecting…", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            } else if (video != null) {
                AndroidView(
                    factory = { context -> PlayerView(context).apply {
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        player = video.player
                    } },
                    update = { if (it.player !== video.player) it.player = video.player },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (camera == null) {
                        Icon(Icons.Default.Add, null, tint = Mint, modifier = Modifier.size(26.dp))
                        Text("Add camera", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    } else {
                        Icon(Icons.Default.CameraAlt, null, tint = Muted, modifier = Modifier.size(25.dp))
                        Text(if (camera.enabled) "Connecting…" else "Camera disabled", color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (camera != null && (video != null || mjpeg != null)) {
                val error = playbackError ?: mjpegState.error
                val live = isPlaying || (mjpeg != null && mjpegState.connected)
                if (error != null) {
                    Box(Modifier.fillMaxSize().background(Color(0xCC080D11)), Alignment.Center) {
                        Text(error, Modifier.padding(12.dp), color = Color(0xFFFFB4AB), style = MaterialTheme.typography.labelSmall)
                    }
                } else if (!live) {
                    Text("Connecting…", Modifier.align(Alignment.Center).background(Color(0x99080D11), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp), color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(Modifier.align(Alignment.TopStart).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if ((video != null && isPlaying) || (mjpeg != null && mjpegState.connected)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Mint))
                    Text("LIVE", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                } else if (video != null || mjpeg != null) {
                    Text("CONNECTING", color = Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            if (audioOwner && camera?.audioEnabled == true && camera.supportsAudio != false && audioTrackAvailable == true) {
                Surface(Modifier.align(Alignment.TopEnd).padding(7.dp), color = Color(0xCC0B1116), shape = CircleShape) {
                    Icon(Icons.Default.GraphicEq, "Audio output", tint = Mint, modifier = Modifier.padding(6.dp).size(15.dp))
                }
            }
            if (camera != null && allowFullscreen && camera.enabled) {
                IconButton(onClick = onFullscreen, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(36.dp).focusable()) {
                    Icon(Icons.Default.OpenInFull, contentDescription = "Show ${camera.name} full screen", tint = Color.White)
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(if (selected) Color(0xFF152B2A) else Panel).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(camera?.name ?: "Available slot", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(camera?.host ?: "Select to configure", color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (selected) Text("SELECTED", color = Mint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusDot() {
    Box(Modifier.size(7.dp).clip(CircleShape).background(Muted))
}

@Composable
private fun ManagementPanel(
    cameras: List<Camera>, onAdd: () -> Unit, onRemove: (String) -> Unit, onClose: () -> Unit,
    discovery: OnvifDiscoveryState, onDiscover: () -> Unit,
    onSelectDevice: (DiscoveredOnvifDevice) -> Unit, onImportProfile: (OnvifProfile) -> Unit,
    nvrs: List<Nvr>, onAddNvr: () -> Unit, onRemoveNvr: (String) -> Unit,
    onToggleCamera: (String, Boolean) -> Unit, onMoveCamera: (String, Int) -> Unit,
    onSetProfile: (String, StreamProfile) -> Unit, onTestCamera: (Camera) -> Unit,
    onEditCamera: (Camera) -> Unit,
    onAddMjpeg: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Panel, RoundedCornerShape(18.dp)).verticalScroll(rememberScrollState()).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Cameras", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Manage local streams and NVR sources", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onClose, modifier = Modifier.focusable()) { Text("Back") }
        }
        Spacer(Modifier.height(18.dp))
        if (cameras.isEmpty()) {
            Text("No cameras yet. Add an RTSP stream manually or discover ONVIF devices on your network.", color = Muted)
        } else {
            cameras.sortedBy { it.displayOrder }.forEachIndexed { index, camera ->
                CameraManagementCard(
                    camera = camera,
                    index = index,
                    total = cameras.size,
                    onEdit = { onEditCamera(camera) },
                    onTest = { onTestCamera(camera) },
                    onRemove = { onRemove(camera.id) },
                    onToggle = { onToggleCamera(camera.id, it) },
                    onMove = { onMoveCamera(camera.id, it) },
                    onSetProfile = { onSetProfile(camera.id, it) },
                )
            }
        }
        nvrs.forEach { nvr ->
            Row(Modifier.fillMaxWidth().padding(top = 5.dp).background(Color(0xFF1A2931), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NVR · ${nvr.name}", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("${nvr.host}:${nvr.port} · ${cameras.count { it.nvrId == nvr.id }} channels", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { onRemoveNvr(nvr.id) }, modifier = Modifier.focusable()) { Text("Remove NVR", color = Color(0xFFFF8A80)) }
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAdd, enabled = cameras.size < 9, modifier = Modifier.weight(1f).focusable()) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add RTSP camera")
                }
                OutlinedButton(onClick = onAddMjpeg, enabled = cameras.size < 9, modifier = Modifier.weight(1f).focusable()) { Text("Add MJPEG") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAddNvr, enabled = cameras.size < 9, modifier = Modifier.weight(1f).focusable()) { Text("Add NVR / channels") }
                OutlinedButton(onClick = onDiscover, modifier = Modifier.weight(1f).focusable(), enabled = !discovery.scanning) {
                    Text(if (discovery.scanning) "Scanning…" else "Discover ONVIF")
                }
            }
        }
        if (discovery.scanning || discovery.inspecting || discovery.importing) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp), color = Mint)
        }
        discovery.error?.let { Text(it, Modifier.padding(top = 10.dp), color = Color(0xFFFFB4AB), style = MaterialTheme.typography.bodySmall) }
        if (discovery.devices.isNotEmpty() && discovery.summary == null) {
            Spacer(Modifier.height(10.dp))
            Text("Discovered ONVIF devices", color = Color.White, fontWeight = FontWeight.SemiBold)
            discovery.devices.forEach { device ->
                TextButton(onClick = { onSelectDevice(device) }, modifier = Modifier.fillMaxWidth().focusable(), contentPadding = PaddingValues(8.dp)) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                        Text(device.hostAddress, color = Color.White, fontWeight = FontWeight.Medium)
                        Text(device.scopes.firstOrNull { it.contains("name/", true) }?.substringAfterLast("name/") ?: device.endpoint, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        discovery.summary?.let { summary ->
            Spacer(Modifier.height(10.dp))
            Text("${summary.info.manufacturer.orEmpty()} ${summary.info.model.orEmpty()}".trim().ifBlank { summary.device.hostAddress }, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("${summary.profiles.size} media profile${if (summary.profiles.size == 1) "" else "s"}${if (summary.supportsPtz) " · PTZ reported" else ""}", color = Muted, style = MaterialTheme.typography.bodySmall)
            summary.profiles.forEach { profile ->
                Row(Modifier.fillMaxWidth().padding(top = 6.dp).background(Tile, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(profile.name, color = Color.White, fontWeight = FontWeight.Medium)
                        Text("${profile.token}${if (profile.hasAudio) " · audio" else ""}", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { onImportProfile(profile) }, enabled = !discovery.importing && cameras.size < 9, modifier = Modifier.focusable()) { Text("Import") }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
                Text("Stream URLs and embedded credentials are encrypted with Android Keystore. Camera settings stay on this device.", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CameraManagementCard(
    camera: Camera, index: Int, total: Int,
    onEdit: () -> Unit, onTest: () -> Unit, onRemove: () -> Unit,
    onToggle: (Boolean) -> Unit, onMove: (Int) -> Unit, onSetProfile: (StreamProfile) -> Unit,
) {
    var profileMenu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp).background(Tile, RoundedCornerShape(12.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Videocam, null, tint = if (camera.enabled) Mint else Muted)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(camera.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${camera.sourceType} · ${camera.host ?: "Local stream"}${camera.channelNumber?.let { " · channel $it" }.orEmpty()}", color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Switch(checked = camera.enabled, onCheckedChange = onToggle, modifier = Modifier.focusable())
        }
        Text("Profile: ${camera.profileName ?: camera.profile.name}${if (camera.supportsAudio == true) " · audio" else ""}${if (camera.supportsPtz) " · PTZ" else ""}", Modifier.padding(start = 34.dp, top = 3.dp), color = Muted, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            TextButton(onClick = onTest, modifier = Modifier.focusable()) { Text("Test video") }
            TextButton(onClick = onEdit, modifier = Modifier.focusable()) { Icon(Icons.Default.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Edit") }
            Box {
                TextButton(onClick = { profileMenu = true }, enabled = camera.subStreamUrl != null, modifier = Modifier.focusable()) { Text("Stream profile") }
                DropdownMenu(expanded = profileMenu, onDismissRequest = { profileMenu = false }) {
                    DropdownMenuItem(text = { Text("Main stream") }, onClick = { onSetProfile(StreamProfile.MAIN); profileMenu = false })
                    if (camera.subStreamUrl != null) DropdownMenuItem(text = { Text("Substream") }, onClick = { onSetProfile(StreamProfile.SUB); profileMenu = false })
                }
            }
            IconButton(onClick = { onMove(-1) }, enabled = index > 0, modifier = Modifier.focusable()) { Icon(Icons.Default.ArrowUpward, "Move camera up", tint = if (index > 0) Color.White else Muted) }
            IconButton(onClick = { onMove(1) }, enabled = index < total - 1, modifier = Modifier.focusable()) { Icon(Icons.Default.ArrowDownward, "Move camera down", tint = if (index < total - 1) Color.White else Muted) }
            TextButton(onClick = onRemove, modifier = Modifier.focusable()) { Text("Delete", color = Color(0xFFFF8A80)) }
        }
    }
}

@Composable
private fun AddCameraDialog(onDismiss: () -> Unit, onAdd: (String, String, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var subUrl by remember { mutableStateOf("") }
    val valid = RtspUrlValidator.normalize(url) != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add RTSP camera") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Camera name") }, singleLine = true)
                OutlinedTextField(
                    value = url, onValueChange = { url = it }, label = { Text("RTSP URL") },
                    placeholder = { Text("rtsp://camera-address:554/stream") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    supportingText = { Text("The complete stream URL is encrypted with Android Keystore on this device.") },
                )
                OutlinedTextField(
                    value = subUrl, onValueChange = { subUrl = it }, label = { Text("Substream URL (optional)") },
                    placeholder = { Text("rtsp://camera-address:554/substream") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    supportingText = { Text("Used for 2×2 and 3×3 views when available.") },
                )
            }
        },
        confirmButton = { Button(onClick = { onAdd(name, url, subUrl.takeIf(String::isNotBlank)) }, enabled = valid && (subUrl.isBlank() || RtspUrlValidator.normalize(subUrl) != null)) { Text("Add camera") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Panel,
    )
}

@Composable
private fun AddMjpegDialog(onDismiss: () -> Unit, onAdd: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val valid = HttpUrlValidator.normalize(url) != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add MJPEG camera") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Camera name") }, singleLine = true)
                OutlinedTextField(url, { url = it }, label = { Text("HTTP MJPEG URL") }, placeholder = { Text("http://camera-address/mjpeg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), minLines = 2)
                OutlinedTextField(username, { username = it }, label = { Text("Username (optional)") }, singleLine = true)
                OutlinedTextField(password, { password = it }, label = { Text("Password (optional)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                Text("Credentials and stream URL are encrypted on this device. The endpoint must return multipart MJPEG.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(onClick = { onAdd(name, url, username, password) }, enabled = valid) { Text("Add camera") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Panel,
    )
}

@Composable
private fun EditCameraDialog(
    camera: Camera,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, String, String) -> Unit,
) {
    var name by remember(camera.id) { mutableStateOf(camera.name) }
    var url by remember(camera.id) { mutableStateOf(redactUserInfo(camera.streamUrl)) }
    var subUrl by remember(camera.id) { mutableStateOf(camera.subStreamUrl.orEmpty().let(::redactUserInfo)) }
    var username by remember(camera.id) { mutableStateOf(camera.username.orEmpty()) }
    var password by remember(camera.id) { mutableStateOf(camera.password.orEmpty()) }
    val mjpeg = camera.sourceType == com.example.ipcameraviewer.model.CameraSourceType.MJPEG
    val valid = (if (mjpeg) HttpUrlValidator.normalize(url) != null else RtspUrlValidator.normalize(url) != null) &&
        (mjpeg || subUrl.isBlank() || RtspUrlValidator.normalize(subUrl) != null)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${camera.name}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Camera name") }, singleLine = true)
                OutlinedTextField(url, { url = it }, label = { Text(if (mjpeg) "HTTP MJPEG URL" else "Main RTSP URL") }, minLines = 2, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                if (!mjpeg) OutlinedTextField(subUrl, { subUrl = it }, label = { Text("Substream URL (optional)") }, minLines = 2, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true)
                OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                Text("Credentials remain encrypted on this device.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(onClick = { onSave(name, url, subUrl.takeIf(String::isNotBlank), username, password) }, enabled = valid) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Panel,
    )
}

private fun redactUserInfo(url: String): String = runCatching {
    val uri = java.net.URI(url)
    val authority = uri.rawAuthority?.substringAfterLast('@') ?: return url
    "${uri.scheme}://$authority${uri.rawPath.orEmpty()}${uri.rawQuery?.let { "?$it" }.orEmpty()}${uri.rawFragment?.let { "#$it" }.orEmpty()}"
}.getOrDefault(url)

@Composable
private fun OnvifCredentialDialog(
    device: DiscoveredOnvifDevice,
    onDismiss: () -> Unit,
    onConnect: (String, String) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect to ${device.hostAddress}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Credentials are sent directly to this device on your local network.", color = Muted, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true)
                OutlinedTextField(
                    password, { password = it }, label = { Text("Password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            }
        },
        confirmButton = { Button(onClick = { onConnect(username, password) }, enabled = username.isNotBlank()) { Text("Connect") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Panel,
    )
}

@Composable
private fun ManualNvrDialog(
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String, String, Int, Int, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("554") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstChannel by remember { mutableStateOf("1") }
    var channelCount by remember { mutableStateOf("1") }
    var template by remember { mutableStateOf("rtsp://{host}:{port}/path/{channel}") }
    var subTemplate by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add NVR channels") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("NVR channel URL patterns vary by manufacturer. Enter the RTSP path documented for your NVR; placeholders are replaced for each channel.", color = Muted, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(name, { name = it }, label = { Text("NVR name") }, singleLine = true)
                OutlinedTextField(host, { host = it }, label = { Text("NVR address / hostname") }, singleLine = true)
                OutlinedTextField(port, { port = it.filter(Char::isDigit) }, label = { Text("RTSP port") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(username, { username = it }, label = { Text("Username (optional)") }, singleLine = true)
                OutlinedTextField(password, { password = it }, label = { Text("Password (optional)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(firstChannel, { firstChannel = it.filter(Char::isDigit) }, label = { Text("First channel") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(channelCount, { channelCount = it.filter(Char::isDigit) }, label = { Text("Channel count") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                OutlinedTextField(template, { template = it }, label = { Text("RTSP URL template") }, minLines = 2, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                OutlinedTextField(subTemplate, { subTemplate = it }, label = { Text("Substream template (optional)") }, minLines = 2, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                Text("Use {host}, {port}, and {channel}. The complete channel URLs and credentials are encrypted on this device.", color = Muted, style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = Color(0xFFFFB4AB), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { Button(onClick = { onSave(name, host, port.toIntOrNull() ?: -1, username, password, firstChannel.toIntOrNull() ?: -1, channelCount.toIntOrNull() ?: -1, template, subTemplate) }, enabled = !saving && host.isNotBlank() && template.contains("{channel}") && (subTemplate.isBlank() || subTemplate.contains("{channel}"))) { Text(if (saving) "Saving…" else "Add channels") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } },
        containerColor = Panel,
    )
}
