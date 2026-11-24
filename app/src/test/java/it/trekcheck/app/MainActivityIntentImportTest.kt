package it.trekcheck.app

import android.app.Application
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performScrollToNode
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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "it")
class MainActivityIntentImportTest {
    private val tempDir: File = Files.createTempDirectory("trekcheck-intent-test").toFile()

    private val validGpxFile: File = File(tempDir, "Anello_Pizzoc.gpx").apply {
        writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
              <trk>
                <name>Anello Monte Pizzoc</name>
                <trkseg>
                  <trkpt lat="46.0000" lon="12.0000"><ele>1000.0</ele></trkpt>
                  <trkpt lat="46.0100" lon="12.0000"><ele>1250.0</ele></trkpt>
                </trkseg>
              </trk>
            </gpx>
            """.trimIndent(),
        )
    }

    private val cleanupRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                val app = ApplicationProvider.getApplicationContext<Application>()
                app.deleteDatabase(TrekCheckDatabase.DATABASE_NAME)
                File(app.filesDir, VersionedRouteFileStorage.ROUTES_DIR_NAME).deleteRecursively()
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

    private val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(cleanupRule)
        .around(composeTestRule)

    @Test
    fun importsActionViewGpxPersistsAcrossRecreationAndHandlesActionSendErrorInOnNewIntent() {
        val viewIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.fromFile(validGpxFile),
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java,
        )
        val controller = Robolectric.buildActivity(MainActivity::class.java, viewIntent).setup()
        try {
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.onAllNodesWithText("Percorso importato senza timestamp")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNode(hasScrollToKeyAction()).performScrollToKey("notice")
            composeTestRule.onNodeWithText("Percorso importato senza timestamp").assertIsDisplayed()

            composeTestRule.onNode(hasScrollToKeyAction()).performScrollToNode(hasText("Anello Monte Pizzoc"))
            composeTestRule.onNodeWithText("Anello Monte Pizzoc").assertIsDisplayed()

            // Recreating activity must keep the Room-saved route and notice without duplicating it
            controller.recreate()
            composeTestRule.waitForIdle()
            composeTestRule.onNode(hasScrollToKeyAction()).performScrollToKey("notice")
            composeTestRule.onNodeWithText("Percorso importato senza timestamp").assertIsDisplayed()
            composeTestRule.onNode(hasScrollToKeyAction()).performScrollToNode(hasText("Anello Monte Pizzoc"))
            assertEquals(1, composeTestRule.onAllNodesWithText("Anello Monte Pizzoc").fetchSemanticsNodes().size)

            // Sending an unsafe GPX via ACTION_SEND triggers onNewIntent and shows recoverable error without losing saved route
            val unsafeFile = File(tempDir, "unsafe.gpx").apply {
                writeText("""<!DOCTYPE gpx><gpx/>""")
            }
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/gpx+xml"
                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(unsafeFile))
            }
            controller.newIntent(sendIntent)

            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.onAllNodesWithText("Struttura XML non consentita")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNode(hasScrollToKeyAction()).performScrollToKey("notice")
            composeTestRule.onNodeWithText("Struttura XML non consentita").assertIsDisplayed()
            composeTestRule.onNodeWithText("Scegli un altro file").performScrollTo().assertIsDisplayed()

            composeTestRule.onNode(hasScrollToKeyAction()).performScrollToNode(hasText("Anello Monte Pizzoc"))
            composeTestRule.onNodeWithText("Anello Monte Pizzoc").assertIsDisplayed()

            composeTestRule.onNode(hasScrollToKeyAction()).performScrollToKey("notice")
            composeTestRule.onNodeWithContentDescription("Chiudi avviso: Struttura XML non consentita")
                .performScrollTo()
                .performClick()
            composeTestRule.onNodeWithText("Struttura XML non consentita").assertDoesNotExist()
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun manifestIntentFiltersResolveActionViewAndActionSendViaPackageManager() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val pm = context.packageManager

        fun matchesMainActivity(intent: Intent): Boolean {
            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }
            return resolved.any { it.activityInfo.name == MainActivity::class.java.name }
        }

        assertTrue(
            matchesMainActivity(
                Intent(Intent.ACTION_VIEW).setDataAndType(
                    Uri.parse("content://com.example.files/docs/a.b.c.d.Gpx"),
                    "text/plain",
                ),
            ),
        )
        assertTrue(
            matchesMainActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("file:///sdcard/a.b.c.gpx")),
            ),
        )
        assertTrue(
            matchesMainActivity(
                Intent(Intent.ACTION_VIEW).setDataAndType(
                    Uri.parse("content://com.example.files/track"),
                    "application/gpx+xml",
                ),
            ),
        )
        assertTrue(
            matchesMainActivity(
                Intent(Intent.ACTION_SEND).apply { type = "application/gpx+xml" },
            ),
        )
        assertFalse(
            matchesMainActivity(
                Intent(Intent.ACTION_SEND).apply { type = "text/plain" },
            ),
        )
    }

    @Test
    @Config(sdk = [26], qualifiers = "it")
    fun handlesActionSendExtraStreamSafelyOnSdk26() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val invalidParcelableIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, Bundle().apply { putString("unexpected", "bundle") })
        }

        val controller = Robolectric.buildActivity(MainActivity::class.java, invalidParcelableIntent).setup()
        try {
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.onAllNodesWithText("Impossibile leggere il file")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText("Impossibile leggere il file").assertIsDisplayed()

            val validSendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/gpx+xml"
                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(validGpxFile))
            }
            controller.newIntent(validSendIntent)

            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.onAllNodesWithText("Anello Monte Pizzoc")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNode(hasScrollToKeyAction()).performScrollToNode(hasText("Anello Monte Pizzoc"))
            composeTestRule.onNodeWithText("Anello Monte Pizzoc").assertIsDisplayed()
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun ignoresHistoryReplayHandlesNullUriReadFailedAndFallsBackToClipData() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val historyIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.fromFile(validGpxFile),
            context,
            MainActivity::class.java,
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
        }

        val controller = Robolectric.buildActivity(MainActivity::class.java, historyIntent).setup()
        try {
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Nessun percorso salvato").assertIsDisplayed()

            controller.newIntent(Intent(Intent.ACTION_VIEW))
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.onAllNodesWithText("Impossibile leggere il file")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText("Impossibile leggere il file").assertIsDisplayed()

            val clipOnlySendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/gpx+xml"
                clipData = ClipData.newRawUri("gpx", Uri.fromFile(validGpxFile))
            }
            controller.newIntent(clipOnlySendIntent)

            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.onAllNodesWithText("Anello Monte Pizzoc")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNode(hasScrollToKeyAction()).performScrollToNode(hasText("Anello Monte Pizzoc"))
            composeTestRule.onNodeWithText("Anello Monte Pizzoc").assertIsDisplayed()
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
