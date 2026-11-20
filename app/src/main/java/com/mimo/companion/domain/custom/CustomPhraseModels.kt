package com.mimo.companion.domain.custom

import java.util.UUID

/**
 * The event / trigger that a [CustomPhrase] is linked to.
 *
 * NONE  = no auto-trigger; phrase only appears when user manually taps the companion.
 * TAP   = played for any tap (replaces / supplements the PhraseBank tap reply).
 * PRAYER  = played at prayer time (replaces the built-in prayer acknowledgement).
 * SLEEP   = played at a user-set bedtime (see CustomPhraseTrigger.sleepHour/Min).
 * REMINDER = played when any reminder fires.
 * SCHEDULE = played at an exact time each day (hour/minute in CustomPhraseTrigger).
 */
enum class PhraseEvent {
    NONE,
    TAP,
    PRAYER,
    SLEEP,
    REMINDER,
    SCHEDULE,
}

/**
 * A single user-created phrase, optionally paired with a custom voice clip.
 *
 * @param id         Stable UUID.
 * @param text       The text to show and speak (TTS falls back if no clipId).
 * @param clipId     ID of a [com.mimo.companion.speech.VoiceClip] in CustomVoiceStore, or null.
 * @param event      When to play this phrase automatically.
 * @param scheduleHour  Hour (0-23) for SCHEDULE / SLEEP triggers; -1 = unset.
 * @param scheduleMin   Minute (0-59) for SCHEDULE / SLEEP triggers; -1 = unset.
 * @param enabled    If false the phrase is kept but never triggered.
 * @param createdAt  Unix seconds for ordering.
 */
data class CustomPhrase(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val clipId: String? = null,
    val event: PhraseEvent = PhraseEvent.NONE,
    val scheduleHour: Int = -1,
    val scheduleMin: Int = -1,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis() / 1_000L,
)
