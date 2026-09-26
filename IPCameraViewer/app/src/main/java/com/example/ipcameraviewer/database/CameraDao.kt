package com.example.ipcameraviewer.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CameraDao {
    @Query("SELECT COUNT(*) FROM cameras")
    suspend fun count(): Int

    @Query("SELECT * FROM cameras ORDER BY displayOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<CameraEntity>>

    @Query("SELECT * FROM cameras WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): CameraEntity?

    @Query("SELECT * FROM cameras WHERE nvrId = :nvrId")
    suspend fun findByNvrId(nvrId: String): List<CameraEntity>

    @Query("SELECT * FROM cameras WHERE nvrId = :nvrId")
    fun observeByNvrId(nvrId: String): Flow<List<CameraEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(camera: CameraEntity)

    @Query("DELETE FROM cameras WHERE id = :id")
    suspend fun deleteById(id: String)
}
