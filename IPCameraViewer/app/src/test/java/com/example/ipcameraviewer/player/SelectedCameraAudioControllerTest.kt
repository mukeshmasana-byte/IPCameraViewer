package com.example.ipcameraviewer.player

import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectedCameraAudioControllerTest {
    @Test fun onlySelectedCameraCanOwnOutputAndDetachMutesIt() {
        val controller = SelectedCameraAudioController()
        val front = FakeVideoPlayer()
        val garage = FakeVideoPlayer()
        controller.attach("front", front, cameraAudioEnabled = true)
        controller.attach("garage", garage, cameraAudioEnabled = true)
        assertEquals(false, front.requests.last())
        assertEquals(false, garage.requests.last())

        controller.selectOwner("front")
        controller.selectOwner("garage")
        assertEquals(false, front.requests.last())
        assertEquals(true, garage.requests.last())

        controller.detach("garage")
        assertEquals(false, garage.requests.last())
    }

    private class FakeVideoPlayer : VideoPlayer {
        override val player: Player get() = error("The audio controller does not access the video player surface.")
        override val audioTrackAvailable: StateFlow<Boolean?> = MutableStateFlow(true)
        val requests = mutableListOf<Boolean>()
        override fun setAudioEnabled(enabled: Boolean) { requests += enabled }
        override fun release() = Unit
    }
}
