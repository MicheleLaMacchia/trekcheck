package it.trekcheck.app.domain.geo

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoDistanceTest {
    @Test
    fun identicalPointsAreZeroAndOneDegreeLatitudeIsAbout111Km() {
        assertEquals(0.0, GeoDistance.haversineDistanceMeters(46.0, 12.0, 46.0, 12.0), 0.0)
        assertEquals(111_195.0, GeoDistance.haversineDistanceMeters(0.0, 0.0, 1.0, 0.0), 10.0)
    }
}
