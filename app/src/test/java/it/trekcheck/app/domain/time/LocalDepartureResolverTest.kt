package it.trekcheck.app.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDepartureResolverTest {
    @Test
    fun resolvesStandardLocalDateTimeInDifferentRouteZonesIndependentlyOfDeviceZone() {
        val rome = ZoneId.of("Europe/Rome")
        val newYork = ZoneId.of("America/New_York")
        val tokyo = ZoneId.of("Asia/Tokyo")

        val romeResult = LocalDepartureResolver.resolve(
            localDate = LocalDate.of(2026, 7, 15),
            localTime = LocalTime.of(8, 30, 45),
            zoneId = rome,
        )
        assertTrue(romeResult is DepartureResolution.Valid)
        val validRome = romeResult as DepartureResolution.Valid
        assertEquals(Instant.parse("2026-07-15T06:30:00Z"), validRome.instantUtc)
        assertEquals(ZoneOffset.ofHours(2), validRome.zonedDateTime.offset)

        val nyResult = LocalDepartureResolver.resolve(
            localDate = LocalDate.of(2026, 7, 15),
            localTime = LocalTime.of(8, 30),
            zoneId = newYork,
        ) as DepartureResolution.Valid
        assertEquals(Instant.parse("2026-07-15T12:30:00Z"), nyResult.instantUtc)

        val tokyoResult = LocalDepartureResolver.resolve(
            localDate = LocalDate.of(2026, 7, 15),
            localTime = LocalTime.of(8, 30),
            zoneId = tokyo,
        ) as DepartureResolution.Valid
        assertEquals(Instant.parse("2026-07-14T23:30:00Z"), tokyoResult.instantUtc)
    }

    @Test
    fun detectsNonExistentTimeOnSpringForwardGapAndSuggestsFirstValidInstant() {
        val rome = ZoneId.of("Europe/Rome")
        val result = LocalDepartureResolver.resolve(
            localDate = LocalDate.of(2026, 3, 29),
            localTime = LocalTime.of(2, 30),
            zoneId = rome,
        )

        assertTrue(result is DepartureResolution.NonExistent)
        val gap = result as DepartureResolution.NonExistent
        assertEquals(LocalDateTime.of(2026, 3, 29, 2, 0), gap.gapStartLocal)
        assertEquals(LocalDateTime.of(2026, 3, 29, 3, 0), gap.gapEndLocal)
        assertEquals(LocalDateTime.of(2026, 3, 29, 3, 0), gap.suggestedValidZonedDateTime.toLocalDateTime())
        assertEquals(ZoneOffset.ofHours(2), gap.suggestedValidZonedDateTime.offset)
        assertEquals(Instant.parse("2026-03-29T01:00:00Z"), gap.suggestedValidZonedDateTime.toInstant())
    }

    @Test
    fun detectsAmbiguousTimeOnFallBackOverlapWithEarlierAndLaterOccurrences() {
        val rome = ZoneId.of("Europe/Rome")
        val result = LocalDepartureResolver.resolve(
            localDate = LocalDate.of(2026, 10, 25),
            localTime = LocalTime.of(2, 30),
            zoneId = rome,
        )

        assertTrue(result is DepartureResolution.Ambiguous)
        val overlap = result as DepartureResolution.Ambiguous
        assertEquals(ZoneOffset.ofHours(2), overlap.earlierOption.offset)
        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), overlap.earlierOption.toInstant())
        assertEquals(ZoneOffset.ofHours(1), overlap.laterOption.offset)
        assertEquals(Instant.parse("2026-10-25T01:30:00Z"), overlap.laterOption.toInstant())
        assertTrue(overlap.earlierOption.toInstant() < overlap.laterOption.toInstant())
    }

    @Test
    fun handlesFixedOffsetAndUtcZonesWithoutDstTransitions() {
        val utc = ZoneId.of("UTC")
        val march = LocalDepartureResolver.resolve(LocalDate.of(2026, 3, 29), LocalTime.of(2, 30), utc)
        val october = LocalDepartureResolver.resolve(LocalDate.of(2026, 10, 25), LocalTime.of(2, 30), utc)
        assertTrue(march is DepartureResolution.Valid)
        assertTrue(october is DepartureResolution.Valid)
        assertEquals(Instant.parse("2026-03-29T02:30:00Z"), (march as DepartureResolution.Valid).instantUtc)
        assertEquals(Instant.parse("2026-10-25T02:30:00Z"), (october as DepartureResolution.Valid).instantUtc)
    }
}
