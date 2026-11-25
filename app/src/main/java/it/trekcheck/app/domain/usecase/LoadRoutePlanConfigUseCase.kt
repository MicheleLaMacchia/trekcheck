package it.trekcheck.app.domain.usecase

import it.trekcheck.app.domain.model.LoadPlanConfigResult
import it.trekcheck.app.domain.model.PlanConfigErrorCode
import it.trekcheck.app.domain.repository.RoutePlanConfigRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoadRoutePlanConfigUseCase(
    private val routePlanConfigRepository: RoutePlanConfigRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(routeId: String): LoadPlanConfigResult = withContext(ioDispatcher) {
        if (routeId.isBlank()) {
            return@withContext LoadPlanConfigResult.Failure(PlanConfigErrorCode.NOT_FOUND)
        }
        try {
            val config = routePlanConfigRepository.getPlanConfig(routeId)
                ?: return@withContext LoadPlanConfigResult.Failure(PlanConfigErrorCode.NOT_FOUND)
            LoadPlanConfigResult.Success(config)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            LoadPlanConfigResult.Failure(PlanConfigErrorCode.STORAGE_FAILED)
        }
    }
}
