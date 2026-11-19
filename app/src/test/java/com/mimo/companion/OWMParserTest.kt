package com.mimo.companion

import com.mimo.companion.domain.weather.OWMParser
import com.mimo.companion.domain.weather.WeatherCondition
import org.junit.Assert.*
import org.junit.Test

class OWMParserTest {

    private fun json(id: Int, temp: Double, windMs: Double = 0.0, city: String = "Amman", dt: Long = 1700000000L) = """
        {
          "weather": [{"id": $id, "main": "X", "description": "x"}],
          "main":    {"temp": $temp, "humidity": 60},
          "wind":    {"speed": $windMs},
          "name":    "$city",
          "dt":      $dt
        }
    """.trimIndent()

    @Test fun parse_clearSky_sunny() {
        val d = OWMParser.parse(json(800, 25.0))
        assertNotNull(d)
        assertEquals(WeatherCondition.SUNNY, d!!.condition)
        assertEquals(25.0, d.tempC, 0.01)
        assertEquals("Amman", d.cityName)
    }

    @Test fun parse_hot_overridesSunny() {
        assertEquals(WeatherCondition.HOT, OWMParser.parse(json(800, 37.0))!!.condition)
    }

    @Test fun parse_cold_overridesCloudy() {
        assertEquals(WeatherCondition.COLD, OWMParser.parse(json(803, 3.0))!!.condition)
    }

    @Test fun parse_rain() {
        assertEquals(WeatherCondition.RAINY, OWMParser.parse(json(501, 18.0))!!.condition)
    }

    @Test fun parse_thunderstorm() {
        assertEquals(WeatherCondition.STORMY, OWMParser.parse(json(201, 22.0))!!.condition)
    }

    @Test fun parse_snow() {
        assertEquals(WeatherCondition.SNOWY, OWMParser.parse(json(601, 7.0))!!.condition)
    }

    @Test fun parse_fog() {
        assertEquals(WeatherCondition.FOGGY, OWMParser.parse(json(741, 15.0))!!.condition)
    }

    @Test fun parse_windy_clearSky() {
        // 12 m/s = 43.2 km/h → WINDY
        assertEquals(WeatherCondition.WINDY, OWMParser.parse(json(800, 20.0, windMs = 12.0))!!.condition)
    }

    @Test fun parse_missingWind_doesNotCrash() {
        val noWind = """{"weather":[{"id":800}],"main":{"temp":22.0},"name":"Test","dt":1700000000}"""
        assertNotNull(OWMParser.parse(noWind))
    }

    @Test fun parse_invalidJson_returnsNull() {
        assertNull(OWMParser.parse("not json"))
    }

    @Test fun parse_emptyObject_returnsNull() {
        assertNull(OWMParser.parse("{}"))
    }

    @Test fun parse_dtPreserved() {
        val d = OWMParser.parse(json(800, 25.0, dt = 1_700_123_456L))
        assertEquals(1_700_123_456L, d!!.fetchedAtSec)
    }
}
