package com.mimo.companion.domain.prayer

import kotlin.math.*

/**
 * Picks the closest city in [Cities.ALL] from a raw GPS fix.
 *
 * Uses the haversine formula (great-circle distance) so the match is correct
 * near the equator and at any longitude wrap.  Pure Kotlin — no Android imports,
 * fully testable offline.
 */
object CityMatcher {

    /**
     * Returns the [City] whose (lat, lng) is closest to the given coordinates,
     * or `null` when the list is empty.
     */
    fun nearest(lat: Double, lng: Double, candidates: List<City> = Cities.ALL): City? =
        candidates.minByOrNull { haversineKm(lat, lng, it.lat, it.lng) }

    /** Great-circle distance in kilometres between two WGS-84 coordinates. */
    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0          // Earth mean radius
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}
