package com.example.ipcameraviewer.onvif

import com.example.ipcameraviewer.model.CameraSourceType

interface CameraDiscovery {
    suspend fun discover(): List<DiscoveredOnvifDevice>
}

data class DiscoveredOnvifDevice(
    val endpointReference: String?,
    val endpoint: String,
    val scopes: List<String>,
    val hostAddress: String,
)

data class OnvifDeviceInfo(val manufacturer: String?, val model: String?, val firmware: String?, val serialNumber: String?)

data class OnvifProfile(
    val token: String,
    val name: String,
    val streamUri: String? = null,
    val hasAudio: Boolean = false,
)

data class OnvifCapabilities(
    val mediaEndpoint: String?,
    val ptzEndpoint: String?,
)

data class OnvifCredentials(val username: String, val password: String)

data class OnvifCameraDetails(
    val device: DiscoveredOnvifDevice,
    val info: OnvifDeviceInfo,
    val profiles: List<OnvifProfile>,
    val mediaEndpoint: String?,
    val ptzEndpoint: String?,
    val supportsPtz: Boolean,
    val credentials: OnvifCredentials,
)

interface CameraSource {
    val sourceType: CameraSourceType
    fun supports(source: DiscoveredOnvifDevice): Boolean
}

data class OnvifDeviceSummary(
    val device: DiscoveredOnvifDevice,
    val info: OnvifDeviceInfo,
    val profiles: List<OnvifProfile>,
    val supportsPtz: Boolean,
)

data class OnvifDiscoveryState(
    val scanning: Boolean = false,
    val inspecting: Boolean = false,
    val importing: Boolean = false,
    val devices: List<DiscoveredOnvifDevice> = emptyList(),
    val summary: OnvifDeviceSummary? = null,
    val importedCameraId: String? = null,
    val error: String? = null,
)
