package com.mimo.companion.domain.focus

/**
 * Focus Mode state.
 *
 * When [active] is true the companion stays silent (no proactive messages).
 * The user can still tap the companion to get a reply — focus mode only
 * blocks INITIATIVE, never responses.
 *
 * [durationMinutes] is informational; the UI may show a countdown.
 * null = no fixed end (manual stop only).
 *
 * Pure Kotlin — no Android imports.
 */
data class FocusState(
    val active: Boolean = false,
    /** When the session started (epoch millis); 0 if not active. */
    val startedAtMillis: Long = 0L,
    /** User-chosen session length; null = open-ended. */
    val durationMinutes: Int? = null,
) {
    /** Elapsed minutes since the session started (0 if not active). */
    fun elapsedMinutes(nowMillis: Long): Int =
        if (!active || startedAtMillis == 0L) 0
        else ((nowMillis - startedAtMillis) / 60_000L).toInt().coerceAtLeast(0)

    /** Remaining minutes; null if no fixed duration or not active. */
    fun remainingMinutes(nowMillis: Long): Int? {
        if (!active || durationMinutes == null) return null
        return (durationMinutes - elapsedMinutes(nowMillis)).coerceAtLeast(0)
    }

    /** true when a timed session has expired. */
    fun isExpired(nowMillis: Long): Boolean =
        active && durationMinutes != null && elapsedMinutes(nowMillis) >= durationMinutes

    companion object {
        val DURATION_OPTIONS = listOf(15, 25, 30, 45, 60, 90) // minutes
    }
}
