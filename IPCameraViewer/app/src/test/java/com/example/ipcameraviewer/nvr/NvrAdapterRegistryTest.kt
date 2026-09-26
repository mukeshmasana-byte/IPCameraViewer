package com.example.ipcameraviewer.nvr

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class NvrAdapterRegistryTest {
    @Test fun unknownNvrReportsManualFallback() = runBlocking {
        val result = NvrAdapterRegistry().listChannels(NvrConnection("id", "nvr.local", 554, null, null), com.example.ipcameraviewer.onvif.OnvifCredentials("user", "pass"))
        assertTrue(result is NvrChannelResult.Unsupported)
        assertTrue((result as NvrChannelResult.Unsupported).explanation.contains("RTSP template"))
    }
}
