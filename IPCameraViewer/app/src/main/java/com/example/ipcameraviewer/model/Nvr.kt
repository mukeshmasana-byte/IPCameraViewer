package com.example.ipcameraviewer.model

data class Nvr(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val createdAt: Long = System.currentTimeMillis(),
)
