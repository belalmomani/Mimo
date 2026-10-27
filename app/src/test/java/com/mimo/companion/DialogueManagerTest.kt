package com.mimo.companion

import com.mimo.companion.domain.dialogue.Dialect
import com.mimo.companion.domain.dialogue.DialogueContext
import com.mimo.companion.domain.dialogue.DialogueDecision
import com.mimo.companion.domain.dialogue.DialogueManager
import com.mimo.companion.domain.dialogue.DialogueSettings
import com.mimo.companion.domain.dialogue.InitiativeMode
import com.mimo.companion.domain.dialogue.Lang
import com.mimo.companion.domain.dialogue.PhraseBank
import com.mimo.companion.domain.dialogue.PhraseCategory
import com.mimo.companion.domain.dialogue.SpeakBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlin.random.Random
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class DialogueManagerTest {
    // 2027-01-15 12:00:00 UTC (midday: outside default quiet hours 23:00-07:00)
    private val noon = Instant.parse("2027-01-15T12:00:00Z").toEpochMilli()
    private fun at(h: Int, m: Int = 0) = Instant.parse("2027-01-15T00:00:00Z").toEpochMilli() + (h * 60 + m) * 60_000L

    private fun mgr(s: DialogueSettings, t: FakeTime = FakeTime(noon), seed: Int = 1) =
        DialogueManager({ s }, t, Random(seed)) to t

    private val social = DialogueSettings(mode = InitiativeMode.SOCIAL, minSecondsBetweenProactive = 0, maxProactivePerHour = 100)

    @Test fun quietModeNeverStartsAConversationButAnswersTaps() {
        val (m, _) = mgr(DialogueSettings(mode = InitiativeMode.QUIET))
        assertEquals(DialogueDecision.Silent(SpeakBlock.NOT_ALLOWED_BY_MODE), m.proactive(PhraseCategory.IDLE_CHAT))
        assertTrue(m.reply() is DialogueDecision.Say)
    }

    @Test fun socialModeSpeaksProactively() {
        val (m, _) = mgr(social)
        assertTrue(m.proactive(PhraseCategory.IDLE_CHAT) is DialogueDecision.Say)
    }

    @Test fun customModeOnlyAllowsChosenCategories() {
        val (m, _) = mgr(social.copy(mode = InitiativeMode.CUSTOM, allowedProactive = setOf(PhraseCategory.BREAK_SUGGESTION)))
        assertEquals(DialogueDecision.Silent(SpeakBlock.NOT_ALLOWED_BY_MODE), m.proactive(PhraseCategory.IDLE_CHAT))
        assertTrue(m.proactive(PhraseCategory.BREAK_SUGGESTION) is DialogueDecision.Say)
    }

    @Test fun silentDuringQuietHoursIncludingWrapAroundMidnight() {
        val s = social.copy(quietStartMin = 22 * 60, quietEndMin = 7 * 60)
        for ((h, quiet) in listOf(23 to true, 3 to true, 6 to true, 7 to false, 12 to false, 21 to false, 22 to true)) {
            val (m, _) = mgr(s, FakeTime(at(h)))
            val d = m.proactive(PhraseCategory.IDLE_CHAT)
            assertEquals("hour $h", quiet, d is DialogueDecision.Silent && d.reason == SpeakBlock.QUIET_HOURS)
        }
    }

    @Test fun tapStillAnswersInQuietHours() {
        val (m, _) = mgr(social, FakeTime(at(3)))
        assertTrue(m.reply() is DialogueDecision.Say)
    }

    @Test fun equalStartAndEndDisablesQuietHours() {
        val (m, _) = mgr(social.copy(quietStartMin = 0, quietEndMin = 0), FakeTime(at(3)))
        assertFalse(m.isQuietHours())
    }

    @Test fun noProactiveSpeechDuringCallsOrRespectedFocus() {
        val (m, _) = mgr(social)
        assertEquals(DialogueDecision.Silent(SpeakBlock.IN_CALL), m.proactive(PhraseCategory.IDLE_CHAT, DialogueContext(inCall = true)))
        assertEquals(DialogueDecision.Silent(SpeakBlock.FOCUS_SESSION), m.proactive(PhraseCategory.IDLE_CHAT, DialogueContext(focusSession = true)))
        assertTrue(m.proactive(PhraseCategory.IDLE_CHAT, DialogueContext(focusSession = true, respectFocus = false)) is DialogueDecision.Say)
    }

    @Test fun minimumGapAndHourlyCapAreEnforced() {
        val s = social.copy(minSecondsBetweenProactive = 600, maxProactivePerHour = 2)
        val (m, t) = mgr(s)
        assertTrue(m.proactive(PhraseCategory.IDLE_CHAT) is DialogueDecision.Say)
        t.advanceSeconds(300)
        assertEquals(DialogueDecision.Silent(SpeakBlock.TOO_SOON), m.proactive(PhraseCategory.IDLE_CHAT))
        t.advanceSeconds(400)
        assertTrue(m.proactive(PhraseCategory.IDLE_CHAT) is DialogueDecision.Say)
        t.advanceSeconds(700)
        assertEquals(DialogueDecision.Silent(SpeakBlock.HOURLY_LIMIT), m.proactive(PhraseCategory.IDLE_CHAT))
        t.advanceSeconds(3600)
        assertTrue(m.proactive(PhraseCategory.IDLE_CHAT) is DialogueDecision.Say)
    }

    @Test fun blockedAttemptsDoNotConsumeTheBudget() {
        val s = social.copy(maxProactivePerHour = 1)
        val (m, _) = mgr(s)
        m.proactive(PhraseCategory.IDLE_CHAT, DialogueContext(inCall = true)) // blocked
        assertTrue(m.proactive(PhraseCategory.IDLE_CHAT) is DialogueDecision.Say)
    }

    @Test fun phrasesAreNotRepeatedUntilThePoolIsUsed() {
        val (m, _) = mgr(social)
        val pool = PhraseBank.ALL.count { it.category == PhraseCategory.TAP_REPLY && (it.dialect == null) }
        val ids = (1..pool).map { (m.reply() as DialogueDecision.Say).phrase.id }
        assertEquals("no repeat within one full pass", ids.size, ids.toSet().size)
    }

    @Test fun neverTheSamePhraseTwiceInARow() {
        for (seed in 1..40) {
            val (m, _) = mgr(social, seed = seed)
            val a = (m.reply() as DialogueDecision.Say).phrase.id
            val b = (m.reply() as DialogueDecision.Say).phrase.id
            assertTrue("seed $seed", a != b)
        }
    }

    @Test fun dialectPhrasesOnlyWhenChosen() {
        val (neutral, _) = mgr(social.copy(dialect = Dialect.NEUTRAL))
        repeat(60) { assertTrue((neutral.reply() as DialogueDecision.Say).phrase.dialect == null) }
        val (jo, _) = mgr(social.copy(dialect = Dialect.JORDANIAN))
        val seen = (1..60).map { (jo.reply() as DialogueDecision.Say).phrase.dialect }.toSet()
        assertTrue(Dialect.JORDANIAN in seen)
    }

    @Test fun languageSelectsTheText() {
        val (ar, _) = mgr(social.copy(lang = Lang.AR)); val (en, _) = mgr(social.copy(lang = Lang.EN))
        val a = (ar.reply() as DialogueDecision.Say); val e = (en.reply() as DialogueDecision.Say)
        assertEquals(a.phrase.ar, a.text); assertEquals(e.phrase.en, e.text)
    }

    @Test fun suppressingACategoryStopsNagging() {
        val (m, t) = mgr(social)
        m.suppress(PhraseCategory.ENCOURAGE, 600)
        assertEquals(DialogueDecision.Silent(SpeakBlock.TOO_SOON), m.proactive(PhraseCategory.ENCOURAGE))
        t.advanceSeconds(601)
        assertTrue(m.proactive(PhraseCategory.ENCOURAGE) is DialogueDecision.Say)
    }

    @Test fun phraseBankIsCleanAndComplete() {
        PhraseBank.ALL.forEach { p ->
            PhraseBank.FORBIDDEN_FRAGMENTS.forEach { bad -> assertFalse("${p.id} contains '$bad'", p.ar.contains(bad)) }
            assertTrue(p.id, p.ar.isNotBlank() && p.en.isNotBlank())
        }
        PhraseCategory.values().forEach { c ->
            val n = PhraseBank.ALL.count { it.category == c && it.dialect == null }
            assertTrue("$c needs >=2 neutral phrases (has $n)", n >= 2)
        }
    }
}
