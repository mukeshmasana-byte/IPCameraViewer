package com.example.ipcameraviewer.data

import androidx.room.Room
import androidx.room.withTransaction
import android.content.Context
import com.example.ipcameraviewer.database.CameraDatabase
import com.example.ipcameraviewer.database.CameraSecrets
import com.example.ipcameraviewer.database.toDomain
import com.example.ipcameraviewer.database.toEntity
import com.example.ipcameraviewer.database.toDomain as nvrToDomain
import com.example.ipcameraviewer.database.toEntity as nvrToEntity
import com.example.ipcameraviewer.model.Camera
import com.example.ipcameraviewer.model.Nvr
import com.example.ipcameraviewer.model.CameraSourceType
import com.example.ipcameraviewer.security.SecretStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CameraRepository(
    context: Context,
    scope: CoroutineScope,
) {
    private val database = Room.databaseBuilder(context.applicationContext, CameraDatabase::class.java, "cameras.db")
        .addMigrations(CameraDatabase.MIGRATION_1_2, CameraDatabase.MIGRATION_2_3, CameraDatabase.MIGRATION_3_4)
        .build()
    private val dao = database.cameraDao()
    private val nvrDao = database.nvrDao()
    private val secrets = SecretStore(context)

    val cameras = dao.observeAll().map { rows ->
        rows.map { row -> row.toDomain(secrets.get(row.id)) }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    val nvrs = nvrDao.observeAll().map { rows -> rows.map { it.nvrToDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    suspend fun add(camera: Camera): Boolean {
        if (dao.count() >= MAX_CAMERAS) return false
        withContext(Dispatchers.IO) {
            secrets.put(camera.id, CameraSecrets(camera.streamUrl, camera.httpUrl, camera.onvifEndpoint, camera.username, camera.password, camera.subStreamUrl, camera.ptzEndpoint))
        }
        try {
            dao.upsert(camera.toEntity())
        } catch (failure: Throwable) {
            withContext(Dispatchers.IO) { secrets.remove(camera.id) }
            throw failure
        }
        return true
    }

    suspend fun remove(id: String) {
        dao.deleteById(id)
        withContext(Dispatchers.IO) { secrets.remove(id) }
    }

    suspend fun update(camera: Camera) {
        val previousSecrets = withContext(Dispatchers.IO) { secrets.get(camera.id) }
        withContext(Dispatchers.IO) {
            secrets.put(camera.id, CameraSecrets(camera.streamUrl, camera.httpUrl, camera.onvifEndpoint, camera.username, camera.password, camera.subStreamUrl, camera.ptzEndpoint))
        }
        try {
            dao.upsert(camera.toEntity())
        } catch (failure: Throwable) {
            withContext(Dispatchers.IO) {
                if (previousSecrets != null) secrets.put(camera.id, previousSecrets) else secrets.remove(camera.id)
            }
            throw failure
        }
    }

    suspend fun addNvr(nvr: Nvr, username: String, password: String, channels: List<Camera>): Boolean {
        require(channels.all { it.nvrId == nvr.id && it.sourceType == CameraSourceType.NVR_CHANNEL })
        if (channels.isEmpty() || dao.count() + channels.size > MAX_CAMERAS) return false
        val secretIds = mutableListOf("$NVR_SECRET_PREFIX${nvr.id}")
        try {
            withContext(Dispatchers.IO) {
                secrets.put(secretIds.last(), CameraSecrets("", null, null, username, password))
                channels.forEach { channel ->
                    secrets.put(channel.id, CameraSecrets(channel.streamUrl, channel.httpUrl, channel.onvifEndpoint, username, password, channel.subStreamUrl, channel.ptzEndpoint))
                    secretIds += channel.id
                }
            }
            database.withTransaction {
                nvrDao.upsert(nvr.nvrToEntity())
                channels.forEach { dao.upsert(it.toEntity()) }
            }
        } catch (failure: Throwable) {
            withContext(Dispatchers.IO) { secretIds.forEach(secrets::remove) }
            throw failure
        }
        return true
    }

    suspend fun removeNvr(id: String) {
        val channels = dao.findByNvrId(id)
        database.withTransaction {
            channels.forEach { dao.deleteById(it.id) }
            nvrDao.deleteById(id)
        }
        withContext(Dispatchers.IO) {
            channels.forEach { secrets.remove(it.id) }
            secrets.remove("$NVR_SECRET_PREFIX$id")
        }
    }

    suspend fun close() = database.close()

    private companion object {
        const val MAX_CAMERAS = 9
        const val NVR_SECRET_PREFIX = "nvr_"
    }
}
