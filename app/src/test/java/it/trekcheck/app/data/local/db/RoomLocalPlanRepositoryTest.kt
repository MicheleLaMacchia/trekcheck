package it.trekcheck.app.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.trekcheck.app.data.local.files.VersionedRouteFileStorage
import it.trekcheck.app.domain.model.GpxBoundingBox
import it.trekcheck.app.domain.model.GpxSegment
import it.trekcheck.app.domain.model.GpxSourceKind
import it.trekcheck.app.domain.model.GpxTrackPoint
import it.trekcheck.app.domain.model.NormalizedRouteGeometry
import it.trekcheck.app.domain.model.ParsedGpxRoute
import it.trekcheck.app.domain.model.SavedPlanDataAvailability
import it.trekcheck.app.domain.model.SavedPlanSummary
import it.trekcheck.app.domain.repository.ImportedRouteRecord
import java.io.File
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomLocalPlanRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var database: TrekCheckDatabase
    private lateinit var storage: VersionedRouteFileStorage
    private lateinit var routesRoot: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TrekCheckDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        routesRoot = tempFolder.newFolder("routes")
        storage = VersionedRouteFileStorage(routesRoot)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun storesSingleMetadataRowWithoutTrackPointTableAndObservesUpdates() = runBlocking {
        val repository = RoomLocalPlanRepository(database.routeMetadataDao(), storage)
        assertTrue(repository.observeSavedPlans().first().isEmpty())

        val record = createImportedRecord(
            routeId = "route-1",
            title = "Sentiero Dolomiti",
            hasElevation = false,
            hasTimestamps = false,
            pointCount = 150,
        )
        repository.saveImportedRoute(record)

        val observed = repository.observeSavedPlans().first()
        assertEquals(1, observed.size)
        val saved = observed.first()
        assertEquals("route-1", saved.id)
        assertEquals("Sentiero Dolomiti", saved.title)
        assertNull(saved.elevationGainMeters)
        assertNull(saved.departureInstantUtc)
        assertNull(saved.estimatedDurationSeconds)
        assertFalse(saved.hasElevation)
        assertFalse(saved.hasTimestamps)
        assertEquals(SavedPlanDataAvailability.ROUTE_ONLY, saved.dataAvailability)

        val entity = database.routeMetadataDao().getById("route-1")!!
        assertEquals("source_v1.gpx", entity.gpxFileName)
        assertEquals(1, entity.gpxVersion)
        assertEquals("geometry_v1.bin", entity.geometryFileName)
        assertEquals(1, entity.geometryVersion)
        assertEquals(1, database.routeMetadataDao().count())

        // Verify no TrackPoint table exists in SQLite schema
        val tables = mutableListOf<String>()
        database.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type = 'table'",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                tables += cursor.getString(0)
            }
        }
        assertTrue(tables.contains("route_metadata"))
        assertFalse(tables.any { it.contains("track", ignoreCase = true) || it.contains("point", ignoreCase = true) })
    }

    @Test
    fun cleansUpCommittedRouteFilesWhenDatabaseInsertFails() = runBlocking {
        val failingDao = object : RouteMetadataDao {
            override fun observeAll(): Flow<List<RouteMetadataEntity>> = database.routeMetadataDao().observeAll()
            override suspend fun getById(id: String): RouteMetadataEntity? = database.routeMetadataDao().getById(id)
            override suspend fun insert(entity: RouteMetadataEntity) {
                throw IllegalStateException("Simulated SQLite constraint failure")
            }

            override suspend fun deleteById(id: String) = database.routeMetadataDao().deleteById(id)
            override suspend fun count(): Int = database.routeMetadataDao().count()
        }
        val repository = RoomLocalPlanRepository(failingDao, storage)

        val record = createImportedRecord(
            routeId = "rollback-route",
            title = "Percorso che fallisce su DB",
            hasElevation = true,
            hasTimestamps = true,
            pointCount = 2,
        )
        assertTrue(File(routesRoot, "rollback-route/source_v1.gpx").exists())
        assertTrue(File(routesRoot, "rollback-route/geometry_v1.bin").exists())

        var failed = false
        try {
            repository.saveImportedRoute(record)
        } catch (_: IllegalStateException) {
            failed = true
        }
        assertTrue(failed)
        assertFalse(File(routesRoot, "rollback-route").exists())
        assertEquals(0, database.routeMetadataDao().count())
    }

    @Test
    fun persistsMetadataAcrossDiskDatabaseCloseAndReopenIncludingBboxLossAndPointCount() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(TrekCheckDatabase.DATABASE_NAME)
        val record = createImportedRecord(
            routeId = "disk-route-1",
            title = "Cima Grappa",
            hasElevation = true,
            hasTimestamps = true,
            pointCount = 42,
        )

        val db1 = TrekCheckDatabase.create(context)
        try {
            val repo1 = RoomLocalPlanRepository(db1.routeMetadataDao(), storage)
            repo1.saveImportedRoute(record)
        } finally {
            db1.close()
        }

        val db2 = TrekCheckDatabase.create(context)
        try {
            val repo2 = RoomLocalPlanRepository(db2.routeMetadataDao(), storage)
            val observed = repo2.observeSavedPlans().first()
            assertEquals(1, observed.size)
            assertEquals("Cima Grappa", observed.first().title)
            assertTrue(observed.first().hasElevation)
            assertTrue(observed.first().hasTimestamps)

            val entity = db2.routeMetadataDao().getById("disk-route-1")!!
            assertEquals(46.0, entity.minLatitude, 0.0)
            assertEquals(46.1, entity.maxLatitude, 0.0)
            assertEquals(12.0, entity.minLongitude, 0.0)
            assertEquals(12.1, entity.maxLongitude, 0.0)
            assertEquals(250.0, entity.elevationGainMeters!!, 0.0)
            assertEquals(100.0, entity.elevationLossMeters!!, 0.0)
            assertEquals(1000.0, entity.minElevationMeters!!, 0.0)
            assertEquals(1250.0, entity.maxElevationMeters!!, 0.0)
            assertEquals(42, entity.pointCount)
            assertEquals(1, entity.segmentCount)
        } finally {
            db2.close()
            context.deleteDatabase(TrekCheckDatabase.DATABASE_NAME)
        }
    }

    @Test
    fun deletesCommittedDatabaseRowAndRouteFilesOnCancellationAfterInsert() = runBlocking {
        val cancellingDao = object : RouteMetadataDao {
            override fun observeAll(): Flow<List<RouteMetadataEntity>> = database.routeMetadataDao().observeAll()
            override suspend fun getById(id: String): RouteMetadataEntity? = database.routeMetadataDao().getById(id)
            override suspend fun insert(entity: RouteMetadataEntity) {
                database.routeMetadataDao().insert(entity)
                throw CancellationException("Cancelled right after insert")
            }

            override suspend fun deleteById(id: String) = database.routeMetadataDao().deleteById(id)
            override suspend fun count(): Int = database.routeMetadataDao().count()
        }
        val repository = RoomLocalPlanRepository(cancellingDao, storage)
        val record = createImportedRecord(
            routeId = "cancel-after-insert",
            title = "Percorso cancellato",
            hasElevation = true,
            hasTimestamps = false,
            pointCount = 3,
        )

        var cancelled = false
        try {
            repository.saveImportedRoute(record)
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
        assertNull(database.routeMetadataDao().getById("cancel-after-insert"))
        assertEquals(0, database.routeMetadataDao().count())
        assertFalse(File(routesRoot, "cancel-after-insert").exists())
    }

    private suspend fun createImportedRecord(
        routeId: String,
        title: String,
        hasElevation: Boolean,
        hasTimestamps: Boolean,
        pointCount: Int,
    ): ImportedRouteRecord {
        val points = List(pointCount) { index ->
            GpxTrackPoint(
                latitude = 46.0 + index * 0.0001,
                longitude = 12.0 + index * 0.0001,
                elevationMeters = if (hasElevation) 1000.0 + index else null,
            )
        }
        val geometry = NormalizedRouteGeometry(listOf(GpxSegment(points)))
        val staging = storage.openStaging()
        staging.outputStream().use { it.write("<gpx/>".toByteArray()) }
        val storedFiles = storage.commitStagedRoute(routeId, staging, geometry)

        val parsed = ParsedGpxRoute(
            routeName = title,
            sourceKind = GpxSourceKind.TRACK,
            geometry = geometry,
            distanceMeters = 5_000.0,
            elevationGainMeters = if (hasElevation) 250.0 else null,
            elevationLossMeters = if (hasElevation) 100.0 else null,
            minElevationMeters = if (hasElevation) 1000.0 else null,
            maxElevationMeters = if (hasElevation) 1250.0 else null,
            boundingBox = GpxBoundingBox(46.0, 46.1, 12.0, 12.1),
            hasElevation = hasElevation,
            hasTimestamps = hasTimestamps,
        )
        val summary = SavedPlanSummary(
            id = routeId,
            title = title,
            distanceMeters = 5_000.0,
            elevationGainMeters = if (hasElevation) 250.0 else null,
            departureInstantUtc = null,
            routeZoneId = ZoneId.of("Europe/Rome"),
            estimatedDurationSeconds = null,
            updatedAtUtc = Instant.parse("2026-11-24T10:00:00Z"),
            hasElevation = hasElevation,
            hasTimestamps = hasTimestamps,
        )
        return ImportedRouteRecord(summary, parsed, storedFiles)
    }
}
