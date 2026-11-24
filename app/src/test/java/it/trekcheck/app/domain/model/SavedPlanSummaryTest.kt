package it.trekcheck.app.domain.model

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class SavedPlanSummaryTest {
    private val sampleInstant = Instant.parse("2026-06-24T06:00:00Z")
    private val sampleZone = ZoneId.of("Europe/Rome")

    @Test
    fun constructsPlanWithCanonicalSiUnitsAndExplicitNulls() {
        val summary = SavedPlanSummary(
            id = "plan-1",
            title = "Anello del Monte Baldo",
            distanceMeters = 10_250.0,
            elevationGainMeters = null,
            departureInstantUtc = null,
            routeZoneId = sampleZone,
            estimatedDurationSeconds = null,
            updatedAtUtc = sampleInstant,
        )

        assertEquals("plan-1", summary.id)
        assertEquals("Anello del Monte Baldo", summary.title)
        assertEquals(10_250.0, summary.distanceMeters, 0.0)
        assertNull(summary.elevationGainMeters)
        assertNull(summary.departureInstantUtc)
        assertNull(summary.estimatedDurationSeconds)
        assertEquals(sampleZone, summary.routeZoneId)
        assertEquals(SavedPlanDataAvailability.ROUTE_ONLY, summary.dataAvailability)
    }

    @Test
    fun acceptsValidZeroBoundaries() {
        val zeroBoundaryPlan = SavedPlanSummary(
            id = "plan-zero",
            title = "Traccia piana di prova",
            distanceMeters = 0.0,
            elevationGainMeters = 0.0,
            departureInstantUtc = sampleInstant,
            routeZoneId = sampleZone,
            estimatedDurationSeconds = 0L,
            updatedAtUtc = sampleInstant,
        )

        assertEquals(0.0, zeroBoundaryPlan.distanceMeters, 0.0)
        assertEquals(0.0, zeroBoundaryPlan.elevationGainMeters!!, 0.0)
        assertEquals(0L, zeroBoundaryPlan.estimatedDurationSeconds)
    }

    @Test
    fun rejectsBlankIdOrTitleAndInvalidMetrics() {
        assertThrows(IllegalArgumentException::class.java) {
            SavedPlanSummary(
                id = "  ",
                title = "Sentiero",
                distanceMeters = 1000.0,
                elevationGainMeters = null,
                departureInstantUtc = null,
                routeZoneId = sampleZone,
                estimatedDurationSeconds = null,
                updatedAtUtc = sampleInstant,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            SavedPlanSummary(
                id = "plan-2",
                title = "",
                distanceMeters = 1000.0,
                elevationGainMeters = null,
                departureInstantUtc = null,
                routeZoneId = sampleZone,
                estimatedDurationSeconds = null,
                updatedAtUtc = sampleInstant,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            SavedPlanSummary(
                id = "plan-3",
                title = "Sentiero",
                distanceMeters = -1.0,
                elevationGainMeters = null,
                departureInstantUtc = null,
                routeZoneId = sampleZone,
                estimatedDurationSeconds = null,
                updatedAtUtc = sampleInstant,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            SavedPlanSummary(
                id = "plan-4",
                title = "Sentiero",
                distanceMeters = Double.NaN,
                elevationGainMeters = null,
                departureInstantUtc = null,
                routeZoneId = sampleZone,
                estimatedDurationSeconds = null,
                updatedAtUtc = sampleInstant,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            SavedPlanSummary(
                id = "plan-4b",
                title = "Sentiero",
                distanceMeters = Double.POSITIVE_INFINITY,
                elevationGainMeters = null,
                departureInstantUtc = null,
                routeZoneId = sampleZone,
                estimatedDurationSeconds = null,
                updatedAtUtc = sampleInstant,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            SavedPlanSummary(
                id = "plan-5",
                title = "Sentiero",
                distanceMeters = 1000.0,
                elevationGainMeters = -10.0,
                departureInstantUtc = null,
                routeZoneId = sampleZone,
                estimatedDurationSeconds = null,
                updatedAtUtc = sampleInstant,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            SavedPlanSummary(
                id = "plan-5b",
                title = "Sentiero",
                distanceMeters = 1000.0,
                elevationGainMeters = Double.NaN,
                departureInstantUtc = null,
                routeZoneId = sampleZone,
                estimatedDurationSeconds = null,
                updatedAtUtc = sampleInstant,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            SavedPlanSummary(
                id = "plan-5c",
                title = "Sentiero",
                distanceMeters = 1000.0,
                elevationGainMeters = Double.POSITIVE_INFINITY,
                departureInstantUtc = null,
                routeZoneId = sampleZone,
                estimatedDurationSeconds = null,
                updatedAtUtc = sampleInstant,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            SavedPlanSummary(
                id = "plan-6",
                title = "Sentiero",
                distanceMeters = 1000.0,
                elevationGainMeters = 150.0,
                departureInstantUtc = null,
                routeZoneId = sampleZone,
                estimatedDurationSeconds = -60L,
                updatedAtUtc = sampleInstant,
            )
        }
    }
}
