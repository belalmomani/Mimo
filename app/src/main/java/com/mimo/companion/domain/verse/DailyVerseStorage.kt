package com.mimo.companion.domain.verse

/** Persistence contract for [DailyVerseRepository]. Android impl uses SharedPreferences. */
interface DailyVerseStorage {
    fun getSaidDate(): String?        // the UTC-day key when the verse was last said
    fun getNextIndex(): Int           // which item in the list is next
    fun save(saidDate: String, nextIndex: Int)
}
