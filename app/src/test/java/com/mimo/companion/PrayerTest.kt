package com.mimo.companion

import com.mimo.companion.domain.prayer.AsrMethod
import com.mimo.companion.domain.prayer.CalcMethod
import com.mimo.companion.domain.prayer.Cities
import com.mimo.companion.domain.prayer.Prayer
import com.mimo.companion.domain.prayer.PrayerCalculator
import com.mimo.companion.domain.prayer.PrayerService
import com.mimo.companion.domain.prayer.PrayerSettings
import com.mimo.companion.domain.prayer.PrayerState
import com.mimo.companion.domain.prayer.PrayerStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

class InMemoryPrayerStorage : PrayerStorage {
    val done = mutableMapOf<String, MutableSet<Prayer>>()
    val states = mutableMapOf<Pair<String, Prayer>, PrayerState>()
    override fun doneSet(dayKey: String) = done[dayKey]?.toSet() ?: emptySet()
    override fun setDone(dayKey: String, prayer: Prayer, done: Boolean) { val s = this.done.getOrPut(dayKey) { mutableSetOf() }; if (done) s.add(prayer) else s.remove(prayer) }
    override fun state(dayKey: String, prayer: Prayer) = states[dayKey to prayer] ?: PrayerState()
    override fun saveState(dayKey: String, prayer: Prayer, state: PrayerState) { states[dayKey to prayer] = state }
    override fun pruneBefore(dayKey: String) { done.keys.removeAll { it < dayKey } }
    override fun clearAll() { done.clear(); states.clear() }
}

class PrayerTest {
    private fun min(h: Int, m: Int) = h * 60 + m
    private fun assertNear(label: String, expected: Int, actual: LocalTime?, tol: Int = 2) {
        assertNotNull("$label missing", actual)
        val a = actual!!.hour * 60 + actual.minute
        assertTrue("$label expected ${expected / 60}:${expected % 60} got $actual (tolerance $tol min)", abs(a - expected) <= tol)
    }

    // Reference tables published by independent sites (fetched 2026-10-27). See docs: tolerance 2 minutes.
    @Test fun ammanUmmAlQuraHanafiMatchesPublishedTable() {
        val d = PrayerCalculator.compute(LocalDate.of(2026, 10, 22), 31.9539, 35.9106, 3.0, CalcMethod.UMM_AL_QURA, AsrMethod.HANAFI)
        assertNear("fajr", min(5, 21), d[Prayer.FAJR]); assertNear("dhuhr", min(12, 21), d[Prayer.DHUHR]); assertNear("asr", min(16, 18), d[Prayer.ASR])
        assertNear("maghrib", min(17, 56), d[Prayer.MAGHRIB]); assertNear("isha", min(19, 26), d[Prayer.ISHA])
    }

    @Test fun meccaKarachiMatchesPublishedTable() {
        val d = PrayerCalculator.compute(LocalDate.of(2026, 10, 20), 21.4225, 39.8262, 3.0, CalcMethod.KARACHI, AsrMethod.STANDARD)
        assertNear("fajr", min(5, 4), d[Prayer.FAJR]); assertNear("sunrise", min(6, 19), d[Prayer.SUNRISE]); assertNear("dhuhr", min(12, 6), d[Prayer.DHUHR])
        assertNear("asr", min(15, 24), d[Prayer.ASR]); assertNear("maghrib", min(17, 52), d[Prayer.MAGHRIB]); assertNear("isha", min(19, 7), d[Prayer.ISHA])
    }

    @Test fun orderOfPrayersIsAlwaysSensible() {
        for (city in Cities.ALL.filter { it.lat < 45 }) for (month in 1..12) {
            val d = PrayerCalculator.compute(LocalDate.of(2027, month, 15), city.lat, city.lng, city.zone, CalcMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD)
            val seq = listOf(Prayer.FAJR, Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA).map { d[it] }
            assertTrue("${city.id}/$month has null: $seq", seq.none { it == null })
            assertEquals("${city.id}/$month not increasing: $seq", seq, seq.sortedBy { it!!.toSecondOfDay() })
        }
    }

    @Test fun hanafiAsrIsLaterThanStandard() {
        val a = PrayerCalculator.compute(LocalDate.of(2027, 3, 1), 31.95, 35.91, 2.0, CalcMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD)[Prayer.ASR]!!
        val h = PrayerCalculator.compute(LocalDate.of(2027, 3, 1), 31.95, 35.91, 2.0, CalcMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.HANAFI)[Prayer.ASR]!!
        assertTrue(h.isAfter(a))
    }

