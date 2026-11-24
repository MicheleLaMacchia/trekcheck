package it.trekcheck.app.domain.usecase

import it.trekcheck.app.domain.geo.RouteGeometrySimplifier
import it.trekcheck.app.domain.model.NormalizedRouteGeometry
import it.trekcheck.app.domain.model.RouteManagementErrorCode
import it.trekcheck.app.domain.model.RoutePreviewDetails
import it.trekcheck.app.domain.model.RoutePreviewResult
import it.trekcheck.app.domain.repository.RouteFileStorage
import it.trekcheck.app.domain.repository.RouteManagementRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoadRoutePreviewUseCase(
    private val routeManagementRepository: RouteManagementRepository,
    private val routeFileStorage: RouteFileStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(routeId: String): RoutePreviewResult = withContext(ioDispatcher) {
        val metadata = try {
            routeManagementRepository.getRouteMetadata(routeId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            return@withContext RoutePreviewResult.Failure(RouteManagementErrorCode.STORAGE_FAILED)
        } ?: return@withContext RoutePreviewResult.Failure(RouteManagementErrorCode.NOT_FOUND)

        val geometry: NormalizedRouteGeometry = try {
            routeFileStorage.readGeometry(routeId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: IOException) {
            return@withContext RoutePreviewResult.Failure(RouteManagementErrorCode.CORRUPTED_GEOMETRY)
        } catch (_: Throwable) {
            return@withContext RoutePreviewResult.Failure(RouteManagementErrorCode.CORRUPTED_GEOMETRY)
        } ?: return@withContext RoutePreviewResult.Failure(RouteManagementErrorCode.NOT_FOUND)

        try {
            val analysis = RouteGeometrySimplifier.analyze(geometry)
            val track = RouteGeometrySimplifier.simplify(geometry, analysis)
            val hasElevation = analysis.minElevationMeters != null
            val profile = if (hasElevation && analysis.elevationGainMeters != null) {
                RouteGeometrySimplifier.sampleProfile(geometry)
            } else {
                null
            }
            RoutePreviewResult.Success(
                RoutePreviewDetails(
                    routeId = routeId,
                    title = metadata.title,
                    distanceMeters = analysis.distanceMeters,
                    start = analysis.start,
                    end = analysis.end,
                    highestPoint = if (hasElevation) analysis.highest else null,
                    minElevationMeters = if (hasElevation) analysis.minElevationMeters else null,
                    maxElevationMeters = if (hasElevation) analysis.maxElevationMeters else null,
                    elevationGainMeters = analysis.elevationGainMeters,
                    elevationLossMeters = analysis.elevationLossMeters,
                    hasElevation = hasElevation,
                    pointCount = geometry.pointCount,
                    segmentCount = geometry.segmentCount,
                    track = track,
                    profile = profile,
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            RoutePreviewResult.Failure(RouteManagementErrorCode.CORRUPTED_GEOMETRY)
        }
    }
}
