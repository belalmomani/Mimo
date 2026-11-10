package com.mimo.companion.domain.context

import com.mimo.companion.domain.dialogue.PhraseCategory

/**
 * Maps a [ContextSnapshot] to a prioritised list of [PhraseCategory] suggestions
 * the companion might proactively deliver.
 *
 * Rules (highest priority first):
 * 1. BREAK_SUGGESTION – the user has been on-screen for ≥ 45 minutes
 * 2. PRAYER_ACK      – handled externally; not suggested here
 * 3. ENCOURAGE       – step goal hit for the first time today (steps ≥ goal)
 * 4. IDLE_CHAT       – default filler; suggested unless a higher-priority item fired
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

        // 1. Screen break — highest priority: the user may have forgotten they're still scrolling
        if (snapshot.screenBreakSuggested) {
            suggestions += PhraseCategory.BREAK_SUGGESTION
        }

        // 2. Step encouragement — goal met and not in a call or focus session
        if (snapshot.stepGoalMet && !snapshot.inCall && !snapshot.focusSession) {
            suggestions += PhraseCategory.ENCOURAGE
        }

        // 3. Idle chat — always a valid fallback
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
