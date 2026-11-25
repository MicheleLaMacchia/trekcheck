package it.trekcheck.app.domain.usecase

import it.trekcheck.app.data.local.InMemoryLocalPlanRepository
import it.trekcheck.app.data.local.files.VersionedRouteFileStorage
import it.trekcheck.app.domain.model.GpxSegment
import it.trekcheck.app.domain.model.GpxTrackPoint
import it.trekcheck.app.domain.model.NormalizedRouteGeometry
import it.trekcheck.app.domain.model.RouteActionResult
import it.trekcheck.app.domain.model.RouteManagementErrorCode
import it.trekcheck.app.domain.model.RouteMetadataSnapshot
import it.trekcheck.app.domain.model.RoutePreviewResult
import it.trekcheck.app.domain.model.SavedPlanSummary
import it.trekcheck.app.domain.repository.RouteManagementRepository
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RouteManagementUseCasesTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun plan(id: String, title: String, hasElevation: Boolean, gain: Double?) = SavedPlanSummary(
        id = id,
        title = title,
        distanceMeters = 1000.0,
        elevationGainMeters = gain,
        departureInstantUtc = null,
        routeZoneId = ZoneId.of("Europe/Rome"),
        estimatedDurationSeconds = null,
        updatedAtUtc = Instant.parse("2026-11-24T10:00:00Z"),
        hasElevation = hasElevation,
    )

    private suspend fun storeRoute(storage: VersionedRouteFileStorage, id: String, geometry: NormalizedRouteGeometry) {
        val staging = storage.openStaging()
        staging.outputStream().use { it.write("<gpx/>".toByteArray()) }
        storage.commitStagedRoute(id, staging, geometry)
    }

    private fun geometry(withElevation: Boolean) = NormalizedRouteGeometry(
        listOf(
            GpxSegment(
                List(20) {
                    GpxTrackPoint(46.0 + it * 0.001, 12.0 + it * 0.0005, if (withElevation) 1000.0 + it * 10 else null)
                },
            ),
        ),
    )

    @Test
    fun loadsFullMetricsWithElevationAndProfile() = runBlocking {
        val root = tempFolder.newFolder("routes")
        val storage = VersionedRouteFileStorage(root)
        storeRoute(storage, "r1", geometry(true))
        val repo = InMemoryLocalPlanRepository(listOf(plan("r1", "Sentiero", true, 190.0)))
        val result = LoadRoutePreviewUseCase(repo, storage, Dispatchers.Unconfined)("r1")

        val details = (result as RoutePreviewResult.Success).details
        assertEquals("Sentiero", details.title)
        assertEquals(20, details.pointCount)
        assertEquals(1190.0, details.highestPoint!!.elevationMeters!!, 0.0)
        assertEquals(1000.0, details.minElevationMeters!!, 0.0)
        assertEquals(190.0, details.elevationGainMeters!!, 0.0)
        assertEquals(0.0, details.elevationLossMeters!!, 0.0)
        assertNotNull(details.profile)
        assertTrue(details.distanceMeters > 0.0)
    }

    @Test
    fun withoutElevationHidesHighestPointDeltasAndProfile() = runBlocking {
        val storage = VersionedRouteFileStorage(tempFolder.newFolder("routes"))
        storeRoute(storage, "r2", geometry(false))
        val repo = InMemoryLocalPlanRepository(listOf(plan("r2", "Piatto", false, null)))
        val details = (LoadRoutePreviewUseCase(repo, storage, Dispatchers.Unconfined)("r2") as RoutePreviewResult.Success).details

        assertFalse(details.hasElevation)
        assertNull(details.highestPoint)
        assertNull(details.minElevationMeters)
        assertNull(details.elevationGainMeters)
        assertNull(details.elevationLossMeters)
        assertNull(details.profile)
    }

    @Test
    fun hasElevationButNoGainKeepsExtremesButNoProfile() = runBlocking {
        val storage = VersionedRouteFileStorage(tempFolder.newFolder("routes"))
        val singleElevationGeometry = NormalizedRouteGeometry(
            listOf(
                GpxSegment(
                    listOf(
                        GpxTrackPoint(46.0, 12.0, 1000.0),
                        GpxTrackPoint(46.001, 12.0, null),
                    ),
                ),
            ),
        )
        storeRoute(storage, "r3", singleElevationGeometry)
        val repo = InMemoryLocalPlanRepository(listOf(plan("r3", "Una quota", true, null)))
        val details = (LoadRoutePreviewUseCase(repo, storage, Dispatchers.Unconfined)("r3") as RoutePreviewResult.Success).details

        assertTrue(details.hasElevation)
        assertNotNull(details.highestPoint)
        assertNull(details.elevationGainMeters)
        assertNull(details.elevationLossMeters)
        assertNull(details.profile)
    }

    @Test
    fun missingMetadataOrGeometryAndCorruptedGeometryMapToOpaqueCodes() = runBlocking {
        val root = tempFolder.newFolder("routes")
        val storage = VersionedRouteFileStorage(root)
        val repo = InMemoryLocalPlanRepository(
            listOf(plan("nogeo", "Segreto Rossi", true, 1.0), plan("bad", "Titolo privato", true, 1.0)),
        )
        val useCase = LoadRoutePreviewUseCase(repo, storage, Dispatchers.Unconfined)

        assertEquals(
            RouteManagementErrorCode.NOT_FOUND,
            (useCase("unknown") as RoutePreviewResult.Failure).code,
        )
        assertEquals(
            RouteManagementErrorCode.NOT_FOUND,
            (useCase("nogeo") as RoutePreviewResult.Failure).code,
        )

        storeRoute(storage, "bad", geometry(true))
        File(root, "bad/geometry_v1.bin").writeBytes(byteArrayOf(1, 2, 3))
        val failure = useCase("bad") as RoutePreviewResult.Failure
        assertEquals(RouteManagementErrorCode.CORRUPTED_GEOMETRY, failure.code)
        val rendered = failure.toString()
        assertFalse(rendered.contains("Titolo privato"))
        assertFalse(rendered.contains("geometry_v1"))
    }

    @Test
    fun renameValidatesSanitizesAndReportsNotFound() = runBlocking {
        val repo = InMemoryLocalPlanRepository(listOf(plan("r1", "Vecchio", false, null)))
        val clock = Clock.fixed(Instant.parse("2026-12-01T00:00:00Z"), ZoneOffset.UTC)
        val rename = RenameRouteUseCase(repo, Dispatchers.Unconfined, clock)

        assertEquals(
            RouteManagementErrorCode.INVALID_TITLE,
            (rename("r1", "   ") as RouteActionResult.Failure).code,
        )
        assertEquals("Vecchio", repo.getRouteMetadata("r1")!!.title)

        val ok = rename("r1", "  Nuovo   titolo ") as RouteActionResult.Success
        assertEquals("Nuovo titolo", ok.title)
        assertEquals("Nuovo titolo", repo.getRouteMetadata("r1")!!.title)

        val long = rename("r1", "x".repeat(400)) as RouteActionResult.Success
        assertEquals(160, long.title!!.length)

        assertEquals(
            RouteManagementErrorCode.NOT_FOUND,
            (rename("missing", "Titolo") as RouteActionResult.Failure).code,
        )
    }

    @Test
    fun renameAndDeleteMapRepositoryFailuresToStorageFailed() = runBlocking {
        val failing = object : RouteManagementRepository {
            override suspend fun getRouteMetadata(routeId: String): RouteMetadataSnapshot? = null
            override suspend fun updateTitle(routeId: String, title: String, updatedAtUtc: Instant): Boolean =
                throw IOException("/data/user/0/secret/path.gpx")

            override suspend fun deleteRoute(routeId: String) = throw IOException("/data/user/0/secret/path.gpx")
        }
        val rename = RenameRouteUseCase(failing, Dispatchers.Unconfined)(" r", "Titolo") as RouteActionResult.Failure
        val delete = DeleteRouteUseCase(failing, Dispatchers.Unconfined)("r") as RouteActionResult.Failure
        assertEquals(RouteManagementErrorCode.STORAGE_FAILED, rename.code)
        assertEquals(RouteManagementErrorCode.STORAGE_FAILED, delete.code)
        assertFalse(rename.toString().contains("secret"))
        assertFalse(delete.toString().contains("secret"))
    }

    @Test
    fun deleteRemovesPlan() = runBlocking {
        val repo = InMemoryLocalPlanRepository(listOf(plan("r1", "A", false, null)))
        assertTrue(DeleteRouteUseCase(repo, Dispatchers.Unconfined)("r1") is RouteActionResult.Success)
        assertNull(repo.getRouteMetadata("r1"))
    }

    @Test
    fun loadPreviewPropagatesConfiguredDepartureZoneAndPaceProfile() = runBlocking {
        val storage = VersionedRouteFileStorage(tempFolder.newFolder("routes"))
        storeRoute(storage, "r1", geometry(true))
        val departure = Instant.parse("2026-07-01T06:30:00Z")
        val pace = it.trekcheck.app.domain.model.PaceProfile(
            it.trekcheck.app.domain.model.PaceProfileType.CUSTOM,
            it.trekcheck.app.domain.model.PaceProfile.kmhToMps(3.8),
        )
        val configuredPlan = plan("r1", "Sentiero configurato", true, 190.0).copy(
            departureInstantUtc = departure,
            routeZoneId = ZoneId.of("America/New_York"),
            paceProfile = pace,
        )
        val repo = InMemoryLocalPlanRepository(listOf(configuredPlan))
        val details = (LoadRoutePreviewUseCase(repo, storage, Dispatchers.Unconfined)("r1") as RoutePreviewResult.Success).details

        assertEquals(departure, details.departureInstantUtc)
        assertEquals(ZoneId.of("America/New_York"), details.routeZoneId)
        assertEquals(pace, details.paceProfile)
    }
}
