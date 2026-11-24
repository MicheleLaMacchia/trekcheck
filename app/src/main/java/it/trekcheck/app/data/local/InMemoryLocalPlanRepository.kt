package it.trekcheck.app.data.local

import it.trekcheck.app.domain.model.RouteMetadataSnapshot
import it.trekcheck.app.domain.model.SavedPlanSummary
import it.trekcheck.app.domain.repository.LocalPlanRepository
import it.trekcheck.app.domain.repository.RouteManagementRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryLocalPlanRepository(
    initialPlans: List<SavedPlanSummary> = emptyList(),
) : LocalPlanRepository, RouteManagementRepository {
    private val plans = MutableStateFlow(initialPlans.toList())

    override fun observeSavedPlans(): Flow<List<SavedPlanSummary>> = plans.asStateFlow()

    fun setPlans(updatedPlans: List<SavedPlanSummary>) {
        plans.value = updatedPlans.toList()
    }

    override suspend fun getRouteMetadata(routeId: String): RouteMetadataSnapshot? =
        plans.value.firstOrNull { it.id == routeId }?.let { plan ->
            RouteMetadataSnapshot(
                routeId = plan.id,
                title = plan.title,
                hasElevation = plan.hasElevation,
                elevationGainMeters = plan.elevationGainMeters,
                elevationLossMeters = null,
                planVersion = 1L,
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
    }
}
