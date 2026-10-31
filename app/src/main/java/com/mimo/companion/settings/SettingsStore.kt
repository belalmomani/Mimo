package com.mimo.companion.settings

import android.content.Context
import com.mimo.companion.domain.dialogue.CompanionSettings
import com.mimo.companion.domain.dialogue.Dialect
import com.mimo.companion.domain.dialogue.DialogueSettings
import com.mimo.companion.domain.dialogue.InitiativeMode
import com.mimo.companion.domain.dialogue.Lang
import com.mimo.companion.domain.dialogue.VoiceSource
import com.mimo.companion.domain.dialogue.PhraseCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Local-only settings (SharedPreferences). Nothing here is ever sent anywhere. */
class SettingsStore(context: Context) {
    private val p = context.getSharedPreferences("companion_settings", Context.MODE_PRIVATE)
    private val _flow = MutableStateFlow(load())
    val flow: StateFlow<CompanionSettings> get() = _flow
    val value: CompanionSettings get() = _flow.value

    fun update(change: (CompanionSettings) -> CompanionSettings) {
        val next = change(_flow.value)
        save(next); _flow.value = next
    }

    private fun load(): CompanionSettings {
        val d = DialogueSettings()
        return try {
            CompanionSettings(
                voiceEnabled = p.getBoolean("voice", true),
                showText = p.getBoolean("showText", true),
                speechRate = p.getFloat("rate", 1f).coerceIn(0.5f, 1.5f),
                voiceSource = enumOr(p.getString("voiceSource", null), VoiceSource.TTS_ONLY),
                dialogue = DialogueSettings(
                    mode = enumOr(p.getString("mode", null), InitiativeMode.QUIET),
                    allowedProactive = p.getStringSet("allowed", emptySet())!!.mapNotNull { n -> PhraseCategory.values().firstOrNull { it.name == n } }.toSet(),
                    dialect = enumOr(p.getString("dialect", null), Dialect.NEUTRAL),
                    lang = enumOr(p.getString("lang", null), Lang.AR),
                    quietStartMin = p.getInt("qs", d.quietStartMin).coerceIn(0, 1439),
                    quietEndMin = p.getInt("qe", d.quietEndMin).coerceIn(0, 1439),
                    maxProactivePerHour = p.getInt("maxHour", d.maxProactivePerHour).coerceAtLeast(0),
                    minSecondsBetweenProactive = p.getInt("minGap", d.minSecondsBetweenProactive).coerceAtLeast(0),
                ),
            )
        } catch (e: Exception) { CompanionSettings() }  // corrupted prefs -> safe defaults
    }

    private fun save(s: CompanionSettings) {
        p.edit()
            .putBoolean("voice", s.voiceEnabled).putBoolean("showText", s.showText).putFloat("rate", s.speechRate).putString("voiceSource", s.voiceSource.name)
            .putString("mode", s.dialogue.mode.name).putStringSet("allowed", s.dialogue.allowedProactive.map { it.name }.toSet())
            .putString("dialect", s.dialogue.dialect.name).putString("lang", s.dialogue.lang.name)
            .putInt("qs", s.dialogue.quietStartMin).putInt("qe", s.dialogue.quietEndMin)
            .putInt("maxHour", s.dialogue.maxProactivePerHour).putInt("minGap", s.dialogue.minSecondsBetweenProactive)
            .apply()
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}
