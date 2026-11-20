package com.mimo.companion

import com.mimo.companion.domain.weather.WeatherCondition
import com.mimo.companion.domain.weather.WeatherData
import com.mimo.companion.domain.weather.WeatherRepository
import com.mimo.companion.domain.weather.WeatherStorage
import org.junit.Assert.*
import org.junit.Test

private class FakeWeatherStorage(var stored: WeatherData? = null) : WeatherStorage {
    override fun load() = stored
    override fun save(data: WeatherData) { stored = data }
}

class WeatherRepositoryTest {

    private val nowSec = System.currentTimeMillis() / 1_000L
    private val freshData = WeatherData(WeatherCondition.SUNNY, 25.0, "Amman", nowSec - 60)
    private val staleData = WeatherData(WeatherCondition.CLOUDY, 20.0, "Amman", nowSec - 40 * 60)

    // Synchronous helpers — WeatherRepository.fetchBlocking() used in tests (see domain model)
    private fun repo(storage: FakeWeatherStorage, result: WeatherData?) =
        WeatherRepository(storage) { _, _, _ -> result }

    private fun repoFailing(storage: FakeWeatherStorage) =
        WeatherRepository(storage) { _, _, _ -> throw RuntimeException("network error") }

    @Test fun freshCache_returnsCacheWithoutFetching() {
        var fetchCount = 0
        val storage = FakeWeatherStorage(freshData)
        val r = WeatherRepository(storage) { _, _, _ -> fetchCount++; null }
        val result = r.current(0.0, 0.0, "key")
        assertEquals(freshData, result)
        assertEquals(0, fetchCount)
    }

    @Test fun staleCache_fetchesAndUpdates() {
        val storage = FakeWeatherStorage(staleData)
        val fresh = WeatherData(WeatherCondition.RAINY, 18.0, "Amman", nowSec)
        val result = repo(storage, fresh).current(0.0, 0.0, "key")
        assertEquals(fresh, result)
        assertEquals(fresh, storage.stored)
    }

    @Test fun networkFail_returnsStaleCacheInsteadOfNull() {
        val storage = FakeWeatherStorage(staleData)
        val result = repoFailing(storage).current(0.0, 0.0, "key")
        assertEquals(staleData, result)
    }

    @Test fun noCache_networkFail_returnsNull() {
        val storage = FakeWeatherStorage(null)
        assertNull(repoFailing(storage).current(0.0, 0.0, "key"))
    }

    @Test fun cachedOrNull_returnsFreshCache() {
        assertEquals(freshData, repo(FakeWeatherStorage(freshData), null).cachedOrNull())
    }

    @Test fun cachedOrNull_returnsNull_whenStale() {
        assertNull(repo(FakeWeatherStorage(staleData), null).cachedOrNull())
    }

    @Test fun invalidate_thenFetch_returnsNewData() {
        val newData = WeatherData(WeatherCondition.HOT, 38.0, "Amman", nowSec)
        val storage = FakeWeatherStorage(freshData)
        val r = repo(storage, newData)
        r.invalidate()
        assertEquals(newData, r.current(0.0, 0.0, "key"))
    }
}
