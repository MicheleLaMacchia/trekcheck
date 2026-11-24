package it.trekcheck.app.data.local

import it.trekcheck.app.domain.model.SavedPlanSummary
import it.trekcheck.app.domain.repository.LocalPlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryLocalPlanRepository(
    initialPlans: List<SavedPlanSummary> = emptyList(),
) : LocalPlanRepository {
    private val plans = MutableStateFlow(initialPlans.toList())

    override fun observeSavedPlans(): Flow<List<SavedPlanSummary>> = plans.asStateFlow()

    fun setPlans(updatedPlans: List<SavedPlanSummary>) {
        plans.value = updatedPlans.toList()
    }
}
