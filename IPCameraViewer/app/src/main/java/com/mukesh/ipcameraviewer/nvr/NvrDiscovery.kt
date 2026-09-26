package com.mukesh.ipcameraviewer.nvr

import com.mukesh.ipcameraviewer.data.CameraEntity

interface NvrDiscovery {
    suspend fun discoverChannels(ip: String, port: Int): List<CameraEntity>
}
