package it.trekcheck.app.data.gpx

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import it.trekcheck.app.domain.repository.GpxInputSource
import java.io.File
import java.io.InputStream

class ContentResolverGpxSource(
    context: Context,
    private val uri: Uri,
) : GpxInputSource {
    private val contentResolver: ContentResolver = context.applicationContext.contentResolver

    private val metadata: SourceMetadata by lazy {
        queryMetadata()
    }

    override val suggestedTitle: String?
        get() = metadata.suggestedTitle

    override val knownByteSize: Long?
        get() = metadata.knownByteSize

    override fun openStream(): InputStream? = contentResolver.openInputStream(uri)

    override fun toString(): String = "ContentResolverGpxSource"

    private fun queryMetadata(): SourceMetadata {
        var displayName: String? = null
        var sizeBytes: Long? = null

        runCatching {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                        displayName = cursor.getString(nameIndex)
                    }
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        sizeBytes = cursor.getLong(sizeIndex).takeIf { it > 0L }
                    }
                }
            }
        }

        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            if (displayName.isNullOrBlank()) {
                displayName = uri.lastPathSegment
            }
            if (sizeBytes == null) {
                sizeBytes = uri.path
                    ?.let(::File)
                    ?.takeIf(File::isFile)
                    ?.length()
                    ?.takeIf { it > 0L }
            }
        }

        val sanitizedName = displayName
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        return SourceMetadata(
            suggestedTitle = sanitizedName,
            knownByteSize = sizeBytes,
        )
    }

    private data class SourceMetadata(
        val suggestedTitle: String?,
        val knownByteSize: Long?,
    )
}
