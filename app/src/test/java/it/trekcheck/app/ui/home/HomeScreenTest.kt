package it.trekcheck.app.ui.home

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.unit.dp
import it.trekcheck.app.domain.model.SavedPlanDataAvailability
import it.trekcheck.app.domain.model.SavedPlanSummary
import it.trekcheck.app.ui.theme.TrekCheckTheme
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "it")
class HomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyHomeDisplaysTitleExplanationOfflineBadgeAndTriggersImportClick() {
        val events = mutableListOf<HomeEvent>()
        composeTestRule.setContent {
            TrekCheckTheme {
                HomeScreen(
                    uiState = HomeUiState(),
                    onEvent = events::add,
                )
            }
        }

        composeTestRule.onNodeWithText("TrekCheck").assertIsDisplayed()
        composeTestRule.onNodeWithText("Locale · Offline (dati sul dispositivo)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nessun percorso salvato").assertIsDisplayed()

        val importButton = composeTestRule.onNodeWithText("Importa GPX")
        importButton.performScrollTo()
        importButton.assertIsDisplayed()
        importButton.assertIsEnabled()
        importButton.performClick()

        assertEquals(listOf(HomeEvent.ImportGpxClicked), events)
    }

    @Test
    fun noticeCardUsesContextualDismissActionWithMinimumTouchTarget() {
        val events = mutableListOf<HomeEvent>()
        composeTestRule.setContent {
            TrekCheckTheme {
                HomeScreen(
                    uiState = HomeUiState(activeNotice = HomeNotice.ImportComingSoon),
                    onEvent = events::add,
                )
            }
        }

        composeTestRule.onNodeWithText("Importazione GPX in arrivo").assertIsDisplayed()

        val dismissButton = composeTestRule.onNodeWithContentDescription("Chiudi avviso: Importazione GPX in arrivo")
        dismissButton.performScrollTo()
        dismissButton.assertWidthIsAtLeast(48.dp)
        dismissButton.assertHeightIsAtLeast(48.dp)
        dismissButton.performClick()

        assertEquals(listOf(HomeEvent.DismissNotice), events)
    }

    @Test
    fun populatedOfflineHomeShowsSavedPlansExplicitMissingLabelsAndHandlesDuplicateIds() {
        val planWithDetails = SavedPlanSummary(
            id = "plan-1",
            title = "Anello del Monte Pizzoc",
            distanceMeters = 10_200.0,
            elevationGainMeters = 620.0,
            departureInstantUtc = Instant.parse("2026-06-24T06:00:00Z"),
            routeZoneId = ZoneId.of("Europe/Rome"),
            estimatedDurationSeconds = 15_600L,
            updatedAtUtc = Instant.parse("2026-06-23T18:30:00Z"),
            dataAvailability = SavedPlanDataAvailability.ROUTE_ONLY,
        )
        val planWithoutOptionalData = SavedPlanSummary(
            id = "plan-2",
            title = "Sentiero senza quota",
            distanceMeters = 999.6,
            elevationGainMeters = null,
            departureInstantUtc = null,
            routeZoneId = ZoneId.of("Europe/Rome"),
            estimatedDurationSeconds = null,
            updatedAtUtc = Instant.parse("2026-06-23T19:00:00Z"),
            dataAvailability = SavedPlanDataAvailability.SAVED_BRIEFING,
        )
        val duplicateIdPlan = planWithoutOptionalData.copy(title = "Copia locale")

        composeTestRule.setContent {
            TrekCheckTheme {
                HomeScreen(
                    uiState = HomeUiState(
                        savedPlans = listOf(planWithDetails, planWithoutOptionalData, duplicateIdPlan),
                        isOffline = true,
                    ),
                    onEvent = {},
                )
            }
        }

        val lazyList = composeTestRule.onNode(hasScrollToKeyAction())

        lazyList.performScrollToKey("saved_plans_heading")
        composeTestRule.onNodeWithText("Percorsi locali").assertIsDisplayed()
        composeTestRule.onNodeWithText("Consultabili offline").assertIsDisplayed()

        lazyList.performScrollToKey("plan_0_plan-1")
        composeTestRule.onNodeWithText("Anello del Monte Pizzoc").assertIsDisplayed()
        composeTestRule.onNodeWithText("10,2 km").assertIsDisplayed()
        composeTestRule.onNodeWithText("+620 m stimato").assertIsDisplayed()
        composeTestRule.onNodeWithText("4 h 20 min stimati").assertIsDisplayed()
        composeTestRule.onNodeWithText("Solo percorso").assertIsDisplayed()

        lazyList.performScrollToKey("plan_1_plan-2")
        composeTestRule.onNodeWithText("Sentiero senza quota").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("1,0 km")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Dislivello non disponibile")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Durata stimata non disponibile")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Partenza da configurare")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Briefing salvato")[0].assertIsDisplayed()

        lazyList.performScrollToKey("plan_2_plan-2")
        composeTestRule.onNodeWithText("Copia locale").assertIsDisplayed()
    }
}
