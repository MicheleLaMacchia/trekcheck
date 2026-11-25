package it.trekcheck.app.ui.home

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal sealed interface DistanceDisplay {
    data class Meters(val meters: Int) : DistanceDisplay
    data class Kilometers(val formattedValue: String) : DistanceDisplay
}

internal sealed interface DurationDisplay {
    data object UnderOneMinute : DurationDisplay
    data class Minutes(val minutes: Long) : DurationDisplay
    data class Hours(val hours: Long) : DurationDisplay
    data class HoursAndMinutes(val hours: Long, val minutes: Long) : DurationDisplay
}

internal object HomeFormatters {
    fun resolveDistance(
        distanceMeters: Double,
        locale: Locale = Locale.getDefault(),
    ): DistanceDisplay {
        val safeMeters = if (distanceMeters.isFinite() && distanceMeters >= 0.0) {
            distanceMeters
        } else {
            0.0
        }
        val roundedMeters = safeMeters.roundToInt()
        return if (roundedMeters < 1000) {
            DistanceDisplay.Meters(roundedMeters)
        } else {
            val km = safeMeters / 1000.0
            DistanceDisplay.Kilometers(String.format(locale, "%.1f", km))
        }
    }

    fun resolveElevationGainMeters(elevationGainMeters: Double?): Long? {
        return elevationGainMeters
            ?.takeIf { it.isFinite() && it >= 0.0 }
            ?.roundToLong()
    }

    fun resolveDuration(durationSeconds: Long?): DurationDisplay? {
        if (durationSeconds == null || durationSeconds < 0L) return null
        if (durationSeconds == 0L) return DurationDisplay.Minutes(0L)
        if (durationSeconds < 60L) return DurationDisplay.UnderOneMinute

        val totalMinutes = durationSeconds / 60L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0L && minutes > 0L -> DurationDisplay.HoursAndMinutes(hours, minutes)
            hours > 0L -> DurationDisplay.Hours(hours)
            else -> DurationDisplay.Minutes(minutes)
        }
    }

    fun formatZonedDateTime(
        instantUtc: Instant,
        zoneId: ZoneId,
        locale: Locale = Locale.getDefault(),
    ): String {
        val zoned = ZonedDateTime.ofInstant(instantUtc, zoneId)
        val formatter = DateTimeFormatter.ofPattern("d MMM · HH:mm", locale)
        return "${zoned.format(formatter)} (${zoneId.id})"
    }

    fun formatZonedDateTimeWithOffset(
        instantUtc: Instant,
        zoneId: ZoneId,
        locale: Locale = Locale.getDefault(),
    ): String {
        val zoned = ZonedDateTime.ofInstant(instantUtc, zoneId)
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", locale)
        return "${zoned.format(formatter)} (${zoneId.id}, ${formatUtcOffset(zoned.offset)})"
    }

    fun formatLocalDate(
        date: LocalDate,
        locale: Locale = Locale.getDefault(),
    ): String {
        val formatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy", locale)
        return date.format(formatter)
    }

    fun formatLocalTime(
        time: LocalTime,
        locale: Locale = Locale.getDefault(),
    ): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm", locale)
        return time.format(formatter)
    }

    fun formatUtcOffset(offset: ZoneOffset): String =
        if (offset == ZoneOffset.UTC) "UTC" else "UTC${offset.id}"
}
