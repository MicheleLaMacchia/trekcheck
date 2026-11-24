package it.trekcheck.app.domain.repository

import it.trekcheck.app.domain.model.RouteMetadataSnapshot
import java.time.Instant

interface RouteManagementRepository {
    suspend fun getRouteMetadata(routeId: String): RouteMetadataSnapshot?

    /** Updates only title and updatedAt (planVersion untouched). Returns false when the route does not exist. */
    suspend fun updateTitle(routeId: String, title: String, updatedAtUtc: Instant): Boolean

    /** Removes the metadata row first, then the private route files. */
    suspend fun deleteRoute(routeId: String)
}
