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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import it.trekcheck.app.data.local.db.TrekCheckDatabase
import it.trekcheck.app.data.local.files.VersionedRouteFileStorage
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class MainActivityTest {
    private val tempDir: File = Files.createTempDirectory("trekcheck-main-test").toFile()

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
                    shadowOf(app).checkActivities(false)
                    tempDir.deleteRecursively()
                    app.deleteDatabase(TrekCheckDatabase.DATABASE_NAME)
                    File(app.filesDir, VersionedRouteFileStorage.ROUTES_DIR_NAME).deleteRecursively()
                }
            }
        }
    }

    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(cleanStateRule)
        .around(composeTestRule)

    @Test
    fun launchesLocallyGuardsDoubleOpenHandlesCancelAndImportsOkResult() {
        composeTestRule.onNodeWithText("TrekCheck").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nessun percorso salvato").assertIsDisplayed()

        composeTestRule.onNodeWithText("Importa GPX").performScrollTo().performClick()

        val activityShadow = shadowOf(composeTestRule.activity)
        val firstLaunch = activityShadow.nextStartedActivityForResult
        assertNotNull("Expected SAF OpenDocument intent to be launched", firstLaunch)
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, firstLaunch.intent.action)
        val mimeTypes = firstLaunch.intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)
        assertNotNull(mimeTypes)
        assertTrue(mimeTypes!!.contains("application/gpx+xml"))

        // Rapid duplicate tap while picker is already open must not launch a second picker
        composeTestRule.onNodeWithText("Importa GPX").performScrollTo().performClick()
        assertNull(activityShadow.nextStartedActivityForResult)

        // Cancelling the picker resets the launch guard without errors
        activityShadow.receiveResult(firstLaunch.intent, Activity.RESULT_CANCELED, null)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Nessun percorso salvato").assertIsDisplayed()

        composeTestRule.onNodeWithText("Importa GPX").performScrollTo().performClick()
        val secondLaunch = activityShadow.nextStartedActivityForResult
        assertNotNull("Expected SAF picker to relaunch after RESULT_CANCELED", secondLaunch)

        val gpxFile = File(tempDir, "Anello_Picker.gpx").apply {
            writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
                  <trk>
                    <name>Anello Picker SAF</name>
                    <trkseg>
                      <trkpt lat="46.0000" lon="12.0000"><ele>900.0</ele></trkpt>
                      <trkpt lat="46.0100" lon="12.0000"><ele>1100.0</ele></trkpt>
                    </trkseg>
                  </trk>
                </gpx>
                """.trimIndent(),
            )
        }
        val resultIntent = Intent().setData(Uri.fromFile(gpxFile))
        activityShadow.receiveResult(secondLaunch.intent, Activity.RESULT_OK, resultIntent)

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Anello Picker SAF")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Percorso importato senza timestamp").assertIsDisplayed()
        composeTestRule.onNode(hasScrollToKeyAction()).performScrollToNode(hasText("Anello Picker SAF"))
        composeTestRule.onNodeWithText("Anello Picker SAF").assertIsDisplayed()
    }

    @Test
    fun activityNotFoundWhenLaunchingDocumentPickerSurfacesReadFailedAndResetsGuard() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).checkActivities(true)

        composeTestRule.onNodeWithText("Importa GPX").performScrollTo().performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Impossibile leggere il file")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Impossibile leggere il file").assertIsDisplayed()

        // Guard must be cleared after ActivityNotFoundException so retry launches once handlers exist
        shadowOf(app).checkActivities(false)
        composeTestRule.onNodeWithText("Scegli un altro file").performScrollTo().performClick()
        val retryLaunch = shadowOf(composeTestRule.activity).nextStartedActivityForResult
        assertNotNull(retryLaunch)
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, retryLaunch.intent.action)
    }
}
