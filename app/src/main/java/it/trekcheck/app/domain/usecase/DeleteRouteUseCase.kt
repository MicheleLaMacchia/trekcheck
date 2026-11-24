package it.trekcheck.app.domain.usecase

import it.trekcheck.app.domain.model.RouteActionResult
import it.trekcheck.app.domain.model.RouteManagementErrorCode
import it.trekcheck.app.domain.repository.RouteManagementRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteRouteUseCase(
    private val routeManagementRepository: RouteManagementRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(routeId: String): RouteActionResult = withContext(ioDispatcher) {
        try {
            routeManagementRepository.deleteRoute(routeId)
            RouteActionResult.Success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            RouteActionResult.Failure(RouteManagementErrorCode.STORAGE_FAILED)
        }
    }
}
