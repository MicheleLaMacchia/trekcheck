package it.trekcheck.app.domain.geo

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoDistance {
    const val EARTH_RADIUS_METERS: Double = 6_371_000.0

    fun haversineDistanceMeters(
        lat1Deg: Double,
        lon1Deg: Double,
        lat2Deg: Double,
        lon2Deg: Double,
    ): Double {
        if (lat1Deg == lat2Deg && lon1Deg == lon2Deg) return 0.0
        val lat1 = Math.toRadians(lat1Deg)
        val lat2 = Math.toRadians(lat2Deg)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(lon2Deg - lon1Deg)
        val sinHalfLat = sin(dLat / 2.0)
        val sinHalfLon = sin(dLon / 2.0)
        val a = sinHalfLat * sinHalfLat + cos(lat1) * cos(lat2) * sinHalfLon * sinHalfLon
        val c = 2.0 * atan2(sqrt(a), sqrt((1.0 - a).coerceAtLeast(0.0)))
        return EARTH_RADIUS_METERS * c
    }
}
