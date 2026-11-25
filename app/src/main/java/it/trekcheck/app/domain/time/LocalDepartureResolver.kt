package it.trekcheck.app.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

sealed interface DepartureResolution {
    val requestedLocalDateTime: LocalDateTime
    val zoneId: ZoneId

    data class Valid(
        override val requestedLocalDateTime: LocalDateTime,
        override val zoneId: ZoneId,
        val instantUtc: Instant,
        val zonedDateTime: ZonedDateTime,
    ) : DepartureResolution

    data class NonExistent(
        override val requestedLocalDateTime: LocalDateTime,
        override val zoneId: ZoneId,
        val gapStartLocal: LocalDateTime,
        val gapEndLocal: LocalDateTime,
        val suggestedValidZonedDateTime: ZonedDateTime,
    ) : DepartureResolution

    data class Ambiguous(
        override val requestedLocalDateTime: LocalDateTime,
        override val zoneId: ZoneId,
        val earlierOption: ZonedDateTime,
        val laterOption: ZonedDateTime,
    ) : DepartureResolution
}

enum class AmbiguousTimeChoice {
    EARLIER,
    LATER,
}

object LocalDepartureResolver {
    fun resolve(
        localDate: LocalDate,
        localTime: LocalTime,
        zoneId: ZoneId,
    ): DepartureResolution {
        val normalizedTime = LocalTime.of(localTime.hour, localTime.minute)
        val localDateTime = LocalDateTime.of(localDate, normalizedTime)
        val rules = zoneId.rules
        val validOffsets = rules.getValidOffsets(localDateTime)

        return when (validOffsets.size) {
            1 -> {
                val zoned = ZonedDateTime.ofLocal(localDateTime, zoneId, validOffsets.single())
                DepartureResolution.Valid(
                    requestedLocalDateTime = localDateTime,
                    zoneId = zoneId,
                    instantUtc = zoned.toInstant(),
                    zonedDateTime = zoned,
                )
            }

            0 -> {
                val transition = rules.getTransition(localDateTime)
                if (transition != null) {
                    val suggestedZoned = ZonedDateTime.ofInstant(transition.instant, zoneId)
                    DepartureResolution.NonExistent(
                        requestedLocalDateTime = localDateTime,
                        zoneId = zoneId,
                        gapStartLocal = transition.dateTimeBefore,
                        gapEndLocal = transition.dateTimeAfter,
                        suggestedValidZonedDateTime = suggestedZoned,
                    )
                } else {
                    val fallback = ZonedDateTime.of(localDateTime, zoneId)
                    DepartureResolution.NonExistent(
                        requestedLocalDateTime = localDateTime,
                        zoneId = zoneId,
                        gapStartLocal = localDateTime,
                        gapEndLocal = fallback.toLocalDateTime(),
                        suggestedValidZonedDateTime = fallback,
                    )
                }
            }

            else -> {
                val options = validOffsets
                    .map { offset -> ZonedDateTime.ofLocal(localDateTime, zoneId, offset) }
                    .sortedBy { it.toInstant() }
                DepartureResolution.Ambiguous(
                    requestedLocalDateTime = localDateTime,
                    zoneId = zoneId,
                    earlierOption = options.first(),
                    laterOption = options.last(),
                )
            }
        }
    }
}
