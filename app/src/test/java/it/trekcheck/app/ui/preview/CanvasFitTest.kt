package it.trekcheck.app.ui.preview

import it.trekcheck.app.domain.model.ElevationProfilePreview
import it.trekcheck.app.domain.model.ElevationSample
import it.trekcheck.app.domain.model.ProjectedPoint
import it.trekcheck.app.domain.model.SimplifiedRoutePreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasFitTest {
    private fun track(minX: Double, maxX: Double, minY: Double, maxY: Double) = SimplifiedRoutePreview(
        segments = listOf(listOf(ProjectedPoint(minX, minY), ProjectedPoint(maxX, maxY))),
        minX = minX, maxX = maxX, minY = minY, maxY = maxY,
        start = ProjectedPoint(minX, minY), end = ProjectedPoint(maxX, maxY), highest = null,
    )

    @Test
    fun degenerateTracksFitWithoutNaN() {
        for (t in listOf(track(0.0, 0.0, 0.0, 0.0), track(-5.0, 5.0, 3.0, 3.0), track(2.0, 2.0, -9.0, 9.0))) {
            val fit = TrackFit.compute(t, 300f, 200f, 20f)
            assertTrue(fit.scale.isFinite() && fit.scale > 0f)
            for (p in t.segments[0]) {
                assertFalse(fit.mapX(p.x).isNaN())
                assertFalse(fit.mapY(p.y).isNaN())
            }
        }
    }

    @Test
    fun trackIsCenteredInsideBox() {
        val fit = TrackFit.compute(track(0.0, 100.0, 0.0, 100.0), 300f, 200f, 20f)
        assertEquals(150f, (fit.mapX(0.0) + fit.mapX(100.0)) / 2f, 0.01f)
        assertEquals(100f, (fit.mapY(0.0) + fit.mapY(100.0)) / 2f, 0.01f)
        assertTrue(fit.mapY(100.0) < fit.mapY(0.0))
    }

    @Test
    fun flatProfileAndZeroLengthAreFinite() {
        val profile = ElevationProfilePreview(listOf(ElevationSample(0.0, 5.0), ElevationSample(0.0, 5.0)), 5.0, 5.0, 0.0)
        val fit = ProfileFit.compute(profile, 300f, 160f, 16f)
        assertEquals(150f, fit.mapX(0.0), 0.01f)
        assertEquals(80f, fit.mapY(5.0), 0.01f)
    }

    @Test
    fun extremeElevationRangeStaysFinite() {
        val profile = ElevationProfilePreview(
            listOf(ElevationSample(0.0, -1e308), ElevationSample(100.0, 1e308)),
            -1e308,
            1e308,
            100.0,
        )
        val fit = ProfileFit.compute(profile, 300f, 160f, 16f)
        assertFalse(fit.mapY(-1e308).isNaN())
        assertFalse(fit.mapY(1e308).isNaN())
    }
}
