package it.trekcheck.app.data.gpx

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ContentResolverGpxSourceTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun readsMetadataAndStreamFromContentUriWithoutExposingUriInToString() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val gpxFile = tempFolder.newFile("Anello_Test.gpx")
        gpxFile.writeText("<gpx/>")
        TestGpxContentProvider.backingFile = gpxFile
        TestGpxContentProvider.displayName = "folder/Sentiero_Privato.gpx"

        Robolectric.buildContentProvider(TestGpxContentProvider::class.java)
            .create("it.trekcheck.app.testprovider")

        val uri = Uri.parse("content://it.trekcheck.app.testprovider/secret_id_123/Anello_Test.gpx")
        val source = ContentResolverGpxSource(context, uri)

        assertEquals("Sentiero_Privato.gpx", source.suggestedTitle)
        assertEquals(gpxFile.length(), source.knownByteSize)
        assertEquals("<gpx/>", source.openStream()!!.bufferedReader().readText())

        val rendered = source.toString()
        assertFalse(rendered.contains("secret_id_123"))
        assertFalse(rendered.contains("content://"))
        assertEquals("ContentResolverGpxSource", rendered)
    }

    @Test
    fun fallsBackToFilenameForFileUriAndFailsSafelyForUnreadableContentUri() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val gpxFile = tempFolder.newFile("Sentiero_Locale.gpx")
        gpxFile.writeText("<gpx/>")

        val fileSource = ContentResolverGpxSource(context, Uri.fromFile(gpxFile))
        assertEquals("Sentiero_Locale.gpx", fileSource.suggestedTitle)
        assertEquals(gpxFile.length(), fileSource.knownByteSize)
        assertEquals("<gpx/>", fileSource.openStream()!!.bufferedReader().readText())

        TestGpxContentProvider.backingFile = null
        TestGpxContentProvider.displayName = null
        Robolectric.buildContentProvider(TestGpxContentProvider::class.java)
            .create("it.trekcheck.app.testprovider")

        val missingSource = ContentResolverGpxSource(
            context,
            Uri.parse("content://it.trekcheck.app.testprovider/missing.gpx"),
        )
        assertNull(missingSource.suggestedTitle)
        assertNull(missingSource.knownByteSize)
        val streamAttempt = runCatching { missingSource.openStream() }
        assertTrue(streamAttempt.isFailure || streamAttempt.getOrNull() == null)
    }

    class TestGpxContentProvider : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor {
            val cols = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
            val cursor = MatrixCursor(cols)
            val file = backingFile
            val row = Array<Any?>(cols.size) { index ->
                when (cols[index]) {
                    OpenableColumns.DISPLAY_NAME -> displayName
                    OpenableColumns.SIZE -> file?.length()
                    else -> null
                }
            }
            cursor.addRow(row)
            return cursor
        }

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            val file = backingFile ?: throw FileNotFoundException("Missing backing file")
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        override fun getType(uri: Uri): String = "application/gpx+xml"
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

        companion object {
            var backingFile: File? = null
            var displayName: String? = null
        }
    }
}
