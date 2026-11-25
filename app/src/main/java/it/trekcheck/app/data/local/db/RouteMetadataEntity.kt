package it.trekcheck.app.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import it.trekcheck.app.domain.model.PaceProfile
import it.trekcheck.app.domain.model.PaceProfileType
import it.trekcheck.app.domain.model.RouteMetadataSnapshot
import it.trekcheck.app.domain.model.RoutePlanConfig
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
    @ColumnInfo(defaultValue = "'NORMAL'")
    val paceProfileType: String = PaceProfileType.NORMAL.name,
    @ColumnInfo(defaultValue = "NULL")
    val customFlatSpeedMps: Double? = null,
) {
    fun toSavedPlanSummary(): SavedPlanSummary {
        val resolvedZone = resolveZoneId()
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
            paceProfile = resolvePaceProfile(),
        )
    }

    fun toRouteMetadataSnapshot(): RouteMetadataSnapshot = RouteMetadataSnapshot(
        routeId = id,
        title = title,
        hasElevation = hasElevation,
        elevationGainMeters = elevationGainMeters,
        elevationLossMeters = elevationLossMeters,
        planVersion = planVersion,
        departureInstantUtc = departureInstantUtcEpochMillis?.let(Instant::ofEpochMilli),
        routeZoneId = resolveZoneId(),
        paceProfile = resolvePaceProfile(),
    )

    fun toRoutePlanConfig(): RoutePlanConfig = RoutePlanConfig(
        routeId = id,
        routeTitle = title,
        hasElevation = hasElevation,
        hasTimestamps = hasTimestamps,
        departureInstantUtc = departureInstantUtcEpochMillis?.let(Instant::ofEpochMilli),
        routeZoneId = resolveZoneId(),
        paceProfile = resolvePaceProfile(),
        planVersion = planVersion.coerceAtLeast(1L),
    )

    private fun resolveZoneId(): ZoneId =
        runCatching { ZoneId.of(routeZoneId) }.getOrElse { ZoneId.of("UTC") }

    private fun resolvePaceProfile(): PaceProfile =
        resolvePaceProfile(paceProfileType, customFlatSpeedMps)

    companion object {
        fun resolvePaceProfile(
            paceProfileTypeRaw: String?,
            customFlatSpeedMps: Double?,
        ): PaceProfile {
            val type = PaceProfileType.entries.firstOrNull { it.name == paceProfileTypeRaw }
                ?: PaceProfileType.NORMAL
            return if (type == PaceProfileType.CUSTOM) {
                val validSpeed = customFlatSpeedMps?.takeIf { PaceProfile.isValidCustomFlatSpeedMps(it) }
                if (validSpeed != null) {
                    PaceProfile(PaceProfileType.CUSTOM, validSpeed)
                } else {
                    PaceProfile(PaceProfileType.NORMAL)
                }
            } else {
                PaceProfile(type)
            }
        }
    }
}
