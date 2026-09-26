package com.example.ipcameraviewer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ipcameraviewer.IPCameraViewerApplication
import com.example.ipcameraviewer.model.Camera
import com.example.ipcameraviewer.model.CameraSourceType
import com.example.ipcameraviewer.model.DashboardLayout
import com.example.ipcameraviewer.model.StreamProfile
import com.example.ipcameraviewer.model.Nvr
import com.example.ipcameraviewer.player.AudioController
import com.example.ipcameraviewer.player.SelectedCameraAudioController
import com.example.ipcameraviewer.onvif.OnvifCameraDetails
import com.example.ipcameraviewer.onvif.OnvifClient
import com.example.ipcameraviewer.onvif.OnvifCredentials
import com.example.ipcameraviewer.onvif.OnvifDeviceSummary
import com.example.ipcameraviewer.onvif.OnvifDiscoveryState
import com.example.ipcameraviewer.onvif.OnvifProfile
import com.example.ipcameraviewer.onvif.WsDiscoveryScanner
import com.example.ipcameraviewer.util.RtspUrlValidator
import com.example.ipcameraviewer.util.HttpUrlValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as IPCameraViewerApplication).cameraRepository
    private val scanner = WsDiscoveryScanner(application)
    private val onvifClient = OnvifClient()
    private var activeDetails: OnvifCameraDetails? = null
    val audioController: AudioController = SelectedCameraAudioController()
    val cameras: StateFlow<List<Camera>> = repository.cameras
    val nvrs = repository.nvrs

    private val mutableLayout = MutableStateFlow(DashboardLayout.SINGLE)
    val layout: StateFlow<DashboardLayout> = mutableLayout.asStateFlow()
    private val mutableSelectedId = MutableStateFlow<String?>(null)
    val selectedId: StateFlow<String?> = mutableSelectedId.asStateFlow()
    private val mutableDiscovery = MutableStateFlow(OnvifDiscoveryState())
    val discovery: StateFlow<OnvifDiscoveryState> = mutableDiscovery.asStateFlow()
    private val mutableNvrAdd = MutableStateFlow(NvrAddState())
    val nvrAdd: StateFlow<NvrAddState> = mutableNvrAdd.asStateFlow()

    fun setLayout(value: DashboardLayout) { mutableLayout.value = value }
    fun select(id: String?) { mutableSelectedId.value = id; audioController.selectOwner(id) }

    fun scanOnvif() {
        viewModelScope.launch {
            mutableDiscovery.value = OnvifDiscoveryState(scanning = true)
            runCatching { scanner.discover() }
                .onSuccess { devices -> mutableDiscovery.value = OnvifDiscoveryState(devices = devices, error = if (devices.isEmpty()) "No ONVIF devices replied. Confirm the phone or TV is on the camera network." else null) }
                .onFailure { mutableDiscovery.value = OnvifDiscoveryState(error = it.message ?: "ONVIF discovery failed. Check the local network.") }
        }
    }

    fun inspectOnvif(device: com.example.ipcameraviewer.onvif.DiscoveredOnvifDevice, username: String, password: String) {
        viewModelScope.launch {
            mutableDiscovery.value = mutableDiscovery.value.copy(inspecting = true, error = null, summary = null)
            runCatching { onvifClient.inspect(device, OnvifCredentials(username.trim(), password)) }
                .onSuccess { details ->
                    activeDetails = details
                    mutableDiscovery.value = mutableDiscovery.value.copy(
                        inspecting = false,
                        summary = OnvifDeviceSummary(details.device, details.info, details.profiles, details.supportsPtz),
                    )
                }
                .onFailure { mutableDiscovery.value = mutableDiscovery.value.copy(inspecting = false, error = it.message ?: "Could not read ONVIF camera profiles.") }
        }
    }

    fun importOnvif(profile: OnvifProfile) {
        val details = activeDetails ?: return
        viewModelScope.launch {
            mutableDiscovery.value = mutableDiscovery.value.copy(importing = true, error = null)
            runCatching {
                val isSub = profile.name.contains("sub", true) || profile.token.contains("sub", true)
                val pairedMain = if (isSub) details.profiles.firstOrNull { it != profile && !it.name.contains("sub", true) && !it.token.contains("sub", true) } else null
                val pairedSub = if (isSub) profile else details.profiles.firstOrNull { it != profile && (it.name.contains("sub", true) || it.token.contains("sub", true)) }
                val selectedUri = onvifClient.getStreamUri(details, profile)
                val streamUrl = if (pairedMain != null) runCatching { onvifClient.getStreamUri(details, pairedMain) }.getOrDefault(selectedUri) else selectedUri
                val subStreamUrl = pairedSub?.let { if (it == profile) selectedUri else runCatching { onvifClient.getStreamUri(details, it) }.getOrNull() }
                val primaryProfile = pairedMain ?: profile
                val uri = java.net.URI(streamUrl)
                val camera = Camera(
                    id = UUID.randomUUID().toString(),
                    name = profile.name.ifBlank { details.info.model ?: "ONVIF camera ${cameras.value.size + 1}" },
                    sourceType = CameraSourceType.ONVIF,
                    streamUrl = streamUrl,
                    subStreamUrl = subStreamUrl,
                    onvifEndpoint = details.device.endpoint,
                    ptzEndpoint = details.ptzEndpoint,
                    username = details.credentials.username,
                    password = details.credentials.password,
                    host = uri.host ?: details.device.hostAddress,
                    port = if (uri.port >= 0) uri.port else 554,
                    profile = StreamProfile.MAIN,
                    profileName = primaryProfile.name,
                    profileToken = primaryProfile.token,
                    displayOrder = cameras.value.size,
                    supportsAudio = primaryProfile.hasAudio,
                    supportsPtz = details.supportsPtz,
                )
                check(repository.add(camera)) { "You can configure up to nine cameras." }
                camera
            }.onSuccess { camera ->
                select(camera.id)
                mutableDiscovery.value = mutableDiscovery.value.copy(importing = false, importedCameraId = camera.id)
                activeDetails = null
            }.onFailure {
                mutableDiscovery.value = mutableDiscovery.value.copy(importing = false, error = it.message ?: "Could not import this camera profile.")
            }
        }
    }

    fun resetDiscovery() {
        mutableDiscovery.value = OnvifDiscoveryState()
        activeDetails = null
    }

    fun addRtsp(name: String, url: String, subStreamUrl: String?) {
        val normalizedUrl = RtspUrlValidator.normalize(url) ?: return
        val normalizedSubUrl = subStreamUrl?.takeIf(String::isNotBlank)?.let(RtspUrlValidator::normalize)
        if (subStreamUrl?.isNotBlank() == true && normalizedSubUrl == null) return
        val uri = java.net.URI(normalizedUrl)
        val userInfo = runCatching { uri.userInfo }.getOrNull()
        val camera = Camera(
            id = UUID.randomUUID().toString(), name = name.trim().ifBlank { "Camera ${cameras.value.size + 1}" },
            sourceType = CameraSourceType.MANUAL_RTSP, streamUrl = normalizedUrl, host = uri.host,
            subStreamUrl = normalizedSubUrl,
            port = if (uri.port >= 0) uri.port else 554, profile = StreamProfile.MAIN,
            username = userInfo?.substringBefore(':')?.takeIf { userInfo.isNotBlank() },
            password = userInfo?.substringAfter(':', "")?.takeIf { userInfo.contains(':') },
            displayOrder = cameras.value.size,
        )
        viewModelScope.launch {
            if (repository.add(camera)) select(camera.id)
        }
    }

    fun addMjpeg(name: String, url: String, username: String, password: String) {
        val normalizedUrl = HttpUrlValidator.normalize(url) ?: return
        val uri = java.net.URI(normalizedUrl)
        val userInfo = uri.userInfo
        val camera = Camera(
            id = UUID.randomUUID().toString(), name = name.trim().ifBlank { "Camera ${cameras.value.size + 1}" },
            sourceType = CameraSourceType.MJPEG, streamUrl = normalizedUrl, httpUrl = normalizedUrl,
            host = uri.host, port = if (uri.port >= 0) uri.port else if (uri.scheme.equals("https", true)) 443 else 80,
            username = username.takeIf(String::isNotBlank) ?: userInfo?.substringBefore(':')?.takeIf(String::isNotBlank),
            password = password.takeIf(String::isNotBlank) ?: userInfo?.substringAfter(':', "")?.takeIf { userInfo.contains(':') },
            displayOrder = cameras.value.size,
        )
        viewModelScope.launch { if (repository.add(camera)) select(camera.id) }
    }

    fun addManualNvr(name: String, host: String, port: Int, username: String, password: String, firstChannel: Int, channelCount: Int, urlTemplate: String, subUrlTemplate: String) {
        viewModelScope.launch {
            mutableNvrAdd.value = NvrAddState(saving = true)
            if (host.isBlank() || port !in 1..65535 || firstChannel < 1 || channelCount !in 1..9) {
                mutableNvrAdd.value = NvrAddState(error = "Enter a valid NVR address, port, first channel, and channel count.")
                return@launch
            }
            if (cameras.value.size + channelCount > 9) {
                mutableNvrAdd.value = NvrAddState(error = "The dashboard supports nine cameras. There is room for ${9 - cameras.value.size} more.")
                return@launch
            }
            if (!urlTemplate.contains("{channel}")) {
                mutableNvrAdd.value = NvrAddState(error = "The RTSP template must include {channel} so each feed maps to its own NVR channel.")
                return@launch
            }
            val safeHost = if (host.contains(':') && !host.startsWith('[')) "[$host]" else host
            val nvrId = UUID.randomUUID().toString()
            val nvr = Nvr(nvrId, name.trim().ifBlank { "NVR ${nvrs.value.size + 1}" }, host.trim(), port)
            val channels = (firstChannel until firstChannel + channelCount).map { channelNumber ->
                val url = urlTemplate.trim().replace("{host}", safeHost).replace("{port}", port.toString()).replace("{channel}", channelNumber.toString())
                val normalized = RtspUrlValidator.normalize(url)
                if (normalized == null) {
                    mutableNvrAdd.value = NvrAddState(error = "The RTSP template is invalid. Use {host} and {channel} in a valid RTSP URL.")
                    return@launch
                }
                val subUrl = subUrlTemplate.trim().takeIf(String::isNotBlank)?.let {
                    RtspUrlValidator.normalize(it.replace("{host}", safeHost).replace("{port}", port.toString()).replace("{channel}", channelNumber.toString()))
                }
                if (subUrlTemplate.isNotBlank() && subUrl == null) {
                    mutableNvrAdd.value = NvrAddState(error = "The substream template is invalid. Use a valid RTSP URL with {channel}.")
                    return@launch
                }
                val uri = java.net.URI(normalized)
                Camera(
                    id = UUID.randomUUID().toString(), name = "${nvr.name} · Channel $channelNumber",
                    sourceType = CameraSourceType.NVR_CHANNEL, streamUrl = normalized, subStreamUrl = subUrl,
                    host = uri.host ?: host.trim(), port = if (uri.port >= 0) uri.port else port,
                    nvrId = nvrId, channelNumber = channelNumber, profile = StreamProfile.MAIN,
                    displayOrder = cameras.value.size + channelNumber - firstChannel,
                    username = username.takeIf(String::isNotBlank), password = password.takeIf(String::isNotBlank),
                )
            }
            runCatching { repository.addNvr(nvr, username.trim(), password, channels) }
                .onSuccess { added ->
                    if (!added) mutableNvrAdd.value = NvrAddState(error = "The dashboard supports no more than nine cameras.")
                    else {
                        select(channels.first().id)
                        mutableNvrAdd.value = NvrAddState(addedNvrId = nvrId)
                    }
                }
                .onFailure { mutableNvrAdd.value = NvrAddState(error = it.message ?: "Could not save NVR channels securely.") }
        }
    }

    fun removeNvr(id: String) {
        viewModelScope.launch {
            val removedIds = cameras.value.filter { it.nvrId == id }.map { it.id }
            repository.removeNvr(id)
            if (mutableSelectedId.value in removedIds) select(null)
        }
    }

    fun clearNvrAddState() { mutableNvrAdd.value = NvrAddState() }

    fun remove(id: String) {
        viewModelScope.launch {
            repository.remove(id)
            if (mutableSelectedId.value == id) select(null)
        }
    }

    fun updateCamera(id: String, name: String, url: String, subUrl: String?, username: String, password: String) {
        val current = cameras.value.firstOrNull { it.id == id } ?: return
        val normalized = if (current.sourceType == CameraSourceType.MJPEG) HttpUrlValidator.normalize(url) else RtspUrlValidator.normalize(url)
        if (normalized == null) return
        val normalizedSub = if (current.sourceType == CameraSourceType.MJPEG) null else subUrl?.takeIf(String::isNotBlank)?.let(RtspUrlValidator::normalize)
        if (current.sourceType != CameraSourceType.MJPEG && subUrl?.isNotBlank() == true && normalizedSub == null) return
        val uri = java.net.URI(normalized)
        viewModelScope.launch {
            repository.update(current.copy(
                name = name.trim().ifBlank { current.name },
                streamUrl = normalized,
                subStreamUrl = normalizedSub,
                httpUrl = normalized.takeIf { current.sourceType == CameraSourceType.MJPEG },
                host = uri.host ?: current.host,
                port = if (uri.port >= 0) uri.port else current.port,
                username = username.takeIf(String::isNotBlank),
                password = password.takeIf(String::isNotBlank),
                updatedAt = System.currentTimeMillis(),
            ))
        }
    }

    fun setCameraEnabled(id: String, enabled: Boolean) {
        val camera = cameras.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch { repository.update(camera.copy(enabled = enabled, updatedAt = System.currentTimeMillis())) }
    }

    fun setCameraProfile(id: String, profile: StreamProfile) {
        val camera = cameras.value.firstOrNull { it.id == id } ?: return
        if (profile == StreamProfile.SUB && camera.subStreamUrl == null) return
        viewModelScope.launch { repository.update(camera.copy(profile = profile, updatedAt = System.currentTimeMillis())) }
    }

    fun moveCamera(id: String, direction: Int) {
        val ordered = cameras.value.sortedBy { it.displayOrder }
        val index = ordered.indexOfFirst { it.id == id }
        val target = index + direction
        if (index < 0 || target !in ordered.indices) return
        val first = ordered[index]
        val second = ordered[target]
        viewModelScope.launch {
            repository.update(first.copy(displayOrder = second.displayOrder, updatedAt = System.currentTimeMillis()))
            repository.update(second.copy(displayOrder = first.displayOrder, updatedAt = System.currentTimeMillis()))
        }
    }
}

data class NvrAddState(val saving: Boolean = false, val error: String? = null, val addedNvrId: String? = null)
