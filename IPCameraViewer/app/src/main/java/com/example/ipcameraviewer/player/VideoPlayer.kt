package com.example.ipcameraviewer.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface VideoPlayer {
    val player: Player
    val audioTrackAvailable: StateFlow<Boolean?>
    fun setAudioEnabled(enabled: Boolean)
    fun release()
}

fun interface VideoPlayerFactory {
    fun create(context: Context, rtspUrl: String): VideoPlayer
}

class Media3VideoPlayerFactory : VideoPlayerFactory {
    override fun create(context: Context, rtspUrl: String): VideoPlayer {
        val exo = ExoPlayer.Builder(context.applicationContext).build().apply {
            setMediaSource(RtspMediaSource.Factory().createMediaSource(MediaItem.fromUri(rtspUrl)))
            volume = 0f
            prepare()
            playWhenReady = true
        }
        return Media3VideoPlayer(exo)
    }
}

private class Media3VideoPlayer(private val exoPlayer: ExoPlayer) : VideoPlayer {
    private val mutableAudioTrackAvailable = MutableStateFlow<Boolean?>(null)
    override val audioTrackAvailable = mutableAudioTrackAvailable.asStateFlow()
    private var outputRequested = false

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                mutableAudioTrackAvailable.value = tracks.groups.any { it.type == C.TRACK_TYPE_AUDIO && it.isSupported }
                applyVolume()
            }
        })
    }

    override val player: Player get() = exoPlayer
    override fun setAudioEnabled(enabled: Boolean) { outputRequested = enabled; applyVolume() }
    override fun release() { exoPlayer.release() }

    private fun applyVolume() {
        exoPlayer.volume = if (outputRequested && mutableAudioTrackAvailable.value == true) 1f else 0f
    }
}

fun playbackErrorMessage(error: PlaybackException): String = when (error.errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Camera unreachable. Check the network and stream URL."
    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> "This device cannot decode the camera's video format."
    else -> "Stream unavailable. Check the camera and RTSP address."
}
