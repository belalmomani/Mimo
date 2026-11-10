package com.mimo.companion.context

import android.content.Context
import android.os.PowerManager
import com.mimo.companion.domain.context.ContextSnapshot
import com.mimo.companion.domain.context.PrayerProximity
import com.mimo.companion.domain.context.TimeOfDay
import com.mimo.companion.domain.dialogue.Lang
import com.mimo.companion.domain.prayer.Prayer
import com.mimo.companion.domain.prayer.PrayerCalculator
import com.mimo.companion.domain.prayer.PrayerSettings
import com.mimo.companion.settings.PrayerSettingsStore
import com.mimo.companion.steps.StepCounter
import java.time.Instant
import java.time.ZoneId

/**
 * Assembles a [ContextSnapshot] from live device data.
 *
 * Data sources:
 * - Clock             → [TimeOfDay]
 * - [StepCounter]     → stepsToday / stepGoal
 * - [PrayerSettingsStore] + [PrayerCalculator] → prayerProximity / nextPrayerLabel
 * - [PowerManager]    → screen-on heuristic (best-effort, not a usage tracker)
 *
 * Call [snapshot] from the UI / companion layer; it is cheap and synchronous.
 * Nothing here runs in the background or holds a wakelock.
 */
class ContextEngine(
    private val context: Context,
    private val stepCounter: StepCounter,
    private val prayerSettings: PrayerSettingsStore,
    private val lang: () -> Lang,
) {

    /** Builds and returns the current context snapshot. Always safe to call on any thread. */
    fun snapshot(): ContextSnapshot {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val localTime = Instant.ofEpochMilli(now).atZone(zone).toLocalTime()
        val minuteOfDay = localTime.hour * 60 + localTime.minute

        val steps = stepCounter.state.value
        val (prayerProx, prayerLabel) = prayerInfo(now, zone, minuteOfDay)

        return ContextSnapshot(
            timeOfDay = TimeOfDay.from(minuteOfDay),
            stepsToday = if (steps.available) steps.todaySteps else null,
            stepGoal = if (steps.available) steps.goal else null,
            screenOnMinutes = screenOnMinutes(),
            prayerProximity = prayerProx,
            nextPrayerLabel = prayerLabel,
            inCall = false,       // set by Graph.currentContext() — AudioManager check
            focusSession = false, // placeholder; wired up when focus mode is added
        )
    }

    // ── prayer proximity ─────────────────────────────────────────────────────

    private fun prayerInfo(
        nowMillis: Long,
        zone: ZoneId,
        minuteOfDay: Int,
    ): Pair<PrayerProximity?, String?> {
        val settings: PrayerSettings = prayerSettings.value
        if (!settings.enabled) return null to null

        val city = com.mimo.companion.domain.prayer.Cities.byId(settings.cityId)
        val date = Instant.ofEpochMilli(nowMillis).atZone(city.zone).toLocalDate()
        val times = PrayerCalculator.calculate(date, city.lat, city.lng, settings)

        // Find the next prayer that is still in the future
        val prayerOrder = listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)
        var nearestMinutes: Int? = null
        var nearestLabel: String? = null

        for (prayer in prayerOrder) {
            val prayerMin = times[prayer] ?: continue
            val minutesUntil = prayerMin - minuteOfDay
            // If past midnight wrap (e.g. FAJR tomorrow when it's 23:30)
            val adjusted = if (minutesUntil < 0) minutesUntil + 1440 else minutesUntil
            if (nearestMinutes == null || adjusted < nearestMinutes) {
                nearestMinutes = adjusted
                nearestLabel = prayerName(prayer, lang())
            }
        }

        return PrayerProximity.from(nearestMinutes) to nearestLabel
    }

    private fun prayerName(prayer: Prayer, lang: Lang): String = when (lang) {
        Lang.AR -> when (prayer) {
            Prayer.FAJR    -> "الفجر"
            Prayer.SUNRISE -> "الشروق"
            Prayer.DHUHR   -> "الظهر"
            Prayer.ASR     -> "العصر"
            Prayer.MAGHRIB -> "المغرب"
            Prayer.ISHA    -> "العشاء"
        }
        Lang.EN -> when (prayer) {
            Prayer.FAJR    -> "Fajr"
            Prayer.SUNRISE -> "Sunrise"
            Prayer.DHUHR   -> "Dhuhr"
            Prayer.ASR     -> "Asr"
            Prayer.MAGHRIB -> "Maghrib"
            Prayer.ISHA    -> "Isha"
        }
    }

    // ── screen-on heuristic ───────────────────────────────────────────────────

    /**
     * Returns the number of minutes the screen appears to have been on continuously.
     *
     * This is a best-effort heuristic using [PowerManager.isInteractive]:
     * - If the screen is off → 0 minutes.
     * - If the screen is on → we return a constant that reflects "has been on for a while"
     *   without tracking the exact start time (no background service needed).
     *
     * A proper screen-time tracker would require a foreground service or
     * UsageStatsManager (needs PACKAGE_USAGE_STATS, which is a user-granted
     * special permission). We deliberately avoid that — Mimo is a companion,
     * not a surveillance tool.  The nudge is therefore conservative: it only
     * fires when the companion is ALREADY on screen (overlay running) so the
     * user is confirmed to be active.
     */
    private fun screenOnMinutes(): Int? {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return null
        return if (pm.isInteractive) SCREEN_ON_PLACEHOLDER_MIN else 0
    }

    companion object {
        /**
         * Placeholder returned when the screen is interactive.
         * Set to [ContextSnapshot.SCREEN_BREAK_THRESHOLD_MIN] + 1 so the
         * break suggestion fires as long as the companion is on screen.
         * Replace with a real elapsed-minutes value if you add a foreground
         * screen-time tracker later.
         */
        internal const val SCREEN_ON_PLACEHOLDER_MIN =
            com.mimo.companion.domain.context.ContextSnapshot.SCREEN_BREAK_THRESHOLD_MIN + 1
    }
}
