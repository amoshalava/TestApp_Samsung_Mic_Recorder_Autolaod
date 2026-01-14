package com.samsung.micrecorder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for TranscriptionHistory entity.
 * Provides methods to interact with the transcription_history table.
 */
@Dao
interface TranscriptionHistoryDao {

    /**
     * Insert a new transcription entry.
     * @param entry The transcription to insert
     * @return The ID of the newly inserted entry
     */
    @Insert
    suspend fun insert(entry: TranscriptionHistory): Long

    /**
     * Get all transcription entries ordered by timestamp (newest first).
     * Returns as Flow for reactive updates.
     */
    @Query("SELECT * FROM transcription_history ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<TranscriptionHistory>>

    /**
     * Get count of entries in the database.
     */
    @Query("SELECT COUNT(*) FROM transcription_history")
    suspend fun getCount(): Int

    /**
     * Delete the oldest entries to maintain the 20-entry limit.
     * @param limit Number of entries to keep (default 20)
     */
    @Query("""
        DELETE FROM transcription_history
        WHERE id NOT IN (
            SELECT id FROM transcription_history
            ORDER BY timestamp DESC
            LIMIT :limit
        )
    """)
    suspend fun deleteOldestEntries(limit: Int = 20)

    /**
     * Delete a specific entry by ID.
     */
    @Query("DELETE FROM transcription_history WHERE id = :entryId")
    suspend fun deleteById(entryId: Long)

    /**
     * Delete all entries (for testing/debugging).
     */
    @Query("DELETE FROM transcription_history")
    suspend fun deleteAll()
}
