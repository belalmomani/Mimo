package com.mimo.companion

import com.mimo.companion.domain.context.ContextSnapshot
import com.mimo.companion.domain.context.PrayerProximity
import com.mimo.companion.domain.context.TimeOfDay
import org.junit.Assert.*
import org.junit.Test

class ContextModelsTest {

    // ── TimeOfDay.from() ──────────────────────────────────────────────────────

    @Test fun timeOfDay_earlyMorning_isNight() {
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.from(3 * 60))   // 03:00
    }

    @Test fun timeOfDay_midnight_isNight() {
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.from(0))         // 00:00
    }

    @Test fun timeOfDay_latestNight_isNight() {
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.from(1439))      // 23:59
    }

    @Test fun timeOfDay_morningStart_isMorning() {
        assertEquals(TimeOfDay.MORNING, TimeOfDay.from(5 * 60))  // 05:00
    }

    @Test fun timeOfDay_midMorning_isMorning() {
        assertEquals(TimeOfDay.MORNING, TimeOfDay.from(9 * 60))  // 09:00
    }

    @Test fun timeOfDay_noon_isAfternoon() {
        assertEquals(TimeOfDay.AFTERNOON, TimeOfDay.from(12 * 60)) // 12:00
    }

    @Test fun timeOfDay_afternoon_isAfternoon() {
        assertEquals(TimeOfDay.AFTERNOON, TimeOfDay.from(14 * 60)) // 14:00
    }

    @Test fun timeOfDay_eveningStart_isEvening() {
        assertEquals(TimeOfDay.EVENING, TimeOfDay.from(17 * 60)) // 17:00
    }

    @Test fun timeOfDay_sunset_isEvening() {
        assertEquals(TimeOfDay.EVENING, TimeOfDay.from(19 * 60)) // 19:00
    }

    @Test fun timeOfDay_nightStart_isNight() {
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.from(21 * 60))   // 21:00
    }

    @Test(expected = IllegalArgumentException::class)
    fun timeOfDay_negativeMinute_throws() {
        TimeOfDay.from(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun timeOfDay_outOfRange_throws() {
        TimeOfDay.from(1440)
    }

    // ── PrayerProximity.from() ────────────────────────────────────────────────

    @Test fun prayerProximity_null_returnsNull() {
        assertNull(PrayerProximity.from(null))
    }

    @Test fun prayerProximity_negative_returnsNull() {
        assertNull(PrayerProximity.from(-1))
    }

    @Test fun prayerProximity_zero_isNear() {
        assertEquals(PrayerProximity.NEAR, PrayerProximity.from(0))
    }

    @Test fun prayerProximity_14min_isNear() {
        assertEquals(PrayerProximity.NEAR, PrayerProximity.from(14))
    }

    @Test fun prayerProximity_15min_isApproaching() {
        assertEquals(PrayerProximity.APPROACHING, PrayerProximity.from(15))
    }

    @Test fun prayerProximity_59min_isApproaching() {
        assertEquals(PrayerProximity.APPROACHING, PrayerProximity.from(59))
    }

    @Test fun prayerProximity_60min_isFar() {
        assertEquals(PrayerProximity.FAR, PrayerProximity.from(60))
    }

    @Test fun prayerProximity_twoHours_isFar() {
        assertEquals(PrayerProximity.FAR, PrayerProximity.from(120))
    }

    // ── ContextSnapshot helpers ───────────────────────────────────────────────

    @Test fun stepGoalMet_whenStepsEqualGoal() {
        assertTrue(ContextSnapshot(TimeOfDay.MORNING, stepsToday = 6000, stepGoal = 6000).stepGoalMet)
    }

    @Test fun stepGoalMet_whenStepsExceedGoal() {
        assertTrue(ContextSnapshot(TimeOfDay.MORNING, stepsToday = 8000, stepGoal = 6000).stepGoalMet)
    }

    @Test fun stepGoalNotMet_belowGoal() {
        assertFalse(ContextSnapshot(TimeOfDay.MORNING, stepsToday = 3000, stepGoal = 6000).stepGoalMet)
    }

    @Test fun stepGoalNotMet_stepsNull() {
        assertFalse(ContextSnapshot(TimeOfDay.MORNING, stepsToday = null, stepGoal = 6000).stepGoalMet)
    }

    @Test fun stepGoalNotMet_goalNull() {
        assertFalse(ContextSnapshot(TimeOfDay.MORNING, stepsToday = 6000, stepGoal = null).stepGoalMet)
    }

    @Test fun screenBreakSuggested_aboveThreshold() {
        val threshold = ContextSnapshot.SCREEN_BREAK_THRESHOLD_MIN
        assertTrue(ContextSnapshot(TimeOfDay.AFTERNOON, screenOnMinutes = threshold + 1).screenBreakSuggested)
    }

    @Test fun screenBreakNotSuggested_atThreshold() {
        val threshold = ContextSnapshot.SCREEN_BREAK_THRESHOLD_MIN
        assertFalse(ContextSnapshot(TimeOfDay.AFTERNOON, screenOnMinutes = threshold).screenBreakSuggested)
    }

    @Test fun screenBreakNotSuggested_null() {
        assertFalse(ContextSnapshot(TimeOfDay.AFTERNOON, screenOnMinutes = null).screenBreakSuggested)
    }

    @Test fun screenBreakNotSuggested_zero() {
        assertFalse(ContextSnapshot(TimeOfDay.AFTERNOON, screenOnMinutes = 0).screenBreakSuggested)
    }
}
