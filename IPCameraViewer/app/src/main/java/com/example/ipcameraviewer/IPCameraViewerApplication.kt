package com.example.ipcameraviewer

import android.app.Application
import com.example.ipcameraviewer.data.CameraRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class IPCameraViewerApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var cameraRepository: CameraRepository
        private set

    override fun onCreate() {
        super.onCreate()
        cameraRepository = CameraRepository(this, applicationScope)
    }
}
