package com.mimo.companion.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.mimo.companion.domain.voice.VoiceInputResult
import java.util.Locale

/**
 * One-shot voice input using the on-device SpeechRecognizer.
 *
 * Design:
 * - Uses ACTION_RECOGNIZE_SPEECH with LANGUAGE_MODEL_FREE_FORM so the
 *   system handles the microphone entirely (no raw audio in the app).
 * - Must be called on the **main thread** (SpeechRecognizer requires it).
 * - [onResult] is also delivered on the main thread.
 * - Call [cancel] to abort a running session (e.g. screen rotation).
 *
 * Permission: RECORD_AUDIO — checked here; the caller must have already
 * requested it via ActivityResultContracts or we return NoPermission.
 */
class VoiceInputLauncher(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    /**
     * Starts a new recognition session.
     * If a session is already active it is cancelled first.
     */
    fun start(onResult: (VoiceInputResult) -> Unit) {
        // 1. Permission check
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            onResult(VoiceInputResult.NoPermission); return
        }

        // 2. Availability check
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onResult(VoiceInputResult.Unavailable); return
        }

        // 3. Cancel any previous session
        cancel()

        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr

        sr.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = list?.firstOrNull()?.trim() ?: ""
                release()
                onResult(if (best.isNotEmpty()) VoiceInputResult.Ok(best) else VoiceInputResult.NoMatch)
            }

            override fun onError(error: Int) {
                release()
                onResult(
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceInputResult.NoMatch
                        else -> VoiceInputResult.Error(error)
                    }
                )
            }

            // Not used — kept to satisfy the interface
            override fun onReadyForSpeech(params: Bundle) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle) = Unit
            override fun onEvent(eventType: Int, params: Bundle) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // Use the app's current locale so recognition matches the user's language setting
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        sr.startListening(intent)
    }

    /** Stops the current session without delivering a result. */
    fun cancel() { release() }

    private fun release() {
        recognizer?.destroy()
        recognizer = null
    }
}
