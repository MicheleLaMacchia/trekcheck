package it.trekcheck.app

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import it.trekcheck.app.data.local.db.TrekCheckDatabase
import it.trekcheck.app.data.local.files.VersionedRouteFileStorage
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "it")
class MainActivityRoutePreviewTest {
    private val tempDir: File = Files.createTempDirectory("trekcheck-preview-test").toFile()

    private val cleanStateRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                val app = ApplicationProvider.getApplicationContext<Application>()
                app.deleteDatabase(TrekCheckDatabase.DATABASE_NAME)
                File(app.filesDir, VersionedRouteFileStorage.ROUTES_DIR_NAME).deleteRecursively()
                shadowOf(app).checkActivities(false)
                try {
                    base.evaluate()
                } finally {
                    tempDir.deleteRecursively()
                    app.deleteDatabase(TrekCheckDatabase.DATABASE_NAME)
                    File(app.filesDir, VersionedRouteFileStorage.ROUTES_DIR_NAME).deleteRecursively()
                }
            }
        }
    }

    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(cleanStateRule).around(composeTestRule)

    private fun importRoute(name: String) {
        composeTestRule.onNodeWithText("Importa GPX").performScrollTo().performClick()
        val activityShadow = shadowOf(composeTestRule.activity)
        val launch = activityShadow.nextStartedActivityForResult
        val file = File(tempDir, "route.gpx").apply {
            writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
                  <trk><name>$name</name><trkseg>
                    <trkpt lat="46.0000" lon="12.0000"><ele>900.0</ele></trkpt>
                    <trkpt lat="46.0050" lon="12.0030"><ele>1100.0</ele></trkpt>
                    <trkpt lat="46.0100" lon="12.0000"><ele>1000.0</ele></trkpt>
                  </trkseg></trk>
                </gpx>
                """.trimIndent(),
            )
        }
        activityShadow.receiveResult(launch.intent, Activity.RESULT_OK, Intent().setData(Uri.fromFile(file)))
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Apri anteprima").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun opensPreviewSurvivesRecreateWithDialogDraftRenamesAndReturnsHomeWithBack() {
        importRoute("Anello Anteprima")
        composeTestRule.onNodeWithText("Apri anteprima").performScrollTo().performClick()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Riepilogo").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("route_preview_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Salita").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("+200 m (stimata)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Discesa").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("−100 m (stimata)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("route_track_canvas").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("elevation_profile_canvas").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("elevation_profile_text_alternative").performScrollTo().assertIsDisplayed()

        // Open RenameDialog with an edited draft and recreate: ViewModel state and draft must survive
        composeTestRule.onNodeWithTag("route_preview_rename").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("route_rename_field").performTextReplacement("Bozza Pre Rotazione")

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Bozza Pre Rotazione").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("route_rename_field").performTextReplacement("Titolo Rinominato")
        composeTestRule.onNodeWithTag("route_rename_confirm").performClick()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Titolo Rinominato").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("route_preview_title").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("route_track_text_alternative").performScrollTo().assertIsDisplayed()

        composeTestRule.runOnUiThread { composeTestRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Percorsi locali").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Titolo Rinominato").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun opensPreviewDirectlyFromSavedPlanCard() {
        importRoute("Sentiero Card")
        composeTestRule.onNodeWithText("Chiudi").performScrollTo().performClick()
        composeTestRule.onNode(hasScrollToKeyAction()).performScrollToNode(hasText("Sentiero Card"))
        composeTestRule.onNodeWithText("Sentiero Card").performClick()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Riepilogo").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("route_preview_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Salita").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun deleteFromPreviewRemovesFilesAndReturnsToEmptyHome() {
        importRoute("Anello Da Eliminare")
        val app = ApplicationProvider.getApplicationContext<Application>()
        val routesRoot = File(app.filesDir, VersionedRouteFileStorage.ROUTES_DIR_NAME)
        assertEquals(1, routesRoot.listFiles { f -> f.isDirectory && f.name != "staging" && !f.name.startsWith(".") }!!.size)

        composeTestRule.onNodeWithText("Apri anteprima").performScrollTo().performClick()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Riepilogo").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("route_preview_delete").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("route_delete_cancel").performClick()
        assertTrue(routesRoot.walkTopDown().any { it.name == "geometry_v1.bin" })

        composeTestRule.onNodeWithTag("route_preview_delete").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("route_delete_confirm").performClick()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Nessun percorso salvato").fetchSemanticsNodes().isNotEmpty()
        }
        assertFalse(routesRoot.walkTopDown().any { it.name == "geometry_v1.bin" || it.name == "source_v1.gpx" })
    }
}
