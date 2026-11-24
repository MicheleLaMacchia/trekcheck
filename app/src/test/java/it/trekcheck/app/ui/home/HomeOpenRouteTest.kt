package it.trekcheck.app.ui.home

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import it.trekcheck.app.data.local.InMemoryLocalPlanRepository
import it.trekcheck.app.domain.model.SavedPlanSummary
import it.trekcheck.app.ui.theme.TrekCheckTheme
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "it")
class HomeOpenRouteTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val plan = SavedPlanSummary(
        id = "plan-1",
        title = "Anello del Lago",
        distanceMeters = 8_000.0,
        elevationGainMeters = null,
        departureInstantUtc = null,
        routeZoneId = ZoneId.of("Europe/Rome"),
        estimatedDurationSeconds = null,
        updatedAtUtc = Instant.parse("2026-11-24T10:00:00Z"),
        hasElevation = false,
    )

    @Test
    fun savedPlanCardOpensPreviewWithAccessibleTarget() {
        val events = mutableListOf<HomeEvent>()
        composeTestRule.setContent {
            TrekCheckTheme {
                HomeScreen(uiState = HomeUiState(savedPlans = listOf(plan)), onEvent = { events.add(it) })
            }
        }
        composeTestRule.onNodeWithText("Anello del Lago").performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .assert(
                SemanticsMatcher("has open click label and button role") { node ->
                    node.config.getOrNull(SemanticsProperties.Role) == Role.Button &&
                        node.config.getOrNull(SemanticsActions.OnClick)?.label == "Apri anteprima di Anello del Lago"
                },
            )
            .performClick()
        assertEquals(listOf<HomeEvent>(HomeEvent.OpenRouteClicked("plan-1")), events)
    }

    @Test
    fun importNoticeOffersOpenPreviewAction() {
        val events = mutableListOf<HomeEvent>()
        composeTestRule.setContent {
            TrekCheckTheme {
                HomeScreen(
                    uiState = HomeUiState(
                        savedPlans = listOf(plan),
                        activeNotice = HomeNotice.ImportSucceeded(
                            planTitle = plan.title,
                            hasElevation = false,
                            hasTimestamps = false,
                            planId = plan.id,
                        ),
                    ),
                    onEvent = { events.add(it) },
                )
            }
        }
        composeTestRule.onNodeWithTag("home_notice_open_preview").performScrollTo()
            .assertContentDescriptionContains("Anello del Lago", substring = true)
            .performClick()
        assertEquals(listOf<HomeEvent>(HomeEvent.OpenRouteClicked("plan-1")), events)
    }

    @Test
    fun viewModelEmitsOpenEffectAndClearsNotice() = runBlocking {
        val viewModel = HomeViewModel(
            localPlanRepository = InMemoryLocalPlanRepository(listOf(plan)),
            dispatcher = Dispatchers.Unconfined,
            initialNotice = HomeNotice.ImportSucceeded("x", hasElevation = true, hasTimestamps = true, planId = "plan-1"),
        )
        viewModel.onEvent(HomeEvent.OpenRouteClicked("plan-1"))
        assertEquals(HomeEffect.OpenRoutePreview("plan-1"), viewModel.effects.first())
        assertNull(viewModel.uiState.value.activeNotice)
    }
}
