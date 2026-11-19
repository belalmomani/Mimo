package com.mimo.companion

import com.mimo.companion.domain.weather.WeatherCondition
import com.mimo.companion.domain.weather.WeatherData
import org.junit.Assert.*
import org.junit.Test

class WeatherModelsTest {

    // ── WeatherCondition.from() ───────────────────────────────────────────────

    @Test fun hotOverridesAll() {
        assertEquals(WeatherCondition.HOT, WeatherCondition.from(800, 36.0, 0.0))
    }

    @Test fun coldOverridesAll() {
        assertEquals(WeatherCondition.COLD, WeatherCondition.from(800, 4.0, 0.0))
    }

    @Test fun thunderstorm() {
        assertEquals(WeatherCondition.STORMY, WeatherCondition.from(211, 20.0, 10.0))
    }

    @Test fun drizzle_isRainy() {
        assertEquals(WeatherCondition.RAINY, WeatherCondition.from(301, 18.0, 5.0))
    }

    @Test fun rain() {
        assertEquals(WeatherCondition.RAINY, WeatherCondition.from(502, 15.0, 10.0))
    }

    @Test fun snow() {
        assertEquals(WeatherCondition.SNOWY, WeatherCondition.from(601, 6.0, 5.0))
    }

    @Test fun fog() {
        assertEquals(WeatherCondition.FOGGY, WeatherCondition.from(741, 12.0, 3.0))
    }

    @Test fun clearSky_notWindy_isSunny() {
        assertEquals(WeatherCondition.SUNNY, WeatherCondition.from(800, 25.0, 10.0))
    }

    @Test fun clearSky_windy_isWindy() {
        assertEquals(WeatherCondition.WINDY, WeatherCondition.from(800, 25.0, 45.0))
    }

    @Test fun fewClouds_notWindy_isCloudy() {
        assertEquals(WeatherCondition.CLOUDY, WeatherCondition.from(801, 20.0, 15.0))
    }

    @Test fun overcast_windy_isWindy() {
        assertEquals(WeatherCondition.WINDY, WeatherCondition.from(804, 22.0, 42.0))
    }

    // ── WeatherData.isFresh() ─────────────────────────────────────────────────

    @Test fun freshData_isRecent() {
        val nowSec = System.currentTimeMillis() / 1_000L
        val d = WeatherData(WeatherCondition.SUNNY, 25.0, fetchedAtSec = nowSec - 100)
        assertTrue(d.isFresh(nowSec))
    }

    @Test fun staleData_isNotFresh() {
        val nowSec = System.currentTimeMillis() / 1_000L
        val d = WeatherData(WeatherCondition.SUNNY, 25.0, fetchedAtSec = nowSec - 31 * 60)
        assertFalse(d.isFresh(nowSec))
    }

    @Test fun zeroFetchedAt_isNotFresh() {
        val d = WeatherData(WeatherCondition.SUNNY, 25.0, fetchedAtSec = 0L)
        assertFalse(d.isFresh())
    }
}
