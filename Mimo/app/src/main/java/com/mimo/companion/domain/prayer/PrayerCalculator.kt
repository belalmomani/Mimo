package com.mimo.companion.domain.prayer

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.tan

enum class Prayer { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA;
    /** Sunrise is shown but is not a prayer to be reminded of / logged. */
    val isPrayer: Boolean get() = this != SUNRISE
}

enum class AsrMethod(val shadowFactor: Int) { STANDARD(1), HANAFI(2) }

/**
 * Conventions differ between authorities. These are the widely used angle sets; [ishaIntervalMin] replaces the Isha angle
 * (Umm al-Qura: 90 minutes after Maghrib). None of them is "the" official time of a specific country:
 * the UI says so and lets the user shift each prayer by a few minutes to match their local authority.
 */
enum class CalcMethod(val fajrAngle: Double, val ishaAngle: Double, val ishaIntervalMin: Int = 0) {
    MUSLIM_WORLD_LEAGUE(18.0, 17.0),
    EGYPTIAN(19.5, 17.5),
    KARACHI(18.0, 18.0),
    UMM_AL_QURA(18.5, 0.0, ishaIntervalMin = 90),
    ISNA(15.0, 15.0),
}

data class PrayerDay(val date: LocalDate, val times: Map<Prayer, LocalTime?>) {
    /** null when the sun never reaches the needed angle on that day (high latitudes): we do NOT guess. */
    operator fun get(p: Prayer): LocalTime? = times[p]
}

/**
 * Astronomical prayer-time calculation (same standard algorithm family as PrayTimes.org: solar declination and equation of time,
 * hour-angle for each twilight angle, shadow-ratio for Asr). Pure maths, no network, no sensors.
 * Accuracy: typically within 1-2 minutes of published tables that use the same method; real timetables may add
 * local safety minutes or rounding conventions, hence the per-prayer user adjustments handled elsewhere.
 */
object PrayerCalculator {
    private const val SUN_ALTITUDE = 0.833 // refraction + sun radius at sea level

    fun compute(date: LocalDate, latitude: Double, longitude: Double, utcOffsetHours: Double, method: CalcMethod, asr: AsrMethod, elevationM: Double = 0.0): PrayerDay {
        val jd = julian(date.year, date.monthValue, date.dayOfMonth) - longitude / (15.0 * 24.0)
        val riseSet = SUN_ALTITUDE + 0.0347 * kotlin.math.sqrt(elevationM.coerceAtLeast(0.0))

        fun sunAngleTime(angle: Double, approx: Double, ccw: Boolean): Double {
            val decl = sunPosition(jd + approx / 24.0).declination
            val noon = midDay(jd, approx)
            val cosArg = (-sin(rad(angle)) - sin(rad(decl)) * sin(rad(latitude))) / (cos(rad(decl)) * cos(rad(latitude)))
            if (abs(cosArg) > 1.0) return Double.NaN
            val t = deg(acos(cosArg)) / 15.0
            return noon + if (ccw) -t else t
        }
        fun asrTime(factor: Int, approx: Double): Double {
            val decl = sunPosition(jd + approx / 24.0).declination
            val angle = -deg(atan(1.0 / (factor + tan(rad(abs(latitude - decl))))))
            return sunAngleTime(angle, approx, ccw = false)
        }

        // first pass uses the classic start values (hours), exactly one refinement step is enough for minute accuracy
        val fajr = sunAngleTime(method.fajrAngle, 5.0, true)
        val sunrise = sunAngleTime(riseSet, 6.0, true)
        val dhuhr = midDay(jd, 12.0)
        val asrT = asrTime(asr.shadowFactor, 13.0)
        val sunset = sunAngleTime(riseSet, 18.0, false)
        val isha = if (method.ishaIntervalMin > 0) sunset + method.ishaIntervalMin / 60.0 else sunAngleTime(method.ishaAngle, 18.0, false)

        val shift = utcOffsetHours - longitude / 15.0
        fun toTime(h: Double): LocalTime? {
            if (h.isNaN()) return null
            val minutes = round((h + shift) * 60.0).toInt()
            val m = ((minutes % 1440) + 1440) % 1440
            return LocalTime.of(m / 60, m % 60)
        }
        return PrayerDay(date, mapOf(
            Prayer.FAJR to toTime(fajr), Prayer.SUNRISE to toTime(sunrise), Prayer.DHUHR to toTime(dhuhr),
            Prayer.ASR to toTime(asrT), Prayer.MAGHRIB to toTime(sunset), Prayer.ISHA to toTime(isha),
        ))
    }

    /** Convenience: takes the zone's real UTC offset (incl. DST) for that date. */
    fun compute(date: LocalDate, latitude: Double, longitude: Double, zone: ZoneId, method: CalcMethod, asr: AsrMethod): PrayerDay {
        val offset = zone.rules.getOffset(date.atTime(12, 0).atZone(zone).toInstant()).totalSeconds / 3600.0
        return compute(date, latitude, longitude, offset, method, asr)
    }

    // ---- astronomy ---------------------------------------------------------------------------------------
    private class Sun(val declination: Double, val equation: Double)

    private fun sunPosition(jd: Double): Sun {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(rad(g)) + 0.020 * sin(rad(2 * g)))
        val e = 23.439 - 0.00000036 * d
        val ra = fixHour(deg(atan2(cos(rad(e)) * sin(rad(l)), cos(rad(l)))) / 15.0)
        val eqt = q / 15.0 - ra
        val decl = deg(asin(sin(rad(e)) * sin(rad(l))))
        return Sun(decl, eqt)
    }

    private fun midDay(jd: Double, approx: Double): Double = fixHour(12.0 - sunPosition(jd + approx / 24.0).equation)

    private fun julian(y0: Int, m0: Int, d: Int): Double {
        var y = y0; var m = m0
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0); val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + b - 1524.5
    }

    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)
    private fun fixAngle(a: Double): Double = a - 360.0 * floor(a / 360.0)
    private fun fixHour(a: Double): Double = a - 24.0 * floor(a / 24.0)
}
