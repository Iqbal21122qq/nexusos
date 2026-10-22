package com.nexusos.agent.core.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceForegroundService : Service(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "NexusVoiceService"
        private const val CHANNEL_ID = "nexus_voice_channel"
        private const val NOTIF_ID = 1001

        var instance: VoiceForegroundService? = null
            private set

        private val _voiceCommands = MutableSharedFlow<String>(extraBufferCapacity = 5)
        val voiceCommands: SharedFlow<String> = _voiceCommands
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isListening = false
    private var wakeWordEnabled = false // Set to true if requiring "Hey Nexus" prefix

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("NexusOS Agent সক্রিয় আছে..."))

        tts = TextToSpeech(this, this)
        initSpeechRecognizer()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val banglaLocale = Locale.forLanguageTag("bn-BD")
            val result = tts?.setLanguage(banglaLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.US
            }
            tts?.setSpeechRate(1.05f)
        }
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech Recognition is not available on this device.")
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                }

                override fun onBeginningOfSpeech() {
                    // VAD Barge-In: Stop AI from speaking if user starts talking
                    if (tts?.isSpeaking == true) {
                        tts?.stop()
                    }
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                    // Restart listening loop automatically for continuous agent mode
                    startListeningLoop()
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull()?.trim().orEmpty()

                    if (spokenText.isNotEmpty()) {
                        handleRecognizedSpeech(spokenText)
                    }
                    startListeningLoop()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    // Interrupt TTS immediately if partial words are detected
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    if (partial.length > 2 && tts?.isSpeaking == true) {
                        tts?.stop()
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        startListeningLoop()
    }

    private fun handleRecognizedSpeech(spokenText: String) {
        Log.i(TAG, "Heard: $spokenText")
        if (wakeWordEnabled) {
            val lower = spokenText.lowercase()
            if (lower.startsWith("hey nexus") || lower.startsWith("হেই নেক্সাস")) {
                val cleanCommand = spokenText.replaceFirst(Regex("(?i)^(hey nexus|হেই নেক্সাস)"), "").trim()
                if (cleanCommand.isNotEmpty()) {
                    serviceScope.launch { _voiceCommands.emit(cleanCommand) }
                } else {
                    speak("জি বস, বলুন কী করতে হবে?")
                }
            }
        } else {
            serviceScope.launch { _voiceCommands.emit(spokenText) }
        }
    }

    fun startListeningLoop() {
        if (isListening) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-US", "bn-BD"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening: ${e.message}")
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nexus_utt_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NexusOS Voice Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NexusOS Autonomous Agent")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
