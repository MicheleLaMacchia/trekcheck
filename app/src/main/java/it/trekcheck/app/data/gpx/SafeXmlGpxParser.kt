package it.trekcheck.app.data.gpx

import it.trekcheck.app.domain.model.GpxBoundingBox
import it.trekcheck.app.domain.model.GpxImportError
import it.trekcheck.app.domain.model.GpxImportLimits
import it.trekcheck.app.domain.model.GpxParseResult
import it.trekcheck.app.domain.model.GpxSegment
import it.trekcheck.app.domain.model.GpxSourceKind
import it.trekcheck.app.domain.model.GpxTrackPoint
import it.trekcheck.app.domain.model.NormalizedRouteGeometry
import it.trekcheck.app.domain.model.ParsedGpxRoute
import it.trekcheck.app.domain.repository.GpxParser
import java.io.CharConversionException
import java.io.IOException
import java.io.InputStream
import java.io.UTFDataFormatException
import java.nio.charset.CharacterCodingException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.xml.sax.Attributes
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.SAXNotRecognizedException
import org.xml.sax.SAXNotSupportedException
import org.xml.sax.ext.LexicalHandler
import org.xml.sax.helpers.DefaultHandler

class SafeXmlGpxParser(
    private val elevationGainThresholdMeters: Double = GpxImportLimits.DEFAULT_ELEVATION_GAIN_THRESHOLD_METERS,
) : GpxParser {

    init {
        require(elevationGainThresholdMeters.isFinite() && elevationGainThresholdMeters >= 0.0) {
            "elevationGainThresholdMeters must be finite and non-negative"
        }
    }

    override fun parse(
        inputStream: InputStream,
        maxBytes: Long,
        maxPoints: Int,
    ): GpxParseResult {
        require(maxBytes > 0L) { "maxBytes must be positive" }
        require(maxPoints >= GpxImportLimits.MIN_POINT_COUNT) {
            "maxPoints must be at least ${GpxImportLimits.MIN_POINT_COUNT}"
        }

        val guardedStream = GuardedXmlInputStream(
            delegate = inputStream,
            maxBytes = maxBytes,
        )
        val handler = GpxSaxHandler(
            maxPoints = maxPoints,
            elevationGainThresholdMeters = elevationGainThresholdMeters,
        )

        return try {
            val factory = createSecureSaxFactory()
            val saxParser = factory.newSAXParser()
            val xmlReader = saxParser.xmlReader
            xmlReader.entityResolver = EntityResolver { _, _ ->
                throw UnsafeXmlParserException()
            }
            xmlReader.dtdHandler = object : org.xml.sax.DTDHandler {
                override fun notationDecl(name: String?, publicId: String?, systemId: String?) {
                    throw UnsafeXmlParserException()
                }

                override fun unparsedEntityDecl(
                    name: String?,
                    publicId: String?,
                    systemId: String?,
                    notationName: String?,
                ) {
                    throw UnsafeXmlParserException()
                }
            }
            try {
                xmlReader.setProperty(LEXICAL_HANDLER_PROPERTY, RejectDtdLexicalHandler())
            } catch (_: SAXNotRecognizedException) {
            } catch (_: SAXNotSupportedException) {
            }
            xmlReader.contentHandler = handler
            xmlReader.errorHandler = handler
            xmlReader.parse(InputSource(guardedStream))
            handler.buildResult()
        } catch (error: Error) {
            throw error
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            mapParseFailure(throwable, guardedStream)
        }
    }

    private fun createSecureSaxFactory(): SAXParserFactory {
        val factory = SAXParserFactory.newInstance()
        factory.isNamespaceAware = true
        factory.isValidating = false
        trySetFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true)
        trySetFeature(factory, FEATURE_DISALLOW_DOCTYPE, true)
        trySetFeature(factory, FEATURE_EXTERNAL_GENERAL_ENTITIES, false)
        trySetFeature(factory, FEATURE_EXTERNAL_PARAMETER_ENTITIES, false)
        trySetFeature(factory, FEATURE_LOAD_EXTERNAL_DTD, false)
        try {
            factory.isXIncludeAware = false
        } catch (_: UnsupportedOperationException) {
        }
        return factory
    }

    private fun trySetFeature(
        factory: SAXParserFactory,
        featureName: String,
        enabled: Boolean,
    ) {
        try {
            factory.setFeature(featureName, enabled)
        } catch (_: SAXNotRecognizedException) {
        } catch (_: SAXNotSupportedException) {
        }
    }

    private fun mapParseFailure(
        throwable: Throwable,
        guardedStream: GuardedXmlInputStream,
    ): GpxParseResult.Failure {
        val error = when {
            findCause<UnsafeXmlParserException>(throwable) != null -> GpxImportError.UnsafeXml
            findCause<FileTooLargeParserException>(throwable) != null -> GpxImportError.FileTooLarge
            findCause<TooManyPointsParserException>(throwable) != null -> GpxImportError.TooManyPoints
            findCause<InvalidCoordinatesParserException>(throwable) != null -> GpxImportError.InvalidCoordinates
            findCause<InsufficientPointsParserException>(throwable) != null -> GpxImportError.InsufficientPoints
            guardedStream.underlyingReadFailed -> GpxImportError.ReadFailed
            isDoctypeFeatureRejection(throwable) -> GpxImportError.UnsafeXml
            throwable is IOException &&
                throwable !is CharConversionException &&
                throwable !is CharacterCodingException &&
                throwable !is UTFDataFormatException -> GpxImportError.ReadFailed
            else -> GpxImportError.MalformedXml
        }
        return GpxParseResult.Failure(error)
    }

    private fun isDoctypeFeatureRejection(throwable: Throwable): Boolean {
        var current: Throwable? = throwable
        val visited = HashSet<Throwable>(4)
        while (current != null && visited.add(current)) {
            if (current is SAXException) {
                val msg = current.message?.lowercase(Locale.US).orEmpty()
                if (msg.contains("doctype") ||
                    msg.contains("disallow-doctype") ||
                    msg.contains("external entity") ||
                    msg.contains("external general entity") ||
                    msg.contains("external parameter entity")
                ) {
                    return true
                }
            }
            current = current.cause
        }
        return false
    }

    private inline fun <reified T : Throwable> findCause(throwable: Throwable): T? {
        var current: Throwable? = throwable
        val visited = HashSet<Throwable>(8)
        while (current != null && visited.add(current)) {
            if (current is T) return current
            current = current.cause
        }
        return null
    }

    private class RejectDtdLexicalHandler : LexicalHandler {
        override fun startDTD(name: String?, publicId: String?, systemId: String?) {
            throw UnsafeXmlParserException()
        }

        override fun endDTD() = Unit
        override fun startEntity(name: String?) = Unit
        override fun endEntity(name: String?) = Unit
        override fun startCDATA() = Unit
        override fun endCDATA() = Unit
        override fun comment(ch: CharArray?, start: Int, length: Int) = Unit
    }

    private class GuardedXmlInputStream(
        private val delegate: InputStream,
        private val maxBytes: Long,
    ) : InputStream() {
        private var bytesRead: Long = 0L
        private var doctypeMatchIndex: Int = 0
        private var entityMatchIndex: Int = 0

        var underlyingReadFailed: Boolean = false
            private set

        override fun read(): Int {
            val byteValue = try {
                delegate.read()
            } catch (ioe: IOException) {
                if (ioe !is CharConversionException &&
                    ioe !is CharacterCodingException &&
                    ioe !is UTFDataFormatException
                ) {
                    underlyingReadFailed = true
                }
                throw ioe
            } catch (re: RuntimeException) {
                underlyingReadFailed = true
                throw re
            }
            if (byteValue == -1) return -1
            onBytesRead(1L)
            inspectByte(byteValue)
            return byteValue
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (len == 0) return 0
            val count = try {
                delegate.read(b, off, len)
            } catch (ioe: IOException) {
                if (ioe !is CharConversionException &&
                    ioe !is CharacterCodingException &&
                    ioe !is UTFDataFormatException
                ) {
                    underlyingReadFailed = true
                }
                throw ioe
            } catch (re: RuntimeException) {
                underlyingReadFailed = true
                throw re
            }
            if (count <= 0) return count
            onBytesRead(count.toLong())
            for (index in off until off + count) {
                inspectByte(b[index].toInt() and 0xFF)
            }
            return count
        }

        override fun close() {
            delegate.close()
        }

        private fun onBytesRead(delta: Long) {
            bytesRead += delta
            if (bytesRead > maxBytes) {
                throw FileTooLargeParserException()
            }
        }

        private fun inspectByte(rawByte: Int) {
            if (rawByte == 0) return
            val normalized = if (rawByte in UPPER_A..UPPER_Z) rawByte + ASCII_CASE_DELTA else rawByte
            doctypeMatchIndex = advancePrefix(normalized, doctypeMatchIndex, DOCTYPE_BYTES)
            if (doctypeMatchIndex == DOCTYPE_BYTES.size) {
                throw UnsafeXmlParserException()
            }
            entityMatchIndex = advancePrefix(normalized, entityMatchIndex, ENTITY_BYTES)
            if (entityMatchIndex == ENTITY_BYTES.size) {
                throw UnsafeXmlParserException()
            }
        }

        private fun advancePrefix(normalizedByte: Int, currentIdx: Int, target: IntArray): Int {
            return when {
                normalizedByte == target[currentIdx] -> currentIdx + 1
                normalizedByte == target[0] -> 1
                else -> 0
            }
        }
    }

    private enum class CapturingField {
        NONE,
        PRIMARY_NAME,
        FALLBACK_NAME,
        ELEVATION,
        TIME,
    }

    private class GpxSaxHandler(
        private val maxPoints: Int,
        private val elevationGainThresholdMeters: Double,
    ) : DefaultHandler() {
        private val elementStack = ArrayList<String>(16)
        private val textBuilder = StringBuilder(128)
        private var capturingField = CapturingField.NONE

        private var sawRootGpx = false
        private var sawTrackSegmentPoints = false
        private var sawRoutePoints = false
        private var totalPoints = 0

        private var primaryRouteName: String? = null
        private var fallbackRouteName: String? = null

        private var currentSegmentPoints: MutableList<GpxTrackPoint>? = null
        private val segments = ArrayList<GpxSegment>()

        private var inPoint = false
        private var pendingLat = 0.0
        private var pendingLon = 0.0
        private var pendingEle: Double? = null

        private var minLat = Double.POSITIVE_INFINITY
        private var maxLat = Double.NEGATIVE_INFINITY
        private var minLon = Double.POSITIVE_INFINITY
        private var maxLon = Double.NEGATIVE_INFINITY

        private var hasTimestamps = false

        override fun startElement(
            uri: String?,
            localName: String?,
            qName: String?,
            attributes: Attributes?,
        ) {
            val tag = resolveTagName(localName, qName)
            val isGpxNs = isSupportedGpxNamespace(uri)
            val parent = elementStack.lastOrNull()

            if (elementStack.isEmpty()) {
                if (tag != "gpx" || !isGpxNs) {
                    throw MalformedGpxParserException()
                }
                sawRootGpx = true
            }

            val effectiveTag = when {
                !isGpxNs -> "!$tag"
                tag == "gpx" -> if (elementStack.isEmpty()) "gpx" else "!$tag"
                tag == "metadata" -> if (parent == "gpx") "metadata" else "!$tag"
                tag == "trk" -> if (parent == "gpx") "trk" else "!$tag"
                tag == "trkseg" -> if (parent == "trk") "trkseg" else "!$tag"
                tag == "trkpt" -> if (parent == "trkseg") "trkpt" else "!$tag"
                tag == "rte" -> if (parent == "gpx") "rte" else "!$tag"
                tag == "rtept" -> if (parent == "rte") "rtept" else "!$tag"
                tag == "ele" -> if (parent == "trkpt" || parent == "rtept") "ele" else "!$tag"
                tag == "time" -> if (parent == "trkpt" || parent == "rtept") "time" else "!$tag"
                tag == "name" -> if (parent == "trk" || parent == "rte" || parent == "metadata" || parent == "gpx") "name" else "!$tag"
                else -> "!$tag"
            }
            elementStack.add(effectiveTag)

            when (effectiveTag) {
                "trkseg",
                "rte",
                -> {
                    currentSegmentPoints = ArrayList()
                }

                "trkpt",
                "rtept",
                -> {
                    totalPoints += 1
                    if (totalPoints > maxPoints) {
                        throw TooManyPointsParserException()
                    }
                    val lat = parseCoordinate(attributes?.getValue("", "lat"), MIN_LATITUDE, MAX_LATITUDE)
                        ?: throw InvalidCoordinatesParserException()
                    val lon = parseCoordinate(attributes?.getValue("", "lon"), MIN_LONGITUDE, MAX_LONGITUDE)
                        ?: throw InvalidCoordinatesParserException()

                    if (effectiveTag == "trkpt") {
                        sawTrackSegmentPoints = true
                    } else {
                        sawRoutePoints = true
                    }

                    inPoint = true
                    pendingLat = lat
                    pendingLon = lon
                    pendingEle = null
                }

                "ele" -> if (inPoint) {
                    startCapturing(CapturingField.ELEVATION)
                }

                "time" -> if (inPoint && !hasTimestamps) {
                    startCapturing(CapturingField.TIME)
                }

                "name" -> {
                    if ((parent == "trk" || parent == "rte") && primaryRouteName == null) {
                        startCapturing(CapturingField.PRIMARY_NAME)
                    } else if ((parent == "metadata" || parent == "gpx") && fallbackRouteName == null) {
                        startCapturing(CapturingField.FALLBACK_NAME)
                    }
                }
            }
        }

        override fun characters(ch: CharArray?, start: Int, length: Int) {
            if (capturingField == CapturingField.NONE || ch == null || length <= 0) return
            val expectedTag = when (capturingField) {
                CapturingField.ELEVATION -> "ele"
                CapturingField.TIME -> "time"
                CapturingField.PRIMARY_NAME,
                CapturingField.FALLBACK_NAME,
                -> "name"

                CapturingField.NONE -> return
            }
            if (elementStack.lastOrNull() != expectedTag) return
            val remaining = MAX_CAPTURED_CHARS - textBuilder.length
            if (remaining > 0) {
                textBuilder.append(ch, start, minOf(length, remaining))
            }
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            val currentTag = elementStack.lastOrNull()
            val parent = if (elementStack.size >= 2) elementStack[elementStack.size - 2] else null

            when (capturingField) {
                CapturingField.ELEVATION -> {
                    if (currentTag == "ele" && (parent == "trkpt" || parent == "rtept")) {
                        val parsedEle = textBuilder.toString().trim().toDoubleOrNull()
                        if (parsedEle != null && parsedEle.isFinite()) {
                            pendingEle = parsedEle
                        }
                        capturingField = CapturingField.NONE
                    }
                }

                CapturingField.TIME -> {
                    if (currentTag == "time" && (parent == "trkpt" || parent == "rtept")) {
                        if (!hasTimestamps && isValidIsoTimestamp(textBuilder.toString())) {
                            hasTimestamps = true
                        }
                        capturingField = CapturingField.NONE
                    }
                }

                CapturingField.PRIMARY_NAME -> {
                    if (currentTag == "name" && (parent == "trk" || parent == "rte")) {
                        val candidate = textBuilder.toString().trim()
                        if (candidate.isNotEmpty() && primaryRouteName == null) {
                            primaryRouteName = candidate
                        }
                        capturingField = CapturingField.NONE
                    }
                }

                CapturingField.FALLBACK_NAME -> {
                    if (currentTag == "name" && (parent == "metadata" || parent == "gpx")) {
                        val candidate = textBuilder.toString().trim()
                        if (candidate.isNotEmpty() && fallbackRouteName == null) {
                            fallbackRouteName = candidate
                        }
                        capturingField = CapturingField.NONE
                    }
                }

                CapturingField.NONE -> Unit
            }

            when {
                inPoint && (currentTag == "trkpt" || currentTag == "rtept") -> {
                    val point = GpxTrackPoint(
                        latitude = pendingLat,
                        longitude = pendingLon,
                        elevationMeters = pendingEle,
                    )
                    currentSegmentPoints?.add(point)
                    if (pendingLat < minLat) minLat = pendingLat
                    if (pendingLat > maxLat) maxLat = pendingLat
                    if (pendingLon < minLon) minLon = pendingLon
                    if (pendingLon > maxLon) maxLon = pendingLon
                    inPoint = false
                    pendingEle = null
                }

                currentTag == "trkseg" || currentTag == "rte" -> {
                    val finished = currentSegmentPoints
                    currentSegmentPoints = null
                    if (!finished.isNullOrEmpty()) {
                        segments.add(GpxSegment(finished))
                    }
                }
            }

            if (elementStack.isNotEmpty()) {
                elementStack.removeAt(elementStack.size - 1)
            }
        }

        fun buildResult(): GpxParseResult {
            if (!sawRootGpx) {
                return GpxParseResult.Failure(GpxImportError.MalformedXml)
            }
            if (totalPoints < GpxImportLimits.MIN_POINT_COUNT || segments.isEmpty()) {
                return GpxParseResult.Failure(GpxImportError.InsufficientPoints)
            }

            var totalDistanceMeters = 0.0
            var totalElevationGainMeters = 0.0
            var totalElevationLossMeters = 0.0
            var minElevationMeters = Double.POSITIVE_INFINITY
            var maxElevationMeters = Double.NEGATIVE_INFINITY
            var hasAnyElevation = false
            var validElevationPairs = 0

            for (segment in segments) {
                val points = segment.points
                var segmentAnchorElevation: Double? = null

                for (index in points.indices) {
                    val point = points[index]
                    if (index > 0) {
                        val prev = points[index - 1]
                        totalDistanceMeters += haversineDistanceMeters(
                            prev.latitude,
                            prev.longitude,
                            point.latitude,
                            point.longitude,
                        )
                        if (!totalDistanceMeters.isFinite()) {
                            throw InvalidCoordinatesParserException()
                        }
                    }

                    val ele = point.elevationMeters
                    if (ele != null) {
                        hasAnyElevation = true
                        if (ele < minElevationMeters) minElevationMeters = ele
                        if (ele > maxElevationMeters) maxElevationMeters = ele

                        val anchor = segmentAnchorElevation
                        if (anchor == null) {
                            segmentAnchorElevation = ele
                        } else {
                            validElevationPairs += 1
                            val delta = ele - anchor
                            if (!delta.isFinite()) {
                                throw InvalidCoordinatesParserException()
                            }
                            if (delta >= elevationGainThresholdMeters) {
                                totalElevationGainMeters += delta
                                if (!totalElevationGainMeters.isFinite()) {
                                    throw InvalidCoordinatesParserException()
                                }
                                segmentAnchorElevation = ele
                            } else if (-delta >= elevationGainThresholdMeters) {
                                totalElevationLossMeters += -delta
                                if (!totalElevationLossMeters.isFinite()) {
                                    throw InvalidCoordinatesParserException()
                                }
                                segmentAnchorElevation = ele
                            }
                        }
                    }
                }
            }

            val hasElevation = hasAnyElevation
            val hasGainLoss = validElevationPairs > 0
            val sourceKind = when {
                sawTrackSegmentPoints && sawRoutePoints -> GpxSourceKind.MIXED
                sawRoutePoints -> GpxSourceKind.ROUTE
                else -> GpxSourceKind.TRACK
            }

            val route = ParsedGpxRoute(
                routeName = primaryRouteName ?: fallbackRouteName,
                sourceKind = sourceKind,
                geometry = NormalizedRouteGeometry(segments),
                distanceMeters = totalDistanceMeters,
                elevationGainMeters = if (hasGainLoss) totalElevationGainMeters else null,
                elevationLossMeters = if (hasGainLoss) totalElevationLossMeters else null,
                minElevationMeters = if (hasElevation) minElevationMeters else null,
                maxElevationMeters = if (hasElevation) maxElevationMeters else null,
                boundingBox = GpxBoundingBox(
                    minLatitude = minLat,
                    maxLatitude = maxLat,
                    minLongitude = minLon,
                    maxLongitude = maxLon,
                ),
                hasElevation = hasElevation,
                hasTimestamps = hasTimestamps,
            )
            return GpxParseResult.Success(route)
        }

        private fun startCapturing(field: CapturingField) {
            textBuilder.setLength(0)
            capturingField = field
        }

        private fun resolveTagName(localName: String?, qName: String?): String {
            val raw = if (!localName.isNullOrEmpty()) localName else qName.orEmpty()
            return raw.substringAfter(':').lowercase(Locale.US)
        }

        private fun isSupportedGpxNamespace(uri: String?): Boolean {
            return uri.isNullOrEmpty() || uri == GPX_1_1_NAMESPACE || uri == GPX_1_0_NAMESPACE
        }

        private fun isValidIsoTimestamp(raw: String): Boolean {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return false
            return try {
                OffsetDateTime.parse(trimmed)
                true
            } catch (_: DateTimeParseException) {
                try {
                    LocalDateTime.parse(trimmed)
                    true
                } catch (_: DateTimeParseException) {
                    false
                }
            }
        }

        private fun parseCoordinate(raw: String?, min: Double, max: Double): Double? {
            val value = raw?.trim()?.toDoubleOrNull() ?: return null
            if (!value.isFinite() || value < min || value > max) return null
            return value
        }
    }

    private class UnsafeXmlParserException : SAXException("UNSAFE_XML")
    private class FileTooLargeParserException : IOException("FILE_TOO_LARGE")
    private class TooManyPointsParserException : SAXException("TOO_MANY_POINTS")
    private class InvalidCoordinatesParserException : SAXException("INVALID_COORDINATES")
    private class InsufficientPointsParserException : SAXException("INSUFFICIENT_POINTS")
    private class MalformedGpxParserException : SAXException("MALFORMED_XML")

    companion object {
        private const val EARTH_RADIUS_METERS = 6_371_000.0
        private const val MIN_LATITUDE = -90.0
        private const val MAX_LATITUDE = 90.0
        private const val MIN_LONGITUDE = -180.0
        private const val MAX_LONGITUDE = 180.0
        private const val MAX_CAPTURED_CHARS = 4096
        private const val GPX_1_1_NAMESPACE = "http://www.topografix.com/GPX/1/1"
        private const val GPX_1_0_NAMESPACE = "http://www.topografix.com/GPX/1/0"

        private const val UPPER_A = 'A'.code
        private const val UPPER_Z = 'Z'.code
        private const val ASCII_CASE_DELTA = 'a'.code - 'A'.code

        private val DOCTYPE_BYTES = "<!doctype".toByteArray(Charsets.US_ASCII).map { it.toInt() }.toIntArray()
        private val ENTITY_BYTES = "<!entity".toByteArray(Charsets.US_ASCII).map { it.toInt() }.toIntArray()

        private const val FEATURE_DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl"
        private const val FEATURE_EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities"
        private const val FEATURE_EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities"
        private const val FEATURE_LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd"
        private const val LEXICAL_HANDLER_PROPERTY = "http://xml.org/sax/properties/lexical-handler"

        internal fun haversineDistanceMeters(
            lat1Deg: Double,
            lon1Deg: Double,
            lat2Deg: Double,
            lon2Deg: Double,
        ): Double {
            if (lat1Deg == lat2Deg && lon1Deg == lon2Deg) return 0.0
            val lat1 = Math.toRadians(lat1Deg)
            val lat2 = Math.toRadians(lat2Deg)
            val dLat = lat2 - lat1
            val dLon = Math.toRadians(lon2Deg - lon1Deg)
            val sinHalfLat = sin(dLat / 2.0)
            val sinHalfLon = sin(dLon / 2.0)
            val a = sinHalfLat * sinHalfLat + cos(lat1) * cos(lat2) * sinHalfLon * sinHalfLon
            val c = 2.0 * atan2(sqrt(a), sqrt((1.0 - a).coerceAtLeast(0.0)))
            return EARTH_RADIUS_METERS * c
        }
    }
}
