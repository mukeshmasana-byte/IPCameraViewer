package com.example.ipcameraviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.ipcameraviewer.ui.CameraViewModel
import com.example.ipcameraviewer.ui.IPCameraApp

class MainActivity : ComponentActivity() {
    private val viewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IPCameraApp(viewModel) }
    }
}
