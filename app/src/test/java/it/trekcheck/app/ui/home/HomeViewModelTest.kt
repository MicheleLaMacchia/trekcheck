package it.trekcheck.app.ui.home

import it.trekcheck.app.data.local.InMemoryLocalPlanRepository
import it.trekcheck.app.domain.model.SavedPlanDataAvailability
import it.trekcheck.app.domain.model.SavedPlanSummary
import it.trekcheck.app.ui.components.PrimaryActionState
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest {
    private val samplePlan = SavedPlanSummary(
        id = "plan-monte-pizzoc",
        title = "Monte Pizzoc da Fregona",
        distanceMeters = 10_200.0,
        elevationGainMeters = 620.0,
        departureInstantUtc = Instant.parse("2026-06-24T06:00:00Z"),
        routeZoneId = ZoneId.of("Europe/Rome"),
        estimatedDurationSeconds = 15_600L,
        updatedAtUtc = Instant.parse("2026-06-23T18:30:00Z"),
        dataAvailability = SavedPlanDataAvailability.ROUTE_ONLY,
    )

    @Test
    fun firstLaunchStateIsEmptyOfflineWithEnabledImportAction() {
        val repository = InMemoryLocalPlanRepository()
        val viewModel = HomeViewModel(
            localPlanRepository = repository,
            dispatcher = Dispatchers.Unconfined,
        )

        val state = viewModel.uiState.value
        assertTrue(state.savedPlans.isEmpty())
        assertTrue(state.isEmpty)
        assertTrue(state.isOffline)
        assertEquals(PrimaryActionState.Enabled, state.importActionState)
        assertNull(state.activeNotice)

        viewModel.clear()
    }

    @Test
    fun populatedOfflineRepositoryUpdatesSavedPlansWithoutLoss() {
        val repository = InMemoryLocalPlanRepository(initialPlans = listOf(samplePlan))
        val viewModel = HomeViewModel(
            localPlanRepository = repository,
            dispatcher = Dispatchers.Unconfined,
            initialIsOffline = true,
        )

        val state = viewModel.uiState.value
        assertFalse(state.isEmpty)
        assertTrue(state.isOffline)
        assertEquals(1, state.savedPlans.size)
        assertEquals(samplePlan, state.savedPlans.first())

        val secondPlan = samplePlan.copy(id = "plan-2", title = "Sentiero senza quota", elevationGainMeters = null)
        repository.setPlans(listOf(samplePlan, secondPlan))

        assertEquals(2, viewModel.uiState.value.savedPlans.size)
        assertNull(viewModel.uiState.value.savedPlans[1].elevationGainMeters)

        viewModel.clear()
    }

    @Test
    fun clearStopsRepositoryCollection() {
        val repository = InMemoryLocalPlanRepository(initialPlans = listOf(samplePlan))
        val viewModel = HomeViewModel(
            localPlanRepository = repository,
            dispatcher = Dispatchers.Unconfined,
        )

        assertEquals(1, viewModel.uiState.value.savedPlans.size)

        viewModel.clear()
        repository.setPlans(emptyList())

        assertEquals(listOf(samplePlan), viewModel.uiState.value.savedPlans)
    }

    @Test
    fun clickingEnabledImportShowsRecoverableNoticeAndCanBeDismissed() {
        var dispatchCount = 0
        val viewModel = HomeViewModel(
            localPlanRepository = InMemoryLocalPlanRepository(),
            dispatcher = Dispatchers.Unconfined,
            onImportDispatched = { dispatchCount += 1 },
        )

        viewModel.onEvent(HomeEvent.ImportGpxClicked)

        assertEquals(1, dispatchCount)
        assertEquals(0, viewModel.ignoredImportClicksCount)
        assertEquals(HomeNotice.ImportComingSoon, viewModel.uiState.value.activeNotice)

        viewModel.onEvent(HomeEvent.DismissNotice)
        assertNull(viewModel.uiState.value.activeNotice)

        viewModel.clear()
    }

    @Test
    fun clickingImportWhenInProgressOrDisabledIsSuppressed() {
        var dispatchCount = 0
        val viewModel = HomeViewModel(
            localPlanRepository = InMemoryLocalPlanRepository(),
            dispatcher = Dispatchers.Unconfined,
            initialImportActionState = PrimaryActionState.InProgress,
            onImportDispatched = { dispatchCount += 1 },
        )

        viewModel.onEvent(HomeEvent.ImportGpxClicked)
        viewModel.onEvent(HomeEvent.ImportGpxClicked)

        assertEquals(0, dispatchCount)
        assertEquals(2, viewModel.ignoredImportClicksCount)
        assertNull(viewModel.uiState.value.activeNotice)
        assertEquals(PrimaryActionState.InProgress, viewModel.uiState.value.importActionState)

        viewModel.setImportActionState(PrimaryActionState.Disabled)
        viewModel.onEvent(HomeEvent.ImportGpxClicked)

        assertEquals(0, dispatchCount)
        assertEquals(3, viewModel.ignoredImportClicksCount)
        assertNull(viewModel.uiState.value.activeNotice)
        assertEquals(PrimaryActionState.Disabled, viewModel.uiState.value.importActionState)

        viewModel.setImportActionState(PrimaryActionState.Enabled)
        viewModel.onEvent(HomeEvent.ImportGpxClicked)

        assertEquals(1, dispatchCount)
        assertEquals(HomeNotice.ImportComingSoon, viewModel.uiState.value.activeNotice)

        viewModel.clear()
    }
}
