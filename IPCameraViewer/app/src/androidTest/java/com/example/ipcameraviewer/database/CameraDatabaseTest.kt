package com.example.ipcameraviewer.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ipcameraviewer.model.CameraSourceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraDatabaseTest {
    private lateinit var database: CameraDatabase

    @Before fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), CameraDatabase::class.java,
        ).build()
    }

    @After fun closeDatabase() = database.close()

    @Test fun upsertAndDeleteCamera() = runBlocking {
        val row = CameraEntity(
            id = "test-camera", name = "Test camera", sourceType = CameraSourceType.MANUAL_RTSP.name,
            host = "camera.local", port = 554, rtspEndpoint = "rtsp://camera.local:554", subRtspEndpoint = null, httpEndpoint = null,
            onvifEndpoint = null, ptzServiceEndpoint = null, nvrId = null, channelNumber = null, streamProfile = "MAIN", profileName = null,
            profileToken = null, enabled = true,
            displayOrder = 0, audioEnabled = true, supportsAudio = null, supportsPtz = false,
            supportsTalkback = false, createdAt = 1L, updatedAt = 1L,
        )
        database.cameraDao().upsert(row)
        assertEquals(row, database.cameraDao().observeAll().first().single())
        database.cameraDao().deleteById(row.id)
        assertEquals(0, database.cameraDao().count())
    }
}
