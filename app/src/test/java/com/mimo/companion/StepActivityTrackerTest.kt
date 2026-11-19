package com.mimo.companion

import com.mimo.companion.domain.steps.StepActivityTracker
import org.junit.Assert.*
import org.junit.Test

class StepActivityTrackerTest {

    private val base = 1_000_000L

    @Test fun noReading_minutesSinceMove_isNull() {
        assertNull(StepActivityTracker().minutesSinceMove(base))
    }

    @Test fun firstReading_minutesSinceMove_isZero() {
        val t = StepActivityTracker()
        t.onSteps(100, base)
        assertEquals(0, t.minutesSinceMove(base))
    }

    @Test fun sameSteps_doesNotUpdateTimestamp() {
        val t = StepActivityTracker()
        t.onSteps(100, base)
        t.onSteps(100, base + 10 * 60_000)   // same count 10 min later
        assertEquals(10, t.minutesSinceMove(base + 10 * 60_000))
    }

    @Test fun newSteps_resetsTimer() {
        val t = StepActivityTracker()
        t.onSteps(100, base)
        t.onSteps(100, base + 30 * 60_000)   // still 100 — no movement
        t.onSteps(150, base + 35 * 60_000)   // moved!
        assertEquals(0, t.minutesSinceMove(base + 35 * 60_000))
    }

    @Test fun afterOneHour_exceeds_threshold() {
        val t = StepActivityTracker()
        t.onSteps(200, base)
        val minutes = t.minutesSinceMove(base + 61 * 60_000)
        assertNotNull(minutes)
        assertTrue(minutes!! >= 60)
    }

    @Test fun minutesClampsToZero() {
        val t = StepActivityTracker()
        t.onSteps(10, base + 1000)       // moved 1 second in
        val result = t.minutesSinceMove(base)   // query before move — clamps to 0
        assertEquals(0, result)
    }
}
