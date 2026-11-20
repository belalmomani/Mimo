package com.mimo.companion.domain.custom

/** Storage contract — implemented in the data layer with SharedPreferences JSON. Pure domain. */
interface CustomPhraseStorage {
    fun loadAll(): List<CustomPhrase>
    fun save(phrase: CustomPhrase)
    fun delete(id: String)
}