    @Test fun highLatitudeSummerGivesNoGuessedTimes() {
        val oslo = PrayerCalculator.compute(LocalDate.of(2027, 6, 21), 59.9139, 10.7522, 2.0, CalcMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD)
        assertNull(oslo[Prayer.FAJR]); assertNull(oslo[Prayer.ISHA]); assertNotNull(oslo[Prayer.DHUHR])
    }

    // ---- service -------------------------------------------------------------------------------------------
    private val amman = ZoneId.of("Asia/Amman")
    private fun settings(vararg change: (PrayerSettings) -> PrayerSettings) = change.fold(PrayerSettings(enabled = true, cityId = "amman")) { s, f -> f(s) }
    private fun at(date: String, h: Int, m: Int) = LocalDate.parse(date).atTime(h, m).atZone(amman).toInstant().toEpochMilli()
    private fun svc(nowMs: Long): Triple<PrayerService, InMemoryPrayerStorage, FakeTime> {
        val st = InMemoryPrayerStorage(); val t = FakeTime(nowMs); return Triple(PrayerService(st, t), st, t)
    }

    @Test fun adjustmentsShiftTheTime() {
        val (p, _, _) = svc(at("2027-01-15", 1, 0))
        val s0 = settings(); val s1 = settings({ it.copy(adjustMin = mapOf(Prayer.DHUHR to 3, Prayer.FAJR to -2)) })
        val d0 = p.dayFor(s0, LocalDate.of(2027, 1, 15)); val d1 = p.dayFor(s1, LocalDate.of(2027, 1, 15))
        assertEquals(d0[Prayer.DHUHR]!!.plusMinutes(3), d1[Prayer.DHUHR]); assertEquals(d0[Prayer.FAJR]!!.minusMinutes(2), d1[Prayer.FAJR])
        assertEquals(d0[Prayer.ASR], d1[Prayer.ASR])
    }

    @Test fun disabledFeatureProducesNoReminders() {
        val (p, _, _) = svc(at("2027-01-15", 1, 0))
        assertTrue(p.upcoming(settings({ it.copy(enabled = false) })).isEmpty())
    }

    @Test fun firstReminderIsMinutesBeforeThePrayer() {
        val (p, _, _) = svc(at("2027-01-15", 1, 0))
        val s = settings({ it.copy(minutesBefore = 15) })
        val next = p.nextTrigger(s)!!
        val fajr = p.dayFor(s, LocalDate.of(2027, 1, 15))[Prayer.FAJR]!!
        assertEquals(Prayer.FAJR, next.prayer)
        assertEquals(LocalDate.of(2027, 1, 15).atTime(fajr).atZone(amman).toInstant().toEpochMilli() - 15 * 60_000L, next.atMillis)
    }

    @Test fun onlyChosenPrayersAreRemindedAndSunriseNever() {
        val (p, _, _) = svc(at("2027-01-15", 1, 0))
        val s = settings({ it.copy(remindFor = setOf(Prayer.MAGHRIB)) })
        assertTrue(p.upcoming(s).all { it.prayer == Prayer.MAGHRIB })
        try { PrayerSettings(remindFor = setOf(Prayer.SUNRISE)); org.junit.Assert.fail() } catch (e: IllegalArgumentException) { }
    }

    @Test fun afterIshaTheNextReminderIsTomorrowsFajr() {
        val (p, _, _) = svc(at("2027-01-15", 23, 30))
        val n = p.nextTrigger(settings())!!
        assertEquals("2027-01-16", n.dayKey); assertEquals(Prayer.FAJR, n.prayer)
    }

    @Test fun markedPrayedIsNotRemindedAgainAndSilenceIsNotConfirmation() {
        val (p, _, _) = svc(at("2027-01-15", 1, 0))
        val s = settings({ it.copy(remindFor = setOf(Prayer.FAJR, Prayer.DHUHR)) })
        val first = p.nextTrigger(s)!!
        assertEquals(Prayer.FAJR, first.prayer)
        // ignoring it (no log entry) keeps the prayer open for follow-ups
        assertFalse(p.isDone(s, LocalDate.of(2027, 1, 15), Prayer.FAJR))
        p.markPrayed(LocalDate.of(2027, 1, 15), Prayer.FAJR)
        assertTrue(p.isDone(s, LocalDate.of(2027, 1, 15), Prayer.FAJR))
        assertEquals(Prayer.DHUHR, p.nextTrigger(s)!!.prayer)
        p.unmarkPrayed(LocalDate.of(2027, 1, 15), Prayer.FAJR)
        assertEquals(Prayer.FAJR, p.nextTrigger(s)!!.prayer)
    }
    private fun assertFalse(b: Boolean) = org.junit.Assert.assertFalse(b)
    private fun assertFalse(msg: String, b: Boolean) = org.junit.Assert.assertFalse(msg, b)

