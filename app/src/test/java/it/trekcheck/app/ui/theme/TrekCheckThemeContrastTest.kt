package it.trekcheck.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrekCheckThemeContrastTest {
    @Test
    fun lightSchemeMatchesDesignMdTokensAndExtendedRoles() {
        assertEquals(Color(0xFF245C48), TrekCheckLightColorScheme.primary)
        assertEquals(Color(0xFFFFFFFF), TrekCheckLightColorScheme.onPrimary)
        assertEquals(Color(0xFFFAFAF5), TrekCheckLightColorScheme.surface)
        assertEquals(Color(0xFF19231E), TrekCheckLightColorScheme.onSurface)
        assertEquals(Color(0xFFE8EEE8), TrekCheckLightColorScheme.surfaceContainer)
        assertEquals(Color(0xFF56675C), TrekCheckLightColorScheme.outline)
        assertEquals(Color(0xFF9B2424), TrekCheckLightColorScheme.error)
        assertEquals(Color(0xFF705500), TrekCheckLightExtendedColors.warning)

        assertEquals(Color(0xFF2B342F), TrekCheckLightColorScheme.inverseSurface)
        assertEquals(Color(0xFFEDF2ED), TrekCheckLightColorScheme.inverseOnSurface)
        assertEquals(Color(0xFF8CD5B6), TrekCheckLightColorScheme.inversePrimary)
        assertEquals(Color(0xFFDADFD9), TrekCheckLightColorScheme.surfaceDim)
        assertEquals(Color(0xFFFAFAF5), TrekCheckLightColorScheme.surfaceBright)
        assertEquals(Color(0xFF245C48), TrekCheckLightColorScheme.surfaceTint)
        assertEquals(Color(0xFF000000), TrekCheckLightColorScheme.scrim)
    }

    @Test
    fun darkSchemeUsesVerifiedForestPaletteNotNaiveInversion() {
        assertEquals(Color(0xFF8CD5B6), TrekCheckDarkColorScheme.primary)
        assertEquals(Color(0xFF003828), TrekCheckDarkColorScheme.onPrimary)
        assertEquals(Color(0xFF111513), TrekCheckDarkColorScheme.surface)
        assertEquals(Color(0xFFE2E7E3), TrekCheckDarkColorScheme.onSurface)
        assertEquals(Color(0xFF1D2420), TrekCheckDarkColorScheme.surfaceContainer)
        assertEquals(Color(0xFF8B968E), TrekCheckDarkColorScheme.outline)
        assertEquals(Color(0xFFFFB4AB), TrekCheckDarkColorScheme.error)
        assertEquals(Color(0xFF690005), TrekCheckDarkColorScheme.onError)
        assertEquals(Color(0xFFE5C158), TrekCheckDarkExtendedColors.warning)
        assertEquals(Color(0xFF3A2C00), TrekCheckDarkExtendedColors.onWarning)

        assertEquals(Color(0xFFE2E7E3), TrekCheckDarkColorScheme.inverseSurface)
        assertEquals(Color(0xFF19231E), TrekCheckDarkColorScheme.inverseOnSurface)
        assertEquals(Color(0xFF245C48), TrekCheckDarkColorScheme.inversePrimary)
        assertEquals(Color(0xFF111513), TrekCheckDarkColorScheme.surfaceDim)
        assertEquals(Color(0xFF343D38), TrekCheckDarkColorScheme.surfaceBright)
        assertEquals(Color(0xFF8CD5B6), TrekCheckDarkColorScheme.surfaceTint)
        assertEquals(Color(0xFF000000), TrekCheckDarkColorScheme.scrim)

        assertNotEquals(TrekCheckLightColorScheme.primary, TrekCheckDarkColorScheme.primary)
        assertNotEquals(TrekCheckLightColorScheme.surface, TrekCheckDarkColorScheme.surface)
    }

    @Test
    fun shapesSpacingAndTypographyMatchDesignTokens() {
        assertEquals(RoundedCornerShape(4.dp), TrekCheckShapes.small)
        assertEquals(RoundedCornerShape(12.dp), TrekCheckShapes.medium)
        assertEquals(RoundedCornerShape(20.dp), TrekCheckShapes.large)

        assertEquals(4.dp, TrekCheckSpacing.space1)
        assertEquals(8.dp, TrekCheckSpacing.space2)
        assertEquals(12.dp, TrekCheckSpacing.space3)
        assertEquals(16.dp, TrekCheckSpacing.space4)
        assertEquals(24.dp, TrekCheckSpacing.space6)

        assertEquals(FontFamily.Default, TrekCheckTypography.headlineSmall.fontFamily)
        assertEquals(24.sp, TrekCheckTypography.headlineSmall.fontSize)
        assertEquals(FontFamily.Default, TrekCheckTypography.bodyLarge.fontFamily)
        assertEquals(16.sp, TrekCheckTypography.bodyLarge.fontSize)
        assertEquals(FontFamily.Default, TrekCheckTypography.labelLarge.fontFamily)
        assertEquals(14.sp, TrekCheckTypography.labelLarge.fontSize)
    }

    @Test
    fun lightThemePairsMeetWcagNormalTextContrastThreshold() {
        assertMinContrast("Light primary / onPrimary", TrekCheckLightColorScheme.primary, TrekCheckLightColorScheme.onPrimary)
        assertMinContrast("Light primaryContainer / onPrimaryContainer", TrekCheckLightColorScheme.primaryContainer, TrekCheckLightColorScheme.onPrimaryContainer)
        assertMinContrast("Light secondary / onSecondary", TrekCheckLightColorScheme.secondary, TrekCheckLightColorScheme.onSecondary)
        assertMinContrast("Light secondaryContainer / onSecondaryContainer", TrekCheckLightColorScheme.secondaryContainer, TrekCheckLightColorScheme.onSecondaryContainer)
        assertMinContrast("Light surface / onSurface", TrekCheckLightColorScheme.surface, TrekCheckLightColorScheme.onSurface)
        assertMinContrast("Light surfaceContainer / onSurface", TrekCheckLightColorScheme.surfaceContainer, TrekCheckLightColorScheme.onSurface)
        assertMinContrast("Light surfaceContainer / onSurfaceVariant", TrekCheckLightColorScheme.surfaceContainer, TrekCheckLightColorScheme.onSurfaceVariant)
        assertMinContrast("Light surfaceContainer / primary", TrekCheckLightColorScheme.surfaceContainer, TrekCheckLightColorScheme.primary)
        assertMinContrast("Light surfaceDim / onSurface", TrekCheckLightColorScheme.surfaceDim, TrekCheckLightColorScheme.onSurface)
        assertMinContrast("Light surfaceBright / onSurface", TrekCheckLightColorScheme.surfaceBright, TrekCheckLightColorScheme.onSurface)
        assertMinContrast("Light inverseSurface / inverseOnSurface", TrekCheckLightColorScheme.inverseSurface, TrekCheckLightColorScheme.inverseOnSurface)
        assertMinContrast("Light inverseSurface / inversePrimary", TrekCheckLightColorScheme.inverseSurface, TrekCheckLightColorScheme.inversePrimary)
        assertMinContrast("Light surface / onSurfaceVariant", TrekCheckLightColorScheme.surface, TrekCheckLightColorScheme.onSurfaceVariant)
        assertMinContrast("Light surfaceVariant / onSurfaceVariant", TrekCheckLightColorScheme.surfaceVariant, TrekCheckLightColorScheme.onSurfaceVariant)
        assertMinContrast("Light surface / primary", TrekCheckLightColorScheme.surface, TrekCheckLightColorScheme.primary)
        assertMinContrast("Light surface / outline", TrekCheckLightColorScheme.surface, TrekCheckLightColorScheme.outline)
        assertMinContrast("Light surfaceContainer / outline", TrekCheckLightColorScheme.surfaceContainer, TrekCheckLightColorScheme.outline)
        assertMinContrast("Light error / onError", TrekCheckLightColorScheme.error, TrekCheckLightColorScheme.onError)
        assertMinContrast("Light errorContainer / onErrorContainer", TrekCheckLightColorScheme.errorContainer, TrekCheckLightColorScheme.onErrorContainer)
        assertMinContrast("Light surface / error", TrekCheckLightColorScheme.surface, TrekCheckLightColorScheme.error)
        assertMinContrast("Light warning / onWarning", TrekCheckLightExtendedColors.warning, TrekCheckLightExtendedColors.onWarning)
        assertMinContrast("Light warningContainer / onWarningContainer", TrekCheckLightExtendedColors.warningContainer, TrekCheckLightExtendedColors.onWarningContainer)
        assertMinContrast("Light surface / warning", TrekCheckLightColorScheme.surface, TrekCheckLightExtendedColors.warning)
    }

    @Test
    fun darkThemePairsMeetWcagNormalTextContrastThreshold() {
        assertMinContrast("Dark primary / onPrimary", TrekCheckDarkColorScheme.primary, TrekCheckDarkColorScheme.onPrimary)
        assertMinContrast("Dark primaryContainer / onPrimaryContainer", TrekCheckDarkColorScheme.primaryContainer, TrekCheckDarkColorScheme.onPrimaryContainer)
        assertMinContrast("Dark secondary / onSecondary", TrekCheckDarkColorScheme.secondary, TrekCheckDarkColorScheme.onSecondary)
        assertMinContrast("Dark secondaryContainer / onSecondaryContainer", TrekCheckDarkColorScheme.secondaryContainer, TrekCheckDarkColorScheme.onSecondaryContainer)
        assertMinContrast("Dark surface / onSurface", TrekCheckDarkColorScheme.surface, TrekCheckDarkColorScheme.onSurface)
        assertMinContrast("Dark surfaceContainer / onSurface", TrekCheckDarkColorScheme.surfaceContainer, TrekCheckDarkColorScheme.onSurface)
        assertMinContrast("Dark surfaceContainer / onSurfaceVariant", TrekCheckDarkColorScheme.surfaceContainer, TrekCheckDarkColorScheme.onSurfaceVariant)
        assertMinContrast("Dark surfaceContainer / primary", TrekCheckDarkColorScheme.surfaceContainer, TrekCheckDarkColorScheme.primary)
        assertMinContrast("Dark surfaceDim / onSurface", TrekCheckDarkColorScheme.surfaceDim, TrekCheckDarkColorScheme.onSurface)
        assertMinContrast("Dark surfaceBright / onSurface", TrekCheckDarkColorScheme.surfaceBright, TrekCheckDarkColorScheme.onSurface)
        assertMinContrast("Dark inverseSurface / inverseOnSurface", TrekCheckDarkColorScheme.inverseSurface, TrekCheckDarkColorScheme.inverseOnSurface)
        assertMinContrast("Dark inverseSurface / inversePrimary", TrekCheckDarkColorScheme.inverseSurface, TrekCheckDarkColorScheme.inversePrimary)
        assertMinContrast("Dark surface / onSurfaceVariant", TrekCheckDarkColorScheme.surface, TrekCheckDarkColorScheme.onSurfaceVariant)
        assertMinContrast("Dark surfaceVariant / onSurfaceVariant", TrekCheckDarkColorScheme.surfaceVariant, TrekCheckDarkColorScheme.onSurfaceVariant)
        assertMinContrast("Dark surface / primary", TrekCheckDarkColorScheme.surface, TrekCheckDarkColorScheme.primary)
        assertMinContrast("Dark surface / outline", TrekCheckDarkColorScheme.surface, TrekCheckDarkColorScheme.outline)
        assertMinContrast("Dark surfaceContainer / outline", TrekCheckDarkColorScheme.surfaceContainer, TrekCheckDarkColorScheme.outline)
        assertMinContrast("Dark error / onError", TrekCheckDarkColorScheme.error, TrekCheckDarkColorScheme.onError)
        assertMinContrast("Dark errorContainer / onErrorContainer", TrekCheckDarkColorScheme.errorContainer, TrekCheckDarkColorScheme.onErrorContainer)
        assertMinContrast("Dark surface / error", TrekCheckDarkColorScheme.surface, TrekCheckDarkColorScheme.error)
        assertMinContrast("Dark warning / onWarning", TrekCheckDarkExtendedColors.warning, TrekCheckDarkExtendedColors.onWarning)
        assertMinContrast("Dark warningContainer / onWarningContainer", TrekCheckDarkExtendedColors.warningContainer, TrekCheckDarkExtendedColors.onWarningContainer)
        assertMinContrast("Dark surface / warning", TrekCheckDarkColorScheme.surface, TrekCheckDarkExtendedColors.warning)
    }

    private fun assertMinContrast(pairName: String, first: Color, second: Color) {
        val ratio = contrastRatio(first, second)
        assertTrue(
            "$pairName contrast ratio was $ratio, expected >= 4.5",
            ratio >= 4.5,
        )
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val lum1 = relativeLuminance(first)
        val lum2 = relativeLuminance(second)
        val lighter = max(lum1, lum2)
        val darker = min(lum1, lum2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        val r = linearizeChannel(color.red.toDouble())
        val g = linearizeChannel(color.green.toDouble())
        val b = linearizeChannel(color.blue.toDouble())
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun linearizeChannel(channel: Double): Double {
        return if (channel <= 0.04045) {
            channel / 12.92
        } else {
            ((channel + 0.055) / 1.055).pow(2.4)
        }
    }
}
