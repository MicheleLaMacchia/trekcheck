package it.trekcheck.app.domain.usecase

import it.trekcheck.app.domain.model.PaceProfile
import it.trekcheck.app.domain.model.PlanConfigErrorCode
import it.trekcheck.app.domain.model.SavePlanConfigResult
import it.trekcheck.app.domain.repository.RoutePlanConfigRepository
import it.trekcheck.app.domain.time.AmbiguousTimeChoice
import it.trekcheck.app.domain.time.DepartureResolution
import it.trekcheck.app.domain.time.LocalDepartureResolver
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveRoutePlanConfigUseCase(
    private val routePlanConfigRepository: RoutePlanConfigRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend operator fun invoke(
        routeId: String,
        localDate: LocalDate,
        localTime: LocalTime,
        routeZoneId: ZoneId,
        ambiguousTimeChoice: AmbiguousTimeChoice? = null,
        paceProfile: PaceProfile,
    ): SavePlanConfigResult = withContext(ioDispatcher) {
        if (routeId.isBlank()) {
            return@withContext SavePlanConfigResult.Failure(PlanConfigErrorCode.NOT_FOUND)
        }
        val departureInstantUtc = when (
            val resolution = LocalDepartureResolver.resolve(localDate, localTime, routeZoneId)
        ) {
            is DepartureResolution.Valid -> resolution.instantUtc
            is DepartureResolution.NonExistent -> {
                return@withContext SavePlanConfigResult.Failure(PlanConfigErrorCode.UNRESOLVED_DST)
            }

            is DepartureResolution.Ambiguous -> when (ambiguousTimeChoice) {
                AmbiguousTimeChoice.EARLIER -> resolution.earlierOption.toInstant()
                AmbiguousTimeChoice.LATER -> resolution.laterOption.toInstant()
                null -> return@withContext SavePlanConfigResult.Failure(PlanConfigErrorCode.UNRESOLVED_DST)
            }
        }

        try {
            val saved = routePlanConfigRepository.savePlanConfig(
                routeId = routeId,
                departureInstantUtc = departureInstantUtc,
                routeZoneId = routeZoneId,
                paceProfile = paceProfile,
                updatedAtUtc = clock.instant(),
            )
            if (saved) {
                SavePlanConfigResult.Success
            } else {
                SavePlanConfigResult.Failure(PlanConfigErrorCode.NOT_FOUND)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            SavePlanConfigResult.Failure(PlanConfigErrorCode.STORAGE_FAILED)
        }
    }
}
