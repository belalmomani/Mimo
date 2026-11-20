package com.mimo.companion.domain.steps

/**
 * Tracks the last time the step count actually changed, so we can tell the
 * Context Engine "the user hasn't moved in X minutes."
 *
 * Pure Kotlin — no Android imports.  Feed it every new step reading from StepCounter;
 * query [minutesSinceMove] when building a [ContextSnapshot].
 */
class StepActivityTracker {

    private var lastStepCount: Int = -1
    private var lastMovedAtMillis: Long = 0L

    /**
     * Call this every time StepCounter emits a new value.
     * If [steps] differs from the previous reading, we record now as the last-move time.
     */
    fun onSteps(steps: Int, nowMillis: Long = System.currentTimeMillis()) {
        if (steps != lastStepCount) {
            lastStepCount  = steps
            lastMovedAtMillis = nowMillis
        }
    }

    /**
     * Returns how many whole minutes have passed since the step count last changed,
     * or null if we haven't received any step reading yet.
     */
    fun minutesSinceMove(nowMillis: Long = System.currentTimeMillis()): Int? {
        if (lastMovedAtMillis == 0L) return null
        val elapsed = nowMillis - lastMovedAtMillis
        return (elapsed / 60_000L).toInt().coerceAtLeast(0)
    }
}
