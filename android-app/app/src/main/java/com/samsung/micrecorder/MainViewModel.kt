package com.samsung.micrecorder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.micrecorder.data.AppDatabase
import com.samsung.micrecorder.data.TranscriptionHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for MainActivity.
 * Manages UI state including transcription history, microphone status, and permission status.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).transcriptionHistoryDao()

    // Transcription history from database
    private val _historyFlow = MutableStateFlow<List<TranscriptionHistory>>(emptyList())
    val historyFlow: StateFlow<List<TranscriptionHistory>> = _historyFlow.asStateFlow()

    init {
        // Collect history from database
        viewModelScope.launch {
            dao.getAllEntries().collect { history ->
                _historyFlow.value = history
            }
        }
    }

    // Microphone status (listening or inactive)
    private val _isMicActive = MutableStateFlow(false)
    val isMicActive: StateFlow<Boolean> = _isMicActive.asStateFlow()

    // Permission status
    private val _areAllPermissionsGranted = MutableStateFlow(false)
    val areAllPermissionsGranted: StateFlow<Boolean> = _areAllPermissionsGranted.asStateFlow()

    /**
     * Update microphone active status.
     */
    fun setMicActive(active: Boolean) {
        _isMicActive.value = active
    }

    /**
     * Update permission status.
     */
    fun setPermissionsGranted(granted: Boolean) {
        _areAllPermissionsGranted.value = granted
    }
}
