package it.trekcheck.app.data.gpx

import it.trekcheck.app.domain.model.GpxImportError
import it.trekcheck.app.domain.model.GpxImportLimits
import it.trekcheck.app.domain.model.GpxParseResult
import it.trekcheck.app.domain.model.GpxSourceKind
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.nio.charset.CharacterCodingException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeXmlGpxParserTest {
    private val parser = SafeXmlGpxParser()

    @Test
    fun parsesGpx11TrackWithMultipleSegmentsAndComputesMetricsPerSegment() {
        val gpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="TrekCheckTest" xmlns="http://www.topografix.com/GPX/1/1">
              <metadata>
                <name>Nome Metadata Secondario</name>
              </metadata>
              <wpt lat="46.9" lon="12.9">
                <name>Waypoint Ignorato</name>
                <ele>9999.0</ele>
              </wpt>
              <trk>
                <name>Anello Monte Pizzoc</name>
                <trkseg>
                  <trkpt lat="46.0000" lon="12.0000">
                    <ele>1000.0</ele>
                    <time>2026-06-20T06:00:00Z</time>
                    <extensions><ele>5000.0</ele></extensions>
                  </trkpt>
                  <trkpt lat="46.0010" lon="12.0000">
                    <ele>1001.5</ele>
                    <time>2026-06-20T06:01:00Z</time>
                  </trkpt>
                  <trkpt lat="46.0020" lon="12.0000">
                    <ele>1100.0</ele>
                    <time>2026-06-20T06:05:00Z</time>
                  </trkpt>
                </trkseg>
                <trkseg />
                <trkseg>
                  <trkpt lat="46.1000" lon="12.1000">
                    <ele>1200.0</ele>
                  </trkpt>
                  <trkpt lat="46.1010" lon="12.1000">
                    <ele>1150.0</ele>
                  </trkpt>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val result = parser.parse(gpx.byteInputStream())
        assertTrue(result is GpxParseResult.Success)
        val route = (result as GpxParseResult.Success).route

        assertEquals("Anello Monte Pizzoc", route.routeName)
        assertEquals(GpxSourceKind.TRACK, route.sourceKind)
        assertEquals(2, route.geometry.segmentCount)
        assertEquals(5, route.geometry.pointCount)
        assertEquals(3, route.geometry.segments[0].points.size)
        assertEquals(2, route.geometry.segments[1].points.size)
        assertTrue(route.hasElevation)
        assertTrue(route.hasTimestamps)

        // Segment 1 gain: 1000 -> 1100 (+100; 1001.5 ignored as < 3m threshold, then 1100 - 1000 = 100)
        // Segment 2 loss: 1200 -> 1150 (50), no cross-segment gain from 1100 to 1200!
        assertEquals(100.0, route.elevationGainMeters!!, 0.01)
        assertEquals(50.0, route.elevationLossMeters!!, 0.01)
        assertEquals(1000.0, route.minElevationMeters!!, 0.01)
        assertEquals(1200.0, route.maxElevationMeters!!, 0.01)

        // 3 steps of 0.001 deg latitude (~111.2 m each) without bridging the 0.1 deg segment gap
        assertTrue(route.distanceMeters in 320.0..350.0)

        assertEquals(46.0000, route.boundingBox.minLatitude, 1e-6)
        assertEquals(46.1010, route.boundingBox.maxLatitude, 1e-6)
        assertEquals(12.0000, route.boundingBox.minLongitude, 1e-6)
        assertEquals(12.1000, route.boundingBox.maxLongitude, 1e-6)
    }

    @Test
    fun parsesGpx10RouteAndTracksWithoutElevationOrTimestampsUsingExplicitNulls() {
        val gpx10 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.0" creator="LegacyGps" xmlns="http://www.topografix.com/GPX/1/0">
              <name>Traversata GPX 1.0</name>
              <rte>
                <rtept lat="45.5000" lon="11.5000"><ele>NaN</ele></rtept>
                <rtept lat="45.5050" lon="11.5000" />
              </rte>
            </gpx>
        """.trimIndent()

        val result = parser.parse(gpx10.byteInputStream())
        assertTrue(result is GpxParseResult.Success)
        val route = (result as GpxParseResult.Success).route

        assertEquals("Traversata GPX 1.0", route.routeName)
        assertEquals(GpxSourceKind.ROUTE, route.sourceKind)
        assertEquals(1, route.geometry.segmentCount)
        assertEquals(2, route.geometry.pointCount)
        assertFalse(route.hasElevation)
        assertFalse(route.hasTimestamps)
        assertNull(route.elevationGainMeters)
        assertNull(route.elevationLossMeters)
        assertNull(route.minElevationMeters)
        assertNull(route.maxElevationMeters)
        assertTrue(route.distanceMeters > 500.0)
        assertNull(route.geometry.segments[0].points[0].elevationMeters)
    }

    @Test
    fun parsesFlatElevationZeroWithoutTreatingAsMissing() {
        val gpx = """
            <gpx version="1.1">
              <trk>
                <trkseg>
                  <trkpt lat="45.0" lon="12.0"><ele>10.0</ele></trkpt>
                  <trkpt lat="45.001" lon="12.0"><ele>11.0</ele></trkpt>
                  <trkpt lat="45.002" lon="12.0"><ele>10.5</ele></trkpt>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val result = parser.parse(gpx.byteInputStream())
        assertTrue(result is GpxParseResult.Success)
        val route = (result as GpxParseResult.Success).route
        assertTrue(route.hasElevation)
        assertEquals(0.0, route.elevationGainMeters!!, 0.0)
        assertEquals(10.0, route.minElevationMeters!!, 0.0)
        assertEquals(11.0, route.maxElevationMeters!!, 0.0)
    }

    @Test
    fun rejectsDoctypeAndXxeWithoutMakingAnyExternalNetworkConnections() {
        val serverSocket = ServerSocket(0)
        serverSocket.soTimeout = 500
        val accepted = AtomicBoolean(false)
        val serverThread = Thread {
            try {
                serverSocket.accept().use {
                    accepted.set(true)
                }
            } catch (_: SocketTimeoutException) {
            } catch (_: IOException) {
            }
        }
        serverThread.start()

        try {
            val xxeGpx = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE gpx [
                  <!ENTITY xxe SYSTEM "http://127.0.0.1:${serverSocket.localPort}/xxe.dtd">
                ]>
                <gpx version="1.1">
                  <trk><name>&xxe;</name><trkseg>
                    <trkpt lat="45.0" lon="11.0" />
                    <trkpt lat="45.1" lon="11.1" />
                  </trkseg></trk>
                </gpx>
            """.trimIndent()

            val result = parser.parse(xxeGpx.byteInputStream())
            assertEquals(GpxParseResult.Failure(GpxImportError.UnsafeXml), result)
            serverThread.join(1000)
            assertFalse("Parser must never connect to external DTD/entity URLs", accepted.get())
        } finally {
            serverSocket.close()
        }
    }

    @Test
    fun rejectsDoctypeSplitAcrossReadChunksAndUtf16Encoded() {
        val lowercaseSplit = """<?xml version="1.0"?><!doctype gpx><gpx><trk><trkseg><trkpt lat="45" lon="11"/><trkpt lat="46" lon="12"/></trkseg></trk></gpx>"""
        val tinyChunkStream = object : InputStream() {
            private val bytes = lowercaseSplit.toByteArray(Charsets.UTF_8)
            private var pos = 0
            override fun read(): Int = if (pos < bytes.size) bytes[pos++].toInt() and 0xFF else -1
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (pos >= bytes.size) return -1
                val chunk = minOf(len, 3, bytes.size - pos)
                System.arraycopy(bytes, pos, b, off, chunk)
                pos += chunk
                return chunk
            }
        }
        assertEquals(GpxParseResult.Failure(GpxImportError.UnsafeXml), parser.parse(tinyChunkStream))

        val utf16Xxe = """<?xml version="1.0" encoding="UTF-16"?><!DOCTYPE gpx><gpx/>"""
        assertEquals(
            GpxParseResult.Failure(GpxImportError.UnsafeXml),
            parser.parse(utf16Xxe.toByteArray(Charsets.UTF_16BE).inputStream()),
        )

        val entityOnly = """<?xml version="1.0"?><!ENTITY test "x"><gpx/>"""
        assertEquals(
            GpxParseResult.Failure(GpxImportError.UnsafeXml),
            parser.parse(entityOnly.byteInputStream()),
        )
    }

    @Test
    fun rejectsMalformedXmlAndNonGpxRoot() {
        assertEquals(
            GpxParseResult.Failure(GpxImportError.MalformedXml),
            parser.parse(ByteArrayInputStream(ByteArray(0))),
        )
        assertEquals(
            GpxParseResult.Failure(GpxImportError.MalformedXml),
            parser.parse("<gpx><trk><trkseg></gpx>".byteInputStream()),
        )
        assertEquals(
            GpxParseResult.Failure(GpxImportError.MalformedXml),
            parser.parse("<html><body>not gpx</body></html>".byteInputStream()),
        )
    }

    @Test
    fun rejectsInvalidCoordinatesAndAcceptsBoundaryCoordinates() {
        val boundaryValid = """
            <gpx version="1.1">
              <trk><trkseg>
                <trkpt lat="-90.0" lon="-180.0" />
                <trkpt lat="90.0" lon="180.0" />
              </trkseg></trk>
            </gpx>
        """.trimIndent()
        assertTrue(parser.parse(boundaryValid.byteInputStream()) is GpxParseResult.Success)

        val invalidSamples = listOf(
            """<gpx><trk><trkseg><trkpt lat="90.0001" lon="11.0"/><trkpt lat="45.0" lon="11.0"/></trkseg></trk></gpx>""",
            """<gpx><trk><trkseg><trkpt lat="-90.0001" lon="11.0"/><trkpt lat="45.0" lon="11.0"/></trkseg></trk></gpx>""",
            """<gpx><trk><trkseg><trkpt lat="45.0" lon="180.0001"/><trkpt lat="45.0" lon="11.0"/></trkseg></trk></gpx>""",
            """<gpx><trk><trkseg><trkpt lat="45.0" lon="-180.0001"/><trkpt lat="45.0" lon="11.0"/></trkseg></trk></gpx>""",
            """<gpx><trk><trkseg><trkpt lat="NaN" lon="11.0"/><trkpt lat="45.0" lon="11.0"/></trkseg></trk></gpx>""",
            """<gpx><trk><trkseg><trkpt lat="Infinity" lon="11.0"/><trkpt lat="45.0" lon="11.0"/></trkseg></trk></gpx>""",
            """<gpx><trk><trkseg><trkpt lat="45.0"/><trkpt lat="45.0" lon="11.0"/></trkseg></trk></gpx>""",
        )

        for (sample in invalidSamples) {
            assertEquals(
                GpxParseResult.Failure(GpxImportError.InvalidCoordinates),
                parser.parse(sample.byteInputStream()),
            )
        }
    }

    @Test
    fun rejectsLessThanTwoValidPoints() {
        val zeroPoints = """<gpx version="1.1"><trk><trkseg /></trk></gpx>"""
        val onePoint = """<gpx version="1.1"><trk><trkseg><trkpt lat="45.0" lon="11.0" /></trkseg></trk></gpx>"""

        assertEquals(
            GpxParseResult.Failure(GpxImportError.InsufficientPoints),
            parser.parse(zeroPoints.byteInputStream()),
        )
        assertEquals(
            GpxParseResult.Failure(GpxImportError.InsufficientPoints),
            parser.parse(onePoint.byteInputStream()),
        )
    }

    @Test
    fun enforcesExact20MbByteLimitInStreamingWithoutReadingExcess() {
        val prefix = """<gpx version="1.1"><trk><trkseg><trkpt lat="45.0" lon="11.0"/><trkpt lat="45.1" lon="11.1"/></trkseg></trk><desc>"""
        val suffix = """</desc></gpx>"""
        val exactLimitStream = paddedXmlStream(
            prefix = prefix,
            suffix = suffix,
            totalSizeBytes = GpxImportLimits.MAX_FILE_BYTES,
        )
        val exactResult = parser.parse(exactLimitStream)
        assertTrue("Exact 20 MB stream must be accepted", exactResult is GpxParseResult.Success)

        val overLimitStream = CountingPaddedXmlStream(
            prefix = prefix,
            suffix = suffix,
            totalSizeBytes = 500L * 1024L * 1024L,
        )
        val overResult = parser.parse(overLimitStream)
        assertEquals(GpxParseResult.Failure(GpxImportError.FileTooLarge), overResult)
        assertTrue(
            "Streaming parser must abort near 20 MB instead of reading 500 MB (read ${overLimitStream.totalRead})",
            overLimitStream.totalRead <= GpxImportLimits.MAX_FILE_BYTES + 64 * 1024,
        )

        val plusOneByteResult = parser.parse(
            paddedXmlStream(
                prefix = prefix,
                suffix = suffix,
                totalSizeBytes = GpxImportLimits.MAX_FILE_BYTES + 1L,
            ),
        )
        assertEquals(GpxParseResult.Failure(GpxImportError.FileTooLarge), plusOneByteResult)
    }

    @Test
    fun enforcesExact200kPointLimitInStreaming() {
        val atLimit = parser.parse(generatedPointsGpxStream(GpxImportLimits.MAX_POINT_COUNT))
        assertTrue(atLimit is GpxParseResult.Success)
        assertEquals(
            GpxImportLimits.MAX_POINT_COUNT,
            (atLimit as GpxParseResult.Success).route.geometry.pointCount,
        )

        val overLimit = parser.parse(generatedPointsGpxStream(GpxImportLimits.MAX_POINT_COUNT + 1))
        assertEquals(GpxParseResult.Failure(GpxImportError.TooManyPoints), overLimit)
    }

    @Test
    fun mapsUnderlyingStreamIoExceptionToReadFailedInsteadOfMalformedXml() {
        val failingStream = object : InputStream() {
            private val header = "<gpx><trk><trkseg><trkpt lat=\"45.0\" lon=\"11.0\"/>".toByteArray()
            private var idx = 0

            override fun read(): Int {
                if (idx < header.size) return header[idx++].toInt() and 0xFF
                throw IOException("Simulated content provider read failure")
            }
        }

        assertEquals(GpxParseResult.Failure(GpxImportError.ReadFailed), parser.parse(failingStream))
    }

    @Test
    fun parsesMixedSourceMetadataNameFallbackIgnoresForeignNamespacesInvalidTimesAndNestedTracks() {
        val mixedGpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1" xmlns:ext="http://example.com/ext">
              <metadata>
                <name>Nome Da Metadata</name>
                <trk>
                  <trkseg>
                    <trkpt lat="47.9" lon="13.9"><ele>9000.0</ele></trkpt>
                    <trkpt lat="47.91" lon="13.91" />
                  </trkseg>
                </trk>
              </metadata>
              <trk>
                <ext:name>Nome Esterno</ext:name>
                <trkseg>
                  <trkpt lat="46.0" lon="12.0">
                    <ext:ele>9999.0</ext:ele>
                    <ext:time>2026-06-20T06:00:00Z</ext:time>
                    <time>not-an-iso-timestamp</time>
                  </trkpt>
                </trkseg>
              </trk>
              <rte>
                <rtept lat="46.01" lon="12.0" />
              </rte>
            </gpx>
        """.trimIndent()

        val result = parser.parse(mixedGpx.byteInputStream())
        assertTrue(result is GpxParseResult.Success)
        val route = (result as GpxParseResult.Success).route
        assertEquals("Nome Da Metadata", route.routeName)
        assertEquals(GpxSourceKind.MIXED, route.sourceKind)
        assertEquals(2, route.geometry.pointCount)
        assertFalse(route.hasElevation)
        assertFalse(route.hasTimestamps)

        val localDateTimeGpx = """
            <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
              <trk><trkseg>
                <trkpt lat="46.0" lon="12.0"><time>2026-06-20T06:30:00</time></trkpt>
                <trkpt lat="46.01" lon="12.0" />
              </trkseg></trk>
            </gpx>
        """.trimIndent()
        val localTimeResult = parser.parse(localDateTimeGpx.byteInputStream()) as GpxParseResult.Success
        assertTrue(localTimeResult.route.hasTimestamps)
    }

    @Test
    fun rejectsSpoofedNamespaceSubstringAndUndeclaredEntityReferenceAsMalformedXml() {
        val spoofedNamespace = """
            <gpx xmlns="http://evil.example/topografix.com/gpx/1/1">
              <trk><trkseg>
                <trkpt lat="45.0" lon="11.0" />
                <trkpt lat="45.1" lon="11.1" />
              </trkseg></trk>
            </gpx>
        """.trimIndent()
        assertEquals(GpxParseResult.Failure(GpxImportError.MalformedXml), parser.parse(spoofedNamespace.byteInputStream()))

        val undeclaredEntity = """
            <gpx version="1.1">
              <trk><name>&foo;</name><trkseg>
                <trkpt lat="45.0" lon="11.0" />
                <trkpt lat="45.1" lon="11.1" />
              </trkseg></trk>
            </gpx>
        """.trimIndent()
        assertEquals(GpxParseResult.Failure(GpxImportError.MalformedXml), parser.parse(undeclaredEntity.byteInputStream()))
    }

    @Test
    fun keepsElevationExtremaForSinglePointSegmentsAndRejectsExtremeElevationOverflow() {
        val disconnectedWithElevation = """
            <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
              <trk>
                <trkseg><trkpt lat="46.0" lon="12.0"><ele>1000.0</ele></trkpt></trkseg>
                <trkseg><trkpt lat="46.1" lon="12.1"><ele>1350.0</ele></trkpt></trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val result = parser.parse(disconnectedWithElevation.byteInputStream())
        assertTrue(result is GpxParseResult.Success)
        val route = (result as GpxParseResult.Success).route
        assertTrue(route.hasElevation)
        assertEquals(1000.0, route.minElevationMeters!!, 0.0)
        assertEquals(1350.0, route.maxElevationMeters!!, 0.0)
        assertNull(route.elevationGainMeters)
        assertNull(route.elevationLossMeters)

        val overflowGpx = """
            <gpx version="1.1">
              <trk><trkseg>
                <trkpt lat="46.0" lon="12.0"><ele>-1e308</ele></trkpt>
                <trkpt lat="46.1" lon="12.1"><ele>1e308</ele></trkpt>
              </trkseg></trk>
            </gpx>
        """.trimIndent()
        assertEquals(
            GpxParseResult.Failure(GpxImportError.InvalidCoordinates),
            parser.parse(overflowGpx.byteInputStream()),
        )
    }

    @Test
    fun mapsCharacterCodingToMalformedXmlSecurityExceptionToReadFailedAndRethrowsErrorAndCancellation() {
        val invalidUtf8 = "<gpx version=\"1.1\"><trk><name>".toByteArray() +
            byteArrayOf(0xFF.toByte(), 0xFE.toByte()) +
            "</name><trkseg><trkpt lat=\"45.0\" lon=\"11.0\"/><trkpt lat=\"45.1\" lon=\"11.1\"/></trkseg></trk></gpx>".toByteArray()
        assertEquals(GpxParseResult.Failure(GpxImportError.MalformedXml), parser.parse(ByteArrayInputStream(invalidUtf8)))

        val charCodingStream = object : InputStream() {
            override fun read(): Int = throw CharacterCodingException()
        }
        assertEquals(GpxParseResult.Failure(GpxImportError.MalformedXml), parser.parse(charCodingStream))

        val securityFailureStream = object : InputStream() {
            private val header = "<gpx><trk><trkseg><trkpt lat=\"45.0\" lon=\"11.0\"/>".toByteArray()
            private var idx = 0
            override fun read(): Int {
                if (idx < header.size) return header[idx++].toInt() and 0xFF
                throw SecurityException("Revoked URI permission mid-stream")
            }
        }
        assertEquals(GpxParseResult.Failure(GpxImportError.ReadFailed), parser.parse(securityFailureStream))

        val cancelledStream = object : InputStream() {
            override fun read(): Int = throw CancellationException("Cancelled")
        }
        assertThrows(CancellationException::class.java) {
            parser.parse(cancelledStream)
        }

        val fatalErrorStream = object : InputStream() {
            override fun read(): Int = throw AssertionError("Fatal error must propagate")
        }
        assertThrows(AssertionError::class.java) {
            parser.parse(fatalErrorStream)
        }
    }

    @Test
    fun measures50kPointGpxParsingDurationAgainstNfr5Target() {
        val gpxBytes = build50kPointFixtureBytes(includeElevationAndTime = true)
        val startNs = System.nanoTime()
        val result = parser.parse(ByteArrayInputStream(gpxBytes))
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0

        assertTrue(result is GpxParseResult.Success)
        val route = (result as GpxParseResult.Success).route
        assertEquals(50_000, route.geometry.pointCount)
        assertTrue(route.hasElevation)
        assertTrue(route.hasTimestamps)
        println("NFR-5 JVM SafeXmlGpxParser 50,000-point parse elapsed=${elapsedMs}ms bytes=${gpxBytes.size}")
    }

    private fun paddedXmlStream(prefix: String, suffix: String, totalSizeBytes: Long): InputStream =
        CountingPaddedXmlStream(prefix, suffix, totalSizeBytes)

    private class CountingPaddedXmlStream(
        prefix: String,
        suffix: String,
        private val totalSizeBytes: Long,
    ) : InputStream() {
        private val prefixBytes = prefix.toByteArray(Charsets.UTF_8)
        private val suffixBytes = suffix.toByteArray(Charsets.UTF_8)
        private val paddingStart = prefixBytes.size.toLong()
        private val suffixStart = totalSizeBytes - suffixBytes.size.toLong()
        private var pos = 0L

        var totalRead: Long = 0L
            private set

        init {
            require(totalSizeBytes >= prefixBytes.size + suffixBytes.size)
        }

        override fun read(): Int {
            if (pos >= totalSizeBytes) return -1
            val b = when {
                pos < paddingStart -> prefixBytes[pos.toInt()].toInt() and 0xFF
                pos >= suffixStart -> suffixBytes[(pos - suffixStart).toInt()].toInt() and 0xFF
                else -> ' '.code
            }
            pos += 1L
            totalRead += 1L
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (len == 0) return 0
            if (pos >= totalSizeBytes) return -1
            val remaining = minOf(len.toLong(), totalSizeBytes - pos).toInt()
            var written = 0
            while (written < remaining) {
                if (pos < paddingStart) {
                    val copy = minOf(remaining - written, (paddingStart - pos).toInt())
                    System.arraycopy(prefixBytes, pos.toInt(), b, off + written, copy)
                    pos += copy
                    written += copy
                } else if (pos >= suffixStart) {
                    val copy = minOf(remaining - written, (totalSizeBytes - pos).toInt())
                    System.arraycopy(suffixBytes, (pos - suffixStart).toInt(), b, off + written, copy)
                    pos += copy
                    written += copy
                } else {
                    val copy = minOf(remaining - written, (suffixStart - pos).toInt())
                    java.util.Arrays.fill(b, off + written, off + written + copy, ' '.code.toByte())
                    pos += copy
                    written += copy
                }
            }
            totalRead += written.toLong()
            return written
        }
    }

    companion object {
        fun generatedPointsGpxStream(pointCount: Int): InputStream {
            val header = "<gpx version=\"1.1\"><trk><trkseg>\n".toByteArray(Charsets.UTF_8)
            val footer = "</trkseg></trk></gpx>\n".toByteArray(Charsets.UTF_8)
            val pointBytes = "<trkpt lat=\"45.5000\" lon=\"11.5000\"/>\n".toByteArray(Charsets.UTF_8)

            return object : InputStream() {
                private var stage = 0
                private var offset = 0
                private var pointsEmitted = 0

                override fun read(): Int {
                    val single = ByteArray(1)
                    val n = read(single, 0, 1)
                    return if (n <= 0) -1 else single[0].toInt() and 0xFF
                }

                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    if (len == 0) return 0
                    var written = 0
                    while (written < len) {
                        when (stage) {
                            0 -> {
                                val copy = minOf(len - written, header.size - offset)
                                System.arraycopy(header, offset, b, off + written, copy)
                                offset += copy
                                written += copy
                                if (offset >= header.size) {
                                    stage = if (pointCount > 0) 1 else 2
                                    offset = 0
                                }
                            }

                            1 -> {
                                val copy = minOf(len - written, pointBytes.size - offset)
                                System.arraycopy(pointBytes, offset, b, off + written, copy)
                                offset += copy
                                written += copy
                                if (offset >= pointBytes.size) {
                                    pointsEmitted += 1
                                    offset = 0
                                    if (pointsEmitted >= pointCount) {
                                        stage = 2
                                    }
                                }
                            }

                            2 -> {
                                val copy = minOf(len - written, footer.size - offset)
                                if (copy == 0) return if (written == 0) -1 else written
                                System.arraycopy(footer, offset, b, off + written, copy)
                                offset += copy
                                written += copy
                                if (offset >= footer.size) {
                                    stage = 3
                                    offset = 0
                                }
                            }

                            else -> return if (written == 0) -1 else written
                        }
                    }
                    return written
                }
            }
        }

        fun build50kPointFixtureBytes(includeElevationAndTime: Boolean): ByteArray {
            val sb = StringBuilder(5_000_000)
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            sb.append("<gpx version=\"1.1\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
            sb.append("<trk><name>Fixture 50k</name><trkseg>\n")
            for (i in 0 until 50_000) {
                val lat = 45.0 + (i % 1000) * 0.0001
                val lon = 11.0 + (i / 1000) * 0.0001
                sb.append("<trkpt lat=\"").append(lat).append("\" lon=\"").append(lon).append("\">")
                if (includeElevationAndTime) {
                    val ele = 500.0 + (i % 500)
                    sb.append("<ele>").append(ele).append("</ele>")
                    sb.append("<time>2026-06-20T06:00:00Z</time>")
                }
                sb.append("</trkpt>\n")
            }
            sb.append("</trkseg></trk></gpx>")
            return sb.toString().toByteArray(Charsets.UTF_8)
        }
    }
}
