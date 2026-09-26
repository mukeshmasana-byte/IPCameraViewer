package com.mukesh.ipcameraviewer.ptz

interface PtzController {
    val isSupported: Boolean
    suspend fun moveUp()
    suspend fun moveDown()
    suspend fun moveLeft()
    suspend fun moveRight()
    suspend fun zoomIn()
    suspend fun zoomOut()
    suspend fun stopMove()
    suspend fun goToPreset(presetToken: String)
}
