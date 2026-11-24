package it.trekcheck.app.domain.usecase

import it.trekcheck.app.domain.model.RouteActionResult
import it.trekcheck.app.domain.model.RouteManagementErrorCode
import it.trekcheck.app.domain.model.RouteTitleSanitizer
import it.trekcheck.app.domain.repository.RouteManagementRepository
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RenameRouteUseCase(
    private val routeManagementRepository: RouteManagementRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend operator fun invoke(routeId: String, rawTitle: String): RouteActionResult = withContext(ioDispatcher) {
        val title = RouteTitleSanitizer.sanitize(rawTitle)
            ?: return@withContext RouteActionResult.Failure(RouteManagementErrorCode.INVALID_TITLE)
        try {
            if (routeManagementRepository.updateTitle(routeId, title, clock.instant())) {
                RouteActionResult.Success(title)
            } else {
                RouteActionResult.Failure(RouteManagementErrorCode.NOT_FOUND)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            RouteActionResult.Failure(RouteManagementErrorCode.STORAGE_FAILED)
        }
    }
}
