package it.trekcheck.app.data.local

import it.trekcheck.app.domain.model.PaceProfile
import it.trekcheck.app.domain.model.RouteMetadataSnapshot
import it.trekcheck.app.domain.model.RoutePlanConfig
import it.trekcheck.app.domain.model.SavedPlanSummary
import it.trekcheck.app.domain.repository.LocalPlanRepository
import it.trekcheck.app.domain.repository.RouteManagementRepository
import it.trekcheck.app.domain.repository.RoutePlanConfigRepository
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryLocalPlanRepository(
    initialPlans: List<SavedPlanSummary> = emptyList(),
) : LocalPlanRepository, RouteManagementRepository, RoutePlanConfigRepository {
    private val plans = MutableStateFlow(initialPlans.toList())
    private val planVersions = initialPlans.associate { it.id to 1L }.toMutableMap()

    override fun observeSavedPlans(): Flow<List<SavedPlanSummary>> = plans.asStateFlow()

    fun setPlans(updatedPlans: List<SavedPlanSummary>) {
        plans.value = updatedPlans.toList()
        updatedPlans.forEach { plan ->
            planVersions.putIfAbsent(plan.id, 1L)
        }
    }

    override suspend fun getRouteMetadata(routeId: String): RouteMetadataSnapshot? =
        plans.value.firstOrNull { it.id == routeId }?.let { plan ->
            RouteMetadataSnapshot(
                routeId = plan.id,
                title = plan.title,
                hasElevation = plan.hasElevation,
                elevationGainMeters = plan.elevationGainMeters,
                elevationLossMeters = null,
                planVersion = planVersions[plan.id] ?: 1L,
                departureInstantUtc = plan.departureInstantUtc,
                routeZoneId = plan.routeZoneId,
                paceProfile = plan.paceProfile,
            )
        }

    override suspend fun updateTitle(routeId: String, title: String, updatedAtUtc: Instant): Boolean {
        var found = false
        plans.value = plans.value.map { plan ->
            if (plan.id == routeId) {
                found = true
                plan.copy(title = title, updatedAtUtc = updatedAtUtc)
            } else {
                plan
            }
        }
        return found
    }

    override suspend fun deleteRoute(routeId: String) {
        plans.value = plans.value.filterNot { it.id == routeId }
        planVersions.remove(routeId)
    }

    override suspend fun getPlanConfig(routeId: String): RoutePlanConfig? =
        plans.value.firstOrNull { it.id == routeId }?.let { plan ->
            RoutePlanConfig(
                routeId = plan.id,
                routeTitle = plan.title,
                hasElevation = plan.hasElevation,
                hasTimestamps = plan.hasTimestamps,
                departureInstantUtc = plan.departureInstantUtc,
                routeZoneId = plan.routeZoneId,
                paceProfile = plan.paceProfile,
                planVersion = planVersions[plan.id] ?: 1L,
            )
        }

    override suspend fun savePlanConfig(
        routeId: String,
        departureInstantUtc: Instant,
        routeZoneId: ZoneId,
        paceProfile: PaceProfile,
        updatedAtUtc: Instant,
    ): Boolean {
        var found = false
        plans.value = plans.value.map { plan ->
            if (plan.id == routeId) {
                found = true
                val nextVersion = (planVersions[routeId] ?: 1L) + 1L
                planVersions[routeId] = nextVersion
                plan.copy(
                    departureInstantUtc = departureInstantUtc,
                    routeZoneId = routeZoneId,
                    paceProfile = paceProfile,
                    estimatedDurationSeconds = null,
                    updatedAtUtc = updatedAtUtc,
                )
            } else {
                plan
            }
        }
        return found
    }
}
