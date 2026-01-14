package com.samsung.micrecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.samsung.micrecorder.data.AppDatabase
import com.samsung.micrecorder.data.TranscriptionHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Foreground service that continuously listens for the wake word "SuperDuper"
 * and performs speech-to-text conversion after detection.
 */
class MicRecorderService : Service() {

    private val tag = "MicRecorderService"
    private val channelId = "mic_recorder_service"
    private val notificationId = 1001
    private val wakeWord = "superduper"

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isWakeWordDetected = false

    private lateinit var serviceScope: CoroutineScope
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "Service onCreate")

        serviceScope = CoroutineScope(Dispatchers.IO + Job())
        database = AppDatabase.getInstance(applicationContext)

        createNotificationChannel()
        initializeSpeechRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "Service onStartCommand")

        try {
            // 1. Check if Microphone is available (not busy)
            if (!isMicAvailable()) {
                showToast("Microphone is currently in use by another app.")
                saveError("Microphone busy or unavailable")
                stopSelf()
                return START_NOT_STICKY
            }

            // 2. Start as foreground service
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ requires specific foreground service type
                startForeground(
                    notificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    notificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(notificationId, notification)
            }

            // 3. Start listening for wake word
            startListening()

        } catch (se: SecurityException) {
            Log.e(tag, "SecurityException: Missing Microphone Permissions", se)
            showToast("Error: Missing Microphone Permissions.")
            saveError("Missing Microphone Permissions")
            stopSelf()
        } catch (ie: IllegalStateException) {
            Log.e(tag, "IllegalStateException: Microphone busy or failed", ie)
            showToast("Microphone is busy or failed to initialize.")
            saveError("Microphone busy or failed to initialize")
            stopSelf()
        } catch (e: Exception) {
            Log.e(tag, "Unexpected error in onStartCommand", e)
            showToast("An unexpected error occurred.")
            saveError("Unexpected error: ${e.message}")
            stopSelf()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d(tag, "Service onDestroy")
        stopListening()
        speechRecognizer?.destroy()
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Checks if the microphone is currently available and not in use by another app.
     */
    private fun isMicAvailable(): Boolean {
        var recorder: AudioRecord? = null
        return try {
            val bufferSize = AudioRecord.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            if (bufferSize <= 0) return false

            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                return false
            }

            recorder.startRecording()
            val isRecording = recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING
            recorder.stop()
            isRecording
        } catch (e: Exception) {
            Log.e(tag, "Error checking mic availability", e)
            false
        } finally {
            recorder?.release()
        }
    }

    /**
     * Shows a toast message on the main thread.
     */
    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Creates notification channel for Android O and above.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Recording Service"
            val descriptionText = "Microphone recording service for wake word detection"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Creates the persistent notification shown while service is running.
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Samsung Mic Recorder")
            .setContentText("Listening for wake word...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    /**
     * Initializes the SpeechRecognizer instance.
     */
    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(recognitionListener)
            Log.d(tag, "SpeechRecognizer initialized")
        } else {
            Log.e(tag, "Speech recognition not available")
            saveError("Speech recognition not available on this device")
        }
    }

    /**
     * Starts listening for speech input.
     */
    private fun startListening() {
        if (isListening) {
            Log.d(tag, "Already listening, skipping")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
            Log.d(tag, "Started listening")
        } catch (e: Exception) {
            Log.e(tag, "Error starting speech recognizer", e)
            saveError("Error starting speech recognizer: ${e.message}")
            isListening = false
        }
    }

    /**
     * Stops listening for speech input.
     */
    private fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            Log.d(tag, "Stopped listening")
        }
    }

    /**
     * RecognitionListener implementation for handling speech recognition callbacks.
     */
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: android.os.Bundle?) {
            Log.d(tag, "Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(tag, "Beginning of speech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // RMS volume level, can be used for visual feedback
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Audio buffer received
        }

        override fun onEndOfSpeech() {
            Log.d(tag, "End of speech")
            isListening = false
        }

        override fun onError(error: Int) {
            Log.e(tag, "Recognition error: $error")
            isListening = false

            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> {
                    // No speech detected, restart listening
                    Log.d(tag, "No match, restarting")
                }
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    Log.d(tag, "Speech timeout, restarting")
                }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    Log.e(tag, "Insufficient permissions for recognition")
                    saveError("Insufficient permissions for recognition")
                    stopSelf()
                    return
                }
                else -> {
                    saveError("Recognition error: $error")
                }
            }

            // Restart listening after brief delay if service is still running
            Handler(Looper.getMainLooper()).postDelayed({
                isWakeWordDetected = false
                startListening()
            }, 500)
        }

        override fun onResults(results: android.os.Bundle?) {
            Log.d(tag, "onResults")
            isListening = false

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.let { processResults(it) }

            // Restart listening after processing
            Handler(Looper.getMainLooper()).postDelayed({
                isWakeWordDetected = false
                startListening()
            }, 500)
        }

        override fun onPartialResults(partialResults: android.os.Bundle?) {
            Log.d(tag, "onPartialResults")
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.let { results ->
                if (!isWakeWordDetected) {
                    checkForWakeWord(results)
                }
            }
        }

        override fun onEvent(eventType: Int, params: android.os.Bundle?) {
            // Reserved for future use
        }
    }

    /**
     * Checks if any of the results contain the wake word.
     */
    private fun checkForWakeWord(results: List<String>) {
        for (result in results) {
            val normalized = result.lowercase(Locale.ROOT).replace(" ", "")
            if (normalized.contains(wakeWord)) {
                Log.d(tag, "Wake word detected: $result")
                isWakeWordDetected = true
                break
            }
        }
    }

    /**
     * Processes final recognition results.
     * If wake word was detected, saves the transcription.
     */
    private fun processResults(results: List<String>) {
        if (results.isEmpty()) return

        val bestMatch = results[0]
        Log.d(tag, "Best match: $bestMatch")

        // Check if this contains the wake word
        checkForWakeWord(results)

        if (isWakeWordDetected) {
            // Remove wake word from transcription
            val cleaned = bestMatch
                .replace(Regex("super\\s*duper", RegexOption.IGNORE_CASE), "")
                .trim()

            if (cleaned.isNotEmpty()) {
                saveTranscription(cleaned)
            }
        }
    }

    /**
     * Saves a transcription to the database.
     */
    private fun saveTranscription(text: String) {
        serviceScope.launch {
            try {
                val entry = TranscriptionHistory(
                    text = text,
                    timestamp = System.currentTimeMillis(),
                    isError = false
                )
                database.transcriptionHistoryDao().insert(entry)
                database.transcriptionHistoryDao().deleteOldestEntries(20)
                Log.d(tag, "Saved transcription: $text")
            } catch (e: Exception) {
                Log.e(tag, "Error saving transcription", e)
            }
        }
    }

    /**
     * Saves an error message to the database.
     */
    private fun saveError(message: String) {
        serviceScope.launch {
            try {
                val entry = TranscriptionHistory(
                    text = message,
                    timestamp = System.currentTimeMillis(),
                    isError = true
                )
                database.transcriptionHistoryDao().insert(entry)
                database.transcriptionHistoryDao().deleteOldestEntries(20)
                Log.d(tag, "Saved error: $message")
            } catch (e: Exception) {
                Log.e(tag, "Error saving error message", e)
            }
        }
    }
}
