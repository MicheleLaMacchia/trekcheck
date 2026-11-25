package it.trekcheck.app.ui.home

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFormattersTest {
    @Test
    fun resolvesDistanceRoundingBeforeKilometerThresholdAcrossLocales() {
        assertEquals(
            DistanceDisplay.Meters(999),
            HomeFormatters.resolveDistance(999.4, Locale.ITALIAN),
        )
        assertEquals(
            DistanceDisplay.Kilometers("1,0"),
            HomeFormatters.resolveDistance(999.6, Locale.ITALIAN),
        )
        assertEquals(
            DistanceDisplay.Kilometers("10,2"),
            HomeFormatters.resolveDistance(10_200.0, Locale.ITALIAN),
        )
        assertEquals(
            DistanceDisplay.Kilometers("10.2"),
            HomeFormatters.resolveDistance(10_200.0, Locale.US),
        )
    }

    @Test
    fun resolvesElevationGainOrPreservesNullWithoutZeroSubstitution() {
        assertNull(HomeFormatters.resolveElevationGainMeters(null))
        assertNull(HomeFormatters.resolveElevationGainMeters(Double.NaN))
        assertNull(HomeFormatters.resolveElevationGainMeters(-5.0))
        assertEquals(0L, HomeFormatters.resolveElevationGainMeters(0.0))
        assertEquals(620L, HomeFormatters.resolveElevationGainMeters(620.4))
    }

    @Test
    fun resolvesDurationUsingLongArithmeticAndSubMinuteHandling() {
        assertNull(HomeFormatters.resolveDuration(null))
        assertNull(HomeFormatters.resolveDuration(-1L))
        assertEquals(DurationDisplay.Minutes(0L), HomeFormatters.resolveDuration(0L))
        assertEquals(DurationDisplay.UnderOneMinute, HomeFormatters.resolveDuration(1L))
        assertEquals(DurationDisplay.UnderOneMinute, HomeFormatters.resolveDuration(59L))
        assertEquals(DurationDisplay.Minutes(45L), HomeFormatters.resolveDuration(2_700L))
        assertEquals(DurationDisplay.Hours(2L), HomeFormatters.resolveDuration(7_200L))
        assertEquals(DurationDisplay.HoursAndMinutes(4L, 20L), HomeFormatters.resolveDuration(15_600L))

        val overIntMinutesSeconds = (Int.MAX_VALUE.toLong() + 10L) * 60L
        assertEquals(
            DurationDisplay.HoursAndMinutes(
                hours = (Int.MAX_VALUE.toLong() + 10L) / 60L,
                minutes = (Int.MAX_VALUE.toLong() + 10L) % 60L,
            ),
            HomeFormatters.resolveDuration(overIntMinutesSeconds),
        )
    }

    @Test
    fun formatsInstantDateAndTimeAcrossMidnightAndRouteZones() {
        val utcCrossMidnight = Instant.parse("2026-06-24T22:30:00Z")
        val romeFormatted = HomeFormatters.formatZonedDateTime(
            instantUtc = utcCrossMidnight,
            zoneId = ZoneId.of("Europe/Rome"),
            locale = Locale.ITALIAN,
        )
        val newYorkFormatted = HomeFormatters.formatZonedDateTime(
            instantUtc = utcCrossMidnight,
            zoneId = ZoneId.of("America/New_York"),
            locale = Locale.ENGLISH,
        )

        assertEquals("25 giu · 00:30 (Europe/Rome)", romeFormatted)
        assertEquals("24 Jun · 18:30 (America/New_York)", newYorkFormatted)
    }

    @Test
    fun formatsUtcOffsetsLocalDateLocalTimeAndZonedDateTimeWithOffset() {
        assertEquals("UTC", HomeFormatters.formatUtcOffset(java.time.ZoneOffset.UTC))
        assertEquals("UTC+02:00", HomeFormatters.formatUtcOffset(java.time.ZoneOffset.ofHours(2)))
        assertEquals("UTC-05:00", HomeFormatters.formatUtcOffset(java.time.ZoneOffset.ofHours(-5)))

        assertEquals(
            "08:05",
            HomeFormatters.formatLocalTime(java.time.LocalTime.of(8, 5), Locale.ITALIAN),
        )
        assertEquals(
            "26 nov 2026 · 08:00 (Europe/Rome, UTC+01:00)",
            HomeFormatters.formatZonedDateTimeWithOffset(
                Instant.parse("2026-11-26T07:00:00Z"),
                ZoneId.of("Europe/Rome"),
                Locale.ITALIAN,
            ),
        )
        assertTrue(
            HomeFormatters.formatLocalDate(java.time.LocalDate.of(2026, 11, 26), Locale.ITALIAN)
                .contains("26"),
        )
    }
}
