package it.trekcheck.app.domain.model

import java.time.Instant
import java.time.ZoneId

enum class PlanConfigErrorCode {
    NOT_FOUND,
    INVALID_ZONE_ID,
    INVALID_CUSTOM_PACE,
    UNRESOLVED_DST,
    STORAGE_FAILED,
}

data class RoutePlanConfig(
    val routeId: String,
    val routeTitle: String,
    val hasElevation: Boolean,
    val hasTimestamps: Boolean,
    val departureInstantUtc: Instant?,
    val routeZoneId: ZoneId,
    val paceProfile: PaceProfile,
    val planVersion: Long,
) {
    init {
        require(routeId.isNotBlank()) { "routeId must not be blank" }
        require(routeTitle.isNotBlank()) { "routeTitle must not be blank" }
        require(planVersion >= 1L) { "planVersion must be >= 1" }
    }
}

sealed interface LoadPlanConfigResult {
    data class Success(val config: RoutePlanConfig) : LoadPlanConfigResult
    data class Failure(val code: PlanConfigErrorCode) : LoadPlanConfigResult
}

sealed interface SavePlanConfigResult {
    data object Success : SavePlanConfigResult
    data class Failure(val code: PlanConfigErrorCode) : SavePlanConfigResult
}
