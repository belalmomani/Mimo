package com.mimo.companion.domain.context

import com.mimo.companion.domain.dialogue.PhraseCategory

/**
 * Maps a [ContextSnapshot] to a prioritised list of [PhraseCategory] suggestions
 * the companion might proactively deliver.
 *
 * Rules (highest priority first):
 * 1. DAILY_VERSE     – once per day, highest priority
 * 2. BREAK_SUGGESTION – the user has been on-screen for ≥ 45 minutes
 * 3. MOVE_REMINDER   – hasn't moved for ≥ 60 minutes
 * 4. WEATHER         – fresh weather data available (once per session)
 * 5. ENCOURAGE       – step goal hit for the first time today (steps ≥ goal)
 * 6. IDLE_CHAT       – default filler; suggested unless a higher-priority item fired
 *
 * None of these are GUARANTEED to be spoken – the DialogueManager still checks
 * quiet hours, initiative mode, and rate limits before allowing a proactive line.
 *
 * Pure Kotlin — no Android imports.
 */
object ContextAdvisor {

    /**
     * Returns a list of categories the companion could proactively mention right now,
     * ordered from most to least relevant.  The caller picks the first one the
     * DialogueManager is willing to deliver.
     */
    fun suggest(snapshot: ContextSnapshot): List<PhraseCategory> {
        val suggestions = mutableListOf<PhraseCategory>()

        // 1. Daily verse — highest priority, fires once a day
        if (snapshot.dailyVerseText != null) {
            suggestions += PhraseCategory.DAILY_VERSE
        }

        // 2. Screen break — user may have forgotten they're still scrolling
        if (snapshot.screenBreakSuggested) {
            suggestions += PhraseCategory.BREAK_SUGGESTION
        }

        // 3. Move reminder — hasn't moved for ≥ 60 minutes
        if (snapshot.moveReminderSuggested && !snapshot.inCall && !snapshot.focusSession) {
            suggestions += PhraseCategory.MOVE_REMINDER
        }

        // 4. Weather — fresh reading available (companion mentions it once)
        if (snapshot.weatherAvailable) {
            suggestions += PhraseCategory.WEATHER
        }

        // 5. Step encouragement — goal met
        if (snapshot.stepGoalMet && !snapshot.inCall && !snapshot.focusSession) {
            suggestions += PhraseCategory.ENCOURAGE
        }

        // 6. Idle chat — always a valid fallback
        suggestions += PhraseCategory.IDLE_CHAT

        return suggestions
    }

    /**
     * Returns the best single-line context hint for the companion's next phrase,
     * in the form of a [PhraseCategory], or null if nothing proactive makes sense.
     *
     * "Nothing makes sense" = the user is in a call, in a focus session, or at night.
     */
    fun bestCategory(snapshot: ContextSnapshot): PhraseCategory? {
        if (snapshot.inCall) return null
        if (snapshot.focusSession) return null
        if (snapshot.timeOfDay == TimeOfDay.NIGHT) return null
        return suggest(snapshot).firstOrNull()
    }
}
