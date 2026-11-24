package it.trekcheck.app.domain.geo

import it.trekcheck.app.domain.model.ElevationProfilePreview
import it.trekcheck.app.domain.model.ElevationSample
import it.trekcheck.app.domain.model.GpxImportLimits
import it.trekcheck.app.domain.model.NormalizedRouteGeometry
import it.trekcheck.app.domain.model.ProjectedPoint
import it.trekcheck.app.domain.model.RouteWaypointSummary
import it.trekcheck.app.domain.model.SimplifiedRoutePreview
import java.util.TreeSet
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Pure-Kotlin analysis (full geometry, AD-2) and rendering helpers (simplified geometry only).
 */
object RouteGeometrySimplifier {
    const val DEFAULT_MAX_PROFILE_SAMPLES: Int = 240
    private const val TOLERANCE_EXTENT_RATIO = 0.0015
    private const val MIN_TOLERANCE_METERS = 1.0

    data class Analysis(
        val distanceMeters: Double,
        val start: RouteWaypointSummary,
        val end: RouteWaypointSummary,
        val highest: RouteWaypointSummary?,
        val highestSegmentIndex: Int,
        val highestPointIndex: Int,
        val minElevationMeters: Double?,
        val maxElevationMeters: Double?,
        val elevationGainMeters: Double?,
        val elevationLossMeters: Double?,
        val minLatitude: Double,
        val maxLatitude: Double,
        val minLongitude: Double,
        val maxLongitude: Double,
    )

    /** Metrics computed on every point of the complete geometry; missing elevation stays null. */
    fun analyze(
        geometry: NormalizedRouteGeometry,
        elevationGainThresholdMeters: Double = GpxImportLimits.DEFAULT_ELEVATION_GAIN_THRESHOLD_METERS,
    ): Analysis {
        var total = 0.0
        var minEle: Double? = null
        var maxEle: Double? = null
        var gainTotal = 0.0
        var lossTotal = 0.0
        var validElevationPairs = 0
        var highestSegment = -1
        var highestIndex = -1
        var highestDistance = 0.0
        var minLat = Double.POSITIVE_INFINITY
        var maxLat = Double.NEGATIVE_INFINITY
        var minLon = Double.POSITIVE_INFINITY
        var maxLon = Double.NEGATIVE_INFINITY

        geometry.segments.forEachIndexed { segmentIndex, segment ->
            val points = segment.points
            var segmentAnchorElevation: Double? = null
            for (index in points.indices) {
                val point = points[index]
                if (index > 0) {
                    val prev = points[index - 1]
                    total += GeoDistance.haversineDistanceMeters(
                        prev.latitude,
                        prev.longitude,
                        point.latitude,
                        point.longitude,
                    )
                }
                if (point.latitude < minLat) minLat = point.latitude
                if (point.latitude > maxLat) maxLat = point.latitude
                if (point.longitude < minLon) minLon = point.longitude
                if (point.longitude > maxLon) maxLon = point.longitude
                val ele = point.elevationMeters
                if (ele != null) {
                    if (minEle == null || ele < minEle!!) minEle = ele
                    if (maxEle == null || ele > maxEle!!) {
                        maxEle = ele
                        highestSegment = segmentIndex
                        highestIndex = index
                        highestDistance = total
                    }
                    val anchor = segmentAnchorElevation
                    if (anchor == null) {
                        segmentAnchorElevation = ele
                    } else {
                        validElevationPairs += 1
                        val delta = ele - anchor
                        if (delta.isFinite()) {
                            if (delta >= elevationGainThresholdMeters) {
                                val updated = gainTotal + delta
                                if (updated.isFinite()) gainTotal = updated
                                segmentAnchorElevation = ele
                            } else if (-delta >= elevationGainThresholdMeters) {
                                val updated = lossTotal - delta
                                if (updated.isFinite()) lossTotal = updated
                                segmentAnchorElevation = ele
                            }
                        }
                    }
                }
            }
        }

        val startPoint = geometry.startPoint
        val endPoint = geometry.endPoint
        val highestPoint = if (highestSegment >= 0) {
            geometry.segments[highestSegment].points[highestIndex]
        } else {
            null
        }
        val hasGainLoss = validElevationPairs > 0
        return Analysis(
            distanceMeters = total,
            start = RouteWaypointSummary(
                latitude = startPoint.latitude,
                longitude = startPoint.longitude,
                elevationMeters = startPoint.elevationMeters,
                distanceFromStartMeters = 0.0,
            ),
            end = RouteWaypointSummary(
                latitude = endPoint.latitude,
                longitude = endPoint.longitude,
                elevationMeters = endPoint.elevationMeters,
                distanceFromStartMeters = total,
            ),
            highest = highestPoint?.let {
                RouteWaypointSummary(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    elevationMeters = it.elevationMeters,
                    distanceFromStartMeters = highestDistance,
                )
            },
            highestSegmentIndex = highestSegment,
            highestPointIndex = highestIndex,
            minElevationMeters = minEle,
            maxElevationMeters = maxEle,
            elevationGainMeters = if (hasGainLoss) gainTotal else null,
            elevationLossMeters = if (hasGainLoss) lossTotal else null,
            minLatitude = minLat,
            maxLatitude = maxLat,
            minLongitude = minLon,
            maxLongitude = maxLon,
        )
    }

