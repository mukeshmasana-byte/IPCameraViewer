package com.example.ipcameraviewer.ptz

import com.example.ipcameraviewer.model.Camera

data class PtzVector(val pan: Float, val tilt: Float, val zoom: Float)
data class PtzPreset(val token: String, val name: String)

sealed interface PtzResult {
    data object Success : PtzResult
    data class Unsupported(val reason: String) : PtzResult
    data class Failed(val reason: String) : PtzResult
}

/** PTZ transport is deliberately separate from playback and can be backed by ONVIF or a vendor API. */
interface PtzController {
    suspend fun move(camera: Camera, vector: PtzVector): PtzResult
    suspend fun stop(camera: Camera): PtzResult
    suspend fun presets(camera: Camera): Result<List<PtzPreset>>
    suspend fun goToPreset(camera: Camera, preset: PtzPreset): PtzResult
}
