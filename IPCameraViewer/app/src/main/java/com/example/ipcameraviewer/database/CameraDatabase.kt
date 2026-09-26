package com.example.ipcameraviewer.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CameraEntity::class, NvrEntity::class], version = 4, exportSchema = true)
abstract class CameraDatabase : RoomDatabase() {
    abstract fun cameraDao(): CameraDao
    abstract fun nvrDao(): NvrDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS nvrs (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, host TEXT NOT NULL, port INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
            }
        }
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cameras ADD COLUMN subRtspEndpoint TEXT")
            }
        }
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cameras ADD COLUMN ptzServiceEndpoint TEXT")
            }
        }
    }
}