    /**
     * Projects to local metres and simplifies each segment separately (iterative Ramer–Douglas–Peucker),
     * anchoring segment ends and the global highest point. Never merges segments.
     */
    fun simplify(
        geometry: NormalizedRouteGeometry,
        analysis: Analysis,
        toleranceMeters: Double? = null,
    ): SimplifiedRoutePreview {
        val lat0 = (analysis.minLatitude + analysis.maxLatitude) / 2.0
        val lon0 = (analysis.minLongitude + analysis.maxLongitude) / 2.0
        val metersPerDegree = Math.PI / 180.0 * GeoDistance.EARTH_RADIUS_METERS
        val lonScale = cos(Math.toRadians(lat0)) * metersPerDegree

        fun projectX(lon: Double) = (lon - lon0) * lonScale
        fun projectY(lat: Double) = (lat - lat0) * metersPerDegree

        val extent = max(
            (analysis.maxLongitude - analysis.minLongitude) * lonScale,
            (analysis.maxLatitude - analysis.minLatitude) * metersPerDegree,
        )
        val tolerance = toleranceMeters
            ?: max(extent * TOLERANCE_EXTENT_RATIO, MIN_TOLERANCE_METERS)

        var minX = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY

        val simplified = geometry.segments.mapIndexed { segmentIndex, segment ->
            val points = segment.points
            val xs = DoubleArray(points.size) { projectX(points[it].longitude) }
            val ys = DoubleArray(points.size) { projectY(points[it].latitude) }
            val forced = if (segmentIndex == analysis.highestSegmentIndex) analysis.highestPointIndex else -1
            val keep = douglasPeucker(xs, ys, tolerance, forced)
            val result = ArrayList<ProjectedPoint>()
            for (i in points.indices) {
                if (keep[i]) {
                    result.add(ProjectedPoint(xs[i], ys[i]))
                }
            }
            for (i in points.indices) {
                if (xs[i] < minX) minX = xs[i]
                if (xs[i] > maxX) maxX = xs[i]
                if (ys[i] < minY) minY = ys[i]
                if (ys[i] > maxY) maxY = ys[i]
            }
            result.toList()
        }

        val startPoint = geometry.startPoint
        val endPoint = geometry.endPoint
        val highest = analysis.highest
        return SimplifiedRoutePreview(
            segments = simplified,
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY,
            start = ProjectedPoint(projectX(startPoint.longitude), projectY(startPoint.latitude)),
            end = ProjectedPoint(projectX(endPoint.longitude), projectY(endPoint.latitude)),
            highest = highest?.let { ProjectedPoint(projectX(it.longitude), projectY(it.latitude)) },
        )
    }

