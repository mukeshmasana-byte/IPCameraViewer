package com.example.ipcameraviewer.util

import org.junit.Assert.assertEquals
import org.junit.Test

class RtspCredentialsTest {
    @Test fun addsEncodedCredentialsToAnNvrTemplateExpansion() {
        assertEquals(
            "rtsp://viewer:p%40ss@192.168.1.10:554/ch/2",
            RtspCredentials.playbackUrl("rtsp://192.168.1.10:554/ch/2", "viewer", "p@ss"),
        )
    }

    @Test fun keepsAlreadyEmbeddedCredentials() {
        val url = "rtsp://viewer:p%40ss@192.168.1.10:554/ch/2"
        assertEquals(url, RtspCredentials.playbackUrl(url, "other", "secret"))
    }
}
