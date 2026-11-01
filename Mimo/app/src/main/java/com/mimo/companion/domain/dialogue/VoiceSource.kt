package com.mimo.companion.domain.dialogue

/**
 * Where the companion's voice comes from. Adding your own clips never removes the built-in voice:
 *  - TTS_ONLY   : built-in text-to-speech only (default; clips are kept but not played).
 *  - CLIPS_ONLY : plays one of the user's clips instead of speaking (text is still shown if enabled);
 *                 falls back to the built-in voice when there are no clips or a clip fails.
 *  - CLIP_THEN_TTS : plays a clip, then speaks the text with the built-in voice.
 */
enum class VoiceSource { TTS_ONLY, CLIPS_ONLY, CLIP_THEN_TTS }

/** Picks the next clip: random, but never the same one twice in a row when there is a choice. */
object ClipRotation {
    fun <T> pick(clips: List<T>, last: T?, random: () -> Int): T? {
        if (clips.isEmpty()) return null
        val pool = if (clips.size > 1 && last != null) clips.filter { it != last } else clips
        return pool[Math.floorMod(random(), pool.size)]
    }
}
