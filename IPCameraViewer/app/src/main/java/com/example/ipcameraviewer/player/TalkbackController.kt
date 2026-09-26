package com.example.ipcameraviewer.player

import com.example.ipcameraviewer.model.Camera

sealed interface TalkbackResult {
    data object Started : TalkbackResult
    data class Unsupported(val reason: String) : TalkbackResult
    data class Failed(val reason: String) : TalkbackResult
}

/** Protocol-specific implementations can be added without making generic RTSP look bidirectional. */
interface TalkbackController {
    suspend fun start(camera: Camera): TalkbackResult
    suspend fun stop()
}

class UnsupportedTalkbackController : TalkbackController {
    override suspend fun start(camera: Camera): TalkbackResult = TalkbackResult.Unsupported(
        if (camera.supportsTalkback) "No compatible talkback transport is configured for this camera."
        else "This camera does not expose a supported talkback capability.",
    )

    override suspend fun stop() = Unit
}
