package com.mimo.companion.mood

import android.content.Context
import com.mimo.companion.domain.mood.MoodEntry
import com.mimo.companion.domain.mood.MoodLevel
import java.util.UUID

class MoodStore(context: Context) {
    private val prefs = context.getSharedPreferences("mimo_mood", Context.MODE_PRIVATE)
    companion object { private const val KEY = "entries"; private const val SEP = "|"; private const val REC = "§" }

    fun getAll(): List<MoodEntry> {
        val raw = prefs.getString(KEY, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(REC).mapNotNull { rec ->
            val p = rec.split(SEP)
            if (p.size < 5) null
            else try { MoodEntry(p[0], p[1], MoodLevel.valueOf(p[2]), p[3], p[4].toLong()) } catch (e: Exception) { null }
        }
    }

    fun save(entries: List<MoodEntry>) {
        val raw = entries.joinToString(REC) { "${it.id}$SEP${it.day}$SEP${it.mood.name}$SEP${it.note}$SEP${it.recordedAt}" }
        prefs.edit().putString(KEY, raw).apply()
    }
}
