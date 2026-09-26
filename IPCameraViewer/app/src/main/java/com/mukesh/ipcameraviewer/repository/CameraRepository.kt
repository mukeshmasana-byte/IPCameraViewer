package com.mukesh.ipcameraviewer.repository

import com.mukesh.ipcameraviewer.Camera
import com.mukesh.ipcameraviewer.data.CameraDao
import com.mukesh.ipcameraviewer.data.CameraEntity
import com.mukesh.ipcameraviewer.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CameraRepository(private val cameraDao: CameraDao) {

    val allCameras: Flow<List<Camera>> = cameraDao.getAll().map { entities ->
        entities.map { it.toDomainModel() }
    }

    suspend fun insert(camera: Camera): Long {
        return cameraDao.insert(camera.toEntity())
    }

    suspend fun update(camera: Camera) {
        cameraDao.update(camera.toEntity())
    }

    suspend fun delete(camera: Camera) {
        cameraDao.delete(camera.toEntity())
    }
}

fun CameraEntity.toDomainModel() = Camera(
    id = id,
    name = name,
    rtspUrl = rtspUrl,
    onvifUrl = onvifUrl,
    username = username,
    password = if (passwordEncrypted.isNotBlank()) String(CryptoManager.decrypt(passwordEncrypted)) else ""
)

fun Camera.toEntity() = CameraEntity(
    id = if (id > 1000000000L) 0 else id, // Avoid timestamp ID for autoGenerate
    name = name,
    rtspUrl = rtspUrl,
    onvifUrl = onvifUrl,
    username = username,
    passwordEncrypted = if (password.isNotBlank()) CryptoManager.encrypt(password.toByteArray()) else ""
)
