package com.mimo.companion.domain.voice

/**
 * Result of a single voice-input session.
 * Pure Kotlin — no Android imports.
 */
sealed interface VoiceInputResult {
    /** Recognition succeeded; [text] is the best hypothesis, trimmed. */
    data class Ok(val text: String) : VoiceInputResult
    /** Microphone permission was not granted. */
    object NoPermission : VoiceInputResult
    /** SpeechRecognizer is not available on this device. */
    object Unavailable : VoiceInputResult
    /** The user spoke but nothing was understood. */
    object NoMatch : VoiceInputResult
    /** Any other error from the recogniser; [code] is SpeechRecognizer.ERROR_*. */
    data class Error(val code: Int) : VoiceInputResult
}

/**
 * Processing helpers that can be unit-tested without Android.
 */
object VoiceInputHelper {

    /**
     * Cleans up a raw recognition hypothesis for use as a reminder name:
     * - Trims whitespace
     * - Capitalises the first character
     * - Removes a trailing period added by some recognition engines
     */
    fun clean(raw: String): String {
        val t = raw.trim().trimEnd('.')
        return if (t.isEmpty()) t else t[0].uppercaseChar() + t.drop(1)
    }

    /**
     * Returns true when [text] is long enough to be a usable reminder name
     * (at least 2 characters after cleaning).
     */
    fun isUsable(text: String): Boolean = clean(text).length >= 2
}
