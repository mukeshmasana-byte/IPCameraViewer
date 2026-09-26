package com.mukesh.ipcameraviewer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cameras")
data class CameraEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val rtspUrl: String,
    val onvifUrl: String = "",
    val username: String = "",
    val passwordEncrypted: String = "" // In a real app this would be encrypted
)
