package com.mimo.companion

import com.mimo.companion.domain.steps.StepMath
import com.mimo.companion.domain.steps.StepState
import org.junit.Test
import org.junit.Assert.*

class StepMathTest {

    // -- todaySteps --

    @Test fun normalDay_subtractsBaseline() {
        // Sensor reads 12 000 total; baseline at start of day was 10 000
        assertEquals(2_000, StepMath.todaySteps(12_000L, 10_000L))
    }

    @Test fun firstReadingOfDay_zeroSteps() {
        // Sensor and baseline are the same → 0 steps so far
        assertEquals(0, StepMath.todaySteps(5_000L, 5_000L))
    }

    @Test fun rebootReset_sensorSmallerThanBaseline() {
        // Reboot happened; sensor restarted from 0; current value is 800
        assertEquals(800, StepMath.todaySteps(800L, 50_000L))
    }

    @Test fun largeStepCount() {
        assertEquals(10_000, StepMath.todaySteps(110_000L, 100_000L))
    }

    // -- newDayBaseline --

    @Test fun newDay_normalCase_capturesCurrentTotal() {
        assertEquals(15_000L, StepMath.newDayBaseline(15_000L, 12_000L))
    }

    @Test fun newDay_afterReboot_baselineIsZero() {
        // After reboot sensor restarted from a small number; old baseline was large
        assertEquals(0L, StepMath.newDayBaseline(300L, 50_000L))
    }

    // -- StepState helpers --

    @Test fun progress_zeroWhenNoSteps() {
        assertEquals(0f, StepState(todaySteps = 0, goal = 6_000).progress, 0.001f)
    }

    @Test fun progress_halfWay() {
        assertEquals(0.5f, StepState(todaySteps = 3_000, goal = 6_000).progress, 0.001f)
    }

    @Test fun progress_clampedAtOne() {
        val s = StepState(todaySteps = 9_000, goal = 6_000)
        assertEquals(1f, s.progress, 0.001f)
        assertTrue(s.done)
    }

    @Test fun progress_zeroGoalIsSafe() {
        assertEquals(0f, StepState(todaySteps = 100, goal = 0).progress, 0.001f)
    }

    @Test fun goalBounds() {
        assertEquals(StepState.MIN_GOAL, 1_000)
        assertEquals(StepState.MAX_GOAL, 30_000)
        assertEquals(StepState.GOAL_STEP, 500)
    }
}
