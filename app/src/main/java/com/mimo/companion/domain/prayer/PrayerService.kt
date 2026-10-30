package com.mimo.companion.domain.prayer

import com.mimo.companion.domain.SystemTimeSource
import com.mimo.companion.domain.TimeSource
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Prayer times for the user's settings + the reminder logic around them. Pure Kotlin.
 * Rules: silence never counts as having prayed; a prayer marked as prayed is not reminded again; times that cannot be
 * computed are skipped (never guessed); follow-ups stop after the configured count.
 */
class PrayerService(private val storage: PrayerStorage, private val time: TimeSource = SystemTimeSource) {

    fun dayFor(s: PrayerSettings, date: LocalDate): PrayerDay {
        val c = Cities.byId(s.cityId)
        val raw = PrayerCalculator.compute(date, c.lat, c.lng, c.zone, s.method, s.asr)
        val adjusted = raw.times.mapValues { (p, t) -> t?.let { shift(it, s.adjustMin[p] ?: 0) } }
        return PrayerDay(date, adjusted)
    }

    fun today(s: PrayerSettings): PrayerDay = dayFor(s, dateNow(s))

    fun dateNow(s: PrayerSettings, atMillis: Long = time.nowMillis()): LocalDate =
        Instant.ofEpochMilli(atMillis).atZone(Cities.byId(s.cityId).zone).toLocalDate()

    private fun shift(t: LocalTime, minutes: Int): LocalTime = t.plusMinutes(minutes.toLong())

    // ---- log -------------------------------------------------------------------------------------------
    fun isDone(s: PrayerSettings, date: LocalDate, p: Prayer) = p in storage.doneSet(date.toString())

    /** "I prayed": stops reminders for that prayer on that day. */
    fun markPrayed(date: LocalDate, p: Prayer) {
        storage.setDone(date.toString(), p, true)
        storage.saveState(date.toString(), p, PrayerState(0, null))
    }

    /** Edit/undo a log entry. */
    fun unmarkPrayed(date: LocalDate, p: Prayer) = storage.setDone(date.toString(), p, false)

    fun deleteAllData() = storage.clearAll()

    /** Housekeeping: forget log rows older than [keepDays] days. */
    fun pruneOld(s: PrayerSettings, keepDays: Long) = storage.pruneBefore(dateNow(s).minusDays(keepDays).toString())

    // ---- reminders ---------------------------------------------------------------------------------------
    /** All upcoming reminder moments (today + tomorrow) strictly after [after]. */
    fun upcoming(s: PrayerSettings, after: Long = time.nowMillis()): List<PrayerTrigger> {
        if (!s.enabled) return emptyList()
        val zone = Cities.byId(s.cityId).zone
        val first = dateNow(s, after)
        val out = mutableListOf<PrayerTrigger>()
        for (offset in 0L..1L) {
            val date = first.plusDays(offset)
            val key = date.toString()
            val day = dayFor(s, date)
            val done = storage.doneSet(key)
            for (p in s.remindFor) {
                if (p in done) continue
                val t = day[p] ?: continue                       // not computable -> no reminder, never a guess
                val st = storage.state(key, p)
                val at = when {
                    st.sentCount == 0 -> date.atTime(t).atZone(zone).toInstant().toEpochMilli() - s.minutesBefore * 60_000L
                    st.nextAtMillis != null -> st.nextAtMillis
                    else -> continue                              // follow-ups exhausted
                }
                if (at > after) out += PrayerTrigger(key, p, at)
            }
        }
        return out.sortedBy { it.atMillis }
    }

    fun nextTrigger(s: PrayerSettings, after: Long = time.nowMillis()): PrayerTrigger? = upcoming(s, after).firstOrNull()

    /** Reminders whose moment lies in (from, to]. */
    fun due(s: PrayerSettings, from: Long, to: Long): List<PrayerTrigger> = upcoming(s, from).filter { it.atMillis <= to }

    /** Call after showing a reminder: schedules the follow-up (if any are left). */
    fun onFired(s: PrayerSettings, t: PrayerTrigger, nowMillis: Long = time.nowMillis()) {
        val st = storage.state(t.dayKey, t.prayer)
        val sent = st.sentCount + 1
        val next = if (sent < s.reminderCount) nowMillis + s.gapMinutes * 60_000L else null
        storage.saveState(t.dayKey, t.prayer, PrayerState(sent, next))
    }

    /** User chose "snooze": ask again after the configured gap, even if the follow-up budget was used up. */
    fun snooze(s: PrayerSettings, dayKey: String, p: Prayer, nowMillis: Long = time.nowMillis()) {
        val st = storage.state(dayKey, p)
        storage.saveState(dayKey, p, PrayerState(minOf(st.sentCount, s.reminderCount - 1).coerceAtLeast(1), nowMillis + s.gapMinutes * 60_000L))
    }
}
