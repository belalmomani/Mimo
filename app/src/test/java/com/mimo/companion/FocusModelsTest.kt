package com.mimo.companion

import com.mimo.companion.domain.focus.FocusState
import org.junit.Assert.*
import org.junit.Test

class FocusModelsTest {

    private val base = 1_000_000L // arbitrary epoch ms

    // ── elapsedMinutes ────────────────────────────────────────────────────────

    @Test fun elapsed_notActive_isZero() {
        assertEquals(0, FocusState().elapsedMinutes(base))
    }

    @Test fun elapsed_zeroMillisIn_isZero() {
        val s = FocusState(active = true, startedAtMillis = base)
        assertEquals(0, s.elapsedMinutes(base))
    }

    @Test fun elapsed_oneMinute() {
        val s = FocusState(active = true, startedAtMillis = base)
        assertEquals(1, s.elapsedMinutes(base + 60_000))
    }

    @Test fun elapsed_25Minutes() {
        val s = FocusState(active = true, startedAtMillis = base)
        assertEquals(25, s.elapsedMinutes(base + 25 * 60_000))
    }

    // ── remainingMinutes ──────────────────────────────────────────────────────

    @Test fun remaining_notActive_isNull() {
        assertNull(FocusState().remainingMinutes(base))
    }

    @Test fun remaining_activeNoDuration_isNull() {
        val s = FocusState(active = true, startedAtMillis = base, durationMinutes = null)
        assertNull(s.remainingMinutes(base))
    }

    @Test fun remaining_fullDurationLeft() {
        val s = FocusState(active = true, startedAtMillis = base, durationMinutes = 25)
        assertEquals(25, s.remainingMinutes(base))
    }

    @Test fun remaining_halfElapsed() {
        val s = FocusState(active = true, startedAtMillis = base, durationMinutes = 30)
        assertEquals(15, s.remainingMinutes(base + 15 * 60_000))
    }

    @Test fun remaining_clampsToZero_whenExpired() {
        val s = FocusState(active = true, startedAtMillis = base, durationMinutes = 10)
        assertEquals(0, s.remainingMinutes(base + 20 * 60_000))
    }

    // ── isExpired ─────────────────────────────────────────────────────────────

    @Test fun expired_notActive_isFalse() {
        assertFalse(FocusState().isExpired(base))
    }

    @Test fun expired_activeNoDuration_isFalse() {
        val s = FocusState(active = true, startedAtMillis = base)
        assertFalse(s.isExpired(base + 999_999_999))
    }

    @Test fun expired_beforeDuration_isFalse() {
        val s = FocusState(active = true, startedAtMillis = base, durationMinutes = 25)
        assertFalse(s.isExpired(base + 24 * 60_000))
    }

    @Test fun expired_exactlyAtDuration_isTrue() {
        val s = FocusState(active = true, startedAtMillis = base, durationMinutes = 25)
        assertTrue(s.isExpired(base + 25 * 60_000))
    }

    @Test fun expired_afterDuration_isTrue() {
        val s = FocusState(active = true, startedAtMillis = base, durationMinutes = 25)
        assertTrue(s.isExpired(base + 60 * 60_000))
    }

    // ── DURATION_OPTIONS ──────────────────────────────────────────────────────

    @Test fun durationOptions_notEmpty() {
        assertTrue(FocusState.DURATION_OPTIONS.isNotEmpty())
    }

    @Test fun durationOptions_allPositive() {
        assertTrue(FocusState.DURATION_OPTIONS.all { it > 0 })
    }

    @Test fun durationOptions_sorted() {
        assertEquals(FocusState.DURATION_OPTIONS, FocusState.DURATION_OPTIONS.sorted())
    }
}
