package it.trekcheck.app.domain.model

object GpxImportLimits {
    const val MAX_FILE_BYTES: Long = 20L * 1024L * 1024L
    const val MAX_POINT_COUNT: Int = 200_000
    const val MIN_POINT_COUNT: Int = 2
    const val DEFAULT_ELEVATION_GAIN_THRESHOLD_METERS: Double = 3.0
}

data class GpxTrackPoint(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double? = null,
) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0) {
            "latitude must be finite and within [-90.0, 90.0]"
        }
        require(longitude.isFinite() && longitude in -180.0..180.0) {
            "longitude must be finite and within [-180.0, 180.0]"
        }
        elevationMeters?.let { elevation ->
            require(elevation.isFinite()) {
                "elevationMeters must be finite when present"
            }
        }
    }
}

data class GpxSegment(
    val points: List<GpxTrackPoint>,
) {
    init {
        require(points.isNotEmpty()) { "GpxSegment must contain at least one point" }
    }

    val startPoint: GpxTrackPoint
        get() = points.first()

    val endPoint: GpxTrackPoint
        get() = points.last()
}

data class GpxBoundingBox(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double,
) {
    init {
        require(minLatitude.isFinite() && maxLatitude.isFinite()) {
            "Bounding box latitudes must be finite"
        }
        require(minLongitude.isFinite() && maxLongitude.isFinite()) {
            "Bounding box longitudes must be finite"
        }
        require(minLatitude in -90.0..90.0 && maxLatitude in -90.0..90.0) {
            "Bounding box latitudes must be within [-90.0, 90.0]"
        }
        require(minLongitude in -180.0..180.0 && maxLongitude in -180.0..180.0) {
            "Bounding box longitudes must be within [-180.0, 180.0]"
        }
        require(minLatitude <= maxLatitude) {
            "minLatitude must be <= maxLatitude"
        }
        require(minLongitude <= maxLongitude) {
            "minLongitude must be <= maxLongitude"
        }
    }
}

enum class GpxSourceKind {
    TRACK,
    ROUTE,
    MIXED,
}

data class NormalizedRouteGeometry(
    val segments: List<GpxSegment>,
) {
    init {
        require(segments.isNotEmpty()) { "NormalizedRouteGeometry must contain at least one segment" }
        require(pointCount >= GpxImportLimits.MIN_POINT_COUNT) {
            "NormalizedRouteGeometry must contain at least ${GpxImportLimits.MIN_POINT_COUNT} points"
        }
    }

    val pointCount: Int
        get() = segments.sumOf { it.points.size }

    val segmentCount: Int
        get() = segments.size

    val startPoint: GpxTrackPoint
        get() = segments.first().startPoint

    val endPoint: GpxTrackPoint
        get() = segments.last().endPoint
}

data class ParsedGpxRoute(
    val routeName: String?,
    val sourceKind: GpxSourceKind,
    val geometry: NormalizedRouteGeometry,
    val distanceMeters: Double,
    val elevationGainMeters: Double?,
    val elevationLossMeters: Double?,
    val minElevationMeters: Double?,
    val maxElevationMeters: Double?,
    val boundingBox: GpxBoundingBox,
    val hasElevation: Boolean,
    val hasTimestamps: Boolean,
) {
    init {
        require(distanceMeters.isFinite() && distanceMeters >= 0.0) {
            "distanceMeters must be finite and non-negative"
        }
        elevationGainMeters?.let { gain ->
            require(gain.isFinite() && gain >= 0.0) {
                "elevationGainMeters must be finite and non-negative"
            }
        }
        elevationLossMeters?.let { loss ->
            require(loss.isFinite() && loss >= 0.0) {
                "elevationLossMeters must be finite and non-negative"
            }
        }
        require((minElevationMeters == null) == (maxElevationMeters == null)) {
            "minElevationMeters and maxElevationMeters must both be present or both null"
        }
        require(hasElevation == (minElevationMeters != null && maxElevationMeters != null)) {
            "hasElevation must match presence of minElevationMeters and maxElevationMeters"
        }
        require((elevationGainMeters == null) == (elevationLossMeters == null)) {
            "elevationGainMeters and elevationLossMeters must both be present or both null"
        }
        require(hasElevation || (elevationGainMeters == null && elevationLossMeters == null)) {
            "Elevation metrics must be null when hasElevation is false"
        }
        if (minElevationMeters != null && maxElevationMeters != null) {
            require(minElevationMeters.isFinite() && maxElevationMeters.isFinite()) {
                "Elevation extrema must be finite"
            }
            require(minElevationMeters <= maxElevationMeters) {
                "minElevationMeters must be <= maxElevationMeters"
            }
        }
    }
}
