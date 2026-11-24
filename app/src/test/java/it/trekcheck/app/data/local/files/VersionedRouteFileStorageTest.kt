package it.trekcheck.app.data.local.files

import it.trekcheck.app.domain.model.GpxImportLimits
import it.trekcheck.app.domain.model.GpxSegment
import it.trekcheck.app.domain.model.GpxTrackPoint
import it.trekcheck.app.domain.model.NormalizedRouteGeometry
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VersionedRouteFileStorageTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun commitsVersionedGpxAndBinaryGeometryPreservingSegmentsAndNullElevations() = runBlocking {
        val routesRoot = tempFolder.newFolder("routes")
        val storage = VersionedRouteFileStorage(routesRoot)

        val gpxText = "<gpx version=\"1.1\"><trk><trkseg><trkpt lat=\"45.1\" lon=\"11.1\"/></trkseg></trk></gpx>"
        val geometry = NormalizedRouteGeometry(
            segments = listOf(
                GpxSegment(
                    listOf(
                        GpxTrackPoint(45.1, 11.1, 1200.5),
                        GpxTrackPoint(45.2, 11.2, null),
                    ),
                ),
                GpxSegment(
                    listOf(
                        GpxTrackPoint(45.5, 11.5, 1450.0),
                    ),
                ),
            ),
        )

        val staging = storage.openStaging()
        staging.outputStream().use { out ->
            out.write(gpxText.toByteArray(Charsets.UTF_8))
        }

        val ref = storage.commitStagedRoute(
            routeId = "route-123",
            staging = staging,
            geometry = geometry,
        )

        assertEquals("source_v1.gpx", ref.gpxFileName)
        assertEquals(1, ref.gpxVersion)
        assertEquals("geometry_v1.bin", ref.geometryFileName)
        assertEquals(1, ref.geometryVersion)
        assertEquals(gpxText.toByteArray(Charsets.UTF_8).size.toLong(), ref.gpxByteCount)

        val routeDir = File(routesRoot, "route-123")
        val gpxFile = File(routeDir, "source_v1.gpx")
        val binFile = File(routeDir, "geometry_v1.bin")
        assertTrue(gpxFile.exists())
        assertTrue(binFile.exists())
        assertEquals(gpxText, gpxFile.readText(Charsets.UTF_8))

        DataInputStream(binFile.inputStream()).use { data ->
            val magic = ByteArray(4)
            data.readFully(magic)
            assertArrayEquals("TCGM".toByteArray(Charsets.US_ASCII), magic)
            assertEquals(1, data.readInt())
            assertEquals(2, data.readInt())
        }

        val restored = storage.readGeometry("route-123")
        assertEquals(geometry, restored)
    }

    @Test
    fun rollsBackPartialCommitOnErrorWithoutTouchingExistingRoutes() = runBlocking {
        val routesRoot = tempFolder.newFolder("routes")
        val existingStorage = VersionedRouteFileStorage(routesRoot)

        val existingGeometry = NormalizedRouteGeometry(
            segments = listOf(
                GpxSegment(
                    listOf(
                        GpxTrackPoint(45.0, 11.0, 500.0),
                        GpxTrackPoint(45.01, 11.01, 550.0),
                    ),
                ),
            ),
        )
        val existingStaging = existingStorage.openStaging()
        existingStaging.outputStream().use { it.write("<gpx/>".toByteArray()) }
        existingStorage.commitStagedRoute("existing-route", existingStaging, existingGeometry)

        val failingStorage = VersionedRouteFileStorage(
            routesRootDir = routesRoot,
            beforeGeometryMoveHook = { throw IOException("Simulated disk error during commit") },
        )
        val failingStaging = failingStorage.openStaging()
        failingStaging.outputStream().use { it.write("<gpx/>".toByteArray()) }

        var thrown: Throwable? = null
        try {
            failingStorage.commitStagedRoute("failed-route", failingStaging, existingGeometry)
        } catch (t: Throwable) {
            thrown = t
        }
        assertNotNull(thrown)

        assertFalse(File(routesRoot, "failed-route").exists())
        assertNull(failingStorage.readGeometry("failed-route"))
        assertEquals(existingGeometry, failingStorage.readGeometry("existing-route"))
        assertTrue(File(routesRoot, "existing-route/source_v1.gpx").exists())
        assertTrue(File(routesRoot, "existing-route/geometry_v1.bin").exists())
    }

    @Test
    fun rejectsUnsafeRouteIdsAndDeletesExistingRouteFiles() {
        runBlocking {
            val routesRoot = tempFolder.newFolder("routes")
            val storage = VersionedRouteFileStorage(routesRoot)
            val geometry = NormalizedRouteGeometry(
                segments = listOf(
                    GpxSegment(
                        listOf(
                            GpxTrackPoint(45.0, 11.0, null),
                            GpxTrackPoint(45.1, 11.1, null),
                        ),
                    ),
                ),
            )

            val staging = storage.openStaging()
            staging.outputStream().use { it.write("<gpx/>".toByteArray()) }
            storage.commitStagedRoute("route-to-delete", staging, geometry)
            assertTrue(File(routesRoot, "route-to-delete").exists())

            storage.deleteRoute("route-to-delete")
            assertFalse(File(routesRoot, "route-to-delete").exists())

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { storage.readGeometry("../escape") }
            }
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { storage.deleteRoute("nested/path") }
            }
        }
    }

    @Test
    fun binaryFormatStores50kPointsInCompactSizeAndReadsBackFast() = runBlocking {
        val routesRoot = tempFolder.newFolder("routes")
        val storage = VersionedRouteFileStorage(routesRoot)

        val points = List(50_000) { i ->
            GpxTrackPoint(45.0 + i * 0.00001, 11.0 + i * 0.00001, 1000.0 + (i % 300))
        }
        val geometry = NormalizedRouteGeometry(listOf(GpxSegment(points)))
        val staging = storage.openStaging()
        staging.outputStream().use { it.write("<gpx/>".toByteArray()) }

        val startWriteNs = System.nanoTime()
        storage.commitStagedRoute("route-50k", staging, geometry)
        val writeMs = (System.nanoTime() - startWriteNs) / 1_000_000.0

        val binFile = File(routesRoot, "route-50k/geometry_v1.bin")
        // 12 byte header + 4 byte pointCount + 50,000 * (8+8+1+8) = 1,250,016 bytes (~1.25 MB)
        assertEquals(1_250_016L, binFile.length())

        val startReadNs = System.nanoTime()
        val restored = storage.readGeometry("route-50k")
        val readMs = (System.nanoTime() - startReadNs) / 1_000_000.0

        assertEquals(50_000, restored?.pointCount)
        println("VersionedRouteFileStorage 50k write=${writeMs}ms read=${readMs}ms bytes=${binFile.length()}")
    }

    @Test
    fun readGeometryRejectsCorruptTruncatedOversizedOverflowAndTrailingBytesFiles() {
        val routesRoot = tempFolder.newFolder("routes")
        val storage = VersionedRouteFileStorage(routesRoot)

        fun buildBinary(block: DataOutputStream.() -> Unit): ByteArray {
            val out = ByteArrayOutputStream()
            DataOutputStream(out).use(block)
            return out.toByteArray()
        }

        val samples = mapOf(
            "bad-magic" to buildBinary {
                write("NOPE".toByteArray(Charsets.US_ASCII))
                writeInt(1)
            },
            "bad-version" to buildBinary {
                write("TCGM".toByteArray(Charsets.US_ASCII))
                writeInt(2)
            },
            "truncated" to buildBinary {
                write("TCGM".toByteArray(Charsets.US_ASCII))
                writeInt(1)
                writeInt(1)
                writeInt(2)
                writeDouble(45.0)
            },
            "huge-segment-count" to buildBinary {
                write("TCGM".toByteArray(Charsets.US_ASCII))
                writeInt(1)
                writeInt(Int.MAX_VALUE)
            },
            "overflow-point-count" to buildBinary {
                write("TCGM".toByteArray(Charsets.US_ASCII))
                writeInt(1)
                writeInt(2)
                writeInt(1)
                writeDouble(45.0)
                writeDouble(11.0)
                writeBoolean(false)
                writeInt(Int.MAX_VALUE)
            },
            "exceeds-remaining-capacity" to buildBinary {
                write("TCGM".toByteArray(Charsets.US_ASCII))
                writeInt(1)
                writeInt(2)
                writeInt(1)
                writeDouble(45.0)
                writeDouble(11.0)
                writeBoolean(false)
                writeInt(GpxImportLimits.MAX_POINT_COUNT)
            },
            "corrupt-coords" to buildBinary {
                write("TCGM".toByteArray(Charsets.US_ASCII))
                writeInt(1)
                writeInt(1)
                writeInt(2)
                writeDouble(95.0)
                writeDouble(11.0)
                writeBoolean(false)
                writeDouble(45.1)
                writeDouble(11.1)
                writeBoolean(false)
            },
            "single-point-geometry" to buildBinary {
                write("TCGM".toByteArray(Charsets.US_ASCII))
                writeInt(1)
                writeInt(1)
                writeInt(1)
                writeDouble(45.0)
                writeDouble(11.0)
                writeBoolean(false)
            },
            "trailing-bytes" to buildBinary {
                write("TCGM".toByteArray(Charsets.US_ASCII))
                writeInt(1)
                writeInt(1)
                writeInt(2)
                writeDouble(45.0)
                writeDouble(11.0)
                writeBoolean(false)
                writeDouble(45.1)
                writeDouble(11.1)
                writeBoolean(false)
                writeByte(0x7F)
            },
        )

        for ((routeId, payload) in samples) {
            val routeDir = File(routesRoot, routeId).apply { mkdirs() }
            File(routeDir, VersionedRouteFileStorage.GEOMETRY_FILE_NAME).writeBytes(payload)
            assertThrows("Expected IOException for corrupt sample '$routeId'", IOException::class.java) {
                runBlocking { storage.readGeometry(routeId) }
            }
        }
    }

    @Test
    fun deleteRouteThrowsIoExceptionWhenDirectoryDeletionFails() = runBlocking {
        val routesRoot = tempFolder.newFolder("routes")
        val storage = VersionedRouteFileStorage(routesRoot)
        val geometry = NormalizedRouteGeometry(
            listOf(GpxSegment(listOf(GpxTrackPoint(45.0, 11.0, 500.0), GpxTrackPoint(45.01, 11.01, 550.0)))),
        )
        val staging = storage.openStaging()
        staging.outputStream().use { it.write("<gpx/>".toByteArray()) }
        storage.commitStagedRoute("locked-route", staging, geometry)

        val routeDir = File(routesRoot, "locked-route")
        assertTrue(routeDir.setWritable(false))
        try {
            var thrown: IOException? = null
            try {
                storage.deleteRoute("locked-route")
            } catch (io: IOException) {
                thrown = io
            }
            assertNotNull(thrown)
        } finally {
            routeDir.setWritable(true)
        }
    }
}
