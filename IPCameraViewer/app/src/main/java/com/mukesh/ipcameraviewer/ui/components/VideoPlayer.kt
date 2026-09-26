package com.mukesh.ipcameraviewer.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import com.mukesh.ipcameraviewer.Camera

@Composable
fun VideoPlayer(
    camera: Camera,
    columns: Int,
    isSelected: Boolean,
    onError: () -> Unit
) {
    val context = LocalContext.current
    val uri = remember(camera) { buildUri(camera) }

    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            val source = RtspMediaSource.Factory().setForceUseRtpTcp(true)
                .createMediaSource(MediaItem.fromUri(uri))
            setMediaSource(source)
            volume = if (columns == 1 || isSelected) 1f else 0f
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    onError()
                }
            })
            prepare()
        }
    }

    LaunchedEffect(columns, isSelected) {
        player.volume = if (columns == 1 || isSelected) 1f else 0f
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                this.player = player
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { playerView ->
            if (playerView.player != player) {
                playerView.player = player
            }
        }
    )
}

fun buildUri(c: Camera): String {
    if (c.username.isBlank()) return c.rtspUrl
    return try {
        val uri = android.net.Uri.parse(c.rtspUrl)
        val host = uri.host ?: return c.rtspUrl
        val port = if (uri.port > 0) ":${uri.port}" else ""
        val path = uri.encodedPath ?: ""
        val query = if (uri.encodedQuery != null) "?${uri.encodedQuery}" else ""
        "rtsp://${android.net.Uri.encode(c.username)}:${android.net.Uri.encode(c.password)}@$host$port$path$query"
    } catch (_: Exception) { c.rtspUrl }
}
