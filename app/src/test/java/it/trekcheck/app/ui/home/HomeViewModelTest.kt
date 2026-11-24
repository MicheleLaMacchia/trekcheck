package it.trekcheck.app.ui.home

import it.trekcheck.app.data.gpx.SafeXmlGpxParser
import it.trekcheck.app.data.local.InMemoryLocalPlanRepository
import it.trekcheck.app.data.local.files.VersionedRouteFileStorage
import it.trekcheck.app.domain.model.GpxImportError
import it.trekcheck.app.domain.model.ImportErrorCode
import it.trekcheck.app.domain.model.ImportGpxResult
import it.trekcheck.app.domain.model.SavedPlanDataAvailability
import it.trekcheck.app.domain.model.SavedPlanSummary
import it.trekcheck.app.domain.repository.GpxInputSource
import it.trekcheck.app.domain.repository.ImportedRouteRecord
import it.trekcheck.app.domain.repository.RouteImportRepository
import it.trekcheck.app.domain.usecase.ImportGpxUseCase
import it.trekcheck.app.ui.components.PrimaryActionState
import java.io.InputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

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

        val secondPlan = samplePlan.copy(id = "plan-2", title = "Sentiero senza quota", elevationGainMeters = null, hasElevation = false)
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
    fun clickingEnabledImportOrChooseAnotherFileEmitsLaunchPickerEffect() = runTest {
        var dispatchCount = 0
        val viewModel = HomeViewModel(
            localPlanRepository = InMemoryLocalPlanRepository(),
            dispatcher = Dispatchers.Unconfined,
            onImportDispatched = { dispatchCount += 1 },
        )

        viewModel.onEvent(HomeEvent.ImportGpxClicked)
        assertEquals(HomeEffect.LaunchGpxPicker, viewModel.effects.first())
        assertEquals(1, dispatchCount)
        assertEquals(0, viewModel.ignoredImportClicksCount)

        viewModel.onEvent(HomeEvent.ChooseAnotherFileClicked)
        assertEquals(HomeEffect.LaunchGpxPicker, viewModel.effects.first())
        assertEquals(2, dispatchCount)

        viewModel.clear()
    }

    @Test
    fun clickingImportWhenInProgressOrDisabledIsSuppressed() = runTest {
        var dispatchCount = 0
        val viewModel = HomeViewModel(
            localPlanRepository = InMemoryLocalPlanRepository(),
            dispatcher = Dispatchers.Unconfined,
            initialImportActionState = PrimaryActionState.InProgress,
            onImportDispatched = { dispatchCount += 1 },
        )

        viewModel.onEvent(HomeEvent.ImportGpxClicked)
        viewModel.onEvent(HomeEvent.ChooseAnotherFileClicked)

        assertEquals(0, dispatchCount)
        assertEquals(2, viewModel.ignoredImportClicksCount)
        assertEquals(PrimaryActionState.InProgress, viewModel.uiState.value.importActionState)

        viewModel.setImportActionState(PrimaryActionState.Disabled)
        viewModel.onEvent(HomeEvent.ImportGpxClicked)

        assertEquals(0, dispatchCount)
        assertEquals(3, viewModel.ignoredImportClicksCount)
        assertEquals(PrimaryActionState.Disabled, viewModel.uiState.value.importActionState)

        viewModel.setImportActionState(PrimaryActionState.Enabled)
        viewModel.onEvent(HomeEvent.ImportGpxClicked)

        assertEquals(1, dispatchCount)
        assertEquals(HomeEffect.LaunchGpxPicker, viewModel.effects.first())

        viewModel.clear()
    }

    @Test
    fun importGpxSourceShowsInProgressBlocksDuplicatesThenUpdatesSavedPlansAndNotice() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val routesRoot = tempFolder.newFolder("routes")
        val storage = VersionedRouteFileStorage(routesRoot)
        val localRepo = InMemoryLocalPlanRepository()
        val gate = CompletableDeferred<Unit>()
        val importRepo = object : RouteImportRepository {
            override suspend fun saveImportedRoute(record: ImportedRouteRecord) {
                gate.await()
                localRepo.setPlans(localRepo.observeSavedPlans().first() + record.summary)
            }
        }
        val useCase = ImportGpxUseCase(
            gpxParser = SafeXmlGpxParser(),
            routeFileStorage = storage,
            routeImportRepository = importRepo,
            ioDispatcher = testDispatcher,
            clock = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneId.of("UTC")),
            defaultZoneProvider = { ZoneId.of("Europe/Rome") },
            idGenerator = { "imported-1" },
        )

        val viewModel = HomeViewModel(
            localPlanRepository = localRepo,
            importGpxUseCase = useCase,
            dispatcher = testDispatcher,
        )

        val gpx = """
            <gpx version="1.1"><trk><name>Cima Carega</name><trkseg>
              <trkpt lat="45.7" lon="11.1" />
              <trkpt lat="45.71" lon="11.11" />
            </trkseg></trk></gpx>
        """.trimIndent()
        val source = InMemoryGpxSource("carega.gpx", gpx.toByteArray())

        viewModel.importGpxSource(source)
        runCurrent()
        assertEquals(PrimaryActionState.InProgress, viewModel.uiState.value.importActionState)

        // Duplicate clicks and duplicate imports while InProgress must be suppressed
        viewModel.onEvent(HomeEvent.ImportGpxClicked)
        viewModel.importGpxSource(source)
        assertEquals(2, viewModel.ignoredImportClicksCount)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(PrimaryActionState.Enabled, viewModel.uiState.value.importActionState)
        assertEquals(1, viewModel.uiState.value.savedPlans.size)
        assertEquals("Cima Carega", viewModel.uiState.value.savedPlans.first().title)
        assertEquals(
            HomeNotice.ImportSucceeded(
                planTitle = "Cima Carega",
                hasElevation = false,
                hasTimestamps = false,
            ),
            viewModel.uiState.value.activeNotice,
        )

        viewModel.onEvent(HomeEvent.DismissNotice)
        assertNull(viewModel.uiState.value.activeNotice)

        viewModel.clear()
    }

    @Test
    fun failedImportEmitsOpaqueErrorCodeAndLeavesExistingPlansIntact() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val routesRoot = tempFolder.newFolder("routes")
        val storage = VersionedRouteFileStorage(routesRoot)
        val localRepo = InMemoryLocalPlanRepository(initialPlans = listOf(samplePlan))
        val importRepo = object : RouteImportRepository {
            override suspend fun saveImportedRoute(record: ImportedRouteRecord) = Unit
        }
        val useCase = ImportGpxUseCase(
            gpxParser = SafeXmlGpxParser(),
            routeFileStorage = storage,
            routeImportRepository = importRepo,
            ioDispatcher = testDispatcher,
        )
        val viewModel = HomeViewModel(
            localPlanRepository = localRepo,
            importGpxUseCase = useCase,
            dispatcher = testDispatcher,
        )

        val unsafeGpx = """<!DOCTYPE gpx><gpx/>"""
        viewModel.importGpxSource(InMemoryGpxSource("evil.gpx", unsafeGpx.toByteArray()))
        advanceUntilIdle()

        assertEquals(PrimaryActionState.Enabled, viewModel.uiState.value.importActionState)
        assertEquals(HomeNotice.ImportFailed(ImportErrorCode.UNSAFE_XML), viewModel.uiState.value.activeNotice)
        assertEquals(listOf(samplePlan), viewModel.uiState.value.savedPlans)

        viewModel.clear()
    }

    @Test
    fun invokeOnScopeCompletionWaitsForActiveImportToFinishUnwinding() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val routesRoot = tempFolder.newFolder("routes")
        val storage = VersionedRouteFileStorage(routesRoot)
        val localRepo = InMemoryLocalPlanRepository()
        val cleanupFinished = CompletableDeferred<Unit>()
        var dbClosed = false

        val importRepo = object : RouteImportRepository {
            override suspend fun saveImportedRoute(record: ImportedRouteRecord) {
                try {
                    kotlinx.coroutines.awaitCancellation()
                } finally {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                        cleanupFinished.await()
                    }
                }
            }
        }
        val useCase = ImportGpxUseCase(
            gpxParser = SafeXmlGpxParser(),
            routeFileStorage = storage,
            routeImportRepository = importRepo,
            ioDispatcher = testDispatcher,
            idGenerator = { "unwind-1" },
        )
        val viewModel = HomeViewModel(
            localPlanRepository = localRepo,
            importGpxUseCase = useCase,
            dispatcher = testDispatcher,
        )

        val gpx = "<gpx version=\"1.1\"><trk><trkseg><trkpt lat=\"45.0\" lon=\"11.0\"/><trkpt lat=\"45.1\" lon=\"11.1\"/></trkseg></trk></gpx>"
        viewModel.importGpxSource(InMemoryGpxSource("unwind.gpx", gpx.toByteArray()))
        runCurrent()

        viewModel.invokeOnScopeCompletion { dbClosed = true }
        viewModel.clear()
        runCurrent()
        assertFalse("Database must not close while NonCancellable cleanup is still running", dbClosed)

        cleanupFinished.complete(Unit)
        advanceUntilIdle()
        assertTrue(dbClosed)
    }

    private class InMemoryGpxSource(
        override val suggestedTitle: String?,
        private val bytes: ByteArray,
    ) : GpxInputSource {
        override val knownByteSize: Long = bytes.size.toLong()
        override fun openStream(): InputStream = bytes.inputStream()
    }
}
