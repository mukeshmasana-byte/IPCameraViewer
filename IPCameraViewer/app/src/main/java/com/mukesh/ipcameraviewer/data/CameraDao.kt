package com.mukesh.ipcameraviewer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CameraDao {
    @Query("SELECT * FROM cameras")
    fun getAll(): Flow<List<CameraEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(camera: CameraEntity): Long

    @Update
    suspend fun update(camera: CameraEntity)

    @Delete
    suspend fun delete(camera: CameraEntity)
}