    @Test fun followUpsFollowTheConfiguredCountAndGap() {
        val (p, _, t) = svc(at("2027-01-15", 1, 0))
        val s = settings({ it.copy(remindFor = setOf(Prayer.FAJR), reminderCount = 3, gapMinutes = 10) })
        val t1 = p.nextTrigger(s)!!
        t.now = t1.atMillis; p.onFired(s, t1)
        val t2 = p.nextTrigger(s, t.now)!!
        assertEquals(Prayer.FAJR, t2.prayer); assertEquals(t1.atMillis + 10 * 60_000L, t2.atMillis)
        t.now = t2.atMillis; p.onFired(s, t2)
        val t3 = p.nextTrigger(s, t.now)!!; assertEquals(t2.atMillis + 10 * 60_000L, t3.atMillis)
        t.now = t3.atMillis; p.onFired(s, t3)
        val after = p.nextTrigger(s, t.now)!!                     // budget exhausted: Fajr is finished, next is a different day/prayer
        assertTrue(after.prayer != Prayer.FAJR || after.dayKey != "2027-01-15")
    }

    @Test fun singleReminderMeansNoFollowUp() {
        val (p, _, t) = svc(at("2027-01-15", 1, 0))
        val s = settings({ it.copy(remindFor = setOf(Prayer.FAJR), reminderCount = 1) })
        val t1 = p.nextTrigger(s)!!; t.now = t1.atMillis; p.onFired(s, t1)
        assertTrue(p.upcoming(s, t.now).none { it.dayKey == "2027-01-15" })
    }

    @Test fun snoozeAlwaysWorksEvenWithTheBudgetUsedUp() {
        val (p, _, t) = svc(at("2027-01-15", 1, 0))
        val s = settings({ it.copy(remindFor = setOf(Prayer.FAJR), reminderCount = 1, gapMinutes = 7) })
        val t1 = p.nextTrigger(s)!!; t.now = t1.atMillis; p.onFired(s, t1)
        p.snooze(s, t1.dayKey, Prayer.FAJR, t.now)
        val n = p.nextTrigger(s, t.now)!!
        assertEquals(Prayer.FAJR, n.prayer); assertEquals(t.now + 7 * 60_000L, n.atMillis)
    }

    @Test fun dueWindowReturnsOnlyWhatIsInRange() {
        val (p, _, _) = svc(at("2027-01-15", 1, 0))
        val s = settings({ it.copy(remindFor = setOf(Prayer.FAJR, Prayer.DHUHR)) })
        val fajr = p.nextTrigger(s)!!
        assertEquals(listOf(Prayer.FAJR), p.due(s, fajr.atMillis - 1000, fajr.atMillis + 1000).map { it.prayer })
        assertTrue(p.due(s, fajr.atMillis - 5000, fajr.atMillis - 1000).isEmpty())
    }

    @Test fun uncomputableTimesAreSkippedNotGuessed() {
        val st = InMemoryPrayerStorage(); val t = FakeTime(Instant.parse("2027-06-20T22:00:00Z").toEpochMilli())
        val p = PrayerService(st, t)
        val s = PrayerSettings(enabled = true, cityId = "london", remindFor = setOf(Prayer.FAJR, Prayer.ISHA, Prayer.DHUHR))
        assertTrue(p.upcoming(s).none { it.prayer == Prayer.FAJR || it.prayer == Prayer.ISHA })
        assertTrue(p.upcoming(s).any { it.prayer == Prayer.DHUHR })
    }

    @Test fun deleteAllDataClearsLogAndState() {
        val (p, st, _) = svc(at("2027-01-15", 1, 0))
        p.markPrayed(LocalDate.of(2027, 1, 15), Prayer.FAJR); p.deleteAllData()
        assertTrue(st.done.isEmpty() && st.states.isEmpty())
    }

    @Test fun settingsValidation() {
        for (bad in listOf<() -> PrayerSettings>({ PrayerSettings(minutesBefore = 61) }, { PrayerSettings(reminderCount = 0) }, { PrayerSettings(reminderCount = 4) },
            { PrayerSettings(adjustMin = mapOf(Prayer.ASR to 31)) }, { PrayerSettings(gapMinutes = 0) })) {
            try { bad(); org.junit.Assert.fail() } catch (e: IllegalArgumentException) { }
        }
    }
}
