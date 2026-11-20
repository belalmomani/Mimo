package com.mimo.companion.domain.context

import com.mimo.companion.domain.weather.WeatherCondition
import com.mimo.companion.domain.weather.WeatherData

/**
 * Broad segment of the day, derived from clock time only.
 * Never guessed from sensor data or battery level.
 */
enum class TimeOfDay {
    /** 05:00 – 11:59 */
    MORNING,
    /** 12:00 – 16:59 */
    AFTERNOON,
    /** 17:00 – 20:59 */
    EVENING,
    /** 21:00 – 04:59 */
    NIGHT;

    companion object {
        /** [minuteOfDay] is 0–1439 (hour*60 + minute in the device time zone). */
        fun from(minuteOfDay: Int): TimeOfDay {
            require(minuteOfDay in 0..1439) { "minuteOfDay out of range: $minuteOfDay" }
            return when (minuteOfDay) {
                in 5 * 60 until 12 * 60  -> MORNING
                in 12 * 60 until 17 * 60 -> AFTERNOON
                in 17 * 60 until 21 * 60 -> EVENING
                else                      -> NIGHT
            }
        }
    }
}

/**
 * How many minutes until the next prayer (null = prayer times not available).
 * Binned so the Context Engine can suggest a "wrap up soon" nudge without
 * knowing the exact prayer name.
 */
enum class PrayerProximity {
    /** More than 60 minutes away — no nudge needed. */
    FAR,
    /** 15 – 60 minutes away — a gentle reminder might be welcome. */
    APPROACHING,
    /** < 15 minutes away — time to prepare. */
    NEAR;

    companion object {
        fun from(minutesUntil: Int?): PrayerProximity? {
            if (minutesUntil == null || minutesUntil < 0) return null
            return when {
                minutesUntil < 15 -> NEAR
                minutesUntil < 60 -> APPROACHING
                else              -> FAR
            }
        }
    }
}

/**
 * A snapshot of everything the Context Engine knows at a given moment.
 * All fields are optional / nullable so the engine degrades gracefully
 * when sensors or data are unavailable.
 *
 * Pure Kotlin — no Android imports.
 */
data class ContextSnapshot(
    /** Time-of-day segment, derived from the device clock. */
    val timeOfDay: TimeOfDay,
    /** Steps taken today; null if the sensor is unavailable or permission not granted. */
    val stepsToday: Int? = null,
    /** User's daily step goal; null if not set. */
    val stepGoal: Int? = null,
    /** Minutes of continuous foreground screen-on time (estimated, never exact). */
    val screenOnMinutes: Int? = null,
    /** How far away the next prayer is; null if prayer times are not configured. */
    val prayerProximity: PrayerProximity? = null,
    /** Name of the upcoming prayer in the app's current language (AR or EN). */
    val nextPrayerLabel: String? = null,
    /** true when the user is in an active call. */
    val inCall: Boolean = false,
    /** true when a focus / Do Not Disturb session is running. */
    val focusSession: Boolean = false,
    /**
     * Minutes since the step count last changed (i.e. user has been still).
     * null = step sensor unavailable or not enough data yet.
     */
    val minutesSinceMove: Int? = null,
    /**
     * A verse or hadith text to say today, or null if already said today / unavailable.
     * The ContextEngine sets this once per day after fetching from DailyVerseRepository.
     */
    val dailyVerseText: String? = null,
    /**
     * Most recent weather snapshot; null if the API key is not set or no data is cached yet.
     */
    val weather: WeatherData? = null,
) {
    /** Whether the user is on track for or has hit the step goal today. */
    val stepGoalMet: Boolean
        get() = stepsToday != null && stepGoal != null && stepsToday >= stepGoal

    /** Whether a screen-break suggestion is worth making (strictly above threshold). */
    val screenBreakSuggested: Boolean
        get() = screenOnMinutes != null && screenOnMinutes > SCREEN_BREAK_THRESHOLD_MIN

    /** Whether a move nudge is worth making (hasn't moved for ≥ MOVE_THRESHOLD_MIN minutes). */
    val moveReminderSuggested: Boolean
        get() = minutesSinceMove != null && minutesSinceMove >= MOVE_THRESHOLD_MIN

    /** Whether a weather comment is worth making (fresh data available). */
    val weatherAvailable: Boolean
        get() = weather != null && weather.isFresh()

    /** Convenience accessor for the weather condition. */
    val weatherCondition: WeatherCondition?
        get() = if (weatherAvailable) weather?.condition else null

    companion object {
        /** Continuous screen-on minutes above which a break nudge becomes relevant. */
        const val SCREEN_BREAK_THRESHOLD_MIN = 45
        /** Minutes of no step activity after which a move nudge is worth making. */
        const val MOVE_THRESHOLD_MIN = 60
    }
}
