package it.trekcheck.app.domain.geo

import it.trekcheck.app.domain.model.GpxSegment
import it.trekcheck.app.domain.model.GpxTrackPoint
import it.trekcheck.app.domain.model.NormalizedRouteGeometry
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteGeometrySimplifierTest {
    private fun segment(vararg pts: Triple<Double, Double, Double?>) =
        GpxSegment(pts.map { GpxTrackPoint(it.first, it.second, it.third) })

    @Test
    fun analyzeUsesFullGeometryForDistanceExtremesAndHighestPoint() {
        val geometry = NormalizedRouteGeometry(
            listOf(
                segment(Triple(46.0, 12.0, 1000.0), Triple(46.001, 12.0, 1300.0), Triple(46.002, 12.0, 1100.0)),
            ),
        )
        val analysis = RouteGeometrySimplifier.analyze(geometry)
        assertEquals(1300.0, analysis.maxElevationMeters!!, 0.0)
        assertEquals(1000.0, analysis.minElevationMeters!!, 0.0)
        val highest = analysis.highest!!
        assertEquals(46.001, highest.latitude, 0.0)
        assertEquals(analysis.distanceMeters / 2.0, highest.distanceFromStartMeters, 1.0)
        assertEquals(0.0, analysis.start.distanceFromStartMeters, 0.0)
        assertEquals(analysis.distanceMeters, analysis.end.distanceFromStartMeters, 0.0)
        assertEquals(222.4, analysis.distanceMeters, 1.0)
        assertEquals(300.0, analysis.elevationGainMeters!!, 0.0)
        assertEquals(200.0, analysis.elevationLossMeters!!, 0.0)
    }

    @Test
    fun separateSegmentsWithOneQuotedPointEachYieldNullGainAndNullProfile() {
        val geometry = NormalizedRouteGeometry(
            listOf(
                segment(Triple(46.0, 12.0, 1000.0)),
                segment(Triple(46.01, 12.01, 1300.0)),
            ),
        )
        val analysis = RouteGeometrySimplifier.analyze(geometry)
        assertEquals(1300.0, analysis.maxElevationMeters!!, 0.0)
        assertNull(analysis.elevationGainMeters)
        assertNull(analysis.elevationLossMeters)
        assertNull(RouteGeometrySimplifier.sampleProfile(geometry))
        val preview = RouteGeometrySimplifier.simplify(geometry, analysis)
        assertEquals(1, preview.segments[0].size)
        assertEquals(1, preview.segments[1].size)
    }

    @Test
    fun sampleProfileSplitsRunsAcrossMissingElevationGapsAndSegmentBreaks() {
        val geometry = NormalizedRouteGeometry(
            listOf(
                segment(
                    Triple(46.0, 12.0, 1000.0),
                    Triple(46.001, 12.0, 1050.0),
                    Triple(46.002, 12.0, null),
                    Triple(46.003, 12.0, 1200.0),
                    Triple(46.004, 12.0, 1250.0),
                ),
                segment(
                    Triple(46.01, 12.01, 1400.0),
                    Triple(46.011, 12.01, 1450.0),
                ),
            ),
        )
        val profile = RouteGeometrySimplifier.sampleProfile(geometry)!!
        assertEquals(3, profile.runs.size)
        assertEquals(listOf(2, 2, 2), profile.runs.map { it.size })
        assertEquals(6, profile.samples.size)
    }

    @Test
    fun missingElevationYieldsNullsNotZeros() {
        val geometry = NormalizedRouteGeometry(
            listOf(segment(Triple(46.0, 12.0, null), Triple(46.001, 12.0, null))),
        )
        val analysis = RouteGeometrySimplifier.analyze(geometry)
        assertNull(analysis.highest)
        assertNull(analysis.minElevationMeters)
        assertNull(analysis.maxElevationMeters)
        assertNull(analysis.elevationGainMeters)
        assertNull(analysis.elevationLossMeters)
        assertNull(analysis.start.elevationMeters)
        assertNull(RouteGeometrySimplifier.sampleProfile(geometry))
    }

    @Test
    fun singleElevationHasNoProfile() {
        val geometry = NormalizedRouteGeometry(
            listOf(segment(Triple(46.0, 12.0, 900.0), Triple(46.001, 12.0, null))),
        )
        assertNull(RouteGeometrySimplifier.sampleProfile(geometry))
        assertEquals(900.0, RouteGeometrySimplifier.analyze(geometry).highest!!.elevationMeters!!, 0.0)
    }

    @Test
    fun simplifiesSegmentsSeparatelyWithoutSpuriousBridgeBetweenSegments() {
        val first = GpxSegment(List(50) { GpxTrackPoint(46.0 + it * 0.0001, 12.0, 1000.0 + it) })
        val second = GpxSegment(List(50) { GpxTrackPoint(46.5 + it * 0.0001, 12.5, 2000.0 + it) })
        val geometry = NormalizedRouteGeometry(listOf(first, second))
        val analysis = RouteGeometrySimplifier.analyze(geometry)
        val preview = RouteGeometrySimplifier.simplify(geometry, analysis)
        assertEquals(2, preview.segments.size)
        assertEquals(2, preview.segments[0].size)
        assertEquals(2, preview.segments[1].size)
        assertTrue(preview.segments[0].all { it.y < preview.segments[1].first().y })
        // distance does not include the gap between disjoint segments
        assertEquals(2 * 49 * 0.0001 * 111_195.0, analysis.distanceMeters, 20.0)
    }

    @Test
    fun highestPointIsAnchoredInSimplifiedGeometry() {
        val points = List(101) { i ->
            val ele = if (i == 50) 3000.0 else 1000.0
            GpxTrackPoint(46.0 + i * 0.0001, 12.0, ele)
        }
        val geometry = NormalizedRouteGeometry(listOf(GpxSegment(points)))
        val analysis = RouteGeometrySimplifier.analyze(geometry)
        val preview = RouteGeometrySimplifier.simplify(geometry, analysis)
        val highest = preview.highest!!
        assertTrue(preview.segments[0].any { it == highest })
        assertEquals(3, preview.segments[0].size)
    }

    @Test
    fun degenerateGeometriesProduceFiniteProjection() {
        val coincident = NormalizedRouteGeometry(listOf(segment(Triple(46.0, 12.0, 100.0), Triple(46.0, 12.0, 100.0))))
        val flatLine = NormalizedRouteGeometry(
            listOf(segment(Triple(46.0, 12.0, null), Triple(46.0, 12.01, null), Triple(46.0, 12.02, null))),
        )
        val vertical = NormalizedRouteGeometry(
            listOf(segment(Triple(46.0, 12.0, 5.0), Triple(46.01, 12.0, 5.0), Triple(46.02, 12.0, 5.0))),
        )
        for (geometry in listOf(coincident, flatLine, vertical)) {
            val analysis = RouteGeometrySimplifier.analyze(geometry)
            val preview = RouteGeometrySimplifier.simplify(geometry, analysis)
            assertTrue(preview.segments.flatten().all { it.x.isFinite() && it.y.isFinite() })
            assertTrue(preview.widthMeters >= 0.0 && preview.heightMeters >= 0.0)
            assertFalse(preview.start.x.isNaN())
        }
        val flat = RouteGeometrySimplifier.sampleProfile(coincident)!!
        assertEquals(0.0, flat.maxElevationMeters - flat.minElevationMeters, 0.0)
    }

    @Test
    fun profileSamplingIsBoundedAndKeepsExtremes() {
        val points = List(10_000) { i ->
            GpxTrackPoint(46.0 + i * 0.00001, 12.0, 1000.0 + 500.0 * sin(i / 300.0))
        }
        val geometry = NormalizedRouteGeometry(listOf(GpxSegment(points)))
        val profile = RouteGeometrySimplifier.sampleProfile(geometry, maxSamples = 100)!!
        assertTrue(profile.samples.size <= 103)
        assertEquals(profile.maxElevationMeters, profile.samples.maxOf { it.elevationMeters }, 0.0)
        assertEquals(profile.minElevationMeters, profile.samples.minOf { it.elevationMeters }, 0.0)
        assertEquals(profile.totalDistanceMeters, profile.samples.last().distanceMeters, 1e-6)
    }

    @Test
    fun analysisOf50kPointsStaysUnderOneSecond() {
        val points = List(50_000) { i ->
            GpxTrackPoint(
                latitude = 46.0 + i * 0.000005 + 0.0005 * sin(i / 50.0),
                longitude = 12.0 + i * 0.000004 + 0.0005 * sin(i / 70.0),
                elevationMeters = 1000.0 + 300.0 * sin(i / 900.0),
            )
        }
        val geometry = NormalizedRouteGeometry(listOf(GpxSegment(points)))
        RouteGeometrySimplifier.simplify(geometry, RouteGeometrySimplifier.analyze(geometry))

        val started = System.nanoTime()
        val analysis = RouteGeometrySimplifier.analyze(geometry)
        val preview = RouteGeometrySimplifier.simplify(geometry, analysis)
        val profile = RouteGeometrySimplifier.sampleProfile(geometry)
        val elapsedMillis = (System.nanoTime() - started) / 1_000_000
        println("NFR-5 analysis of 50k points: $elapsedMillis ms")
        assertTrue("analysis took ${elapsedMillis}ms", elapsedMillis < 1_000)
        assertTrue(preview.segments[0].size < points.size)
        assertNotNull(profile)
    }
}
