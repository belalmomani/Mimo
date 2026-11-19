package com.mimo.companion.domain.weather

/**
 * Parses the JSON response from OpenWeatherMap's /data/2.5/weather endpoint.
 *
 * Uses only the Kotlin stdlib — no org.json, no Gson, no Moshi.
 * This keeps the class in the pure-Kotlin domain layer and testable on the JVM
 * without Android stubs.
 *
 * We extract values with simple regex rather than a full JSON parser:
 * the OWM response is well-structured and we only need a handful of fields.
 *
 * Pure Kotlin — no Android imports.
 */
object OWMParser {

    // ── Patterns for the fields we need ──────────────────────────────────────
    // "weather":[{"id":800,...}]
    private val RE_OWM_ID  = Regex(""""id"\s*:\s*(\d+)""")
    // "temp":28.4  or  "temp":28
    private val RE_TEMP    = Regex(""""temp"\s*:\s*(-?\d+(?:\.\d+)?)""")
    // "speed":3.2  (wind)
    private val RE_WIND    = Regex(""""speed"\s*:\s*(\d+(?:\.\d+)?)""")
    // "name":"Amman"
    private val RE_CITY    = Regex(""""name"\s*:\s*"([^"]*)"""")
    // "dt":1700000000
    private val RE_DT      = Regex(""""dt"\s*:\s*(\d+)""")

    /**
     * Parses a raw OWM JSON response string into a [WeatherData] instance.
     * Returns null on any parse error or missing required field.
     */
    fun parse(json: String): WeatherData? = runCatching {
        val owmId   = RE_OWM_ID.find(json)?.groupValues?.get(1)?.toIntOrNull()    ?: return null
        val tempC   = RE_TEMP.find(json)?.groupValues?.get(1)?.toDoubleOrNull()   ?: return null
        val windMs  = RE_WIND.find(json)?.groupValues?.get(1)?.toDoubleOrNull()   ?: 0.0
        val windKmh = windMs * 3.6
        val city    = RE_CITY.find(json)?.groupValues?.get(1) ?: ""
        val dt      = RE_DT.find(json)?.groupValues?.get(1)?.toLongOrNull()
            ?: (System.currentTimeMillis() / 1_000L)
        WeatherData(
            condition    = WeatherCondition.from(owmId, tempC, windKmh),
            tempC        = tempC,
            cityName     = city,
            fetchedAtSec = dt,
        )
    }.getOrNull()
}
