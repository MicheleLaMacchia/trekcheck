package it.trekcheck.app.domain.model

enum class RouteManagementErrorCode {
    NOT_FOUND,
    CORRUPTED_GEOMETRY,
    INVALID_TITLE,
    STORAGE_FAILED,
}

/** Point in local planar meters (x = east, y = north) relative to the route bounding box centre. */
data class ProjectedPoint(
    val x: Double,
    val y: Double,
)

data class RouteWaypointSummary(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double?,
    val distanceFromStartMeters: Double,
)

data class SimplifiedRoutePreview(
    val segments: List<List<ProjectedPoint>>,
    val minX: Double,
    val maxX: Double,
    val minY: Double,
    val maxY: Double,
    val start: ProjectedPoint,
    val end: ProjectedPoint,
    val highest: ProjectedPoint?,
) {
    val widthMeters: Double
        get() = maxX - minX

    val heightMeters: Double
        get() = maxY - minY
}

data class ElevationSample(
    val distanceMeters: Double,
    val elevationMeters: Double,
)

data class ElevationProfilePreview(
    val samples: List<ElevationSample>,
    val minElevationMeters: Double,
    val maxElevationMeters: Double,
    val totalDistanceMeters: Double,
    val runs: List<List<ElevationSample>> = if (samples.isEmpty()) emptyList() else listOf(samples),
)

data class RoutePreviewDetails(
    val routeId: String,
    val title: String,
    val distanceMeters: Double,
    val start: RouteWaypointSummary,
    val end: RouteWaypointSummary,
    val highestPoint: RouteWaypointSummary?,
    val minElevationMeters: Double?,
    val maxElevationMeters: Double?,
    val elevationGainMeters: Double?,
    val elevationLossMeters: Double?,
    val hasElevation: Boolean,
    val pointCount: Int,
    val segmentCount: Int,
    val track: SimplifiedRoutePreview,
    val profile: ElevationProfilePreview?,
)

/** Scalar metadata of a saved route, as persisted by the metadata store. */
data class RouteMetadataSnapshot(
    val routeId: String,
    val title: String,
    val hasElevation: Boolean,
    val elevationGainMeters: Double?,
    val elevationLossMeters: Double?,
    val planVersion: Long,
)

sealed interface RoutePreviewResult {
    data class Success(val details: RoutePreviewDetails) : RoutePreviewResult
    data class Failure(val code: RouteManagementErrorCode) : RoutePreviewResult
}

sealed interface RouteActionResult {
    data class Success(val title: String? = null) : RouteActionResult
    data class Failure(val code: RouteManagementErrorCode) : RouteActionResult
}
