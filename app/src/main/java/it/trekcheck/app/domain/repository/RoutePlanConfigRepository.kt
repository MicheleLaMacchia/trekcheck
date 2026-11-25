package it.trekcheck.app.domain.repository

import it.trekcheck.app.domain.model.PaceProfile
import it.trekcheck.app.domain.model.RoutePlanConfig
import java.time.Instant
import java.time.ZoneId

interface RoutePlanConfigRepository {
    suspend fun getPlanConfig(routeId: String): RoutePlanConfig?

    /**
     * Persists departure instant, route timezone, and pace profile, invalidates stale
     * estimated duration, and increments [planVersion]. Returns false if the route does not exist.
     */
    suspend fun savePlanConfig(
        routeId: String,
        departureInstantUtc: Instant,
        routeZoneId: ZoneId,
        paceProfile: PaceProfile,
        updatedAtUtc: Instant,
    ): Boolean
}
