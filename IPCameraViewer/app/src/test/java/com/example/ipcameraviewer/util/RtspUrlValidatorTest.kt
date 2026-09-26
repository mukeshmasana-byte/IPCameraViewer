package com.example.ipcameraviewer.util

import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test

class RtspUrlValidatorTest {
    @Test fun acceptsRtspWithHostAndPath() {
        assertEquals("rtsp://camera.local:8554/stream/main", RtspUrlValidator.normalize(" rtsp://camera.local:8554/stream/main "))
    }

    @Test fun acceptsCredentialsWithoutRewritingThem() {
        val url = "rtsp://viewer:p%40ss@192.168.1.4:554/live/ch00_0"
        assertEquals(url, RtspUrlValidator.normalize(url))
    }

    @Test fun rejectsWrongSchemeMissingHostAndInvalidPort() {
        assertNull(RtspUrlValidator.normalize("http://camera.local/live"))
        assertNull(RtspUrlValidator.normalize("rtsp:///live"))
        assertNull(RtspUrlValidator.normalize("rtsp://camera.local:70000/live"))
        assertNull(RtspUrlValidator.normalize("rtsp://camera.local:0/live"))
        assertNull(RtspUrlValidator.normalize("not a url"))
    }

    @Test fun acceptsHttpAndHttpsButRejectsOtherSchemes() {
        assertEquals("http://camera.local:8080/mjpeg", HttpUrlValidator.normalize(" http://camera.local:8080/mjpeg "))
        assertEquals("https://camera.local/stream", HttpUrlValidator.normalize("https://camera.local/stream"))
        assertNull(HttpUrlValidator.normalize("rtsp://camera.local/live"))
        assertNull(HttpUrlValidator.normalize("http:///mjpeg"))
        assertNull(HttpUrlValidator.normalize("http://camera.local:0/live"))
    }
}
