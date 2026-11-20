package com.mimo.companion.verse

import android.content.Context
import com.mimo.companion.domain.verse.DailyVerseStorage

/** SharedPreferences-backed implementation of [DailyVerseStorage]. */
class DailyVerseStore(context: Context) : DailyVerseStorage {
    private val p = context.getSharedPreferences("daily_verse", Context.MODE_PRIVATE)

    override fun getSaidDate(): String? = p.getString("said_date", null)
    override fun getNextIndex(): Int    = p.getInt("next_index", 0)
    override fun save(saidDate: String, nextIndex: Int) {
        p.edit().putString("said_date", saidDate).putInt("next_index", nextIndex).apply()
    }
}
