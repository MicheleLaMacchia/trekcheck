package it.trekcheck.app.data.local.db

import it.trekcheck.app.domain.model.SavedPlanSummary
import it.trekcheck.app.domain.repository.ImportedRouteRecord
import it.trekcheck.app.domain.repository.LocalPlanRepository
import it.trekcheck.app.domain.repository.RouteFileStorage
import it.trekcheck.app.domain.repository.RouteImportRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomLocalPlanRepository(
    private val routeMetadataDao: RouteMetadataDao,
    private val routeFileStorage: RouteFileStorage,
) : LocalPlanRepository, RouteImportRepository {

    override fun observeSavedPlans(): Flow<List<SavedPlanSummary>> =
        routeMetadataDao.observeAll().map { entities ->
            entities.map { it.toSavedPlanSummary() }
        }

    override suspend fun saveImportedRoute(record: ImportedRouteRecord) {
        val entity = record.toEntity()
        try {
            routeMetadataDao.insert(entity)
        } catch (cancellation: CancellationException) {
            withContext(NonCancellable) {
                runCatching { routeMetadataDao.deleteById(entity.id) }
                cleanupFilesSafely(record.storedFiles.routeId)
            }
            throw cancellation
        } catch (throwable: Throwable) {
            cleanupFilesSafely(record.storedFiles.routeId)
            throw throwable
        }
    }

    private suspend fun cleanupFilesSafely(routeId: String) {
        withContext(NonCancellable) {
            try {
                routeFileStorage.deleteRoute(routeId)
            } catch (_: Throwable) {
            }
        }
    }

    private fun ImportedRouteRecord.toEntity(): RouteMetadataEntity {
        val start = parsedRoute.geometry.startPoint
        val end = parsedRoute.geometry.endPoint
        val bbox = parsedRoute.boundingBox
        return RouteMetadataEntity(
            id = summary.id,
            title = summary.title,
            distanceMeters = summary.distanceMeters,
            elevationGainMeters = summary.elevationGainMeters,
            elevationLossMeters = parsedRoute.elevationLossMeters,
            minElevationMeters = parsedRoute.minElevationMeters,
            maxElevationMeters = parsedRoute.maxElevationMeters,
            startLatitude = start.latitude,
            startLongitude = start.longitude,
            endLatitude = end.latitude,
            endLongitude = end.longitude,
            minLatitude = bbox.minLatitude,
            maxLatitude = bbox.maxLatitude,
            minLongitude = bbox.minLongitude,
            maxLongitude = bbox.maxLongitude,
            pointCount = parsedRoute.geometry.pointCount,
            segmentCount = parsedRoute.geometry.segmentCount,
            hasElevation = parsedRoute.hasElevation,
            hasTimestamps = parsedRoute.hasTimestamps,
            departureInstantUtcEpochMillis = summary.departureInstantUtc?.toEpochMilli(),
            routeZoneId = summary.routeZoneId.id,
            estimatedDurationSeconds = summary.estimatedDurationSeconds,
            updatedAtUtcEpochMillis = summary.updatedAtUtc.toEpochMilli(),
            dataAvailability = summary.dataAvailability.name,
            gpxFileName = storedFiles.gpxFileName,
            gpxVersion = storedFiles.gpxVersion,
            geometryFileName = storedFiles.geometryFileName,
            geometryVersion = storedFiles.geometryVersion,
            gpxByteCount = storedFiles.gpxByteCount,
            planVersion = 1L,
        )
    }
}
