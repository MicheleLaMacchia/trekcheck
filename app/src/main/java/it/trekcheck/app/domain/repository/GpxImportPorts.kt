package it.trekcheck.app.domain.repository

import it.trekcheck.app.domain.model.GpxImportLimits
import it.trekcheck.app.domain.model.GpxParseResult
import it.trekcheck.app.domain.model.NormalizedRouteGeometry
import it.trekcheck.app.domain.model.ParsedGpxRoute
import it.trekcheck.app.domain.model.SavedPlanSummary
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream

interface GpxInputSource {
    val suggestedTitle: String?
    val knownByteSize: Long?
    fun openStream(): InputStream?
}

interface GpxParser {
    fun parse(
        inputStream: InputStream,
        maxBytes: Long = GpxImportLimits.MAX_FILE_BYTES,
        maxPoints: Int = GpxImportLimits.MAX_POINT_COUNT,
    ): GpxParseResult
}

interface RouteStagingSession : Closeable {
    fun outputStream(): OutputStream
    fun discard()
}

data class StoredRouteFiles(
    val routeId: String,
    val gpxFileName: String,
    val gpxVersion: Int,
    val geometryFileName: String,
    val geometryVersion: Int,
    val gpxByteCount: Long,
)

interface RouteFileStorage {
    fun openStaging(): RouteStagingSession

    suspend fun commitStagedRoute(
        routeId: String,
        staging: RouteStagingSession,
        geometry: NormalizedRouteGeometry,
    ): StoredRouteFiles

    suspend fun readGeometry(routeId: String): NormalizedRouteGeometry?

    suspend fun deleteRoute(routeId: String)
}

data class ImportedRouteRecord(
    val summary: SavedPlanSummary,
    val parsedRoute: ParsedGpxRoute,
    val storedFiles: StoredRouteFiles,
)

interface RouteImportRepository {
    suspend fun saveImportedRoute(record: ImportedRouteRecord)
}
