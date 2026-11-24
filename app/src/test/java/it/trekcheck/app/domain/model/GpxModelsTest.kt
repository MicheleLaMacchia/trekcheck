package it.trekcheck.app.domain.model

import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxModelsTest {
    private val sampleGeometry = NormalizedRouteGeometry(
        segments = listOf(
            GpxSegment(listOf(GpxTrackPoint(45.0, 11.0, 1000.0))),
            GpxSegment(listOf(GpxTrackPoint(45.1, 11.1, 1200.0))),
        ),
    )
    private val sampleBbox = GpxBoundingBox(
        minLatitude = 45.0,
        maxLatitude = 45.1,
        minLongitude = 11.0,
        maxLongitude = 11.1,
    )

    @Test
    fun allowsElevationExtremaWithNullGainLossForSinglePointSegments() {
        val route = ParsedGpxRoute(
            routeName = "Cresta",
            sourceKind = GpxSourceKind.TRACK,
            geometry = sampleGeometry,
            distanceMeters = 0.0,
            elevationGainMeters = null,
            elevationLossMeters = null,
            minElevationMeters = 1000.0,
            maxElevationMeters = 1200.0,
            boundingBox = sampleBbox,
            hasElevation = true,
            hasTimestamps = false,
        )

        assertTrue(route.hasElevation)
        assertNull(route.elevationGainMeters)
        assertNull(route.elevationLossMeters)
    }

    @Test
    fun enforcesConsistencyBetweenHasElevationExtremaAndGainLoss() {
        assertThrows(IllegalArgumentException::class.java) {
            ParsedGpxRoute(
                routeName = "Inconsistent",
                sourceKind = GpxSourceKind.TRACK,
                geometry = sampleGeometry,
                distanceMeters = 100.0,
                elevationGainMeters = null,
                elevationLossMeters = null,
                minElevationMeters = 1000.0,
                maxElevationMeters = 1200.0,
                boundingBox = sampleBbox,
                hasElevation = false,
                hasTimestamps = false,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            ParsedGpxRoute(
                routeName = "Missing extrema",
                sourceKind = GpxSourceKind.TRACK,
                geometry = sampleGeometry,
                distanceMeters = 100.0,
                elevationGainMeters = null,
                elevationLossMeters = null,
                minElevationMeters = null,
                maxElevationMeters = null,
                boundingBox = sampleBbox,
                hasElevation = true,
                hasTimestamps = false,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            ParsedGpxRoute(
                routeName = "Gain without loss",
                sourceKind = GpxSourceKind.TRACK,
                geometry = sampleGeometry,
                distanceMeters = 100.0,
                elevationGainMeters = 50.0,
                elevationLossMeters = null,
                minElevationMeters = 1000.0,
                maxElevationMeters = 1200.0,
                boundingBox = sampleBbox,
                hasElevation = true,
                hasTimestamps = false,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            ParsedGpxRoute(
                routeName = "Loss without gain",
                sourceKind = GpxSourceKind.TRACK,
                geometry = sampleGeometry,
                distanceMeters = 100.0,
                elevationGainMeters = null,
                elevationLossMeters = 30.0,
                minElevationMeters = 1000.0,
                maxElevationMeters = 1200.0,
                boundingBox = sampleBbox,
                hasElevation = true,
                hasTimestamps = false,
            )
        }
    }
}