    /**
     * Samples contiguous quoted runs per segment for rendering; returns null when no contiguous
     * elevation pair exists (prevents bridging missing-elevation gaps or disjoint segments).
     */
    fun sampleProfile(
        geometry: NormalizedRouteGeometry,
        maxSamples: Int = DEFAULT_MAX_PROFILE_SAMPLES,
    ): ElevationProfilePreview? {
        val rawRuns = ArrayList<ArrayList<ElevationSample>>()
        var total = 0.0
        var totalQuoted = 0
        var minEle = Double.POSITIVE_INFINITY
        var maxEle = Double.NEGATIVE_INFINITY

        for (segment in geometry.segments) {
            var currentRun: ArrayList<ElevationSample>? = null
            val points = segment.points
            for (index in points.indices) {
                val point = points[index]
                if (index > 0) {
                    val prev = points[index - 1]
                    total += GeoDistance.haversineDistanceMeters(
                        prev.latitude,
                        prev.longitude,
                        point.latitude,
                        point.longitude,
                    )
                }
                val ele = point.elevationMeters
                if (ele != null) {
                    if (currentRun == null) {
                        currentRun = ArrayList()
                        rawRuns.add(currentRun)
                    }
                    currentRun.add(ElevationSample(total, ele))
                    totalQuoted += 1
                    if (ele < minEle) minEle = ele
                    if (ele > maxEle) maxEle = ele
                } else {
                    currentRun = null
                }
            }
        }
        val validRuns = rawRuns.filter { it.size >= 2 }
        if (validRuns.isEmpty()) return null

        val limit = maxSamples.coerceAtLeast(4)
        val sampledRuns = if (totalQuoted <= limit) {
            validRuns.map { it.toList() }
        } else {
            validRuns.map { run ->
                val quota = ((run.size.toDouble() / totalQuoted.toDouble()) * limit).toInt().coerceAtLeast(2)
                downsampleRun(run, quota)
            }
        }
        val samples = sampledRuns.flatten()
        return ElevationProfilePreview(
            samples = samples,
            minElevationMeters = minEle,
            maxElevationMeters = maxEle,
            totalDistanceMeters = total,
            runs = sampledRuns,
        )
    }

    private fun downsampleRun(run: List<ElevationSample>, limit: Int): List<ElevationSample> {
        if (run.size <= limit) return run
        val chosen = TreeSet<Int>()
        val last = run.size - 1
        val step = last.toDouble() / (limit - 1).toDouble()
        for (i in 0 until limit) chosen.add((i * step).toInt().coerceAtMost(last))
        chosen.add(last)
        var minIdx = 0
        var maxIdx = 0
        for (i in run.indices) {
            if (run[i].elevationMeters < run[minIdx].elevationMeters) minIdx = i
            if (run[i].elevationMeters > run[maxIdx].elevationMeters) maxIdx = i
        }
        chosen.add(minIdx)
        chosen.add(maxIdx)
        return chosen.map { run[it] }
    }

    internal fun douglasPeucker(
        xs: DoubleArray,
        ys: DoubleArray,
        tolerance: Double,
        forcedIndex: Int = -1,
    ): BooleanArray {
        val n = xs.size
        val keep = BooleanArray(n)
        if (n == 0) return keep
        keep[0] = true
        keep[n - 1] = true
        val anchors = ArrayList<Int>(3)
        anchors.add(0)
        if (forcedIndex in 1 until n - 1) {
            keep[forcedIndex] = true
            anchors.add(forcedIndex)
        }
        if (n > 1) anchors.add(n - 1)

        val stack = ArrayList<Int>()
        for (a in 0 until anchors.size - 1) {
            stack.add(anchors[a])
            stack.add(anchors[a + 1])
            while (stack.isNotEmpty()) {
                val hi = stack.removeAt(stack.size - 1)
                val lo = stack.removeAt(stack.size - 1)
                if (hi - lo < 2) continue
                var maxDist = -1.0
                var maxIdx = -1
                for (i in lo + 1 until hi) {
                    val d = pointToSegmentDistance(xs[i], ys[i], xs[lo], ys[lo], xs[hi], ys[hi])
                    if (d > maxDist) {
                        maxDist = d
                        maxIdx = i
                    }
                }
                if (maxIdx >= 0 && maxDist > tolerance) {
                    keep[maxIdx] = true
                    stack.add(lo)
                    stack.add(maxIdx)
                    stack.add(maxIdx)
                    stack.add(hi)
                }
            }
        }
        return keep
    }

    private fun pointToSegmentDistance(
        px: Double,
        py: Double,
        ax: Double,
        ay: Double,
        bx: Double,
        by: Double,
    ): Double {
        val dx = bx - ax
        val dy = by - ay
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0.0) {
            return sqrt((px - ax) * (px - ax) + (py - ay) * (py - ay))
        }
        val t = (((px - ax) * dx + (py - ay) * dy) / lengthSquared).coerceIn(0.0, 1.0)
        val cx = ax + t * dx
        val cy = ay + t * dy
        return abs(sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy)))
    }
}
