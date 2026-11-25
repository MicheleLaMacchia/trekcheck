package it.trekcheck.app.ui.planconfig

import it.trekcheck.app.data.local.InMemoryLocalPlanRepository
import it.trekcheck.app.domain.model.PaceProfile
import it.trekcheck.app.domain.model.PaceProfileType
import it.trekcheck.app.domain.model.PaceSpeedInputError
import it.trekcheck.app.domain.model.PlanConfigErrorCode
import it.trekcheck.app.domain.model.RoutePlanConfig
import it.trekcheck.app.domain.model.SavedPlanSummary
import it.trekcheck.app.domain.repository.RoutePlanConfigRepository
import it.trekcheck.app.domain.time.AmbiguousTimeChoice
import it.trekcheck.app.domain.time.DepartureResolution
import it.trekcheck.app.domain.usecase.LoadRoutePlanConfigUseCase
import it.trekcheck.app.domain.usecase.SaveRoutePlanConfigUseCase
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanConfigViewModelTest {
    private val fixedClock = Clock.fixed(Instant.parse("2026-11-24T15:30:00Z"), ZoneOffset.UTC)

    private fun buildPlan(
        departureInstantUtc: Instant? = null,
        routeZoneId: ZoneId = ZoneId.of("Europe/Rome"),
        hasTimestamps: Boolean = true,
        paceProfile: PaceProfile = PaceProfile(PaceProfileType.NORMAL),
    ) = SavedPlanSummary(
        id = "r1",
        title = "Sentiero Dolomiti",
        distanceMeters = 9_500.0,
        elevationGainMeters = 480.0,
        departureInstantUtc = departureInstantUtc,
        routeZoneId = routeZoneId,
        estimatedDurationSeconds = null,
        updatedAtUtc = Instant.parse("2026-11-24T10:00:00Z"),
        hasElevation = true,
        hasTimestamps = hasTimestamps,
        paceProfile = paceProfile,
    )

    private fun createViewModel(
        repo: RoutePlanConfigRepository,
        deviceZone: ZoneId = ZoneId.of("Europe/Rome"),
        locale: Locale = Locale.ITALIAN,
    ): PlanConfigViewModel = PlanConfigViewModel(
        routeId = "r1",
        loadPlanConfig = LoadRoutePlanConfigUseCase(repo, Dispatchers.Unconfined),
        savePlanConfig = SaveRoutePlanConfigUseCase(repo, Dispatchers.Unconfined, fixedClock),
        clock = fixedClock,
        deviceZoneProvider = { deviceZone },
        localeProvider = { locale },
        dispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun initializesUnconfiguredPlanWithTomorrowAtEightIgnoringGpxTimestamps() {
        val repo = InMemoryLocalPlanRepository(listOf(buildPlan(hasTimestamps = true)))
        val vm = createViewModel(repo)
        val state = vm.uiState.value

        assertEquals(PlanConfigStatus.READY, state.status)
        assertTrue(state.hasTimestamps)
        assertFalse(state.hasSavedConfig)
        // fixedClock is 2026-11-24T15:30Z -> 16:30 Europe/Rome -> tomorrow is 2026-11-25 08:00
        assertEquals(LocalDate.of(2026, 11, 25), state.localDate)
        assertEquals(LocalTime.of(8, 0), state.localTime)
        assertEquals(ZoneId.of("Europe/Rome"), state.routeZoneId)
        assertEquals(Instant.parse("2026-11-25T07:00:00Z"), state.resolvedDepartureInstant)
        assertTrue(state.canSave)
    }

    @Test
    fun dstGapBlocksSavingUntilSuggestedValidTimeOrManualEditApplied() = runBlocking {
        val repo = InMemoryLocalPlanRepository(listOf(buildPlan()))
        val vm = createViewModel(repo)

        vm.onEvent(PlanConfigEvent.DateSelected(LocalDate.of(2026, 3, 29)))
        vm.onEvent(PlanConfigEvent.TimeSelected(LocalTime.of(2, 30)))

        var state = vm.uiState.value
        assertTrue(state.resolution is DepartureResolution.NonExistent)
        assertNull(state.resolvedDepartureInstant)
        assertTrue(state.isDstUnresolved)
        assertFalse(state.canSave)

        // Programmatic save click must not persist
        vm.onEvent(PlanConfigEvent.SaveClicked)
        assertEquals(PlanConfigErrorCode.UNRESOLVED_DST, vm.uiState.value.saveErrorCode)
        assertNull(repo.getPlanConfig("r1")!!.departureInstantUtc)

        // Applying suggested valid time resolves to 03:00 (+02:00)
        vm.onEvent(PlanConfigEvent.ApplySuggestedDstTime)
        state = vm.uiState.value
        assertTrue(state.resolution is DepartureResolution.Valid)
        assertEquals(LocalTime.of(3, 0), state.localTime)
        assertEquals(Instant.parse("2026-03-29T01:00:00Z"), state.resolvedDepartureInstant)
        assertTrue(state.canSave)
    }

    @Test
    fun dstOverlapRequiresExplicitEarlierOrLaterChoiceBeforeSaving() = runBlocking {
        val repo = InMemoryLocalPlanRepository(listOf(buildPlan()))
        val vm = createViewModel(repo)

        vm.onEvent(PlanConfigEvent.DateSelected(LocalDate.of(2026, 10, 25)))
        vm.onEvent(PlanConfigEvent.TimeSelected(LocalTime.of(2, 30)))

        var state = vm.uiState.value
        assertTrue(state.resolution is DepartureResolution.Ambiguous)
        assertNull(state.selectedAmbiguousChoice)
        assertNull(state.resolvedDepartureInstant)
        assertFalse(state.canSave)

        vm.onEvent(PlanConfigEvent.AmbiguousChoiceSelected(AmbiguousTimeChoice.LATER))
        state = vm.uiState.value
        assertEquals(Instant.parse("2026-10-25T01:30:00Z"), state.resolvedDepartureInstant)
        assertTrue(state.canSave)

        vm.onEvent(PlanConfigEvent.SaveClicked)
        assertEquals(PlanConfigEffect.SavedAndClose, vm.effects.first())
        val saved = repo.getPlanConfig("r1")!!
        assertEquals(Instant.parse("2026-10-25T01:30:00Z"), saved.departureInstantUtc)
        assertEquals(2L, saved.planVersion)
    }

    @Test
    fun changingTimezoneReflectsDifferenceFromDeviceZoneAndRecalculatesInstant() = runBlocking {
        val repo = InMemoryLocalPlanRepository(listOf(buildPlan(routeZoneId = ZoneId.of("Europe/Rome"))))
        val vm = createViewModel(repo, deviceZone = ZoneId.of("Europe/Rome"))
        assertFalse(vm.uiState.value.isRouteZoneDifferentFromDevice)

        vm.onEvent(PlanConfigEvent.DateSelected(LocalDate.of(2026, 7, 10)))
        vm.onEvent(PlanConfigEvent.TimeSelected(LocalTime.of(9, 0)))
        vm.onEvent(PlanConfigEvent.ZoneSelected(ZoneId.of("America/New_York")))

        val state = vm.uiState.value
        assertTrue(state.isRouteZoneDifferentFromDevice)
        assertEquals(ZoneId.of("America/New_York"), state.routeZoneId)
        assertEquals(Instant.parse("2026-07-10T13:00:00Z"), state.resolvedDepartureInstant)

        vm.onEvent(PlanConfigEvent.SaveClicked)
        assertEquals(PlanConfigEffect.SavedAndClose, vm.effects.first())
        val saved = repo.getPlanConfig("r1")!!
        assertEquals(ZoneId.of("America/New_York"), saved.routeZoneId)
        assertEquals(Instant.parse("2026-07-10T13:00:00Z"), saved.departureInstantUtc)
    }

    @Test
    fun customPaceValidatesInputRangeAndPersistsInSiMetersPerSecond() = runBlocking {
        val repo = InMemoryLocalPlanRepository(listOf(buildPlan()))
        val vm = createViewModel(repo)

        vm.onEvent(PlanConfigEvent.PaceTypeSelected(PaceProfileType.CUSTOM))
        assertFalse(vm.uiState.value.canSave)

        vm.onEvent(PlanConfigEvent.CustomSpeedChanged("1,5"))
        assertEquals(PaceSpeedInputError.OUT_OF_RANGE, vm.uiState.value.customSpeedError)
        assertFalse(vm.uiState.value.canSave)

        vm.onEvent(PlanConfigEvent.CustomSpeedChanged("abc"))
        assertEquals(PaceSpeedInputError.NOT_A_NUMBER, vm.uiState.value.customSpeedError)

        vm.onEvent(PlanConfigEvent.CustomSpeedChanged("4,5"))
        assertNull(vm.uiState.value.customSpeedError)
        assertTrue(vm.uiState.value.canSave)

        vm.onEvent(PlanConfigEvent.SaveClicked)
        assertEquals(PlanConfigEffect.SavedAndClose, vm.effects.first())
        val saved = repo.getPlanConfig("r1")!!
        assertEquals(PaceProfileType.CUSTOM, saved.paceProfile.type)
        assertEquals(4.5 / 3.6, saved.paceProfile.customFlatSpeedMetersPerSecond!!, 1e-12)
    }

    @Test
    fun loadsExistingAmbiguousDepartureAndCustomPacePreservingExactOffset() {
        val rome = ZoneId.of("Europe/Rome")
        val secondOccurrenceUtc = Instant.parse("2026-10-25T01:30:00Z")
        val existingPlan = buildPlan(
            departureInstantUtc = secondOccurrenceUtc,
            routeZoneId = rome,
            paceProfile = PaceProfile(PaceProfileType.CUSTOM, PaceProfile.kmhToMps(3.25)),
        )
        val repo = InMemoryLocalPlanRepository(listOf(existingPlan))
        val vm = createViewModel(repo, locale = Locale.ITALIAN)
        val state = vm.uiState.value

        assertTrue(state.hasSavedConfig)
        assertEquals(LocalDate.of(2026, 10, 25), state.localDate)
        assertEquals(LocalTime.of(2, 30), state.localTime)
        assertEquals(AmbiguousTimeChoice.LATER, state.selectedAmbiguousChoice)
        assertEquals(secondOccurrenceUtc, state.resolvedDepartureInstant)
        assertEquals(PaceProfileType.CUSTOM, state.selectedPaceType)
        assertEquals("3,25", state.customSpeedInput)
        assertTrue(state.canSave)
    }

    @Test
    fun guardsDuplicateSaveInFlightAndSurfacesRepositoryFailureWithoutClosing() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val saveGate = CompletableDeferred<Unit>()
        var saveCalls = 0
        val repo = object : RoutePlanConfigRepository {
            override suspend fun getPlanConfig(routeId: String) = RoutePlanConfig(
                routeId = "r1",
                routeTitle = "Sentiero",
                hasElevation = true,
                hasTimestamps = false,
                departureInstantUtc = null,
                routeZoneId = ZoneId.of("UTC"),
                paceProfile = PaceProfile(PaceProfileType.NORMAL),
                planVersion = 1L,
            )

            override suspend fun savePlanConfig(
                routeId: String,
                departureInstantUtc: Instant,
                routeZoneId: ZoneId,
                paceProfile: PaceProfile,
                updatedAtUtc: Instant,
            ): Boolean {
                saveCalls += 1
                saveGate.await()
                throw IOException("disk error")
            }
        }
        val vm = PlanConfigViewModel(
            routeId = "r1",
            loadPlanConfig = LoadRoutePlanConfigUseCase(repo, dispatcher),
            savePlanConfig = SaveRoutePlanConfigUseCase(repo, dispatcher, fixedClock),
            clock = fixedClock,
            deviceZoneProvider = { ZoneId.of("UTC") },
            dispatcher = dispatcher,
        )
        advanceUntilIdle()
        assertEquals(PlanConfigStatus.READY, vm.uiState.value.status)

        vm.onEvent(PlanConfigEvent.SaveClicked)
        runCurrent()
        assertTrue(vm.uiState.value.isSaving)

        // Second tap while saving and back press must be ignored
        vm.onEvent(PlanConfigEvent.SaveClicked)
        vm.onEvent(PlanConfigEvent.BackClicked)
        assertEquals(1, saveCalls)

        saveGate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isSaving)
        assertEquals(PlanConfigErrorCode.STORAGE_FAILED, vm.uiState.value.saveErrorCode)

        vm.onEvent(PlanConfigEvent.DismissSaveError)
        assertNull(vm.uiState.value.saveErrorCode)

        var completed = false
        vm.invokeOnScopeCompletion { completed = true }
        vm.clear()
        assertTrue(completed)
    }

    @Test
    fun missingRouteSetsLoadErrorAndBackEmitsCancel() = runBlocking {
        val repo = InMemoryLocalPlanRepository(emptyList())
        val vm = createViewModel(repo)
        assertEquals(PlanConfigStatus.ERROR, vm.uiState.value.status)
        assertEquals(PlanConfigErrorCode.NOT_FOUND, vm.uiState.value.loadErrorCode)
        assertNotNull(vm.uiState.value.loadErrorCode)

        vm.onEvent(PlanConfigEvent.BackClicked)
        assertEquals(PlanConfigEffect.Cancel, vm.effects.first())
    }
}
