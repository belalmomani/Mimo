package com.mimo.companion.domain.dialogue

import com.mimo.companion.domain.SystemTimeSource
import com.mimo.companion.domain.TimeSource
import com.mimo.companion.domain.weather.WeatherCondition
import java.time.Instant
import kotlin.random.Random

enum class SpeakBlock { NOT_ALLOWED_BY_MODE, QUIET_HOURS, IN_CALL, FOCUS_SESSION, TOO_SOON, HOURLY_LIMIT, NO_PHRASE }

sealed interface DialogueDecision {
    data class Say(val phrase: Phrase, val text: String) : DialogueDecision
    data class Silent(val reason: SpeakBlock) : DialogueDecision
}

/**
 * Decides WHETHER the companion speaks and WHAT it says. Pure logic, no Android.
 *
 * - User-initiated lines (tap) are always answered, even in quiet hours or a call-free focus session.
 * - Proactive lines need: mode allows the category, not in quiet hours, not in a call / focus session,
 *   minimum gap since the last proactive line, and under the per-hour cap.
 * - A phrase is not repeated until most of its category pool has been used (history window).
 * - After [markHandled]-style feedback the caller can suppress a category (e.g. a reminder just completed).
 */
class DialogueManager(
    private val settings: () -> DialogueSettings,
    private val time: TimeSource = SystemTimeSource,
    private val random: Random = Random.Default,
    private val bank: List<Phrase> = PhraseBank.ALL,
    private val globalHistorySize: Int = 8,
) {
    // Per-category history so a phrase is never repeated until the whole category pool is used.
    private val categoryHistory = mutableMapOf<PhraseCategory, ArrayDeque<String>>()
    private val recentIds = ArrayDeque<String>()
    private val proactiveTimes = ArrayDeque<Long>()
    private var lastProactiveAt = 0L
    private val suppressedUntil = mutableMapOf<PhraseCategory, Long>()

    fun isQuietHours(): Boolean {
        val s = settings()
        if (s.quietStartMin == s.quietEndMin) return false
        val now = Instant.ofEpochMilli(time.nowMillis()).atZone(time.zone()).toLocalTime()
        val m = now.hour * 60 + now.minute
        return if (s.quietStartMin < s.quietEndMin) m in s.quietStartMin until s.quietEndMin
        else m >= s.quietStartMin || m < s.quietEndMin
    }

    /** The user asked for it (tap). Never blocked by mode, quiet hours or limits. */
    fun reply(category: PhraseCategory = PhraseCategory.TAP_REPLY): DialogueDecision = pick(category, proactive = false)

    /** The app wants to say something on its own initiative. */
    fun proactive(category: PhraseCategory, ctx: DialogueContext = DialogueContext()): DialogueDecision {
        val s = settings()
        val allowed = when (s.mode) {
            InitiativeMode.QUIET -> false
            InitiativeMode.SOCIAL -> category != PhraseCategory.TAP_REPLY && category != PhraseCategory.ACK_DONE && category != PhraseCategory.PRAYER_ACK
            InitiativeMode.CUSTOM -> category in s.allowedProactive
        }
        if (!allowed) return DialogueDecision.Silent(SpeakBlock.NOT_ALLOWED_BY_MODE)
        if (isQuietHours()) return DialogueDecision.Silent(SpeakBlock.QUIET_HOURS)
        if (ctx.inCall) return DialogueDecision.Silent(SpeakBlock.IN_CALL)
        if (ctx.focusSession && ctx.respectFocus) return DialogueDecision.Silent(SpeakBlock.FOCUS_SESSION)
        val now = time.nowMillis()
        if (suppressedUntil[category]?.let { now < it } == true) return DialogueDecision.Silent(SpeakBlock.TOO_SOON)
        if (lastProactiveAt > 0 && now - lastProactiveAt < s.minSecondsBetweenProactive * 1000L) return DialogueDecision.Silent(SpeakBlock.TOO_SOON)
        while (proactiveTimes.isNotEmpty() && now - proactiveTimes.first() >= 3_600_000L) proactiveTimes.removeFirst()
        if (proactiveTimes.size >= s.maxProactivePerHour) return DialogueDecision.Silent(SpeakBlock.HOURLY_LIMIT)
        val d = pick(category, proactive = true)
        if (d is DialogueDecision.Say) { lastProactiveAt = now; proactiveTimes.addLast(now) }
        return d
    }

    /** E.g. the user just confirmed a habit: do not nag in this category for [seconds]. */
    fun suppress(category: PhraseCategory, seconds: Long) { suppressedUntil[category] = time.nowMillis() + seconds * 1000 }

    /**
     * Like [proactive] but filters WEATHER phrases to only those matching [condition].
     * The rate-limit and quiet-hours checks still apply.
     */
    fun proactiveWeather(condition: WeatherCondition, ctx: DialogueContext = DialogueContext()): DialogueDecision {
        val s = settings()
        val allowed = when (s.mode) {
            InitiativeMode.QUIET  -> false
            InitiativeMode.SOCIAL -> true
            InitiativeMode.CUSTOM -> PhraseCategory.WEATHER in s.allowedProactive
        }
        if (!allowed) return DialogueDecision.Silent(SpeakBlock.NOT_ALLOWED_BY_MODE)
        if (isQuietHours()) return DialogueDecision.Silent(SpeakBlock.QUIET_HOURS)
        if (ctx.inCall) return DialogueDecision.Silent(SpeakBlock.IN_CALL)
        if (ctx.focusSession && ctx.respectFocus) return DialogueDecision.Silent(SpeakBlock.FOCUS_SESSION)
        val now = time.nowMillis()
        if (lastProactiveAt > 0 && now - lastProactiveAt < s.minSecondsBetweenProactive * 1000L)
            return DialogueDecision.Silent(SpeakBlock.TOO_SOON)
        while (proactiveTimes.isNotEmpty() && now - proactiveTimes.first() >= 3_600_000L) proactiveTimes.removeFirst()
        if (proactiveTimes.size >= s.maxProactivePerHour) return DialogueDecision.Silent(SpeakBlock.HOURLY_LIMIT)
        val prefix = conditionPrefix(condition)
        val d = pickWeather(prefix, proactive = true)
        if (d is DialogueDecision.Say) { lastProactiveAt = now; proactiveTimes.addLast(now) }
        return d
    }

    private fun conditionPrefix(c: WeatherCondition) = when (c) {
        WeatherCondition.HOT    -> "wt_hot"
        WeatherCondition.COLD   -> "wt_cold"
        WeatherCondition.RAINY  -> "wt_rain"
        WeatherCondition.STORMY -> "wt_storm"
        WeatherCondition.SNOWY  -> "wt_snow"
        WeatherCondition.FOGGY  -> "wt_fog"
        WeatherCondition.SUNNY  -> "wt_sun"
        WeatherCondition.CLOUDY -> "wt_cloud"
        WeatherCondition.WINDY  -> "wt_wind"
    }

    private fun pickWeather(prefix: String, proactive: Boolean): DialogueDecision {
        val s = settings()
        val pool = bank.filter {
            it.category == PhraseCategory.WEATHER &&
            it.id.startsWith(prefix) &&
            (it.dialect == null || it.dialect == s.dialect)
        }.ifEmpty {
            // fall back to any WEATHER phrase if prefix has no match in chosen dialect
            bank.filter { it.category == PhraseCategory.WEATHER && (it.dialect == null || it.dialect == s.dialect) }
        }
        if (pool.isEmpty()) return DialogueDecision.Silent(SpeakBlock.NO_PHRASE)
        val catDeque = categoryHistory.getOrPut(PhraseCategory.WEATHER) { ArrayDeque() }
        val window = maxOf(0, pool.size - 1)
        val avoid = catDeque.toList().takeLast(window).toSet()
        val candidates = pool.filter { it.id !in avoid }.ifEmpty { pool }
        val chosen = candidates[random.nextInt(candidates.size)]
        catDeque.addLast(chosen.id)
        while (catDeque.size > bank.count { it.category == PhraseCategory.WEATHER }) catDeque.removeFirst()
        recentIds.addLast(chosen.id); while (recentIds.size > 64) recentIds.removeFirst()
        return DialogueDecision.Say(chosen, chosen.text(s.lang))
    }

    private fun pick(category: PhraseCategory, proactive: Boolean): DialogueDecision {
        val s = settings()
        val pool = bank.filter { it.category == category && (it.dialect == null || it.dialect == s.dialect) }
        if (pool.isEmpty()) return DialogueDecision.Silent(SpeakBlock.NO_PHRASE)

        // Per-category history: avoid repeating a phrase until all others in the pool are used.
        val catDeque = categoryHistory.getOrPut(category) { ArrayDeque() }
        val window = maxOf(0, pool.size - 1)   // never repeat last phrase if pool > 1
        val avoid = catDeque.toList().takeLast(window).toSet()
        val candidates = pool.filter { it.id !in avoid }.ifEmpty { pool }
        val chosen = candidates[random.nextInt(candidates.size)]

        catDeque.addLast(chosen.id)
        while (catDeque.size > pool.size) catDeque.removeFirst()   // keep window = pool size

        // Also keep global history for cross-category deduplication
        recentIds.addLast(chosen.id); while (recentIds.size > 64) recentIds.removeFirst()
        return DialogueDecision.Say(chosen, chosen.text(s.lang))
    }
}
