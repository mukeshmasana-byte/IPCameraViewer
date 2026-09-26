package com.example.ipcameraviewer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraModelTest {
    @Test fun sourceTypesAndLayoutsExposeExpectedGridSizes() {
        assertEquals(4, DashboardLayout.QUAD.cameraCount)
        assertEquals(9, DashboardLayout.NINE.cameraCount)
        assertTrue(CameraSourceType.entries.contains(CameraSourceType.NVR_CHANNEL))
    }
}
