package com.mimo.companion

import com.mimo.companion.domain.reminders.Delivery
import com.mimo.companion.domain.reminders.Reminder
import com.mimo.companion.domain.reminders.ReminderManager
import com.mimo.companion.domain.reminders.ReminderScheduler
import com.mimo.companion.domain.reminders.ReminderState
import com.mimo.companion.domain.reminders.ReminderStorage
import com.mimo.companion.domain.reminders.RepeatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset

class InMemoryReminderStorage : ReminderStorage {
    private val items = linkedMapOf<String, Reminder>()
    private val done = mutableMapOf<String, MutableSet<String>>()
    private val snooze = mutableMapOf<String, Long>()
    override fun all() = items.values.toList()
    override fun get(id: String) = items[id]
    override fun upsert(r: Reminder) { items[r.id] = r }
    override fun delete(id: String) { items.remove(id); done.remove(id); snooze.remove(id) }
    override fun completedDays(id: String) = done[id]?.toSet() ?: emptySet()
    override fun addCompletion(id: String, dayKey: String) { done.getOrPut(id) { mutableSetOf() }.add(dayKey) }
    override fun snoozedUntil(id: String) = snooze[id] ?: 0L
    override fun setSnoozedUntil(id: String, millis: Long) { snooze[id] = millis }
}

class RemindersTest {
    private val z = ZoneOffset.UTC
    private fun ms(iso: String) = Instant.parse(iso).toEpochMilli()
    // 2027-01-15 is a Friday
    private val friNoon = ms("2027-01-15T12:00:00Z")
    private fun water(h: Int, m: Int = 0, repeat: RepeatType = RepeatType.DAILY, days: Set<DayOfWeek> = emptySet(), date: String? = null) =
        Reminder("r", "water", timeMin = h * 60 + m, repeat = repeat, days = days, onceDate = date)

    @Test fun dailyFiresLaterTodayOtherwiseTomorrow() {
        assertEquals(ms("2027-01-15T18:00:00Z"), ReminderScheduler.nextTrigger(water(18), ReminderState(), friNoon, z))
        assertEquals(ms("2027-01-16T09:00:00Z"), ReminderScheduler.nextTrigger(water(9), ReminderState(), friNoon, z))
    }

    @Test fun triggerIsStrictlyAfterTheReferenceTime() {
        assertEquals(ms("2027-01-16T12:00:00Z"), ReminderScheduler.nextTrigger(water(12), ReminderState(), friNoon, z))
    }

