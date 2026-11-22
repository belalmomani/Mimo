package com.mimo.companion.mood

import android.content.Context
import com.mimo.companion.domain.mood.MoodEntry
import com.mimo.companion.domain.mood.MoodLevel
import com.mimo.companion.domain.mood.MoodStorage

/**
 * SharedPreferences-backed mood storage.
 * Format per key (prefix "mood_<id>"): id|day|moodName|note|recordedAt
 */
class MoodStore(context: Context) : MoodStorage {

    private val prefs = context.getSharedPreferences("mimo_mood", Context.MODE_PRIVATE)

    override fun loadAll(): List<MoodEntry> =
        prefs.all.keys
            .filter { it.startsWith("mood_") }
            .mapNotNull { key -> prefs.getString(key, null)?.let { deserialize(it) } }

    override fun save(entry: MoodEntry) {
        prefs.edit().putString("mood_${entry.id}", serialize(entry)).apply()
    }

    override fun delete(id: String) {
        prefs.edit().remove("mood_$id").apply()
    }

    // ── Serialization ────────────────────────────────────────────────────────

    private fun serialize(e: MoodEntry): String =
        listOf(e.id, e.day, e.mood.name, escape(e.note), e.recordedAt.toString())
            .joinToString("|")

    private fun deserialize(raw: String): MoodEntry? = runCatching {
        val p = raw.split("|")
        MoodEntry(
            id = p[0],
            day = p[1],
            mood = MoodLevel.valueOf(p[2]),
            note = unescape(p[3]),
            recordedAt = p[4].toLong(),
        )
    }.getOrNull()

    private fun escape(s: String)   = s.replace("\\", "\\\\").replace("|", "\\|")
    private fun unescape(s: String) = s.replace("\\|", "|").replace("\\\\", "\\")
}
