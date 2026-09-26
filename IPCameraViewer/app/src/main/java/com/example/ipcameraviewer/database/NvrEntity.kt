package com.example.ipcameraviewer.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ipcameraviewer.model.Nvr

@Entity(tableName = "nvrs")
data class NvrEntity(
    @PrimaryKey val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val createdAt: Long,
)

fun Nvr.toEntity() = NvrEntity(id, name, host, port, createdAt)
fun NvrEntity.toDomain() = Nvr(id, name, host, port, createdAt)
