package it.trekcheck.app.ui.preview

import it.trekcheck.app.data.local.InMemoryLocalPlanRepository
import it.trekcheck.app.data.local.files.VersionedRouteFileStorage
import it.trekcheck.app.domain.model.GpxSegment
import it.trekcheck.app.domain.model.GpxTrackPoint
import it.trekcheck.app.domain.model.NormalizedRouteGeometry
import it.trekcheck.app.domain.model.RouteManagementErrorCode
import it.trekcheck.app.domain.model.RouteMetadataSnapshot
import it.trekcheck.app.domain.model.SavedPlanSummary
import it.trekcheck.app.domain.repository.RouteManagementRepository
import it.trekcheck.app.domain.usecase.DeleteRouteUseCase
import it.trekcheck.app.domain.usecase.LoadRoutePreviewUseCase
import it.trekcheck.app.domain.usecase.RenameRouteUseCase
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class RoutePreviewViewModelTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val plan = SavedPlanSummary(
        id = "r1",
        title = "Monte Pizzoc",
        distanceMeters = 1000.0,
        elevationGainMeters = 190.0,
        departureInstantUtc = null,
        routeZoneId = ZoneId.of("Europe/Rome"),
        estimatedDurationSeconds = null,
        updatedAtUtc = Instant.parse("2026-11-24T10:00:00Z"),
        hasElevation = true,
    )

    private class Fixture(
        val repo: InMemoryLocalPlanRepository,
        val storage: VersionedRouteFileStorage,
        val rootDir: File,
        val viewModel: RoutePreviewViewModel,
    )

    private fun fixture(routeId: String = "r1", withGeometry: Boolean = true): Fixture = runBlocking {
        val rootDir = tempFolder.newFolder()
        val storage = VersionedRouteFileStorage(rootDir)
        if (withGeometry) {
            val geometry = NormalizedRouteGeometry(
                listOf(GpxSegment(List(20) { GpxTrackPoint(46.0 + it * 0.001, 12.0, 1000.0 + it * 10) })),
            )
            val staging = storage.openStaging()
            staging.outputStream().use { it.write("<gpx/>".toByteArray()) }
            storage.commitStagedRoute("r1", staging, geometry)
        }
        val repo = InMemoryLocalPlanRepository(listOf(plan))
        Fixture(
            repo,
            storage,
            rootDir,
            RoutePreviewViewModel(
                routeId = routeId,
                loadRoutePreview = LoadRoutePreviewUseCase(repo, storage, Dispatchers.Unconfined),
                renameRoute = RenameRouteUseCase(repo, Dispatchers.Unconfined),
                deleteRoute = DeleteRouteUseCase(repo, Dispatchers.Unconfined),
                dispatcher = Dispatchers.Unconfined,
            ),
        )
    }

    @Test
    fun loadsReadyStateWithDetails() {
        val f = fixture()
        val state = f.viewModel.uiState.value
        assertEquals(RoutePreviewStatus.READY, state.status)
        assertEquals("Monte Pizzoc", state.details!!.title)
        assertNotNull(state.details!!.profile)
    }

    @Test
    fun missingRouteMissingGeometryAndCorruptedGeometryExposeRecoverableErrors() {
        assertEquals(
            RouteManagementErrorCode.NOT_FOUND,
            fixture(routeId = "nope").viewModel.uiState.value.loadErrorCode,
        )
        val noGeometry = fixture(withGeometry = false).viewModel
        assertEquals(RoutePreviewStatus.ERROR, noGeometry.uiState.value.status)
        assertEquals(RouteManagementErrorCode.NOT_FOUND, noGeometry.uiState.value.loadErrorCode)
        noGeometry.onEvent(RoutePreviewEvent.RetryClicked)
        assertEquals(RoutePreviewStatus.ERROR, noGeometry.uiState.value.status)

        val corruptedFixture = fixture(routeId = "r1", withGeometry = true)
        File(corruptedFixture.rootDir, "r1/geometry_v1.bin").writeBytes(byteArrayOf(1, 2, 3))
        corruptedFixture.viewModel.onEvent(RoutePreviewEvent.RetryClicked)
        assertEquals(RoutePreviewStatus.ERROR, corruptedFixture.viewModel.uiState.value.status)
        assertEquals(RouteManagementErrorCode.CORRUPTED_GEOMETRY, corruptedFixture.viewModel.uiState.value.loadErrorCode)
    }

    @Test
    fun renameFlowValidatesTruncatesSurrogatesSafelyThenPersistsAndUpdatesState() {
        val f = fixture()
        val vm = f.viewModel
        vm.onEvent(RoutePreviewEvent.RenameClicked)
        assertEquals("Monte Pizzoc", vm.uiState.value.renameDialog!!.draft)

        vm.onEvent(RoutePreviewEvent.RenameDraftChanged("   "))
        vm.onEvent(RoutePreviewEvent.RenameConfirmed)
        assertEquals(RouteManagementErrorCode.INVALID_TITLE, vm.uiState.value.renameDialog!!.errorCode)
        assertEquals("Monte Pizzoc", vm.uiState.value.details!!.title)

        val surrogateDraft = "a".repeat(399) + "\uD83D\uDE00" + "z"
        vm.onEvent(RoutePreviewEvent.RenameDraftChanged(surrogateDraft))
        assertEquals(399, vm.uiState.value.renameDialog!!.draft.length)

        vm.onEvent(RoutePreviewEvent.RenameDraftChanged("Nuovo nome"))
        assertNull(vm.uiState.value.renameDialog!!.errorCode)
        vm.onEvent(RoutePreviewEvent.RenameConfirmed)
        assertNull(vm.uiState.value.renameDialog)
        assertEquals("Nuovo nome", vm.uiState.value.details!!.title)
        assertEquals("Nuovo nome", runBlocking { f.repo.observeSavedPlans().first().single().title })
    }

    @Test
    fun renameRepositoryFailureKeepsDialogOpenWithErrorAndResetsIsSaving() = runBlocking {
        val storage = VersionedRouteFileStorage(tempFolder.newFolder())
        val geometry = NormalizedRouteGeometry(
            listOf(GpxSegment(listOf(GpxTrackPoint(46.0, 12.0, 1000.0), GpxTrackPoint(46.01, 12.0, 1100.0)))),
        )
        val staging = storage.openStaging()
        staging.outputStream().use { it.write("<gpx/>".toByteArray()) }
        storage.commitStagedRoute("r1", staging, geometry)

        var updateReturnsFalse = false
        val repo = object : RouteManagementRepository {
            override suspend fun getRouteMetadata(routeId: String) =
                RouteMetadataSnapshot("r1", "Monte Pizzoc", true, 100.0, 0.0, 1L)

            override suspend fun updateTitle(routeId: String, title: String, updatedAtUtc: Instant): Boolean =
                if (updateReturnsFalse) false else throw IOException("disk full")

            override suspend fun deleteRoute(routeId: String) {}
        }
        val vm = RoutePreviewViewModel(
            routeId = "r1",
            loadRoutePreview = LoadRoutePreviewUseCase(repo, storage, Dispatchers.Unconfined),
            renameRoute = RenameRouteUseCase(repo, Dispatchers.Unconfined),
            deleteRoute = DeleteRouteUseCase(repo, Dispatchers.Unconfined),
            dispatcher = Dispatchers.Unconfined,
        )

        vm.onEvent(RoutePreviewEvent.RenameClicked)
        vm.onEvent(RoutePreviewEvent.RenameDraftChanged("Titolo 2"))
        vm.onEvent(RoutePreviewEvent.RenameConfirmed)
        var dialog = vm.uiState.value.renameDialog!!
        assertFalse(dialog.isSaving)
        assertEquals(RouteManagementErrorCode.STORAGE_FAILED, dialog.errorCode)

        updateReturnsFalse = true
        vm.onEvent(RoutePreviewEvent.RenameConfirmed)
        dialog = vm.uiState.value.renameDialog!!
        assertFalse(dialog.isSaving)
        assertEquals(RouteManagementErrorCode.NOT_FOUND, dialog.errorCode)
    }

    @Test
    fun deleteFailureSetsDismissibleActionErrorAndGuardsActionsWhileDeleting() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val storage = VersionedRouteFileStorage(tempFolder.newFolder())
        val geometry = NormalizedRouteGeometry(
            listOf(GpxSegment(listOf(GpxTrackPoint(46.0, 12.0, 1000.0), GpxTrackPoint(46.01, 12.0, 1100.0)))),
        )
        val staging = storage.openStaging()
        staging.outputStream().use { it.write("<gpx/>".toByteArray()) }
        storage.commitStagedRoute("r1", staging, geometry)

        val deleteGate = CompletableDeferred<Unit>()
        val repo = object : RouteManagementRepository {
            override suspend fun getRouteMetadata(routeId: String) =
                RouteMetadataSnapshot("r1", "Monte Pizzoc", true, 100.0, 0.0, 1L)

            override suspend fun updateTitle(routeId: String, title: String, updatedAtUtc: Instant) = true
            override suspend fun deleteRoute(routeId: String) {
                deleteGate.await()
                throw IOException("cannot remove dir")
            }
        }
        val vm = RoutePreviewViewModel(
            routeId = "r1",
            loadRoutePreview = LoadRoutePreviewUseCase(repo, storage, dispatcher),
            renameRoute = RenameRouteUseCase(repo, dispatcher),
            deleteRoute = DeleteRouteUseCase(repo, dispatcher),
            dispatcher = dispatcher,
        )
        advanceUntilIdle()
        assertEquals(RoutePreviewStatus.READY, vm.uiState.value.status)

        vm.onEvent(RoutePreviewEvent.DeleteClicked)
        vm.onEvent(RoutePreviewEvent.DeleteConfirmed)
        runCurrent()
        assertTrue(vm.uiState.value.isDeleting)

        vm.onEvent(RoutePreviewEvent.RenameClicked)
        assertNull(vm.uiState.value.renameDialog)
        vm.onEvent(RoutePreviewEvent.BackClicked)

        deleteGate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isDeleting)
        assertFalse(vm.uiState.value.isDeleteDialogVisible)
        assertEquals(RouteManagementErrorCode.STORAGE_FAILED, vm.uiState.value.actionErrorCode)

        vm.onEvent(RoutePreviewEvent.DismissActionError)
        assertNull(vm.uiState.value.actionErrorCode)

        var completed = false
        vm.invokeOnScopeCompletion { completed = true }
        vm.clear()
        assertTrue(completed)
    }

    @Test
    fun renameDismissClosesDialogWithoutChanges() {
        val vm = fixture().viewModel
        vm.onEvent(RoutePreviewEvent.RenameClicked)
        vm.onEvent(RoutePreviewEvent.RenameDismissed)
        assertNull(vm.uiState.value.renameDialog)
        assertEquals("Monte Pizzoc", vm.uiState.value.details!!.title)
    }

    @Test
    fun deleteCancelKeepsDataAndConfirmDeletesAndNavigatesHome() = runBlocking {
        val f = fixture()
        val vm = f.viewModel
        vm.onEvent(RoutePreviewEvent.DeleteClicked)
        assertTrue(vm.uiState.value.isDeleteDialogVisible)
        vm.onEvent(RoutePreviewEvent.DeleteDismissed)
        assertFalse(vm.uiState.value.isDeleteDialogVisible)
        assertEquals(1, f.repo.observeSavedPlans().first().size)

        vm.onEvent(RoutePreviewEvent.DeleteClicked)
        vm.onEvent(RoutePreviewEvent.DeleteConfirmed)
        assertTrue(f.repo.observeSavedPlans().first().isEmpty())
        assertEquals(RoutePreviewEffect.NavigateHome, vm.effects.first())
    }

    @Test
    fun backEmitsNavigateHome() = runBlocking {
        val vm = fixture().viewModel
        vm.onEvent(RoutePreviewEvent.BackClicked)
        assertEquals(RoutePreviewEffect.NavigateHome, vm.effects.first())
    }

    @Test
    fun configureClickedEmitsNavigationAndRefreshRequestedReloadsUpdatedPlanConfig() = runBlocking {
        val f = fixture()
        val vm = f.viewModel
        vm.onEvent(RoutePreviewEvent.ConfigureClicked)
        assertEquals(RoutePreviewEffect.NavigateToPlanConfig("r1"), vm.effects.first())

        val departure = Instant.parse("2026-08-10T06:00:00Z")
        val pace = it.trekcheck.app.domain.model.PaceProfile(
            it.trekcheck.app.domain.model.PaceProfileType.FAST,
        )
        f.repo.savePlanConfig("r1", departure, ZoneId.of("America/New_York"), pace, Instant.parse("2026-11-25T10:00:00Z"))
        vm.onEvent(RoutePreviewEvent.RefreshRequested)

        val details = vm.uiState.value.details!!
        assertEquals(departure, details.departureInstantUtc)
        assertEquals(ZoneId.of("America/New_York"), details.routeZoneId)
        assertEquals(pace, details.paceProfile)
    }
}
