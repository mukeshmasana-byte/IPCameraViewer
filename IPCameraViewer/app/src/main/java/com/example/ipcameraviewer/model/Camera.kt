package com.example.ipcameraviewer.model

enum class CameraSourceType { ONVIF, NVR_CHANNEL, MANUAL_RTSP, MJPEG }

enum class StreamProfile { MAIN, SUB, CUSTOM }

data class Camera(
    val id: String,
    val name: String,
    val sourceType: CameraSourceType,
    val streamUrl: String,
    val subStreamUrl: String? = null,
    val httpUrl: String? = null,
    val onvifEndpoint: String? = null,
    val ptzEndpoint: String? = null,
    val username: String? = null,
    val password: String? = null,
    val host: String? = null,
    val port: Int? = null,
    val nvrId: String? = null,
    val channelNumber: Int? = null,
    val profile: StreamProfile = StreamProfile.MAIN,
    val profileName: String? = null,
    val profileToken: String? = null,
    val enabled: Boolean = true,
    val displayOrder: Int = 0,
    val audioEnabled: Boolean = true,
    val supportsAudio: Boolean? = null,
    val supportsPtz: Boolean = false,
    val supportsTalkback: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class DashboardLayout(val columns: Int, val cameraCount: Int, val label: String) {
    SINGLE(1, 1, "1 × 1"), QUAD(2, 4, "2 × 2"), NINE(3, 9, "3 × 3")
}
