package it.trekcheck.app.ui.preview

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import it.trekcheck.app.domain.geo.RouteGeometrySimplifier
import it.trekcheck.app.domain.model.GpxSegment
import it.trekcheck.app.domain.model.GpxTrackPoint
import it.trekcheck.app.domain.model.NormalizedRouteGeometry
import it.trekcheck.app.domain.model.RouteManagementErrorCode
import it.trekcheck.app.domain.model.RoutePreviewDetails
import it.trekcheck.app.ui.theme.TrekCheckTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "it")
class RoutePreviewScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun details(withElevation: Boolean, gain: Double? = if (withElevation) 190.0 else null): RoutePreviewDetails {
        val geometry = NormalizedRouteGeometry(
            listOf(
                GpxSegment(
                    List(30) {
                        GpxTrackPoint(46.0 + it * 0.001, 12.0 + it * 0.0005, if (withElevation) 1000.0 + it * 10 else null)
                    },
                ),
            ),
        )
        val analysis = RouteGeometrySimplifier.analyze(geometry)
        return RoutePreviewDetails(
            routeId = "r1",
            title = "Monte Pizzoc",
            distanceMeters = analysis.distanceMeters,
            start = analysis.start,
            end = analysis.end,
            highestPoint = analysis.highest,
            minElevationMeters = analysis.minElevationMeters,
            maxElevationMeters = analysis.maxElevationMeters,
            elevationGainMeters = gain,
            elevationLossMeters = gain?.let { 10.0 },
            hasElevation = withElevation,
            pointCount = geometry.pointCount,
            segmentCount = geometry.segmentCount,
            track = RouteGeometrySimplifier.simplify(geometry, analysis),
            profile = if (gain != null) RouteGeometrySimplifier.sampleProfile(geometry) else null,
        )
    }

    private fun show(
        state: RoutePreviewUiState,
        events: MutableList<RoutePreviewEvent> = mutableListOf(),
        darkTheme: Boolean = false,
        fontScale: Float = 1f,
    ): MutableState<RoutePreviewUiState> {
        val stateHolder = mutableStateOf(state)
        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale)) {
                TrekCheckTheme(darkTheme = darkTheme) {
                    RoutePreviewScreen(uiState = stateHolder.value, onEvent = { events.add(it) })
                }
            }
        }
        return stateHolder
    }

    private fun ready(d: RoutePreviewDetails) = RoutePreviewUiState(status = RoutePreviewStatus.READY, details = d)

    @Test
    fun showsMetricsEstimatedLabelsCanvasesAndTextAlternatives() {
        show(ready(details(true)))
        composeTestRule.onNodeWithText("Monte Pizzoc").assertIsDisplayed()
        composeTestRule.onNodeWithTag("route_preview_summary").assertIsDisplayed()
        composeTestRule.onNodeWithText("Punto più alto").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Salita").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("+190 m (stimata)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Discesa").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("−10 m (stimata)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("route_track_canvas").performScrollTo().assertIsDisplayed()
            .assertContentDescriptionContains("Disegno 2D", substring = true)
        composeTestRule.onNodeWithText("Cerchio: inizio · Quadrato: fine · Triangolo: punto più alto")
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("route_track_text_alternative").performScrollTo()
            .assertTextContains("Tracciato di 30 punti in 1 segmenti separati", substring = true)
        composeTestRule.onNodeWithTag("elevation_profile_canvas").performScrollTo().assertIsDisplayed()
        val profileAlt = composeTestRule.onNodeWithTag("elevation_profile_text_alternative").performScrollTo()
        profileAlt.assertTextContains("Profilo altimetrico stimato", substring = true)
        profileAlt.assertTextContains("Salita +190 m (stimata). Discesa −10 m (stimata).", substring = true)
        profileAlt.assert(
            SemanticsMatcher("does not duplicate stimata") { node ->
                val text = node.config[SemanticsProperties.Text].joinToString { it.text }
                !text.contains("stimata +") && !text.contains("stimata −")
            },
        )
        composeTestRule.onNodeWithTag("route_preview_offline_note").assertTextContains("senza rete", substring = true)
    }

    @Test
    fun withoutElevationShowsLimitNoticeWithoutProfileHighestPointOrZeroMeters() {
        show(ready(details(false)))
        composeTestRule.onNodeWithTag("route_preview_elevation_notice").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Quota non presente nel file").assertIsDisplayed()
        composeTestRule.onNodeWithTag("route_track_canvas").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Cerchio: inizio · Quadrato: fine").performScrollTo().assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("Triangolo", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithTag("elevation_profile_canvas").fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithText("Punto più alto").fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithText("0 m", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun singleElevationShowsPartialNoticeButKeepsHighestPoint() {
        show(ready(details(true, gain = null)))
        composeTestRule.onNodeWithText("Quota insufficiente per i dislivelli").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Punto più alto").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithTag("elevation_profile_canvas").fetchSemanticsNodes().size)
    }

    @Test
    fun configureCtaAndActionsAreEnabledMeetTouchTargetsAndEmitEvents() {
        val events = mutableListOf<RoutePreviewEvent>()
        show(ready(details(true)), events)
        composeTestRule.onNodeWithTag("route_preview_configure").performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeTestRule.onNodeWithTag("route_preview_back").performScrollTo()
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick()
        composeTestRule.onNodeWithTag("route_preview_rename").performScrollTo()
            .assertHeightIsAtLeast(48.dp).performClick()
        composeTestRule.onNodeWithTag("route_preview_delete").performScrollTo()
            .assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(
            listOf(
                RoutePreviewEvent.ConfigureClicked,
                RoutePreviewEvent.BackClicked,
                RoutePreviewEvent.RenameClicked,
                RoutePreviewEvent.DeleteClicked,
            ),
            events,
        )
    }

    @Test
    fun renameDialogShowsInlineErrorsCounterImeDoneAndSavingDisabledState() {
        val events = mutableListOf<RoutePreviewEvent>()
        val state = show(
            ready(details(true)).copy(
                renameDialog = RenameDialogState("", RouteManagementErrorCode.INVALID_TITLE),
            ),
            events,
        )
        composeTestRule.onNodeWithText("Inserisci un titolo non vuoto.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("route_rename_field").performTextReplacement("Altro")
        composeTestRule.onNodeWithTag("route_rename_field").performImeAction()
        composeTestRule.onNodeWithTag("route_rename_confirm").assertHeightIsAtLeast(48.dp).performClick()
        composeTestRule.onNodeWithText("Annulla").performClick()
        assertTrue(events.contains(RoutePreviewEvent.RenameDraftChanged("Altro")))
        assertEquals(2, events.count { it == RoutePreviewEvent.RenameConfirmed })
        assertTrue(events.contains(RoutePreviewEvent.RenameDismissed))

        state.value = ready(details(true)).copy(
            renameDialog = RenameDialogState("Titolo", RouteManagementErrorCode.NOT_FOUND),
        )
        composeTestRule.onNodeWithText("Percorso non trovato sul dispositivo.").assertIsDisplayed()

        state.value = ready(details(true)).copy(
            renameDialog = RenameDialogState("Titolo", errorCode = null, isSaving = true),
        )
        composeTestRule.onNodeWithText("6/160 caratteri", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("route_rename_field").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("route_rename_confirm").assertIsNotEnabled()
    }

    @Test
    fun deleteDialogOffersCancelAndConfirmAndDisablesDuringDelete() {
        val events = mutableListOf<RoutePreviewEvent>()
        val state = show(ready(details(true)).copy(isDeleteDialogVisible = true), events)
        composeTestRule.onNodeWithText("Eliminare il percorso?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("route_delete_cancel").assertHeightIsAtLeast(48.dp).performClick()
        composeTestRule.onNodeWithTag("route_delete_confirm").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(listOf(RoutePreviewEvent.DeleteDismissed, RoutePreviewEvent.DeleteConfirmed), events)

        state.value = ready(details(true)).copy(isDeleteDialogVisible = true, isDeleting = true)
        composeTestRule.onNodeWithText("Eliminazione in corso…").assertIsDisplayed()
        composeTestRule.onNodeWithTag("route_delete_confirm").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("route_delete_cancel").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("route_preview_back").assertIsNotEnabled()
    }

    @Test
    fun loadingNotFoundStorageErrorsAndDismissibleActionNoticeRenderProperly() {
        val events = mutableListOf<RoutePreviewEvent>()
        val state = show(RoutePreviewUiState(status = RoutePreviewStatus.LOADING), events)
        composeTestRule.onNodeWithText("Caricamento del percorso…").assertIsDisplayed()

        state.value = RoutePreviewUiState(status = RoutePreviewStatus.ERROR, loadErrorCode = RouteManagementErrorCode.NOT_FOUND)
        composeTestRule.onNodeWithText("Percorso non trovato").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("Riprova").fetchSemanticsNodes().size)

        state.value = RoutePreviewUiState(status = RoutePreviewStatus.ERROR, loadErrorCode = RouteManagementErrorCode.STORAGE_FAILED)
        composeTestRule.onNodeWithText("Operazione non riuscita").assertIsDisplayed()
        composeTestRule.onNodeWithText("Riprova").assertIsDisplayed()

        state.value = ready(details(true)).copy(actionErrorCode = RouteManagementErrorCode.STORAGE_FAILED)
        composeTestRule.onNodeWithTag("route_preview_notice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Operazione non riuscita").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chiudi").performClick()
        assertEquals(listOf<RoutePreviewEvent>(RoutePreviewEvent.DismissActionError), events)
    }

    @Test
    fun corruptedGeometryOffersRetryBackAndDelete() {
        val events = mutableListOf<RoutePreviewEvent>()
        show(
            RoutePreviewUiState(
                status = RoutePreviewStatus.ERROR,
                loadErrorCode = RouteManagementErrorCode.CORRUPTED_GEOMETRY,
            ),
            events,
        )
        composeTestRule.onNodeWithText("Dati del percorso non leggibili").assertIsDisplayed()
        composeTestRule.onNodeWithText("Riprova").performClick()
        composeTestRule.onNodeWithText("Torna ai percorsi").performClick()
        composeTestRule.onNodeWithTag("route_preview_delete").performClick()
        assertEquals(
            listOf(RoutePreviewEvent.RetryClicked, RoutePreviewEvent.BackClicked, RoutePreviewEvent.DeleteClicked),
            events,
        )
    }

    @Test
    fun darkThemeAndLargeFontScaleKeepPreviewScrollableAndAccessible() {
        show(ready(details(true)), darkTheme = true, fontScale = 2f)
        composeTestRule.onNodeWithText("Monte Pizzoc").assertIsDisplayed()
        composeTestRule.onNodeWithTag("route_preview_delete").performScrollTo().assertHeightIsAtLeast(48.dp)
        assertFalse(composeTestRule.onAllNodesWithTag("elevation_profile_canvas").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun configuredDepartureShowsRouteTimezoneDifferencePaceAndEditCta() {
        val configured = details(true).copy(
            departureInstantUtc = java.time.Instant.parse("2026-07-15T12:00:00Z"),
            routeZoneId = java.time.ZoneId.of("America/New_York"),
            paceProfile = it.trekcheck.app.domain.model.PaceProfile(
                it.trekcheck.app.domain.model.PaceProfileType.CUSTOM,
                it.trekcheck.app.domain.model.PaceProfile.kmhToMps(4.5),
            ),
        )
        composeTestRule.setContent {
            TrekCheckTheme {
                RoutePreviewScreen(
                    uiState = ready(configured),
                    onEvent = {},
                    deviceZoneId = java.time.ZoneId.of("Europe/Rome"),
                )
            }
        }
        composeTestRule.onNodeWithText("15 lug 2026 · 08:00 (America/New_York, UTC-04:00)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("America/New_York (diverso dal telefono: Europe/Rome)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Personalizzato (4,5 km/h)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Modifica configurazione").performScrollTo().assertIsDisplayed()
    }
}
