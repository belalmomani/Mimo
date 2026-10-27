package com.mimo.companion.domain.reminders

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** Pure date arithmetic: when does a reminder fire next? */
object ReminderScheduler {
    /**
     * Next trigger strictly after [afterMillis], or null when there is none
     * (disabled, one-time already done/past). Occurrences on days already marked done are skipped,
     * so a habit completed this morning does not ask again tonight. A pending snooze wins if it comes first.
     */
    fun nextTrigger(r: Reminder, state: ReminderState, afterMillis: Long, zone: ZoneId): Long? {
        if (!r.enabled) return null
        val snooze = state.snoozedUntil.takeIf { it > afterMillis }
        val regular = nextRegular(r, state, afterMillis, zone)
        return listOfNotNull(snooze, regular).minOrNull()
    }

    private fun nextRegular(r: Reminder, state: ReminderState, after: Long, zone: ZoneId): Long? {
        val today = java.time.Instant.ofEpochMilli(after).atZone(zone).toLocalDate()
        if (r.repeat == RepeatType.ONCE) {
            val d = LocalDate.parse(r.onceDate!!)
            return at(d, r, zone).takeIf { it > after && d.toString() !in state.completedDays }
        }
        for (i in 0..8) {                       // a week plus a day always contains the next matching day
            val d = today.plusDays(i.toLong())
            if (r.repeat == RepeatType.WEEKLY && d.dayOfWeek !in r.days) continue
            if (d.toString() in state.completedDays) continue
            val t = at(d, r, zone)
            if (t > after) return t
        }
        return null
    }

    private fun at(d: LocalDate, r: Reminder, zone: ZoneId): Long =
        d.atTime(r.timeMin / 60, r.timeMin % 60).atZone(zone).toInstant().toEpochMilli()
}
