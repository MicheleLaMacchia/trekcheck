package it.trekcheck.app.domain.usecase

import it.trekcheck.app.domain.model.GpxImportError
import it.trekcheck.app.domain.model.GpxImportLimits
import it.trekcheck.app.domain.model.GpxParseResult
import it.trekcheck.app.domain.model.ImportGpxResult
import it.trekcheck.app.domain.model.SavedPlanDataAvailability
import it.trekcheck.app.domain.model.SavedPlanSummary
import it.trekcheck.app.domain.repository.GpxInputSource
import it.trekcheck.app.domain.repository.GpxParser
import it.trekcheck.app.domain.repository.ImportedRouteRecord
import it.trekcheck.app.domain.repository.RouteFileStorage
import it.trekcheck.app.domain.repository.RouteImportRepository
import it.trekcheck.app.domain.repository.RouteStagingSession
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class ImportGpxUseCase(
    private val gpxParser: GpxParser,
    private val routeFileStorage: RouteFileStorage,
    private val routeImportRepository: RouteImportRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock = Clock.systemUTC(),
    private val defaultZoneProvider: () -> ZoneId = { ZoneId.systemDefault() },
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val fallbackRouteTitle: String = DEFAULT_FALLBACK_ROUTE_TITLE,
) {
    suspend operator fun invoke(source: GpxInputSource): ImportGpxResult = withContext(ioDispatcher) {
        val knownBytes = source.knownByteSize
        if (knownBytes != null && knownBytes > GpxImportLimits.MAX_FILE_BYTES) {
            return@withContext ImportGpxResult.Failure(GpxImportError.FileTooLarge)
        }

        val rawStream = try {
            source.openStream()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            return@withContext ImportGpxResult.Failure(GpxImportError.ReadFailed)
        } ?: return@withContext ImportGpxResult.Failure(GpxImportError.ReadFailed)

        val staging: RouteStagingSession = try {
            routeFileStorage.openStaging()
        } catch (cancellation: CancellationException) {
            closeQuietly(rawStream)
            throw cancellation
        } catch (_: Throwable) {
            closeQuietly(rawStream)
            return@withContext ImportGpxResult.Failure(GpxImportError.StorageFailed)
        }

        var committedRouteId: String? = null
        try {
            val teeStream = TeeInputStream(
                delegate = rawStream,
                mirrorOutput = staging.outputStream(),
                maxCopyBytes = GpxImportLimits.MAX_FILE_BYTES,
            )

            val parseResult = try {
                gpxParser.parse(
                    inputStream = teeStream,
                    maxBytes = GpxImportLimits.MAX_FILE_BYTES,
                    maxPoints = GpxImportLimits.MAX_POINT_COUNT,
                )
            } finally {
                closeQuietly(rawStream)
            }

            if (teeStream.stagingWriteFailed) {
                staging.discard()
                return@withContext ImportGpxResult.Failure(GpxImportError.StorageFailed)
            }

            val parsedRoute = when (parseResult) {
                is GpxParseResult.Failure -> {
                    staging.discard()
                    return@withContext ImportGpxResult.Failure(parseResult.error)
                }

                is GpxParseResult.Success -> parseResult.route
            }

            val routeId = idGenerator()
            val storedFiles = try {
                routeFileStorage.commitStagedRoute(
                    routeId = routeId,
                    staging = staging,
                    geometry = parsedRoute.geometry,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                staging.discard()
                return@withContext ImportGpxResult.Failure(GpxImportError.StorageFailed)
            }
            committedRouteId = routeId

            val resolvedTitle = resolveRouteTitle(
                parsedName = parsedRoute.routeName,
                suggestedTitle = source.suggestedTitle,
                fallbackTitle = fallbackRouteTitle,
            )

            val summary = SavedPlanSummary(
                id = routeId,
                title = resolvedTitle,
                distanceMeters = parsedRoute.distanceMeters,
                elevationGainMeters = parsedRoute.elevationGainMeters,
                departureInstantUtc = null,
                routeZoneId = defaultZoneProvider(),
                estimatedDurationSeconds = null,
                updatedAtUtc = clock.instant(),
                dataAvailability = SavedPlanDataAvailability.ROUTE_ONLY,
                hasElevation = parsedRoute.hasElevation,
                hasTimestamps = parsedRoute.hasTimestamps,
            )

            try {
                routeImportRepository.saveImportedRoute(
                    ImportedRouteRecord(
                        summary = summary,
                        parsedRoute = parsedRoute,
                        storedFiles = storedFiles,
                    ),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                cleanupRouteSafely(routeId)
                return@withContext ImportGpxResult.Failure(GpxImportError.StorageFailed)
            }

            ImportGpxResult.Success(
                plan = summary,
                hasElevation = parsedRoute.hasElevation,
                hasTimestamps = parsedRoute.hasTimestamps,
            )
        } catch (cancellation: CancellationException) {
            withContext(NonCancellable) {
                staging.discard()
                committedRouteId?.let { cleanupRouteSafely(it) }
            }
            throw cancellation
        } catch (_: Throwable) {
            staging.discard()
            committedRouteId?.let { cleanupRouteSafely(it) }
            ImportGpxResult.Failure(GpxImportError.StorageFailed)
        } finally {
            closeQuietly(rawStream)
        }
    }

    private suspend fun cleanupRouteSafely(routeId: String) {
        withContext(NonCancellable) {
            try {
                routeFileStorage.deleteRoute(routeId)
            } catch (_: Throwable) {
            }
        }
    }

    private fun closeQuietly(stream: InputStream) {
        try {
            stream.close()
        } catch (_: IOException) {
        }
    }

    private class TeeInputStream(
        private val delegate: InputStream,
        private val mirrorOutput: OutputStream,
        private val maxCopyBytes: Long,
    ) : InputStream() {
        private var copiedBytes: Long = 0L
        var stagingWriteFailed: Boolean = false
            private set

        override fun read(): Int {
            val value = delegate.read()
            if (value >= 0 && copiedBytes < maxCopyBytes) {
                try {
                    mirrorOutput.write(value)
                    copiedBytes += 1L
                } catch (ioe: IOException) {
                    stagingWriteFailed = true
                    throw StagingWriteIOException(ioe)
                }
            }
            return value
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val count = delegate.read(b, off, len)
            if (count > 0) {
                val allowedToMirror = (maxCopyBytes - copiedBytes).coerceAtLeast(0L).toInt()
                val toWrite = minOf(count, allowedToMirror)
                if (toWrite > 0) {
                    try {
                        mirrorOutput.write(b, off, toWrite)
                        copiedBytes += toWrite.toLong()
                    } catch (ioe: IOException) {
                        stagingWriteFailed = true
                        throw StagingWriteIOException(ioe)
                    }
                }
            }
            return count
        }

        override fun close() = Unit
    }

    private class StagingWriteIOException(cause: IOException) : IOException("STAGING_WRITE_FAILED", cause)

    companion object {
        private const val DEFAULT_FALLBACK_ROUTE_TITLE = "Percorso GPX"
        private const val MAX_TITLE_LENGTH = 160

        internal fun resolveRouteTitle(
            parsedName: String?,
            suggestedTitle: String?,
            fallbackTitle: String = DEFAULT_FALLBACK_ROUTE_TITLE,
        ): String {
            val sanitizedParsed = sanitizeTitle(parsedName)
            if (sanitizedParsed != null) return sanitizedParsed

            val withoutExt = suggestedTitle
                ?.trim()
                ?.let { name ->
                    when {
                        name.endsWith(".gpx", ignoreCase = true) -> name.dropLast(4)
                        name.endsWith(".xml", ignoreCase = true) -> name.dropLast(4)
                        else -> name
                    }
                }
            val sanitizedSuggested = sanitizeTitle(withoutExt)
            if (sanitizedSuggested != null) return sanitizedSuggested

            return sanitizeTitle(fallbackTitle) ?: DEFAULT_FALLBACK_ROUTE_TITLE
        }

        private fun sanitizeTitle(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            val collapsed = buildString(raw.length) {
                var lastWasSpace = true
                for (ch in raw) {
                    if (ch.isISOControl() || ch.isWhitespace()) {
                        if (!lastWasSpace) {
                            append(' ')
                            lastWasSpace = true
                        }
                    } else {
                        append(ch)
                        lastWasSpace = false
                    }
                }
            }.trim()
            if (collapsed.isEmpty()) return null
            return if (collapsed.length <= MAX_TITLE_LENGTH) {
                collapsed
            } else {
                var end = MAX_TITLE_LENGTH
                if (Character.isHighSurrogate(collapsed[end - 1])) {
                    end -= 1
                }
                collapsed.substring(0, end).trimEnd().ifEmpty { null }
            }
        }
    }
}
