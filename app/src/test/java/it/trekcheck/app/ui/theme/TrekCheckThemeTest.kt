package it.trekcheck.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TrekCheckThemeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appliesLightColorSchemeExtendedColorsShapesAndSpacing() {
        var observedColors: ColorScheme? = null
        var observedExtended: TrekCheckExtendedColors? = null
        var observedShapes: Shapes? = null
        var observedSpacing: TrekCheckSpacingValues? = null

        composeTestRule.setContent {
            TrekCheckTheme(darkTheme = false) {
                observedColors = MaterialTheme.colorScheme
                observedExtended = TrekCheckTheme.extendedColors
                observedShapes = MaterialTheme.shapes
                observedSpacing = TrekCheckTheme.spacing
            }
        }

        assertEquals(TrekCheckLightColorScheme, observedColors)
        assertEquals(TrekCheckLightExtendedColors, observedExtended)
        assertEquals(TrekCheckShapes, observedShapes)
        assertEquals(TrekCheckSpacing, observedSpacing)
    }

    @Test
    fun appliesDarkColorSchemeAndDarkExtendedColors() {
        var observedColors: ColorScheme? = null
        var observedExtended: TrekCheckExtendedColors? = null

        composeTestRule.setContent {
            TrekCheckTheme(darkTheme = true) {
                observedColors = MaterialTheme.colorScheme
                observedExtended = TrekCheckTheme.extendedColors
            }
        }

        assertEquals(TrekCheckDarkColorScheme, observedColors)
        assertEquals(TrekCheckDarkExtendedColors, observedExtended)
    }
}
