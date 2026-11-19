package com.mimo.companion.weather

import com.mimo.companion.domain.weather.OWMParser
import com.mimo.companion.domain.weather.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal OpenWeatherMap "current weather" client.
 *
 * Uses only [HttpURLConnection] + [JSONObject] — no Retrofit, no Gson.
 * API endpoint: https://api.openweathermap.org/data/2.5/weather
 *
 * Free tier allows 1 000 calls/day and returns JSON such as:
 * ```json
 * { "weather": [{"id": 800}], "main": {"temp": 28.4}, "wind": {"speed": 3.2},
 *   "name": "Amman", "dt": 1700000000 }
 * ```
 * wind.speed is in m/s — multiply by 3.6 for km/h.
 */
object OWMClient {

    private const val BASE = "https://api.openweathermap.org/data/2.5/weather"
    private const val TIMEOUT_MS = 8_000

    /**
     * Fetches current weather for the given coordinates (blocking, runs on IO dispatcher).
     * Returns null on network error or unexpected response.
     * Never throws — callers can treat null as "no data".
     * Call from a background thread or coroutine; not safe on the main thread.
     */
    fun fetchBlocking(lat: Double, lon: Double, apiKey: String): WeatherData? =
        runCatching {
            val url = URL("$BASE?lat=$lat&lon=$lon&appid=$apiKey&units=metric")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout    = TIMEOUT_MS
            conn.requestMethod  = "GET"
            try {
                if (conn.responseCode != 200) return@runCatching null
                val body = conn.inputStream.bufferedReader().readText()
                OWMParser.parse(body)
            } finally {
                conn.disconnect()
            }
        }.getOrNull()

    /**
     * Suspend variant — wraps [fetchBlocking] on the IO dispatcher.
     * Kept for callers that already have a coroutine context.
     */
    suspend fun fetch(lat: Double, lon: Double, apiKey: String): WeatherData? =
        withContext(Dispatchers.IO) { fetchBlocking(lat, lon, apiKey) }

}
