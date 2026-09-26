package com.example.ipcameraviewer.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NvrDao {
    @Query("SELECT * FROM nvrs ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<NvrEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(nvr: NvrEntity)

    @Query("DELETE FROM nvrs WHERE id = :id")
    suspend fun deleteById(id: String)
}
