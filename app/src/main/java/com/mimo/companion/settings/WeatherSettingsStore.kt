package com.mimo.companion.settings

import android.content.Context

/**
 * Persists the user's weather configuration:
 *  - OpenWeatherMap API key (free tier, up to 1 000 calls/day)
 *  - Latitude / longitude for the weather location
 *
 * The companion only fetches weather when [apiKey] is non-blank.
 * Default coordinates are set to Amman, Jordan.
 */
class WeatherSettingsStore(context: Context) {

    private val p = context.getSharedPreferences("weather_settings", Context.MODE_PRIVATE)

    /** OWM free-tier key; empty = weather feature disabled. */
    var apiKey: String
        get() = p.getString(KEY_API, "") ?: ""
        set(v) = p.edit().putString(KEY_API, v.trim()).apply()

    /** Latitude for weather lookup (default: Amman, Jordan). */
    var lat: Double
        get() = p.getFloat(KEY_LAT, 31.95f).toDouble()
        set(v) = p.edit().putFloat(KEY_LAT, v.toFloat()).apply()

    /** Longitude for weather lookup (default: Amman, Jordan). */
    var lon: Double
        get() = p.getFloat(KEY_LON, 35.93f).toDouble()
        set(v) = p.edit().putFloat(KEY_LON, v.toFloat()).apply()

    /** Whether the user has enabled weather commentary. */
    var enabled: Boolean
        get() = p.getBoolean(KEY_ENABLED, false)   // off by default until user sets API key
        set(v) = p.edit().putBoolean(KEY_ENABLED, v).apply()

    private companion object {
        const val KEY_API     = "owm_api_key"
        const val KEY_LAT     = "lat"
        const val KEY_LON     = "lon"
        const val KEY_ENABLED = "enabled"
    }
}
