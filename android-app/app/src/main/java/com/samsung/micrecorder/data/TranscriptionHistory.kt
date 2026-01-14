package com.samsung.micrecorder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database entity representing a transcription history entry.
 * Stores speech-to-text results with timestamps.
 */
@Entity(tableName = "transcription_history")
data class TranscriptionHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val text: String,

    val timestamp: Long,

    val isError: Boolean = false
)
