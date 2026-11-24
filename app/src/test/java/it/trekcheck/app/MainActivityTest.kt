package it.trekcheck.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "it")
class MainActivityTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesLocallyShowsImportActionAndRestoresNoticeAcrossRecreation() {
        composeTestRule.onNodeWithText("TrekCheck").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nessun percorso salvato").assertIsDisplayed()

        composeTestRule.onNodeWithText("Importa GPX").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Importazione GPX in arrivo").assertIsDisplayed()

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Importazione GPX in arrivo").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Chiudi avviso: Importazione GPX in arrivo")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithText("Importazione GPX in arrivo").assertDoesNotExist()
    }
}
