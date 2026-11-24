package it.trekcheck.app.ui.components

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import it.trekcheck.app.ui.theme.TrekCheckTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "it")
class TrekCheckPrimaryActionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun enabledStateInvokesClickAndMeetsMinimumTouchTarget() {
        var clicks = 0
        composeTestRule.setContent {
            TrekCheckTheme {
                TrekCheckPrimaryAction(
                    label = "Importa GPX",
                    state = PrimaryActionState.Enabled,
                    onClick = { clicks += 1 },
                )
            }
        }

        val button = composeTestRule.onNodeWithText("Importa GPX")
        button.assertIsEnabled()
        button.assertHasClickAction()
        button.assertWidthIsAtLeast(48.dp)
        button.assertHeightIsAtLeast(48.dp)
        button.performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun disabledStateUsesDefaultStateDescriptionAndIgnoresClicks() {
        var clicks = 0
        composeTestRule.setContent {
            TrekCheckTheme {
                TrekCheckPrimaryAction(
                    label = "Importa GPX",
                    state = PrimaryActionState.Disabled,
                    onClick = { clicks += 1 },
                )
            }
        }

        val button = composeTestRule.onNodeWithText("Importa GPX")
        button.assertIsNotEnabled()
        button.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Non disponibile"))
        button.performClick()

        assertEquals(0, clicks)
    }

    @Test
    fun inProgressStateStaysFocusableWithoutDisabledSemanticsAndSuppressesClicks() {
        var clicks = 0
        composeTestRule.setContent {
            TrekCheckTheme {
                TrekCheckPrimaryAction(
                    label = "Importa GPX",
                    state = PrimaryActionState.InProgress,
                    inProgressLabel = "Importazione GPX in corso…",
                    onClick = { clicks += 1 },
                )
            }
        }

        val button = composeTestRule.onNodeWithText("Importazione GPX in corso…")
        button.assertIsEnabled()
        button.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused))
        button.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Operazione in corso"))
        button.assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        button.requestFocus()
        button.assertIsFocused()
        button.performClick()

        assertEquals(0, clicks)
    }
}
