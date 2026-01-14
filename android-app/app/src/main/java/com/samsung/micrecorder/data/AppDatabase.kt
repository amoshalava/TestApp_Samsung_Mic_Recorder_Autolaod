package com.samsung.micrecorder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for the Samsung Test Autoload Mic Record App.
 * Stores transcription history with a limit of 20 entries.
 */
@Database(
    entities = [TranscriptionHistory::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides access to the TranscriptionHistoryDao.
     */
    abstract fun transcriptionHistoryDao(): TranscriptionHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Gets the singleton database instance.
         * Uses double-check locking for thread safety.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "transcription_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
