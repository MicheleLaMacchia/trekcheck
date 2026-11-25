package it.trekcheck.app.domain.usecase

import it.trekcheck.app.data.local.InMemoryLocalPlanRepository
import it.trekcheck.app.domain.model.LoadPlanConfigResult
import it.trekcheck.app.domain.model.PaceProfile
import it.trekcheck.app.domain.model.PaceProfileType
import it.trekcheck.app.domain.model.PlanConfigErrorCode
import it.trekcheck.app.domain.model.RoutePlanConfig
import it.trekcheck.app.domain.model.SavePlanConfigResult
import it.trekcheck.app.domain.model.SavedPlanSummary
import it.trekcheck.app.domain.repository.RoutePlanConfigRepository
import it.trekcheck.app.domain.time.AmbiguousTimeChoice
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutePlanConfigUseCasesTest {
    private val samplePlan = SavedPlanSummary(
        id = "route-1",
        title = "Anello delle Creste",
        distanceMeters = 12_000.0,
        elevationGainMeters = 650.0,
        departureInstantUtc = null,
        routeZoneId = ZoneId.of("Europe/Rome"),
        estimatedDurationSeconds = 14_400L,
        updatedAtUtc = Instant.parse("2026-11-24T10:00:00Z"),
        hasElevation = true,
        hasTimestamps = true,
    )

    @Test
    fun loadsExistingRoutePlanConfigAndMapsMissingOrBlankIdToNotFound() = runBlocking {
        val repo = InMemoryLocalPlanRepository(listOf(samplePlan))
        val load = LoadRoutePlanConfigUseCase(repo, Dispatchers.Unconfined)

        val success = load("route-1") as LoadPlanConfigResult.Success
        assertEquals("Anello delle Creste", success.config.routeTitle)
        assertTrue(success.config.hasTimestamps)
        assertEquals(1L, success.config.planVersion)

        assertEquals(
            LoadPlanConfigResult.Failure(PlanConfigErrorCode.NOT_FOUND),
            load("missing"),
        )
        assertEquals(
            LoadPlanConfigResult.Failure(PlanConfigErrorCode.NOT_FOUND),
            load("  "),
        )
    }

    @Test
    fun saveRejectsUnresolvedDstGapAndAmbiguityWithoutCallingRepository() = runBlocking {
        var repoSaveCalls = 0
        val spyRepo = object : RoutePlanConfigRepository {
            override suspend fun getPlanConfig(routeId: String): RoutePlanConfig? = null
            override suspend fun savePlanConfig(
                routeId: String,
                departureInstantUtc: Instant,
                routeZoneId: ZoneId,
                paceProfile: PaceProfile,
                updatedAtUtc: Instant,
            ): Boolean {
                repoSaveCalls += 1
                return true
            }
        }
        val save = SaveRoutePlanConfigUseCase(
            routePlanConfigRepository = spyRepo,
            ioDispatcher = Dispatchers.Unconfined,
        )
        val rome = ZoneId.of("Europe/Rome")

        val gapFailure = save(
            routeId = "route-1",
            localDate = LocalDate.of(2026, 3, 29),
            localTime = LocalTime.of(2, 30),
            routeZoneId = rome,
            ambiguousTimeChoice = null,
            paceProfile = PaceProfile(PaceProfileType.NORMAL),
        )
        assertEquals(SavePlanConfigResult.Failure(PlanConfigErrorCode.UNRESOLVED_DST), gapFailure)

        val overlapFailure = save(
            routeId = "route-1",
            localDate = LocalDate.of(2026, 10, 25),
            localTime = LocalTime.of(2, 30),
            routeZoneId = rome,
            ambiguousTimeChoice = null,
            paceProfile = PaceProfile(PaceProfileType.NORMAL),
        )
        assertEquals(SavePlanConfigResult.Failure(PlanConfigErrorCode.UNRESOLVED_DST), overlapFailure)
        assertEquals(0, repoSaveCalls)
    }

    @Test
    fun saveResolvesExplicitAmbiguousChoicePersistsAndIncrementsPlanVersion() = runBlocking {
        val repo = InMemoryLocalPlanRepository(listOf(samplePlan))
        val fixedClock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC)
        val save = SaveRoutePlanConfigUseCase(repo, Dispatchers.Unconfined, fixedClock)
        val rome = ZoneId.of("Europe/Rome")
        val customPace = PaceProfile(PaceProfileType.CUSTOM, PaceProfile.kmhToMps(3.5))

        val result = save(
            routeId = "route-1",
            localDate = LocalDate.of(2026, 10, 25),
            localTime = LocalTime.of(2, 30),
            routeZoneId = rome,
            ambiguousTimeChoice = AmbiguousTimeChoice.LATER,
            paceProfile = customPace,
        )
        assertEquals(SavePlanConfigResult.Success, result)

        val updated = (LoadRoutePlanConfigUseCase(repo, Dispatchers.Unconfined)("route-1") as LoadPlanConfigResult.Success).config
        assertEquals(Instant.parse("2026-10-25T01:30:00Z"), updated.departureInstantUtc)
        assertEquals(rome, updated.routeZoneId)
        assertEquals(customPace, updated.paceProfile)
        assertEquals(2L, updated.planVersion)
        assertNull(repo.observeSavedPlans().first().single().estimatedDurationSeconds)
    }

    @Test
    fun repositoryExceptionsMapToOpaqueStorageFailedWithoutLeakingPaths() = runBlocking {
        val failingRepo = object : RoutePlanConfigRepository {
            override suspend fun getPlanConfig(routeId: String): RoutePlanConfig? {
                throw IOException("/data/user/0/it.trekcheck.app/secret-title")
            }

            override suspend fun savePlanConfig(
                routeId: String,
                departureInstantUtc: Instant,
                routeZoneId: ZoneId,
                paceProfile: PaceProfile,
                updatedAtUtc: Instant,
            ): Boolean {
                throw IOException("/data/user/0/it.trekcheck.app/secret-title")
            }
        }
        val loadFailure = LoadRoutePlanConfigUseCase(failingRepo, Dispatchers.Unconfined)("route-1")
        val saveFailure = SaveRoutePlanConfigUseCase(failingRepo, Dispatchers.Unconfined)(
            routeId = "route-1",
            localDate = LocalDate.of(2026, 7, 1),
            localTime = LocalTime.of(8, 0),
            routeZoneId = ZoneId.of("UTC"),
            paceProfile = PaceProfile(PaceProfileType.FAST),
        )

        assertEquals(LoadPlanConfigResult.Failure(PlanConfigErrorCode.STORAGE_FAILED), loadFailure)
        assertEquals(SavePlanConfigResult.Failure(PlanConfigErrorCode.STORAGE_FAILED), saveFailure)
        assertFalse(loadFailure.toString().contains("secret"))
        assertFalse(saveFailure.toString().contains("secret"))
    }
}
