package com.mimo.companion.domain.reminders

import com.mimo.companion.domain.SystemTimeSource
import com.mimo.companion.domain.TimeSource
import com.mimo.companion.domain.dayKey

data class UpcomingReminder(val reminder: Reminder, val atMillis: Long)

class ReminderManager(private val storage: ReminderStorage, private val time: TimeSource = SystemTimeSource) {
    fun list(): List<Reminder> = storage.all().sortedBy { it.timeMin }

    fun save(r: Reminder) = storage.upsert(r)

    fun delete(id: String) = storage.delete(id)

    fun setEnabled(id: String, enabled: Boolean) {
        val r = storage.get(id) ?: return
        storage.upsert(r.copy(enabled = enabled))
        if (!enabled) storage.setSnoozedUntil(id, 0L)
    }

    fun state(id: String) = ReminderState(storage.completedDays(id), storage.snoozedUntil(id))

    fun isDoneToday(id: String) = time.dayKey() in storage.completedDays(id)

    /** User confirms it is done: today's occurrence is not repeated, any snooze is cancelled. */
    fun markDone(id: String) {
        if (storage.get(id) == null) return
        storage.addCompletion(id, time.dayKey())
        storage.setSnoozedUntil(id, 0L)
    }

    /** Postpone by the reminder's own snooze length. @return when it will fire again. */
    fun snooze(id: String): Long? {
        val r = storage.get(id) ?: return null
        val until = time.nowMillis() + r.snoozeMinutes * 60_000L
        storage.setSnoozedUntil(id, until)
        return until
    }

    /** A snooze that already fired is consumed so it does not linger. */
    fun consumeSnooze(id: String) = storage.setSnoozedUntil(id, 0L)

    fun nextFor(r: Reminder, after: Long = time.nowMillis()): Long? = ReminderScheduler.nextTrigger(r, state(r.id), after, time.zone())

    /** The earliest upcoming trigger across all reminders (what the alarm should be set to). */
    fun nextUpcoming(after: Long = time.nowMillis()): UpcomingReminder? =
        storage.all().mapNotNull { r -> nextFor(r, after)?.let { UpcomingReminder(r, it) } }.minByOrNull { it.atMillis }

    /** Reminders whose trigger lies in (from, to] - what to show when the alarm wakes us. */
    fun due(from: Long, to: Long): List<Reminder> =
        storage.all().filter { r -> nextFor(r, from)?.let { it <= to } == true }
}
