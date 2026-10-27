package com.mimo.companion.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.mimo.companion.domain.dialogue.Lang
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.UUID

enum class VoiceStatus { INITIALIZING, READY, LANGUAGE_MISSING, UNAVAILABLE }

/**
 * Android TextToSpeech wrapper. It never assumes a voice exists: if the engine or the language is
 * missing, [status] says so, speak() returns false and everything else in the app keeps working.
 *
 * [speaking] is true from the engine's onStart until done/error/stop; the face uses it for an
 * APPROXIMATE mouth movement (it is not synchronised to phonemes - the TTS API gives no timing we use).
 */
class SpeechController(context: Context) {
    private val app = context.applicationContext
    private val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var tts: TextToSpeech? = null
    private var focus: AudioFocusRequest? = null

    private val _status = MutableStateFlow(VoiceStatus.INITIALIZING)
    val status: StateFlow<VoiceStatus> get() = _status
    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> get() = _speaking

    fun init() {
        if (tts != null) return
        tts = TextToSpeech(app) { code ->
            if (code != TextToSpeech.SUCCESS) { _status.value = VoiceStatus.UNAVAILABLE; return@TextToSpeech }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { _speaking.value = true }
                override fun onDone(utteranceId: String?) { finished() }
                @Deprecated("required override") override fun onError(utteranceId: String?) { finished() }
                override fun onError(utteranceId: String?, errorCode: Int) { finished() }
                override fun onStop(utteranceId: String?, interrupted: Boolean) { finished() }
            })
            _status.value = VoiceStatus.READY
        }
    }

    private fun localeFor(lang: Lang) = if (lang == Lang.AR) Locale("ar") else Locale.ENGLISH

    /** Checks whether a voice for [lang] is installed (also updates [status]). */
    fun isLanguageAvailable(lang: Lang): Boolean {
        val engine = tts ?: return false
        if (_status.value == VoiceStatus.UNAVAILABLE || _status.value == VoiceStatus.INITIALIZING) return false
        val ok = engine.isLanguageAvailable(localeFor(lang)) >= TextToSpeech.LANG_AVAILABLE
        _status.value = if (ok) VoiceStatus.READY else VoiceStatus.LANGUAGE_MISSING
        return ok
    }

    /** @return false when nothing was spoken (engine missing, language missing, not initialised yet). */
    fun speak(text: String, lang: Lang, rate: Float): Boolean {
        val engine = tts ?: return false
        if (!isLanguageAvailable(lang)) return false
        engine.language = localeFor(lang)
        engine.setSpeechRate(rate)
        requestFocus()
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0.9f) }
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, UUID.randomUUID().toString())
        if (result != TextToSpeech.SUCCESS) { finished(); return false }
        return true
    }

    fun stop() { tts?.stop(); finished() }

    fun shutdown() { stop(); tts?.shutdown(); tts = null; _status.value = VoiceStatus.INITIALIZING }

    private fun requestFocus() {
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        // Ask other audio to duck (lower), never to stop: the companion must not interrupt music or videos abruptly.
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attrs).build()
        focus = req
        audio.requestAudioFocus(req)
    }

    private fun finished() {
        _speaking.value = false
        focus?.let { audio.abandonAudioFocusRequest(it) }; focus = null
    }
}
