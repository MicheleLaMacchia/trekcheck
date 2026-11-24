package it.trekcheck.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val TrekCheckLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF245C48),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC6E6D7),
    onPrimaryContainer = Color(0xFF0D3527),
    inversePrimary = Color(0xFF8CD5B6),
    secondary = Color(0xFF476457),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE4D8),
    onSecondaryContainer = Color(0xFF152C22),
    tertiary = Color(0xFF705500),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF7E9BC),
    onTertiaryContainer = Color(0xFF433200),
    background = Color(0xFFFAFAF5),
    onBackground = Color(0xFF19231E),
    surface = Color(0xFFFAFAF5),
    onSurface = Color(0xFF19231E),
    surfaceVariant = Color(0xFFDFE6E0),
    onSurfaceVariant = Color(0xFF39463F),
    surfaceTint = Color(0xFF245C48),
    inverseSurface = Color(0xFF2B342F),
    inverseOnSurface = Color(0xFFEDF2ED),
    surfaceDim = Color(0xFFDADFD9),
    surfaceBright = Color(0xFFFAFAF5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F5F0),
    surfaceContainer = Color(0xFFE8EEE8),
    surfaceContainerHigh = Color(0xFFDEE6DF),
    surfaceContainerHighest = Color(0xFFD5DED6),
    outline = Color(0xFF56675C),
    outlineVariant = Color(0xFFC4D1C7),
    scrim = Color(0xFF000000),
    error = Color(0xFF9B2424),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFADAD7),
    onErrorContainer = Color(0xFF5C1010),
)

val TrekCheckDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF8CD5B6),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF144E3B),
    onPrimaryContainer = Color(0xFFB8F0D6),
    inversePrimary = Color(0xFF245C48),
    secondary = Color(0xFFADCBC0),
    onSecondary = Color(0xFF18352B),
    secondaryContainer = Color(0xFF2E4B3F),
    onSecondaryContainer = Color(0xFFCFE4D8),
    tertiary = Color(0xFFE5C158),
    onTertiary = Color(0xFF3A2C00),
    tertiaryContainer = Color(0xFF523E00),
    onTertiaryContainer = Color(0xFFFCE186),
    background = Color(0xFF111513),
    onBackground = Color(0xFFE2E7E3),
    surface = Color(0xFF111513),
    onSurface = Color(0xFFE2E7E3),
    surfaceVariant = Color(0xFF2B342F),
    onSurfaceVariant = Color(0xFFC1CCC4),
    surfaceTint = Color(0xFF8CD5B6),
    inverseSurface = Color(0xFFE2E7E3),
    inverseOnSurface = Color(0xFF19231E),
    surfaceDim = Color(0xFF111513),
    surfaceBright = Color(0xFF343D38),
    surfaceContainerLowest = Color(0xFF0C100E),
    surfaceContainerLow = Color(0xFF171D1A),
    surfaceContainer = Color(0xFF1D2420),
    surfaceContainerHigh = Color(0xFF252D28),
    surfaceContainerHighest = Color(0xFF2F3833),
    outline = Color(0xFF8B968E),
    outlineVariant = Color(0xFF3E4A43),
    scrim = Color(0xFF000000),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF731919),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Immutable
data class TrekCheckExtendedColors(
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

val TrekCheckLightExtendedColors = TrekCheckExtendedColors(
    warning = Color(0xFF705500),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFF7E9BC),
    onWarningContainer = Color(0xFF433200),
)

val TrekCheckDarkExtendedColors = TrekCheckExtendedColors(
    warning = Color(0xFFE5C158),
    onWarning = Color(0xFF3A2C00),
    warningContainer = Color(0xFF523E00),
    onWarningContainer = Color(0xFFFCE186),
)

val TrekCheckShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
)

@Immutable
data class TrekCheckSpacingValues(
    val space1: Dp = 4.dp,
    val space2: Dp = 8.dp,
    val space3: Dp = 12.dp,
    val space4: Dp = 16.dp,
    val space6: Dp = 24.dp,
)

val TrekCheckSpacing = TrekCheckSpacingValues()

val TrekCheckTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

private val LocalTrekCheckExtendedColors = staticCompositionLocalOf { TrekCheckLightExtendedColors }
private val LocalTrekCheckSpacing = staticCompositionLocalOf { TrekCheckSpacing }

object TrekCheckTheme {
    val extendedColors: TrekCheckExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalTrekCheckExtendedColors.current

    val spacing: TrekCheckSpacingValues
        @Composable
        @ReadOnlyComposable
        get() = LocalTrekCheckSpacing.current
}

@Composable
fun TrekCheckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) TrekCheckDarkColorScheme else TrekCheckLightColorScheme
    val extendedColors = if (darkTheme) TrekCheckDarkExtendedColors else TrekCheckLightExtendedColors

    CompositionLocalProvider(
        LocalTrekCheckExtendedColors provides extendedColors,
        LocalTrekCheckSpacing provides TrekCheckSpacing,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = TrekCheckShapes,
            typography = TrekCheckTypography,
            content = content,
        )
    }
}
