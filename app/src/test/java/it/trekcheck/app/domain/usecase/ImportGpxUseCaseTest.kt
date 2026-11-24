package it.trekcheck.app.domain.usecase

import it.trekcheck.app.data.gpx.SafeXmlGpxParser
import it.trekcheck.app.data.gpx.SafeXmlGpxParserTest
import it.trekcheck.app.data.local.files.VersionedRouteFileStorage
import it.trekcheck.app.domain.model.GpxImportError
import it.trekcheck.app.domain.model.GpxImportLimits
import it.trekcheck.app.domain.model.ImportErrorCode
import it.trekcheck.app.domain.model.ImportGpxResult
import it.trekcheck.app.domain.model.NormalizedRouteGeometry
import it.trekcheck.app.domain.repository.GpxInputSource
import it.trekcheck.app.domain.repository.ImportedRouteRecord
import it.trekcheck.app.domain.repository.RouteFileStorage
import it.trekcheck.app.domain.repository.RouteImportRepository
import it.trekcheck.app.domain.repository.RouteStagingSession
import it.trekcheck.app.domain.repository.StoredRouteFiles
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImportGpxUseCaseTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun importsValidGpxOffCallerThreadPreservesSegmentsAndIgnoresHistoricalDeparture() = runBlocking {
        val routesRoot = tempFolder.newFolder("routes")
        val storage = VersionedRouteFileStorage(routesRoot)
        val savedRecords = mutableListOf<ImportedRouteRecord>()
        var saveThreadName: String? = null
        val repo = object : RouteImportRepository {
            override suspend fun saveImportedRoute(record: ImportedRouteRecord) {
                saveThreadName = Thread.currentThread().name
                savedRecords += record
            }
        }
        val callerThreadName = Thread.currentThread().name
        val fixedClock = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneId.of("UTC"))
        val useCase = ImportGpxUseCase(
            gpxParser = SafeXmlGpxParser(),
            routeFileStorage = storage,
            routeImportRepository = repo,
            ioDispatcher = Dispatchers.IO,
            clock = fixedClock,
            defaultZoneProvider = { ZoneId.of("Europe/Rome") },
            idGenerator = { "route-fixed-1" },
        )

        val gpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
              <trk>
                <trkseg>
                  <trkpt lat="46.000" lon="12.000"><time>2020-01-01T08:00:00Z</time></trkpt>
                  <trkpt lat="46.005" lon="12.000"><time>2020-01-01T08:15:00Z</time></trkpt>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val result = useCase(
            InMemoryGpxSource(
                suggestedTitle = "Anello_Storico.gpx",
                bytes = gpx.toByteArray(Charsets.UTF_8),
            ),
        )

        assertTrue(result is ImportGpxResult.Success)
        val success = result as ImportGpxResult.Success
        assertNotEquals(callerThreadName, saveThreadName)
        assertEquals("Anello_Storico", success.plan.title)
        assertNull(success.plan.departureInstantUtc)
        assertNull(success.plan.elevationGainMeters)
        assertFalse(success.hasElevation)
        assertTrue(success.hasTimestamps)
        assertEquals(1, savedRecords.size)
        assertTrue(File(routesRoot, "route-fixed-1/source_v1.gpx").exists())
        assertTrue(File(routesRoot, "route-fixed-1/geometry_v1.bin").exists())
    }

    @Test
    fun cleansUpStagingAndRollsBackFilesOnParseStorageOrDbFailure() = runBlocking {
        val routesRoot = tempFolder.newFolder("routes")
        val storage = VersionedRouteFileStorage(routesRoot)
        val failingRepo = object : RouteImportRepository {
            override suspend fun saveImportedRoute(record: ImportedRouteRecord) {
                throw IllegalStateException("DB failure")
            }
        }
        val useCase = ImportGpxUseCase(
            gpxParser = SafeXmlGpxParser(),
            routeFileStorage = storage,
            routeImportRepository = failingRepo,
            ioDispatcher = Dispatchers.Unconfined,
            idGenerator = { "route-fail-1" },
        )

        val validGpx = """
            <gpx version="1.1"><trk><trkseg>
              <trkpt lat="45.0" lon="11.0" />
              <trkpt lat="45.1" lon="11.1" />
            </trkseg></trk></gpx>
        """.trimIndent()
        val dbFailResult = useCase(InMemoryGpxSource("test.gpx", validGpx.toByteArray()))
        assertEquals(ImportGpxResult.Failure(GpxImportError.StorageFailed), dbFailResult)
        assertFalse(File(routesRoot, "route-fail-1").exists())

        val stagingDir = File(routesRoot, ".staging")
        assertTrue(stagingDir.listFiles().orEmpty().isEmpty())

        val unreadableSource = object : GpxInputSource {
            override val suggestedTitle = "secret_file_name.gpx"
            override val knownByteSize: Long? = null
            override fun openStream(): InputStream? = throw SecurityException("content://secret/path")
        }
        val readFailResult = useCase(unreadableSource)
        assertEquals(ImportGpxResult.Failure(GpxImportError.ReadFailed), readFailResult)

        val tooLargeMetadata = object : GpxInputSource {
            override val suggestedTitle = "huge.gpx"
            override val knownByteSize: Long = GpxImportLimits.MAX_FILE_BYTES + 1L
            override fun openStream(): InputStream? = error("Should fail fast from knownByteSize")
        }
        assertEquals(ImportGpxResult.Failure(GpxImportError.FileTooLarge), useCase(tooLargeMetadata))
    }

    @Test
    fun importErrorsRemainOpaqueAndNeverLeakSensitiveData() {
        val allErrors = listOf(
            GpxImportError.ReadFailed,
            GpxImportError.FileTooLarge,
            GpxImportError.UnsafeXml,
            GpxImportError.MalformedXml,
            GpxImportError.InvalidCoordinates,
            GpxImportError.InsufficientPoints,
            GpxImportError.TooManyPoints,
            GpxImportError.StorageFailed,
        )

        assertEquals(ImportErrorCode.entries.toSet(), allErrors.map { it.code }.toSet())
        allErrors.forEach { error ->
            val rendered = error.toString()
            assertEquals("GpxImportError(code=${error.code.name})", rendered)
            assertFalse(rendered.contains("<gpx", ignoreCase = true))
            assertFalse(rendered.contains("content://", ignoreCase = true))
            assertFalse(rendered.contains("lat", ignoreCase = true))
        }
    }

    @Test
    fun measuresFull50kPointImportPipelineAgainstNfr5Target() = runBlocking {
        val routesRoot = tempFolder.newFolder("routes")
        val storage = VersionedRouteFileStorage(routesRoot)
        val saved = mutableListOf<ImportedRouteRecord>()
        val repo = object : RouteImportRepository {
            override suspend fun saveImportedRoute(record: ImportedRouteRecord) {
                saved += record
            }
        }
        val useCase = ImportGpxUseCase(
            gpxParser = SafeXmlGpxParser(),
            routeFileStorage = storage,
            routeImportRepository = repo,
            ioDispatcher = Dispatchers.IO,
            idGenerator = { "route-50k-e2e" },
        )

        val bytes = SafeXmlGpxParserTest.build50kPointFixtureBytes(includeElevationAndTime = true)
        val startNs = System.nanoTime()
        val result = useCase(InMemoryGpxSource("fixture_50k.gpx", bytes))
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0

        assertTrue(result is ImportGpxResult.Success)
        assertEquals(1, saved.size)
        assertEquals(50_000, saved[0].parsedRoute.geometry.pointCount)
        assertTrue(File(routesRoot, "route-50k-e2e/source_v1.gpx").exists())
        assertTrue(File(routesRoot, "route-50k-e2e/geometry_v1.bin").exists())
        println("NFR-5 JVM ImportGpxUseCase 50,000-point pipeline elapsed=${elapsedMs}ms")
    }

    @Test
    fun failingStagingSessionMapsToStorageFailedAndDiscardsStaging() = runBlocking {
        var discarded = false
        val failingStaging = object : RouteStagingSession {
            override fun outputStream(): OutputStream = object : OutputStream() {
                override fun write(b: Int): Unit = throw IOException("Simulated disk full")
                override fun write(b: ByteArray, off: Int, len: Int): Unit = throw IOException("Simulated disk full")
            }

            override fun close() = Unit
            override fun discard() {
                discarded = true
            }
        }
        val storage = object : RouteFileStorage {
            override fun openStaging(): RouteStagingSession = failingStaging
            override suspend fun commitStagedRoute(
                routeId: String,
                staging: RouteStagingSession,
                geometry: NormalizedRouteGeometry,
            ): StoredRouteFiles = error("Should not commit")

            override suspend fun readGeometry(routeId: String): NormalizedRouteGeometry? = null
            override suspend fun deleteRoute(routeId: String) = Unit
        }
        val repo = object : RouteImportRepository {
            override suspend fun saveImportedRoute(record: ImportedRouteRecord) = error("Should not save")
        }
        val useCase = ImportGpxUseCase(
            gpxParser = SafeXmlGpxParser(),
            routeFileStorage = storage,
            routeImportRepository = repo,
            ioDispatcher = Dispatchers.Unconfined,
        )
        val gpx = "<gpx version=\"1.1\"><trk><trkseg><trkpt lat=\"45.0\" lon=\"11.0\"/><trkpt lat=\"45.1\" lon=\"11.1\"/></trkseg></trk></gpx>"

        val result = useCase(InMemoryGpxSource("staging_fail.gpx", gpx.toByteArray()))
        assertEquals(ImportGpxResult.Failure(GpxImportError.StorageFailed), result)
        assertTrue(discarded)
    }

    @Test
    fun enforcesMaxFileBytesWhenKnownByteSizeIsNullAndCleansUpOnCancellation() = runBlocking {
        val routesRoot = tempFolder.newFolder("routes")
        val storage = VersionedRouteFileStorage(routesRoot)
        val startedSave = CompletableDeferred<Unit>()
        val blocker = CompletableDeferred<Unit>()
        val repo = object : RouteImportRepository {
            override suspend fun saveImportedRoute(record: ImportedRouteRecord) {
                startedSave.complete(Unit)
                blocker.await()
            }
        }
        val useCase = ImportGpxUseCase(
            gpxParser = SafeXmlGpxParser(),
            routeFileStorage = storage,
            routeImportRepository = repo,
            ioDispatcher = Dispatchers.IO,
            idGenerator = { "route-cancel-1" },
        )

        val unknownSizeOverLimitSource = object : GpxInputSource {
            override val suggestedTitle: String? = null
            override val knownByteSize: Long? = null
            override fun openStream(): InputStream {
                val prefix = "<gpx version=\"1.1\"><trk><trkseg><trkpt lat=\"45.0\" lon=\"11.0\"/><trkpt lat=\"45.1\" lon=\"11.1\"/></trkseg></trk><desc>".toByteArray()
                val totalBytes = GpxImportLimits.MAX_FILE_BYTES + 1024L
                return object : InputStream() {
                    private var pos = 0L
                    override fun read(): Int {
                        if (pos >= totalBytes) return -1
                        val b = if (pos < prefix.size) prefix[pos.toInt()].toInt() and 0xFF else 'a'.code
                        pos += 1L
                        return b
                    }
                }
            }
        }
        assertEquals(ImportGpxResult.Failure(GpxImportError.FileTooLarge), useCase(unknownSizeOverLimitSource))
        assertTrue(File(routesRoot, ".staging").listFiles().orEmpty().isEmpty())

        val validGpx = "<gpx version=\"1.1\"><trk><trkseg><trkpt lat=\"45.0\" lon=\"11.0\"/><trkpt lat=\"45.1\" lon=\"11.1\"/></trkseg></trk></gpx>"
        val job = launch {
            useCase(InMemoryGpxSource("cancel.gpx", validGpx.toByteArray()))
        }
        startedSave.await()
        job.cancel(CancellationException("User cancelled"))
        job.join()

        assertTrue(job.isCancelled)
        assertFalse(File(routesRoot, "route-cancel-1").exists())
        assertTrue(File(routesRoot, ".staging").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun ignoresRawStreamCloseIoExceptionAndPreservesExistingDirectoryOnRouteIdCollision() = runBlocking {
        val routesRoot = tempFolder.newFolder("routes")
        val storage = VersionedRouteFileStorage(routesRoot)
        val saved = mutableListOf<ImportedRouteRecord>()
        val repo = object : RouteImportRepository {
            override suspend fun saveImportedRoute(record: ImportedRouteRecord) {
                saved += record
            }
        }
        val validGpx = "<gpx version=\"1.1\"><trk><name>Primo</name><trkseg><trkpt lat=\"45.0\" lon=\"11.0\"/><trkpt lat=\"45.1\" lon=\"11.1\"/></trkseg></trk></gpx>"
        var closeAttempted = false
        val closeFailingSource = object : GpxInputSource {
            override val suggestedTitle = "close_fail.gpx"
            override val knownByteSize: Long? = null
            override fun openStream(): InputStream = object : FilterInputStream(validGpx.byteInputStream()) {
                override fun close() {
                    closeAttempted = true
                    super.close()
                    throw IOException("Simulated stream close error")
                }
            }
        }
        val fixedIdUseCase = ImportGpxUseCase(
            gpxParser = SafeXmlGpxParser(),
            routeFileStorage = storage,
            routeImportRepository = repo,
            ioDispatcher = Dispatchers.Unconfined,
            idGenerator = { "collision-route" },
        )

        val firstResult = fixedIdUseCase(closeFailingSource)
        assertTrue(firstResult is ImportGpxResult.Success)
        assertTrue(closeAttempted)
        assertEquals(1, saved.size)
        val existingGpx = File(routesRoot, "collision-route/source_v1.gpx")
        val existingBin = File(routesRoot, "collision-route/geometry_v1.bin")
        val originalGpxContent = existingGpx.readText()
        assertTrue(existingGpx.exists() && existingBin.exists())

        val secondResult = fixedIdUseCase(InMemoryGpxSource("second.gpx", validGpx.toByteArray()))
        assertEquals(ImportGpxResult.Failure(GpxImportError.StorageFailed), secondResult)
        assertTrue("Collision must not delete existing route GPX", existingGpx.exists())
        assertTrue("Collision must not delete existing route binary", existingBin.exists())
        assertEquals(originalGpxContent, existingGpx.readText())
        assertTrue(File(routesRoot, ".staging").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun fallsBackToDefaultTitleForDotGpxOrNullAndAvoidsSplittingSurrogatePairsAtLimit() = runBlocking {
        val routesRoot = tempFolder.newFolder("routes")
        val storage = VersionedRouteFileStorage(routesRoot)
        val repo = object : RouteImportRepository {
            override suspend fun saveImportedRoute(record: ImportedRouteRecord) = Unit
        }
        var nextId = 0
        val useCase = ImportGpxUseCase(
            gpxParser = SafeXmlGpxParser(),
            routeFileStorage = storage,
            routeImportRepository = repo,
            ioDispatcher = Dispatchers.Unconfined,
            idGenerator = { "title-route-${++nextId}" },
            fallbackRouteTitle = "Percorso Predefinito",
        )

        val unnamedGpx = "<gpx version=\"1.1\"><trk><trkseg><trkpt lat=\"45.0\" lon=\"11.0\"/><trkpt lat=\"45.1\" lon=\"11.1\"/></trkseg></trk></gpx>"
        val dotGpxResult = useCase(InMemoryGpxSource(".gpx", unnamedGpx.toByteArray())) as ImportGpxResult.Success
        assertEquals("Percorso Predefinito", dotGpxResult.plan.title)

        val nullTitleResult = useCase(InMemoryGpxSource(null, unnamedGpx.toByteArray())) as ImportGpxResult.Success
        assertEquals("Percorso Predefinito", nullTitleResult.plan.title)

        val boundaryName = "a".repeat(159) + "\uD83D\uDDFB" + " coda"
        val surrogateGpx = "<gpx version=\"1.1\"><trk><name>$boundaryName</name><trkseg><trkpt lat=\"45.0\" lon=\"11.0\"/><trkpt lat=\"45.1\" lon=\"11.1\"/></trkseg></trk></gpx>"
        val truncatedResult = useCase(InMemoryGpxSource(null, surrogateGpx.toByteArray())) as ImportGpxResult.Success
        assertEquals(159, truncatedResult.plan.title.length)
        assertFalse(Character.isHighSurrogate(truncatedResult.plan.title.last()))
    }

    private class InMemoryGpxSource(
        override val suggestedTitle: String?,
        private val bytes: ByteArray,
    ) : GpxInputSource {
        override val knownByteSize: Long = bytes.size.toLong()
        override fun openStream(): InputStream = bytes.inputStream()
    }
}
