package it.trekcheck.app.domain.repository

import it.trekcheck.app.domain.model.SavedPlanSummary
import kotlinx.coroutines.flow.Flow

interface LocalPlanRepository {
    fun observeSavedPlans(): Flow<List<SavedPlanSummary>>
}
