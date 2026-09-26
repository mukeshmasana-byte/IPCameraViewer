package com.example.ipcameraviewer.player

interface AudioController {
    fun selectOwner(cameraId: String?)
    fun attach(cameraId: String, player: VideoPlayer, cameraAudioEnabled: Boolean)
    fun detach(cameraId: String)
}

/** Guarantees that at most one attached camera player can produce sound. */
class SelectedCameraAudioController : AudioController {
    private data class AttachedPlayer(val player: VideoPlayer, val enabled: Boolean)
    private val players = mutableMapOf<String, AttachedPlayer>()
    private var ownerId: String? = null

    override fun selectOwner(cameraId: String?) {
        ownerId = cameraId
        applyVolumes()
    }

    override fun attach(cameraId: String, player: VideoPlayer, cameraAudioEnabled: Boolean) {
        players[cameraId] = AttachedPlayer(player, cameraAudioEnabled)
        applyVolumes()
    }

    override fun detach(cameraId: String) {
        players.remove(cameraId)?.player?.setAudioEnabled(false)
    }

    private fun applyVolumes() {
        players.forEach { (cameraId, attached) ->
            attached.player.setAudioEnabled(cameraId == ownerId && attached.enabled)
        }
    }
}
