package com.mimo.companion.custom

import android.content.Context
import com.mimo.companion.domain.custom.CustomPhrase
import com.mimo.companion.domain.custom.CustomPhraseStorage
import com.mimo.companion.domain.custom.PhraseEvent

/**
 * SharedPreferences-backed storage for user-created phrases.
 * Uses a minimal hand-rolled JSON format — no external libraries.
 *
 * Format per key "phrase_<id>":
 * text|clipId|event|scheduleHour|scheduleMin|enabled|createdAt
 * Fields separated by | ; clipId is empty string when null.
 */
class CustomPhraseStore(context: Context) : CustomPhraseStorage {

    private val prefs = context.applicationContext
        .getSharedPreferences("custom_phrases", Context.MODE_PRIVATE)

    override fun loadAll(): List<CustomPhrase> =
        prefs.all.entries
            .filter { it.key.startsWith(PREFIX) }
            .mapNotNull { deserialize(it.value as? String ?: return@mapNotNull null) }
            .sortedBy { it.createdAt }

    override fun save(phrase: CustomPhrase) {
        prefs.edit().putString("$PREFIX${phrase.id}", serialize(phrase)).apply()
    }

    override fun delete(id: String) {
        prefs.edit().remove("$PREFIX$id").apply()
    }

    // ── Serialization ────────────────────────────────────────────────────────

    private fun serialize(p: CustomPhrase): String =
        listOf(
            p.id,
            escape(p.text),
            p.clipId ?: "",
            p.event.name,
            p.scheduleHour.toString(),
            p.scheduleMin.toString(),
            if (p.enabled) "1" else "0",
            p.createdAt.toString(),
        ).joinToString(SEP)

    private fun deserialize(raw: String): CustomPhrase? = runCatching {
        val parts = raw.split(SEP)
        if (parts.size < 8) return null
        CustomPhrase(
            id           = parts[0],
            text         = unescape(parts[1]),
            clipId       = parts[2].ifEmpty { null },
            event        = PhraseEvent.valueOf(parts[3]),
            scheduleHour = parts[4].toIntOrNull() ?: -1,
            scheduleMin  = parts[5].toIntOrNull() ?: -1,
            enabled      = parts[6] == "1",
            createdAt    = parts[7].toLongOrNull() ?: 0L,
        )
    }.getOrNull()

    private fun escape(s: String)   = s.replace("\\", "\\\\").replace(SEP, "\\|")
    private fun unescape(s: String) = s.replace("\\|", SEP).replace("\\\\", "\\")

    companion object {
        private const val PREFIX = "phrase_"
        private const val SEP    = "|"
    }
}
