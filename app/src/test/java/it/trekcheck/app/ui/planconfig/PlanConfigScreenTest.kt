package it.trekcheck.app.ui.planconfig

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import it.trekcheck.app.domain.model.PaceProfileType
import it.trekcheck.app.domain.model.PaceSpeedInputError
import it.trekcheck.app.domain.model.PlanConfigErrorCode
import it.trekcheck.app.domain.time.AmbiguousTimeChoice
import it.trekcheck.app.domain.time.LocalDepartureResolver
import it.trekcheck.app.ui.theme.TrekCheckTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "it")
class PlanConfigScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun readyState(
        date: LocalDate = LocalDate.of(2026, 7, 15),
        time: LocalTime = LocalTime.of(8, 0),
        routeZoneId: ZoneId = ZoneId.of("Europe/Rome"),
        deviceZoneId: ZoneId = ZoneId.of("Europe/Rome"),
        ambiguousChoice: AmbiguousTimeChoice? = null,
        paceType: PaceProfileType = PaceProfileType.NORMAL,
        customSpeed: String = "",
        hasEditedCustom: Boolean = false,
        customError: PaceSpeedInputError? = null,
        hasTimestamps: Boolean = false,
    ): PlanConfigUiState = PlanConfigUiState(
        status = PlanConfigStatus.READY,
        routeId = "r1",
        routeTitle = "Monte Pizzoc",
        hasElevation = true,
        hasTimestamps = hasTimestamps,
        localDate = date,
        localTime = time,
        routeZoneId = routeZoneId,
        deviceZoneId = deviceZoneId,
        resolution = LocalDepartureResolver.resolve(date, time, routeZoneId),
        selectedAmbiguousChoice = ambiguousChoice,
        selectedPaceType = paceType,
        customSpeedInput = customSpeed,
        hasEditedCustomSpeed = hasEditedCustom,
        customSpeedError = customError,
    )

    private fun show(
        state: PlanConfigUiState,
        events: MutableList<PlanConfigEvent> = mutableListOf(),
        darkTheme: Boolean = false,
        fontScale: Float = 1f,
    ) = mutableStateOf(state).also { holder ->
        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale)) {
                TrekCheckTheme(darkTheme = darkTheme) {
                    PlanConfigScreen(uiState = holder.value, onEvent = { events.add(it) })
                }
            }
        }
    }

    @Test
    fun displaysRouteTitleGpxTimestampNoticeAndEmitsSaveAndPickerEvents() {
        val events = mutableListOf<PlanConfigEvent>()
        show(readyState(hasTimestamps = true), events)

        composeTestRule.onNodeWithTag("plan_config_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Monte Pizzoc").assertIsDisplayed()
        composeTestRule.onNodeWithTag("plan_config_gpx_timestamps_notice").assertIsDisplayed()
        composeTestRule.onNodeWithTag("plan_config_resolved_departure").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("plan_config_date_button").performScrollTo()
            .assertHeightIsAtLeast(48.dp).performClick()
        composeTestRule.onNodeWithTag("plan_config_time_button").performScrollTo()
            .assertHeightIsAtLeast(48.dp).performClick()
        composeTestRule.onNodeWithTag("plan_config_timezone_button").performScrollTo()
            .assertHeightIsAtLeast(48.dp).performClick()
        composeTestRule.onNodeWithTag("plan_config_save").performScrollTo()
            .assertHeightIsAtLeast(48.dp).assertIsEnabled().performClick()

        assertEquals(
            listOf(
                PlanConfigEvent.OpenDatePicker,
                PlanConfigEvent.OpenTimePicker,
                PlanConfigEvent.OpenTimezoneDialog,
                PlanConfigEvent.SaveClicked,
            ),
            events,
        )
    }

    @Test
    fun nonExistentDstShowsPoliteNoticeBlocksSaveAndAppliesSuggestedTime() {
        val events = mutableListOf<PlanConfigEvent>()
        show(
            readyState(
                date = LocalDate.of(2026, 3, 29),
                time = LocalTime.of(2, 30),
                routeZoneId = ZoneId.of("Europe/Rome"),
            ),
            events,
        )

        val notice = composeTestRule.onNodeWithTag("plan_config_dst_notice").performScrollTo()
        notice.assertIsDisplayed()
        notice.assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        composeTestRule.onNodeWithText("Orario inesistente per cambio d'ora").assertIsDisplayed()

        composeTestRule.onNodeWithTag("plan_config_save").performScrollTo().assertIsNotEnabled()
        composeTestRule.onNodeWithTag("plan_config_dst_use_suggested").performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .assertTextContains("03:00", substring = true)
            .performClick()

        assertEquals(listOf<PlanConfigEvent>(PlanConfigEvent.ApplySuggestedDstTime), events)
    }

    @Test
    fun ambiguousDstShowsTwoExplicitOccurrencesAndRequiresSelection() {
        val events = mutableListOf<PlanConfigEvent>()
        val holder = show(
            readyState(
                date = LocalDate.of(2026, 10, 25),
                time = LocalTime.of(2, 30),
                routeZoneId = ZoneId.of("Europe/Rome"),
                ambiguousChoice = null,
            ),
            events,
        )

        composeTestRule.onNodeWithTag("plan_config_dst_notice").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("plan_config_save").performScrollTo().assertIsNotEnabled()

        composeTestRule.onNodeWithTag("plan_config_dst_option_earlier").performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .assertTextContains("UTC+02:00", substring = true)
        composeTestRule.onNodeWithTag("plan_config_dst_option_later").performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .assertTextContains("UTC+01:00", substring = true)
            .performClick()

        assertEquals(
            listOf<PlanConfigEvent>(PlanConfigEvent.AmbiguousChoiceSelected(AmbiguousTimeChoice.LATER)),
            events,
        )

        holder.value = holder.value.copy(selectedAmbiguousChoice = AmbiguousTimeChoice.LATER)
        composeTestRule.onNodeWithTag("plan_config_save").performScrollTo().assertIsEnabled()
    }

    @Test
    fun differentDeviceZoneShowsExplicitComparisonAndQuickResetButton() {
        val events = mutableListOf<PlanConfigEvent>()
        show(
            readyState(
                routeZoneId = ZoneId.of("America/New_York"),
                deviceZoneId = ZoneId.of("Europe/Rome"),
            ),
            events,
        )

        composeTestRule.onNodeWithTag("plan_config_resolved_departure").performScrollTo()
            .assertTextContains("America/New_York", substring = true)
        composeTestRule.onNodeWithTag("plan_config_device_equivalent").performScrollTo()
            .assertTextContains("Europe/Rome", substring = true)
        composeTestRule.onNodeWithTag("plan_config_timezone_diff_notice").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("plan_config_use_device_zone").performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(listOf<PlanConfigEvent>(PlanConfigEvent.UseDeviceZoneClicked), events)
    }

    @Test
    fun customPaceShowsInlineValidationBlocksInvalidAndAllowsImeDoneWhenValid() {
        val events = mutableListOf<PlanConfigEvent>()
        val holder = show(
            readyState(
                paceType = PaceProfileType.CUSTOM,
                customSpeed = "1,2",
                hasEditedCustom = true,
                customError = PaceSpeedInputError.OUT_OF_RANGE,
            ),
            events,
        )

        composeTestRule.onNodeWithTag("plan_config_custom_speed_field").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("La velocità deve essere compresa tra 2,0 e 6,5 km/h.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("plan_config_save").performScrollTo().assertIsNotEnabled()

        composeTestRule.onNodeWithTag("plan_config_custom_speed_field").performTextReplacement("4,5")
        holder.value = readyState(
            paceType = PaceProfileType.CUSTOM,
            customSpeed = "4,5",
            hasEditedCustom = true,
            customError = null,
        )
        composeTestRule.onNodeWithTag("plan_config_save").performScrollTo().assertIsEnabled()
        composeTestRule.onNodeWithTag("plan_config_custom_speed_field").performScrollTo().performImeAction()

        assertTrue(events.contains(PlanConfigEvent.CustomSpeedChanged("4,5")))
        assertTrue(events.contains(PlanConfigEvent.SaveClicked))
    }

    @Test
    fun timezoneSearchDialogFiltersZonesAndEmitsSelection() {
        val events = mutableListOf<PlanConfigEvent>()
        val state = readyState().copy(isTimezoneDialogVisible = true, timezoneSearchQuery = "Tokyo")
        show(state, events)

        composeTestRule.onNodeWithTag("plan_config_timezone_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("plan_config_timezone_item_Asia/Tokyo")
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(
            listOf<PlanConfigEvent>(PlanConfigEvent.ZoneSelected(ZoneId.of("Asia/Tokyo"))),
            events,
        )
    }

    @Test
    fun darkThemeLargeFontAndSaveErrorsRemainScrollableAndAccessible() {
        val events = mutableListOf<PlanConfigEvent>()
        show(
            readyState().copy(saveErrorCode = PlanConfigErrorCode.STORAGE_FAILED),
            events,
            darkTheme = true,
            fontScale = 2f,
        )

        composeTestRule.onNodeWithTag("plan_config_save_error").assertIsDisplayed()
        composeTestRule.onNodeWithTag("plan_config_save").performScrollTo().assertHeightIsAtLeast(48.dp)
        assertEquals(0, composeTestRule.onAllNodesWithTag("plan_config_load_error").fetchSemanticsNodes().size)
    }

    @Test
    fun dateAndTimePickerDialogsToggleKeyboardAndConfirmSelections() {
        val events = mutableListOf<PlanConfigEvent>()
        val holder = show(readyState().copy(isDatePickerVisible = true), events)

        composeTestRule.onNodeWithTag("plan_config_date_confirm").performClick()
        assertTrue(events.any { it is PlanConfigEvent.DateSelected })

        holder.value = readyState().copy(isTimePickerVisible = true)
        composeTestRule.onNodeWithTag("plan_config_time_toggle_mode").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Usa orologio").assertIsDisplayed()
        composeTestRule.onNodeWithTag("plan_config_time_confirm").performClick()
        assertEquals(PlanConfigEvent.TimeSelected(LocalTime.of(8, 0)), events.last())
    }
}
