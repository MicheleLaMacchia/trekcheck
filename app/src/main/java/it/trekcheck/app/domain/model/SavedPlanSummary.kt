package it.trekcheck.app.domain.model

import java.time.Instant
import java.time.ZoneId

enum class SavedPlanDataAvailability {
    ROUTE_ONLY,
    SAVED_BRIEFING,
}

data class SavedPlanSummary(
    val id: String,
    val title: String,
    val distanceMeters: Double,
    val elevationGainMeters: Double?,
    val departureInstantUtc: Instant?,
    val routeZoneId: ZoneId,
    val estimatedDurationSeconds: Long?,
    val updatedAtUtc: Instant,
    val dataAvailability: SavedPlanDataAvailability = SavedPlanDataAvailability.ROUTE_ONLY,
) {
    init {
        require(id.isNotBlank()) { "Plan id must not be blank" }
        require(title.isNotBlank()) { "Plan title must not be blank" }
        require(distanceMeters.isFinite() && distanceMeters >= 0.0) {
            "distanceMeters must be finite and non-negative"
        }
        elevationGainMeters?.let { gain ->
            require(gain.isFinite() && gain >= 0.0) {
                "elevationGainMeters must be finite and non-negative when present"
            }
        }
        estimatedDurationSeconds?.let { duration ->
            require(duration >= 0L) {
                "estimatedDurationSeconds must be non-negative when present"
            }
        }
    }
}
