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

    @Test
    fun savePlanConfigUpdatesDepartureZonePaceAndPlanVersionWithoutAffectingRename() = runBlocking {
        val repository = InMemoryLocalPlanRepository(initialPlans = listOf(samplePlan))
        assertEquals(1L, repository.getPlanConfig("plan-1")!!.planVersion)

        val departure = Instant.parse("2026-08-01T07:00:00Z")
        val pace = it.trekcheck.app.domain.model.PaceProfile(
            it.trekcheck.app.domain.model.PaceProfileType.RELAXED,
        )
        assertTrue(
            repository.savePlanConfig(
                routeId = "plan-1",
                departureInstantUtc = departure,
                routeZoneId = ZoneId.of("UTC"),
                paceProfile = pace,
                updatedAtUtc = Instant.parse("2026-11-25T10:00:00Z"),
            ),
        )
        val config = repository.getPlanConfig("plan-1")!!
        assertEquals(departure, config.departureInstantUtc)
        assertEquals(ZoneId.of("UTC"), config.routeZoneId)
        assertEquals(pace, config.paceProfile)
        assertEquals(2L, config.planVersion)
        assertEquals(null, repository.observeSavedPlans().first().single().estimatedDurationSeconds)

        assertTrue(repository.updateTitle("plan-1", "Nuovo nome", Instant.parse("2026-11-25T10:05:00Z")))
        assertEquals(2L, repository.getPlanConfig("plan-1")!!.planVersion)
    }
}
