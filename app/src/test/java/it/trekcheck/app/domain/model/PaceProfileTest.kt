package it.trekcheck.app.domain.model

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaceProfileTest {
    @Test
    fun standardProfilesRequireNullCustomSpeedAndCustomRequiresBoundedSpeedInSiUnits() {
        val normal = PaceProfile(PaceProfileType.NORMAL)
        assertEquals(PaceProfileType.NORMAL, normal.type)

        val minCustom = PaceProfile(PaceProfileType.CUSTOM, PaceProfile.kmhToMps(2.0))
        val maxCustom = PaceProfile(PaceProfileType.CUSTOM, PaceProfile.kmhToMps(6.5))
        assertEquals(2.0 / 3.6, minCustom.customFlatSpeedMetersPerSecond!!, 1e-12)
        assertEquals(6.5 / 3.6, maxCustom.customFlatSpeedMetersPerSecond!!, 1e-12)
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonCustomProfileRejectsNonNullSpeed() {
        PaceProfile(PaceProfileType.RELAXED, 1.2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun customProfileRejectsNullSpeed() {
        PaceProfile(PaceProfileType.CUSTOM, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun customProfileRejectsSpeedBelowMinimum() {
        PaceProfile(PaceProfileType.CUSTOM, PaceProfile.kmhToMps(1.99))
    }

    @Test(expected = IllegalArgumentException::class)
    fun customProfileRejectsSpeedAboveMaximum() {
        PaceProfile(PaceProfileType.CUSTOM, PaceProfile.kmhToMps(6.51))
    }

    @Test(expected = IllegalArgumentException::class)
    fun customProfileRejectsNanSpeed() {
        PaceProfile(PaceProfileType.CUSTOM, Double.NaN)
    }

    @Test
    fun parserAcceptsItalianCommaAndEnglishDotSeparatorsAcrossValidBounds() {
        val min = PaceSpeedParser.parseCustomKmh(" 2,0 ")
        assertTrue(min is PaceSpeedParseResult.Valid)
        assertEquals(2.0 / 3.6, (min as PaceSpeedParseResult.Valid).speedMetersPerSecond, 1e-12)

        val max = PaceSpeedParser.parseCustomKmh("6.5")
        assertTrue(max is PaceSpeedParseResult.Valid)
        assertEquals(6.5 / 3.6, (max as PaceSpeedParseResult.Valid).speedMetersPerSecond, 1e-12)

        val twoDecimals = PaceSpeedParser.parseCustomKmh("3,75")
        assertTrue(twoDecimals is PaceSpeedParseResult.Valid)
        assertEquals(3.75 / 3.6, (twoDecimals as PaceSpeedParseResult.Valid).speedMetersPerSecond, 1e-12)
    }

    @Test
    fun parserRejectsEmptyNonNumericNonFiniteAndOutOfRangeInputs() {
        assertEquals(PaceSpeedParseResult.Invalid(PaceSpeedInputError.EMPTY), PaceSpeedParser.parseCustomKmh("   "))
        assertEquals(PaceSpeedParseResult.Invalid(PaceSpeedInputError.NOT_A_NUMBER), PaceSpeedParser.parseCustomKmh("abc"))
        assertEquals(PaceSpeedParseResult.Invalid(PaceSpeedInputError.NOT_A_NUMBER), PaceSpeedParser.parseCustomKmh("NaN"))
        assertEquals(PaceSpeedParseResult.Invalid(PaceSpeedInputError.NOT_A_NUMBER), PaceSpeedParser.parseCustomKmh("4.5d"))
        assertEquals(PaceSpeedParseResult.Invalid(PaceSpeedInputError.NOT_A_NUMBER), PaceSpeedParser.parseCustomKmh("4.5f"))
        assertEquals(PaceSpeedParseResult.Invalid(PaceSpeedInputError.OUT_OF_RANGE), PaceSpeedParser.parseCustomKmh("1.9"))
        assertEquals(PaceSpeedParseResult.Invalid(PaceSpeedInputError.OUT_OF_RANGE), PaceSpeedParser.parseCustomKmh("-3.0"))
        assertEquals(PaceSpeedParseResult.Invalid(PaceSpeedInputError.OUT_OF_RANGE), PaceSpeedParser.parseCustomKmh("6.6"))
    }

    @Test
    fun formatterPreservesOneOrTwoDecimalsAccordingToLocaleAndRoundTrips() {
        assertEquals("4,0", PaceSpeedParser.formatCustomKmh(PaceProfile.kmhToMps(4.0), Locale.ITALIAN))
        assertEquals("4.0", PaceSpeedParser.formatCustomKmh(PaceProfile.kmhToMps(4.0), Locale.US))
        assertEquals("3,25", PaceSpeedParser.formatCustomKmh(PaceProfile.kmhToMps(3.25), Locale.ITALIAN))
        assertEquals("6,5", PaceSpeedParser.formatCustomKmh(PaceProfile.kmhToMps(6.5), Locale.ITALIAN))

        val formatted = PaceSpeedParser.formatCustomKmh(PaceProfile.kmhToMps(4.35), Locale.ITALIAN)
        val reparsed = PaceSpeedParser.parseCustomKmh(formatted) as PaceSpeedParseResult.Valid
        assertEquals(4.35 / 3.6, reparsed.speedMetersPerSecond, 1e-9)
        assertFalse(PaceProfile.isValidCustomFlatSpeedMps(Double.POSITIVE_INFINITY))
    }
}
