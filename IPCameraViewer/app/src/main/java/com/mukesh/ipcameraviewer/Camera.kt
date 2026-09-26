package com.mukesh.ipcameraviewer

data class Camera(
    val id: Long,
    var name: String,
    var rtspUrl: String,
    var onvifUrl: String = "",
    var username: String = "",
    var password: String = ""
)
