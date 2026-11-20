package com.mimo.companion.domain.custom

/**
 * Manages user-created phrases.
 * Pure domain — no Android dependencies.
 */
class CustomPhraseRepository(private val storage: CustomPhraseStorage) {

    private var cache: List<CustomPhrase> = storage.loadAll()

    fun all(): List<CustomPhrase> = cache

    fun enabled(): List<CustomPhrase> = cache.filter { it.enabled }

    /** All enabled phrases linked to the given event. */
    fun forEvent(event: PhraseEvent): List<CustomPhrase> =
        cache.filter { it.enabled && it.event == event }

    /**
     * Pick one phrase for a tap event. Priority:
     * 1. TAP phrases first (custom replies)
     * 2. NONE phrases (generic custom phrases)
     * Returns null if none available.
     */
    fun pickForTap(): CustomPhrase? {
        val tap = forEvent(PhraseEvent.TAP)
        if (tap.isNotEmpty()) return tap.random()
        val none = forEvent(PhraseEvent.NONE)
        return if (none.isNotEmpty()) none.random() else null
    }

    fun add(phrase: CustomPhrase): CustomPhrase {
        val saved = phrase.copy(id = phrase.id)
        cache = cache + saved
        storage.save(saved)
        return saved
    }

    fun update(phrase: CustomPhrase) {
        cache = cache.map { if (it.id == phrase.id) phrase else it }
        storage.save(phrase)
    }

    fun remove(id: String) {
        cache = cache.filterNot { it.id == id }
        storage.delete(id)
    }

    fun setEnabled(id: String, enabled: Boolean) {
        val updated = cache.firstOrNull { it.id == id }?.copy(enabled = enabled) ?: return
        update(updated)
    }

    /** Phrases with SCHEDULE event whose time matches hour:minute. */
    fun scheduledFor(hour: Int, minute: Int): List<CustomPhrase> =
        forEvent(PhraseEvent.SCHEDULE).filter { it.scheduleHour == hour && it.scheduleMin == minute }

    /** All enabled SLEEP phrases (bedtime). */
    fun sleepPhrases(): List<CustomPhrase> = forEvent(PhraseEvent.SLEEP)
}
