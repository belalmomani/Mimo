package com.mimo.companion.domain.weather

/**
 * General weather condition bucket used by ContextAdvisor and PhraseBank.
 * We deliberately keep this coarse so the companion can say something natural
 * without needing an exhaustive match for every OWM code.
 *
 * Pure Kotlin — no Android imports.
 */
enum class WeatherCondition {
    SUNNY,       // clear sky, few clouds
    CLOUDY,      // scattered / broken / overcast
    RAINY,       // light/moderate/heavy rain, drizzle
    STORMY,      // thunderstorm
    SNOWY,       // snow / sleet
    FOGGY,       // mist, fog, haze
    HOT,         // temp > 35 °C (overrides sky condition when extreme)
    COLD,        // temp < 5 °C  (overrides sky condition when extreme)
    WINDY;       // wind speed > 40 km/h (can stack with other conditions)

    companion object {
        /**
         * Map an OpenWeatherMap condition-id + temperature (°C) + wind speed (km/h)
         * to the most relevant [WeatherCondition].
         *
         * OWM code groups: 2xx thunderstorm, 3xx drizzle, 5xx rain, 6xx snow,
         * 7xx atmosphere (fog/mist), 800 clear, 80x clouds.
         */
        fun from(owmId: Int, tempC: Double, windKmh: Double): WeatherCondition {
            if (tempC > 35.0) return HOT
            if (tempC < 5.0)  return COLD
            return when (owmId) {
                in 200..299 -> STORMY
                in 300..399 -> RAINY
                in 500..599 -> RAINY
                in 600..699 -> SNOWY
                in 700..799 -> FOGGY
                800         -> if (windKmh > 40) WINDY else SUNNY
                in 801..804 -> if (windKmh > 40) WINDY else CLOUDY
                else        -> SUNNY
            }
        }
    }
}

/**
 * Lightweight snapshot of current weather delivered to [ContextSnapshot].
 * Intentionally minimal — only what the companion needs to pick a phrase.
 */
data class WeatherData(
    val condition: WeatherCondition,
    val tempC: Double,
    /** City name used for display (not for further API calls). */
    val cityName: String = "",
    /** Epoch-second when this data was fetched. */
    val fetchedAtSec: Long = 0L,
) {
    /** Whether this reading is fresh enough to trust (default stale threshold: 30 min). */
    fun isFresh(nowSec: Long = System.currentTimeMillis() / 1_000L, staleAfterSec: Long = 30 * 60L): Boolean =
        fetchedAtSec > 0L && (nowSec - fetchedAtSec) < staleAfterSec
}
