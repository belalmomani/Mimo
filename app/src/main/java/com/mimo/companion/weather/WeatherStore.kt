package com.mimo.companion.weather

import android.content.Context
import com.mimo.companion.domain.weather.WeatherCondition
import com.mimo.companion.domain.weather.WeatherData
import com.mimo.companion.domain.weather.WeatherStorage

/**
 * SharedPreferences implementation of [WeatherStorage].
 * Serialises [WeatherData] as a flat set of keys — no JSON library needed.
 */
class WeatherStore(context: Context) : WeatherStorage {

    private val p = context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)

    override fun load(): WeatherData? {
        val condition = p.getString(KEY_CONDITION, null)?.let {
            runCatching { WeatherCondition.valueOf(it) }.getOrNull()
        } ?: return null
        val temp = p.getFloat(KEY_TEMP, Float.NaN).toDouble()
        if (temp.isNaN()) return null
        return WeatherData(
            condition   = condition,
            tempC       = temp,
            cityName    = p.getString(KEY_CITY, "") ?: "",
            fetchedAtSec = p.getLong(KEY_FETCHED, 0L),
        )
    }

    override fun save(data: WeatherData) {
        p.edit()
            .putString(KEY_CONDITION, data.condition.name)
            .putFloat(KEY_TEMP, data.tempC.toFloat())
            .putString(KEY_CITY, data.cityName)
            .putLong(KEY_FETCHED, data.fetchedAtSec)
            .apply()
    }

    private companion object {
        const val KEY_CONDITION = "condition"
        const val KEY_TEMP      = "temp_c"
        const val KEY_CITY      = "city"
        const val KEY_FETCHED   = "fetched_at"
    }
}