    @Test fun weeklyPicksTheNextMatchingDay() {
        val r = water(8, repeat = RepeatType.WEEKLY, days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        assertEquals(ms("2027-01-18T08:00:00Z"), ReminderScheduler.nextTrigger(r, ReminderState(), friNoon, z)) // Fri -> Mon
        val sameDayLater = water(20, repeat = RepeatType.WEEKLY, days = setOf(DayOfWeek.FRIDAY))
        assertEquals(ms("2027-01-15T20:00:00Z"), ReminderScheduler.nextTrigger(sameDayLater, ReminderState(), friNoon, z))
        val sameDayPassed = water(8, repeat = RepeatType.WEEKLY, days = setOf(DayOfWeek.FRIDAY))
        assertEquals(ms("2027-01-22T08:00:00Z"), ReminderScheduler.nextTrigger(sameDayPassed, ReminderState(), friNoon, z)) // next week
    }

    @Test fun onceFiresOnlyOnItsDateAndNeverAgain() {
        val r = water(10, repeat = RepeatType.ONCE, date = "2027-01-20")
        assertEquals(ms("2027-01-20T10:00:00Z"), ReminderScheduler.nextTrigger(r, ReminderState(), friNoon, z))
        assertNull(ReminderScheduler.nextTrigger(r, ReminderState(), ms("2027-01-20T10:00:01Z"), z))
        assertNull(ReminderScheduler.nextTrigger(r, ReminderState(setOf("2027-01-20")), friNoon, z))
    }

    @Test fun oneTimeReminderDoesNotRefireAtItsExactTime() {
        val r = water(10, repeat = RepeatType.ONCE, date = "2027-01-20")
        assertNull(ReminderScheduler.nextTrigger(r, ReminderState(), ms("2027-01-20T10:00:00Z"), z))
    }

    @Test fun disabledReminderNeverFires() {
        assertNull(ReminderScheduler.nextTrigger(water(18).copy(enabled = false), ReminderState(), friNoon, z))
    }

    @Test fun completedDaySkipsThatOccurrence() {
        val st = ReminderState(completedDays = setOf("2027-01-15"))
        assertEquals(ms("2027-01-16T18:00:00Z"), ReminderScheduler.nextTrigger(water(18), st, friNoon, z))
    }

    @Test fun snoozeWinsWhenEarlierAndIsIgnoredWhenPast() {
        val st = ReminderState(snoozedUntil = ms("2027-01-15T12:10:00Z"))
        assertEquals(ms("2027-01-15T12:10:00Z"), ReminderScheduler.nextTrigger(water(18), st, friNoon, z))
        assertEquals(ms("2027-01-15T18:00:00Z"), ReminderScheduler.nextTrigger(water(18), st, ms("2027-01-15T12:11:00Z"), z))
    }

    @Test fun rangeAcrossMonthAndYearBoundary() {
        assertEquals(ms("2028-01-01T07:30:00Z"), ReminderScheduler.nextTrigger(water(7, 30), ReminderState(), ms("2027-12-31T23:00:00Z"), z))
    }

    @Test fun timeZoneIsRespected() {
        val amman = java.time.ZoneId.of("Asia/Amman") // UTC+2 in winter (UTC+3 since 2022 permanent) -> use the zone's own rule
        val next = ReminderScheduler.nextTrigger(water(9), ReminderState(), friNoon, amman)!!
        assertEquals(9, Instant.ofEpochMilli(next).atZone(amman).hour)
    }

    // ---- manager --------------------------------------------------------------------------------------
    private fun env(): Triple<ReminderManager, InMemoryReminderStorage, FakeTime> {
        val s = InMemoryReminderStorage(); val t = FakeTime(friNoon)
        return Triple(ReminderManager(s, t), s, t)
    }

    @Test fun markDoneSkipsTodayAndCancelsSnooze() {
        val (m, _, t) = env()
        m.save(water(18)); m.snooze("r")
        m.markDone("r")
        assertTrue(m.isDoneToday("r"))
        assertEquals(0L, m.state("r").snoozedUntil)
        assertEquals(ms("2027-01-16T18:00:00Z"), m.nextFor(m.list().single()))
        t.advanceDays(1)
        assertFalse(m.isDoneToday("r"))
    }

    @Test fun snoozeUsesTheReminderOwnDuration() {
        val (m, _, _) = env()
        m.save(water(18).copy(snoozeMinutes = 25))
        assertEquals(friNoon + 25 * 60_000L, m.snooze("r"))
        assertEquals(friNoon + 25 * 60_000L, m.nextUpcoming()!!.atMillis)
        m.consumeSnooze("r")
        assertEquals(ms("2027-01-15T18:00:00Z"), m.nextUpcoming()!!.atMillis)
    }

    @Test fun nextUpcomingPicksTheEarliestAcrossReminders() {
        val (m, _, _) = env()
        m.save(water(20).copy(id = "a", name = "a")); m.save(water(15).copy(id = "b", name = "b")); m.save(water(14).copy(id = "c", name = "c", enabled = false))
        assertEquals("b", m.nextUpcoming()!!.reminder.id)
        m.setEnabled("b", false)
        assertEquals("a", m.nextUpcoming()!!.reminder.id)
    }

    @Test fun deleteRemovesEverythingAndNothingFiresAfter() {
        val (m, _, _) = env()
        m.save(water(18)); m.markDone("r"); m.delete("r")
        assertNull(m.nextUpcoming()); assertTrue(m.list().isEmpty())
    }

    @Test fun dueListsOnlyRemindersInTheWindow() {
        val (m, _, _) = env()
        m.save(water(13).copy(id = "a", name = "a")); m.save(water(19).copy(id = "b", name = "b"))
        assertEquals(listOf("a"), m.due(friNoon, ms("2027-01-15T14:00:00Z")).map { it.id })
        assertTrue(m.due(friNoon, friNoon + 1000).isEmpty())
    }

    @Test fun validationRejectsBadReminders() {
        for (bad in listOf<() -> Reminder>(
            { Reminder("x", " ", timeMin = 10) }, { Reminder("x", "n", timeMin = 1440) }, { Reminder("x", "n", timeMin = -1) },
            { Reminder("x", "n", timeMin = 10, repeat = RepeatType.WEEKLY) }, { Reminder("x", "n", timeMin = 10, repeat = RepeatType.ONCE) },
            { Reminder("x", "n", timeMin = 10, snoozeMinutes = 0) },
        )) { try { bad(); org.junit.Assert.fail("should be rejected") } catch (e: IllegalArgumentException) { } }
    }

    @Test fun messageUsesCustomTextWhenGiven() {
        assertEquals("حان وقت water", water(9).message("حان وقت"))
        assertEquals("اشرب كوب ماء", water(9).copy(customMessage = "اشرب كوب ماء").message("حان وقت"))
        assertEquals(setOf(Delivery.NOTIFICATION), water(9).delivery)
    }
}
