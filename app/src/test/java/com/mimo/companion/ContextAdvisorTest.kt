package com.mimo.companion

import com.mimo.companion.domain.context.ContextAdvisor
import com.mimo.companion.domain.context.ContextSnapshot
import com.mimo.companion.domain.context.TimeOfDay
import com.mimo.companion.domain.dialogue.PhraseCategory
import org.junit.Assert.*
import org.junit.Test

class ContextAdvisorTest {

    // ── suggest() ────────────────────────────────────────────────────────────

    @Test fun suggest_screenBreakFirst_whenScreenHigh() {
        val snap = ContextSnapshot(
            TimeOfDay.AFTERNOON,
            screenOnMinutes = ContextSnapshot.SCREEN_BREAK_THRESHOLD_MIN + 1,
            stepsToday = 7000, stepGoal = 6000,
        )
        val suggestions = ContextAdvisor.suggest(snap)
        assertEquals(PhraseCategory.BREAK_SUGGESTION, suggestions.first())
    }

    @Test fun suggest_containsIdleChatAlways() {
        val snap = ContextSnapshot(TimeOfDay.MORNING)
        val suggestions = ContextAdvisor.suggest(snap)
        assertTrue(PhraseCategory.IDLE_CHAT in suggestions)
    }

    @Test fun suggest_encourageIncluded_whenGoalMet() {
        val snap = ContextSnapshot(
            TimeOfDay.AFTERNOON,
            stepsToday = 6000, stepGoal = 6000,
        )
        val suggestions = ContextAdvisor.suggest(snap)
        assertTrue(PhraseCategory.ENCOURAGE in suggestions)
    }

    @Test fun suggest_encourageNotIncluded_whenGoalNotMet() {
        val snap = ContextSnapshot(
            TimeOfDay.AFTERNOON,
            stepsToday = 3000, stepGoal = 6000,
        )
        val suggestions = ContextAdvisor.suggest(snap)
        assertFalse(PhraseCategory.ENCOURAGE in suggestions)
    }

    @Test fun suggest_noBreakSuggestion_whenScreenNull() {
        val snap = ContextSnapshot(TimeOfDay.AFTERNOON, screenOnMinutes = null)
        val suggestions = ContextAdvisor.suggest(snap)
        assertFalse(PhraseCategory.BREAK_SUGGESTION in suggestions)
    }

    @Test fun suggest_noEncourage_duringCall() {
        val snap = ContextSnapshot(
            TimeOfDay.AFTERNOON,
            stepsToday = 8000, stepGoal = 6000,
            inCall = true,
        )
        val suggestions = ContextAdvisor.suggest(snap)
        // ENCOURAGE is excluded when inCall
        assertFalse(PhraseCategory.ENCOURAGE in suggestions)
    }

    @Test fun suggest_noEncourage_duringFocus() {
        val snap = ContextSnapshot(
            TimeOfDay.AFTERNOON,
            stepsToday = 8000, stepGoal = 6000,
            focusSession = true,
        )
        val suggestions = ContextAdvisor.suggest(snap)
        assertFalse(PhraseCategory.ENCOURAGE in suggestions)
    }

    // ── bestCategory() ───────────────────────────────────────────────────────

    @Test fun bestCategory_null_duringCall() {
        val snap = ContextSnapshot(TimeOfDay.AFTERNOON, inCall = true)
        assertNull(ContextAdvisor.bestCategory(snap))
    }

    @Test fun bestCategory_null_duringFocus() {
        val snap = ContextSnapshot(TimeOfDay.AFTERNOON, focusSession = true)
        assertNull(ContextAdvisor.bestCategory(snap))
    }

    @Test fun bestCategory_null_atNight() {
        val snap = ContextSnapshot(TimeOfDay.NIGHT)
        assertNull(ContextAdvisor.bestCategory(snap))
    }

    @Test fun bestCategory_returnsSomething_duringDay() {
        val snap = ContextSnapshot(TimeOfDay.MORNING)
        assertNotNull(ContextAdvisor.bestCategory(snap))
    }

    @Test fun bestCategory_breakSuggestion_highestPriority() {
        val snap = ContextSnapshot(
            TimeOfDay.AFTERNOON,
            screenOnMinutes = ContextSnapshot.SCREEN_BREAK_THRESHOLD_MIN + 1,
            stepsToday = 8000, stepGoal = 6000,
        )
        assertEquals(PhraseCategory.BREAK_SUGGESTION, ContextAdvisor.bestCategory(snap))
    }

    @Test fun bestCategory_encourage_whenNoBreak_butGoalMet() {
        val snap = ContextSnapshot(
            TimeOfDay.AFTERNOON,
            screenOnMinutes = 10,   // below threshold
            stepsToday = 6000, stepGoal = 6000,
        )
        assertEquals(PhraseCategory.ENCOURAGE, ContextAdvisor.bestCategory(snap))
    }

    @Test fun bestCategory_idleChat_whenNothingElse() {
        val snap = ContextSnapshot(TimeOfDay.EVENING)
        assertEquals(PhraseCategory.IDLE_CHAT, ContextAdvisor.bestCategory(snap))
    }
}
