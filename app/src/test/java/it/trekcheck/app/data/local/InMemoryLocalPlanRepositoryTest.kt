package it.trekcheck.app.data.local

import it.trekcheck.app.domain.model.SavedPlanSummary
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryLocalPlanRepositoryTest {
    private val samplePlan = SavedPlanSummary(
        id = "plan-1",
        title = "Anello Val d'Orcia",
        distanceMeters = 8_500.0,
        elevationGainMeters = 320.0,
        departureInstantUtc = null,
        routeZoneId = ZoneId.of("Europe/Rome"),
        estimatedDurationSeconds = 9_600L,
        updatedAtUtc = Instant.parse("2026-06-20T09:00:00Z"),
    )

    @Test
    fun defaultsToEmptyList() = runBlocking {
        val repository = InMemoryLocalPlanRepository()
        assertTrue(repository.observeSavedPlans().first().isEmpty())
    }

    @Test
    fun copiesInitialAndUpdatedListsDefensivelyAndEmitsUpdates() = runBlocking {
        val mutableSource = mutableListOf(samplePlan)
        val repository = InMemoryLocalPlanRepository(initialPlans = mutableSource)
        mutableSource.clear()

        assertEquals(listOf(samplePlan), repository.observeSavedPlans().first())

        val secondPlan = samplePlan.copy(id = "plan-2", title = "Monte Grappa")
        val updatedMutable = mutableListOf(samplePlan, secondPlan)
        repository.setPlans(updatedMutable)
        updatedMutable.clear()

        assertEquals(listOf(samplePlan, secondPlan), repository.observeSavedPlans().first())
    }
}
