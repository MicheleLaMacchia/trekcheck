package it.trekcheck.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import it.trekcheck.app.domain.model.SavedPlanDataAvailability
import it.trekcheck.app.domain.model.SavedPlanSummary
import java.time.Instant
import java.time.ZoneId

@Entity(tableName = "route_metadata")
data class RouteMetadataEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val distanceMeters: Double,
    val elevationGainMeters: Double?,
    val elevationLossMeters: Double?,
    val minElevationMeters: Double?,
    val maxElevationMeters: Double?,
    val startLatitude: Double,
    val startLongitude: Double,
    val endLatitude: Double,
    val endLongitude: Double,
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double,
    val pointCount: Int,
    val segmentCount: Int,
    val hasElevation: Boolean,
    val hasTimestamps: Boolean,
    val departureInstantUtcEpochMillis: Long?,
    val routeZoneId: String,
    val estimatedDurationSeconds: Long?,
    val updatedAtUtcEpochMillis: Long,
    val dataAvailability: String,
    val gpxFileName: String,
    val gpxVersion: Int,
    val geometryFileName: String,
    val geometryVersion: Int,
    val gpxByteCount: Long,
    val planVersion: Long,
) {
    fun toSavedPlanSummary(): SavedPlanSummary {
        val resolvedZone = runCatching { ZoneId.of(routeZoneId) }
            .getOrElse { ZoneId.of("UTC") }
        val resolvedAvailability = SavedPlanDataAvailability.entries
            .firstOrNull { it.name == dataAvailability }
            ?: SavedPlanDataAvailability.ROUTE_ONLY

        return SavedPlanSummary(
            id = id,
            title = title,
            distanceMeters = distanceMeters,
            elevationGainMeters = elevationGainMeters,
            departureInstantUtc = departureInstantUtcEpochMillis?.let(Instant::ofEpochMilli),
            routeZoneId = resolvedZone,
            estimatedDurationSeconds = estimatedDurationSeconds,
            updatedAtUtc = Instant.ofEpochMilli(updatedAtUtcEpochMillis),
            dataAvailability = resolvedAvailability,
            hasElevation = hasElevation,
            hasTimestamps = hasTimestamps,
        )
    }
}
