package com.mimo.companion.domain.custom

import android.content.Context
import android.content.SharedPreferences

class CustomPhraseStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("mimo_custom_phrases", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ALL = "phrases"
        private const val SEP = "|"
        private const val REC = "§"
    }

    fun getAll(): List<CustomPhrase> {
        val raw = prefs.getString(KEY_ALL, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(REC).mapNotNull { rec ->
            val p = rec.split(SEP)
            if (p.size < 7) null
            else try {
                CustomPhrase(
                    id = p[0], text = p[1],
                    event = PhraseEvent.valueOf(p[2]),
                    enabled = p[3] == "1",
                    scheduledHour = p[4].toInt(),
                    scheduledMinute = p[5].toInt(),
                    clipId = p[6].takeIf { it.isNotBlank() }
                )
            } catch (e: Exception) { null }
        }
    }

    fun save(phrases: List<CustomPhrase>) {
        val raw = phrases.joinToString(REC) { p ->
            "${p.id}$SEP${p.text}$SEP${p.event.name}$SEP${if (p.enabled) "1" else "0"}$SEP${p.scheduledHour}$SEP${p.scheduledMinute}$SEP${p.clipId ?: ""}"
        }
        prefs.edit().putString(KEY_ALL, raw).apply()
    }
}
