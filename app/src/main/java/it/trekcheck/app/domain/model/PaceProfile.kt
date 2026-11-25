package it.trekcheck.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class PaceProfileType {
    RELAXED,
    NORMAL,
    FAST,
    CUSTOM,
}

data class PaceProfile(
    val type: PaceProfileType,
    val customFlatSpeedMetersPerSecond: Double? = null,
) {
    init {
        if (type == PaceProfileType.CUSTOM) {
            val speed = requireNotNull(customFlatSpeedMetersPerSecond) {
                "customFlatSpeedMetersPerSecond is required when type is CUSTOM"
            }
            require(isValidCustomFlatSpeedMps(speed)) {
                "customFlatSpeedMetersPerSecond must be finite and within [$MIN_CUSTOM_FLAT_SPEED_KMH, $MAX_CUSTOM_FLAT_SPEED_KMH] km/h"
            }
        } else {
            require(customFlatSpeedMetersPerSecond == null) {
                "customFlatSpeedMetersPerSecond must be null when type is $type"
            }
        }
    }

    companion object {
        const val MIN_CUSTOM_FLAT_SPEED_KMH: Double = 2.0
        const val MAX_CUSTOM_FLAT_SPEED_KMH: Double = 6.5
        const val MIN_CUSTOM_FLAT_SPEED_MPS: Double = MIN_CUSTOM_FLAT_SPEED_KMH / 3.6
        const val MAX_CUSTOM_FLAT_SPEED_MPS: Double = MAX_CUSTOM_FLAT_SPEED_KMH / 3.6
        private const val SPEED_BOUND_TOLERANCE_MPS: Double = 1e-12

        fun kmhToMps(kmh: Double): Double = kmh / 3.6

        fun mpsToKmh(mps: Double): Double = mps * 3.6

        fun isValidCustomFlatSpeedMps(speedMetersPerSecond: Double): Boolean =
            speedMetersPerSecond.isFinite() &&
                speedMetersPerSecond >= MIN_CUSTOM_FLAT_SPEED_MPS - SPEED_BOUND_TOLERANCE_MPS &&
                speedMetersPerSecond <= MAX_CUSTOM_FLAT_SPEED_MPS + SPEED_BOUND_TOLERANCE_MPS
    }
}

enum class PaceSpeedInputError {
    EMPTY,
    NOT_A_NUMBER,
    OUT_OF_RANGE,
}

sealed interface PaceSpeedParseResult {
    data class Valid(val speedMetersPerSecond: Double) : PaceSpeedParseResult
    data class Invalid(val error: PaceSpeedInputError) : PaceSpeedParseResult
}

object PaceSpeedParser {
    private val NUMERIC_PATTERN = Regex("^[+-]?\\d+([.,]\\d+)?$")

    fun parseCustomKmh(rawInput: String): PaceSpeedParseResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return PaceSpeedParseResult.Invalid(PaceSpeedInputError.EMPTY)
        }
        if (!NUMERIC_PATTERN.matches(trimmed)) {
            return PaceSpeedParseResult.Invalid(PaceSpeedInputError.NOT_A_NUMBER)
        }
        val normalized = trimmed.replace(',', '.')
        val kmh = normalized.toDoubleOrNull()
            ?.takeIf { it.isFinite() }
            ?: return PaceSpeedParseResult.Invalid(PaceSpeedInputError.NOT_A_NUMBER)
        if (kmh < PaceProfile.MIN_CUSTOM_FLAT_SPEED_KMH || kmh > PaceProfile.MAX_CUSTOM_FLAT_SPEED_KMH) {
            return PaceSpeedParseResult.Invalid(PaceSpeedInputError.OUT_OF_RANGE)
        }
        return PaceSpeedParseResult.Valid(PaceProfile.kmhToMps(kmh))
    }

    fun formatCustomKmh(
        speedMetersPerSecond: Double,
        locale: Locale = Locale.getDefault(),
    ): String {
        val safeMps = speedMetersPerSecond.coerceIn(
            PaceProfile.MIN_CUSTOM_FLAT_SPEED_MPS,
            PaceProfile.MAX_CUSTOM_FLAT_SPEED_MPS,
        )
        val roundedKmh = BigDecimal.valueOf(PaceProfile.mpsToKmh(safeMps))
            .setScale(2, RoundingMode.HALF_UP)
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val formatter = DecimalFormat("#0.0#", symbols).apply {
            isGroupingUsed = false
        }
        return formatter.format(roundedKmh)
    }
}
