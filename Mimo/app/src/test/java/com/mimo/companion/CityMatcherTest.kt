package com.mimo.companion

import com.mimo.companion.domain.prayer.City
import com.mimo.companion.domain.prayer.CityMatcher
import com.mimo.companion.domain.prayer.Cities
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.abs

class CityMatcherTest {

    // -- haversine distance sanity checks --

    @Test fun samePointIsZeroDistance() {
        assertEquals(0.0, CityMatcher.haversineKm(31.9, 35.9, 31.9, 35.9), 0.001)
    }

    @Test fun knownDistanceAmmanZarqa() {
        // Amman ↔ Zarqa is about 22 km
        val d = CityMatcher.haversineKm(31.9539, 35.9106, 32.0728, 36.0876)
        assertTrue("expected ~22 km, got $d", abs(d - 22.0) < 3.0)
    }

    @Test fun equatorLongitudeWrap() {
        // Points straddling the antimeridian: (0, 179) and (0, -179) — should be ~222 km
        val d = CityMatcher.haversineKm(0.0, 179.0, 0.0, -179.0)
        assertTrue("expected ~222 km, got $d", abs(d - 222.0) < 5.0)
    }

    // -- nearest city selection --

    @Test fun nearAmmanPicksAmman() {
        val city = CityMatcher.nearest(31.96, 35.91)   // 0.6 km from Amman centroid
        assertEquals("amman", city?.id)
    }

    @Test fun nearZarqaPicksZarqa() {
        val city = CityMatcher.nearest(32.07, 36.09)
        assertEquals("zarqa", city?.id)
    }

    @Test fun nearMeccaPicksMecca() {
        val city = CityMatcher.nearest(21.42, 39.83)
        assertEquals("mecca", city?.id)
    }

    @Test fun nearLondonPicksLondon() {
        val city = CityMatcher.nearest(51.51, -0.13)
        assertEquals("london", city?.id)
    }

    @Test fun emptyListReturnsNull() {
        val city = CityMatcher.nearest(0.0, 0.0, emptyList())
        assertNull(city)
    }

    @Test fun singleCandidateAlwaysReturned() {
        val only = City("only", "فقط", "Only", 0.0, 0.0, "UTC")
        val city = CityMatcher.nearest(90.0, 0.0, listOf(only))
        assertEquals("only", city?.id)
    }

    @Test fun allCitiesHaveUniqueIds() {
        val ids = Cities.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
