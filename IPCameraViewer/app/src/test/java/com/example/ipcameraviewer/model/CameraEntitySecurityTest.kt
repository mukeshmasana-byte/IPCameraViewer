package com.example.ipcameraviewer.model

import com.example.ipcameraviewer.database.CameraSecrets
import com.example.ipcameraviewer.database.toDomain
import com.example.ipcameraviewer.database.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CameraEntitySecurityTest {
    @Test fun databaseEntityContainsOnlyUrlOriginWhileSecretsKeepFullUrl() {
        val fullUrl = "rtsp://viewer:p%40ss@camera.local:554/live/main?token=private"
        val camera = Camera("id", "Front", CameraSourceType.MANUAL_RTSP, fullUrl, host = "camera.local", port = 554)
        val entity = camera.toEntity()
        assertEquals("rtsp://camera.local:554", entity.rtspEndpoint)
        assertFalse(entity.rtspEndpoint.orEmpty().contains("viewer"))
        assertFalse(entity.rtspEndpoint.orEmpty().contains("private"))
        assertEquals(fullUrl, entity.toDomain(CameraSecrets(fullUrl, null, null)).streamUrl)
    }
}
