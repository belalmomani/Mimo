package com.mimo.companion.domain.weather

/**
 * Offline-first weather source.
 *
 * - Returns the cached [WeatherData] immediately if it is still fresh.
 * - Calls [fetcher] only when the cache is stale or empty.
 * - If the fetcher returns null (network error), falls back to stale cache.
 * - If neither is available, returns null.
 *
 * [fetcher] is a plain (non-suspend) function so this class stays in pure
 * Kotlin domain code with no coroutines dependency.  The Android layer
 * (Graph) wraps the actual network call with a blocking dispatcher call.
 *
 * Usage:
 * ```
 * val repo = WeatherRepository(storage) { lat, lon, apiKey ->
 *     OWMClient.fetchBlocking(lat, lon, apiKey)
 * }
 * val data: WeatherData? = repo.current(lat, lon, apiKey)
 * ```
 */
class WeatherRepository(
    private val storage: WeatherStorage,
    private val staleAfterSec: Long = 30 * 60L,
    private val fetcher: (lat: Double, lon: Double, apiKey: String) -> WeatherData?,
) {
    private var cache: WeatherData? = storage.load()

    /**
     * Returns current weather, refreshing from the network when stale.
     * Returns null only when no data is available at all (first run + no network).
     * Never throws — network errors are swallowed and the stale cache is returned.
     */
    fun current(lat: Double, lon: Double, apiKey: String): WeatherData? {
        val nowSec = System.currentTimeMillis() / 1_000L
        val cached = cache
        if (cached != null && cached.isFresh(nowSec, staleAfterSec)) return cached

        val fresh = try { fetcher(lat, lon, apiKey) } catch (_: Exception) { null }

        return if (fresh != null) {
            storage.save(fresh)
            cache = fresh
            fresh
        } else {
            cached // stale but better than nothing
        }
    }

    /** Returns the cached data only (no network call). Used for synchronous snapshot building. */
    fun cachedOrNull(): WeatherData? = cache?.takeIf { it.isFresh() }

    /** Force-evict the cache (useful after a city change in settings). */
    fun invalidate() { cache = null }
}
