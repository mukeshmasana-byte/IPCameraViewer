package com.example.ipcameraviewer.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ipcameraviewer.model.Camera
import com.example.ipcameraviewer.model.CameraSourceType
import com.example.ipcameraviewer.model.StreamProfile

/** Stores public camera metadata and origin-only URLs; full URLs live encrypted in [SecretStore]. */
@Entity(tableName = "cameras")
data class CameraEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sourceType: String,
    val host: String?,
    val port: Int?,
    val rtspEndpoint: String?,
    val subRtspEndpoint: String?,
    val httpEndpoint: String?,
    val onvifEndpoint: String?,
    val ptzServiceEndpoint: String?,
    val nvrId: String?,
    val channelNumber: Int?,
    val streamProfile: String,
    val profileName: String?,
    val profileToken: String?,
    val enabled: Boolean,
    val displayOrder: Int,
    val audioEnabled: Boolean,
    val supportsAudio: Boolean?,
    val supportsPtz: Boolean,
    val supportsTalkback: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

data class CameraSecrets(
    val streamUrl: String,
    val httpUrl: String?,
    val onvifEndpoint: String?,
    val username: String? = null,
    val password: String? = null,
    val subStreamUrl: String? = null,
    val ptzEndpoint: String? = null,
)

fun Camera.toEntity() = CameraEntity(
    id = id,
    name = name,
    sourceType = sourceType.name,
    host = host,
    port = port,
    rtspEndpoint = if (sourceType == CameraSourceType.MJPEG) null else endpointOnly(streamUrl),
    subRtspEndpoint = subStreamUrl?.let(::endpointOnly),
    httpEndpoint = (httpUrl ?: streamUrl.takeIf { sourceType == CameraSourceType.MJPEG })?.let(::endpointOnly),
    onvifEndpoint = onvifEndpoint?.let(::endpointOnly),
    ptzServiceEndpoint = ptzEndpoint?.let(::endpointOnly),
    nvrId = nvrId,
    channelNumber = channelNumber,
    streamProfile = profile.name,
    profileName = profileName,
    profileToken = profileToken,
    enabled = enabled,
    displayOrder = displayOrder,
    audioEnabled = audioEnabled,
    supportsAudio = supportsAudio,
    supportsPtz = supportsPtz,
    supportsTalkback = supportsTalkback,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun CameraEntity.toDomain(secrets: CameraSecrets?) = Camera(
    id = id,
    name = name,
    sourceType = runCatching { CameraSourceType.valueOf(sourceType) }.getOrDefault(CameraSourceType.MANUAL_RTSP),
    streamUrl = secrets?.streamUrl.orEmpty(),
    subStreamUrl = secrets?.subStreamUrl,
    httpUrl = secrets?.httpUrl,
    onvifEndpoint = secrets?.onvifEndpoint,
    ptzEndpoint = secrets?.ptzEndpoint,
    username = secrets?.username,
    password = secrets?.password,
    host = host,
    port = port,
    nvrId = nvrId,
    channelNumber = channelNumber,
    profile = runCatching { StreamProfile.valueOf(streamProfile) }.getOrDefault(StreamProfile.MAIN),
    profileName = profileName,
    profileToken = profileToken,
    enabled = enabled,
    displayOrder = displayOrder,
    audioEnabled = audioEnabled,
    supportsAudio = supportsAudio,
    supportsPtz = supportsPtz,
    supportsTalkback = supportsTalkback,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun endpointOnly(url: String): String? = runCatching {
    val uri = java.net.URI(url.trim())
    if (uri.scheme.isNullOrBlank() || uri.host.isNullOrBlank()) return null
    java.net.URI(uri.scheme, null, uri.host, uri.port, null, null, null).toASCIIString()
}.getOrNull()
