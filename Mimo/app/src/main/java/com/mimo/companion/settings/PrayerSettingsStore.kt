package com.mimo.companion.settings

import android.content.Context
import com.mimo.companion.domain.prayer.AsrMethod
import com.mimo.companion.domain.prayer.CalcMethod
import com.mimo.companion.domain.prayer.Cities
import com.mimo.companion.domain.prayer.Prayer
import com.mimo.companion.domain.prayer.PrayerSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Prayer preferences, local only (never sent anywhere, never used for ads). */
class PrayerSettingsStore(context: Context) {
    private val p = context.getSharedPreferences("prayer_settings", Context.MODE_PRIVATE)
    private val _flow = MutableStateFlow(load())
    val flow: StateFlow<PrayerSettings> get() = _flow
    val value: PrayerSettings get() = _flow.value

    fun update(change: (PrayerSettings) -> PrayerSettings) {
        val next = try { change(_flow.value) } catch (e: IllegalArgumentException) { return } // ignore out-of-range input
        save(next); _flow.value = next
    }

    private fun load(): PrayerSettings = try {
        val d = PrayerSettings()
        PrayerSettings(
            enabled = p.getBoolean("enabled", false),
            cityId = Cities.byId(p.getString("city", d.cityId) ?: d.cityId).id,
            method = CalcMethod.values().firstOrNull { it.name == p.getString("method", null) } ?: d.method,
            asr = AsrMethod.values().firstOrNull { it.name == p.getString("asr", null) } ?: d.asr,
            adjustMin = (p.getString("adjust", "") ?: "").split(",").mapNotNull { e ->
                val kv = e.split(":"); val pr = Prayer.values().firstOrNull { it.name == kv.getOrNull(0) }; val v = kv.getOrNull(1)?.toIntOrNull()
                if (pr != null && v != null) pr to v.coerceIn(-30, 30) else null
            }.toMap(),
            remindFor = (p.getStringSet("remind", null) ?: d.remindFor.map { it.name }.toSet()).mapNotNull { n -> Prayer.values().firstOrNull { it.name == n && it.isPrayer } }.toSet(),
            minutesBefore = p.getInt("before", d.minutesBefore).coerceIn(0, 60),
            reminderCount = p.getInt("count", d.reminderCount).coerceIn(1, 3),
            gapMinutes = p.getInt("gap", d.gapMinutes).coerceIn(1, 60),
            customMessage = p.getString("message", null)?.takeIf { it.isNotBlank() },
        )
    } catch (e: Exception) { PrayerSettings() }

    private fun save(s: PrayerSettings) {
        p.edit().putBoolean("enabled", s.enabled).putString("city", s.cityId).putString("method", s.method.name).putString("asr", s.asr.name)
            .putString("adjust", s.adjustMin.entries.joinToString(",") { "${it.key.name}:${it.value}" })
            .putStringSet("remind", s.remindFor.map { it.name }.toSet())
            .putInt("before", s.minutesBefore).putInt("count", s.reminderCount).putInt("gap", s.gapMinutes)
            .putString("message", s.customMessage).apply()
    }
}
