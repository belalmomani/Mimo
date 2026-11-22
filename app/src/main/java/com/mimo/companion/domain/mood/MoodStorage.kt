package com.mimo.companion.domain.mood

interface MoodStorage {
    fun loadAll(): List<MoodEntry>
    fun save(entry: MoodEntry)
    fun delete(id: String)
}
