package it.trekcheck.app.data.local.files

import it.trekcheck.app.domain.model.GpxImportLimits
import it.trekcheck.app.domain.model.GpxSegment
import it.trekcheck.app.domain.model.GpxTrackPoint
import it.trekcheck.app.domain.model.NormalizedRouteGeometry
import it.trekcheck.app.domain.repository.RouteFileStorage
import it.trekcheck.app.domain.repository.RouteStagingSession
import it.trekcheck.app.domain.repository.StoredRouteFiles
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

class VersionedRouteFileStorage(
    private val routesRootDir: File,
    private val beforeGeometryMoveHook: (() -> Unit)? = null,
) : RouteFileStorage {

    override fun openStaging(): RouteStagingSession {
        val stagingDir = ensureStagingDir()
        val tempGpxFile = File(stagingDir, "stage-${UUID.randomUUID()}.gpx.tmp")
        return FileRouteStagingSession(tempGpxFile)
    }

    override suspend fun commitStagedRoute(
        routeId: String,
        staging: RouteStagingSession,
        geometry: NormalizedRouteGeometry,
    ): StoredRouteFiles {
        validateRouteId(routeId)
        val fileStaging = staging as? FileRouteStagingSession
            ?: throw IllegalArgumentException("Unsupported staging session type")
        if (fileStaging.isDiscarded) {
            throw IllegalStateException("Staging session has already been discarded")
        }

        val stagingDir = ensureStagingDir()

        val routeDir = File(routesRootDir, routeId)
        if (routeDir.exists()) {
            throw IOException("Route directory already exists")
        }

        val tempGeometryFile = File(stagingDir, "geom-$routeId-${UUID.randomUUID()}.bin.tmp")
        var createdRouteDir = false

        try {
            val gpxByteCount = fileStaging.finishWriting()
            if (!fileStaging.file.exists() || gpxByteCount <= 0L) {
                throw IOException("Staged GPX file is empty or missing")
            }

            writeGeometryBinary(tempGeometryFile, geometry)

            if (!routeDir.mkdirs()) {
                throw IOException("Unable to create route directory")
            }
            createdRouteDir = true

            val targetGpxFile = File(routeDir, GPX_FILE_NAME)
            moveFile(fileStaging.file, targetGpxFile)

            beforeGeometryMoveHook?.invoke()

            val targetGeometryFile = File(routeDir, GEOMETRY_FILE_NAME)
            moveFile(tempGeometryFile, targetGeometryFile)

            return StoredRouteFiles(
                routeId = routeId,
                gpxFileName = GPX_FILE_NAME,
                gpxVersion = GPX_VERSION,
                geometryFileName = GEOMETRY_FILE_NAME,
                geometryVersion = GEOMETRY_VERSION,
                gpxByteCount = gpxByteCount,
            )
        } catch (throwable: Throwable) {
            tempGeometryFile.delete()
            fileStaging.discard()
            if (createdRouteDir) {
                routeDir.deleteRecursively()
            }
            throw throwable
        }
    }

    override suspend fun readGeometry(routeId: String): NormalizedRouteGeometry? {
        validateRouteId(routeId)
        val geometryFile = File(File(routesRootDir, routeId), GEOMETRY_FILE_NAME)
        if (!geometryFile.exists()) return null

        DataInputStream(BufferedInputStream(FileInputStream(geometryFile), IO_BUFFER_BYTES)).use { input ->
            val magic = ByteArray(GEOMETRY_MAGIC.size)
            input.readFully(magic)
            if (!magic.contentEquals(GEOMETRY_MAGIC)) {
                throw IOException("Invalid geometry magic header")
            }
            val version = input.readInt()
            if (version != GEOMETRY_VERSION) {
                throw IOException("Unsupported geometry version")
            }
            val segmentCount = input.readInt()
            if (segmentCount !in 1..GpxImportLimits.MAX_POINT_COUNT) {
                throw IOException("Invalid segment count")
            }

            val segments = ArrayList<GpxSegment>(minOf(segmentCount, READ_PREALLOC_SEGMENT_CAP))
            var totalPoints = 0

            for (segmentIdx in 0 until segmentCount) {
                val remainingCapacity = GpxImportLimits.MAX_POINT_COUNT - totalPoints
                val pointCount = input.readInt()
                if (pointCount !in 1..remainingCapacity) {
                    throw IOException("Invalid segment point count")
                }
                totalPoints += pointCount
                val points = ArrayList<GpxTrackPoint>(minOf(pointCount, READ_PREALLOC_POINT_CAP))
                for (pointIdx in 0 until pointCount) {
                    val lat = input.readDouble()
                    val lon = input.readDouble()
                    val hasElevation = input.readBoolean()
                    val ele = if (hasElevation) input.readDouble() else null
                    try {
                        points.add(GpxTrackPoint(latitude = lat, longitude = lon, elevationMeters = ele))
                    } catch (iae: IllegalArgumentException) {
                        throw IOException("Corrupt point data in geometry file", iae)
                    }
                }
                try {
                    segments.add(GpxSegment(points))
                } catch (iae: IllegalArgumentException) {
                    throw IOException("Corrupt segment data in geometry file", iae)
                }
            }
            if (input.read() != -1) {
                throw IOException("Unexpected trailing bytes in geometry file")
            }
            return try {
                NormalizedRouteGeometry(segments)
            } catch (iae: IllegalArgumentException) {
                throw IOException("Corrupt geometry metadata in geometry file", iae)
            }
        }
    }

    private fun ensureStagingDir(): File {
        val stagingDir = File(routesRootDir, STAGING_DIR_NAME)
        if (!stagingDir.mkdirs() && !stagingDir.isDirectory) {
            throw IOException("Unable to create staging directory")
        }
        return stagingDir
    }

    override suspend fun deleteRoute(routeId: String) {
        validateRouteId(routeId)
        val routeDir = File(routesRootDir, routeId)
        if (routeDir.exists() && !routeDir.deleteRecursively()) {
            throw IOException("Unable to delete route directory")
        }
    }

    fun routeDirectory(routeId: String): File {
        validateRouteId(routeId)
        return File(routesRootDir, routeId)
    }

    private fun writeGeometryBinary(targetFile: File, geometry: NormalizedRouteGeometry) {
        DataOutputStream(BufferedOutputStream(FileOutputStream(targetFile), IO_BUFFER_BYTES)).use { output ->
            output.write(GEOMETRY_MAGIC)
            output.writeInt(GEOMETRY_VERSION)
            output.writeInt(geometry.segments.size)
            for (segment in geometry.segments) {
                output.writeInt(segment.points.size)
                for (point in segment.points) {
                    output.writeDouble(point.latitude)
                    output.writeDouble(point.longitude)
                    val elevation = point.elevationMeters
                    if (elevation != null) {
                        output.writeBoolean(true)
                        output.writeDouble(elevation)
                    } else {
                        output.writeBoolean(false)
                    }
                }
            }
            output.flush()
        }
    }

    private fun moveFile(source: File, destination: File) {
        try {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: IOException) {
            source.inputStream().use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output, IO_BUFFER_BYTES)
                }
            }
            source.delete()
        }
    }

    private fun validateRouteId(routeId: String) {
        require(SAFE_ROUTE_ID_REGEX.matches(routeId)) {
            "routeId contains unsupported characters"
        }
    }

    private class FileRouteStagingSession(
        val file: File,
    ) : RouteStagingSession {
        private var countingStream: CountingOutputStream? = null
        private var closed = false
        var isDiscarded = false
            private set

        override fun outputStream(): OutputStream {
            check(!isDiscarded) { "Staging session already discarded" }
            check(!closed) { "Staging session already closed" }
            return countingStream ?: CountingOutputStream(
                BufferedOutputStream(FileOutputStream(file), IO_BUFFER_BYTES),
            ).also { countingStream = it }
        }

        fun finishWriting(): Long {
            check(!isDiscarded) { "Staging session already discarded" }
            val stream = countingStream
            if (stream != null && !closed) {
                stream.flush()
                stream.close()
                closed = true
            }
            return stream?.bytesWritten ?: file.length()
        }

        override fun close() {
            val stream = countingStream
            if (stream != null && !closed) {
                try {
                    stream.close()
                } catch (_: IOException) {
                }
                closed = true
            }
        }

        override fun discard() {
            isDiscarded = true
            close()
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private class CountingOutputStream(delegate: OutputStream) : FilterOutputStream(delegate) {
        var bytesWritten: Long = 0L
            private set

        override fun write(b: Int) {
            out.write(b)
            bytesWritten += 1L
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            bytesWritten += len.toLong()
        }
    }

    companion object {
        const val ROUTES_DIR_NAME = "routes"
        const val GPX_FILE_NAME = "source_v1.gpx"
        const val GPX_VERSION = 1
        const val GEOMETRY_FILE_NAME = "geometry_v1.bin"
        const val GEOMETRY_VERSION = 1

        private const val STAGING_DIR_NAME = ".staging"
        private const val IO_BUFFER_BYTES = 64 * 1024
        private const val READ_PREALLOC_SEGMENT_CAP = 1_024
        private const val READ_PREALLOC_POINT_CAP = 4_096
        private val GEOMETRY_MAGIC = byteArrayOf(
            'T'.code.toByte(),
            'C'.code.toByte(),
            'G'.code.toByte(),
            'M'.code.toByte(),
        )
        private val SAFE_ROUTE_ID_REGEX = Regex("^[A-Za-z0-9_-]{1,128}$")
    }
}
