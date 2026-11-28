package com.mimo.companion.domain.custom

class CustomPhraseRepository(private val store: CustomPhraseStore) {
    private var cache: MutableList<CustomPhrase> = store.getAll().toMutableList()

    fun allEnabled() = cache.filter { it.enabled }
    fun all() = cache.toList()

    fun add(phrase: CustomPhrase) { cache.add(phrase); store.save(cache) }
    fun remove(id: String) { cache.removeAll { it.id == id }; store.save(cache) }
    fun toggle(id: String) {
        cache = cache.map { if (it.id == id) it.copy(enabled = !it.enabled) else it }.toMutableList()
        store.save(cache)
    }

    fun pickForTap(): CustomPhrase? = allEnabled().filter { it.event == PhraseEvent.TAP }.randomOrNull()
    fun sleepPhrases() = allEnabled().filter { it.event == PhraseEvent.SLEEP }
}
