package com.mimo.companion.domain.steps

/**
 * Everything the UI needs to display step/activity data.
 *
 * [sensorTotal] is the raw cumulative counter from TYPE_STEP_COUNTER
 * (resets only on reboot).  [baseline] is the value recorded at the
 * start of "today" so that todaySteps = sensorTotal - baseline.
 *
 * [goal] is the user-chosen daily target (default 6 000).
 */
data class StepState(
    val todaySteps: Int = 0,
    val goal: Int = DEFAULT_GOAL,
    val available: Boolean = false,       // false when the sensor is absent or permission denied
    val permissionNeeded: Boolean = false, // true on API 29+ before the user grants the permission
) {
    val progress: Float get() = if (goal > 0) (todaySteps.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val done: Boolean get() = todaySteps >= goal

    companion object {
        const val DEFAULT_GOAL = 6_000
        const val MIN_GOAL = 1_000
        const val MAX_GOAL = 30_000
        const val GOAL_STEP = 500
    }
}

/** Minimal step-counting logic that can be unit-tested without the Android sensor framework. */
object StepMath {
    /**
     * Computes today's step count from a cumulative sensor total and the stored
     * baseline captured at the start of the day.
     *
     * Handles sensor reset on reboot: if [sensorTotal] < [baseline] we treat
     * it as a fresh counter and return [sensorTotal] directly.
     */
    fun todaySteps(sensorTotal: Long, baseline: Long): Int =
        if (sensorTotal >= baseline) (sensorTotal - baseline).toInt()
        else sensorTotal.toInt()   // reboot reset

    /**
     * Returns the baseline to store for a new day.
     * If the sensor just reset (total < oldBaseline) the new baseline is 0
     * because the counter already started fresh.
     */
    fun newDayBaseline(sensorTotal: Long, oldBaseline: Long): Long =
        if (sensorTotal >= oldBaseline) sensorTotal else 0L
}
