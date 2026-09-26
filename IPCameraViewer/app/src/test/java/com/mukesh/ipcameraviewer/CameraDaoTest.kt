package com.mukesh.ipcameraviewer

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraDaoTest {
    @Test
    fun testCameraToEntityMapping() {
        val camera = Camera(id = 1, name = "Test Cam", rtspUrl = "rtsp://192.168.1.100")
        assertEquals(1, camera.id)
        assertEquals("Test Cam", camera.name)
        assertEquals("rtsp://192.168.1.100", camera.rtspUrl)
    }
}
