package it.trekcheck.app.ui.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.trekcheck.app.domain.model.ProjectedPoint
import it.trekcheck.app.domain.model.SimplifiedRoutePreview

/** Fit of projected metres into a pixel box; safe for flat/vertical/single-point tracks (no NaN). */
internal data class TrackFit(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val height: Float,
) {
    fun mapX(x: Double): Float = offsetX + x.toFloat() * scale
    fun mapY(y: Double): Float = height - (offsetY + y.toFloat() * scale)

    companion object {
        fun compute(
            track: SimplifiedRoutePreview,
            widthPx: Float,
            heightPx: Float,
            paddingPx: Float,
        ): TrackFit {
            val availW = (widthPx - 2f * paddingPx).coerceAtLeast(1f)
            val availH = (heightPx - 2f * paddingPx).coerceAtLeast(1f)
            val w = track.widthMeters.toFloat()
            val h = track.heightMeters.toFloat()
            val scaleX = if (w > 0f) availW / w else Float.POSITIVE_INFINITY
            val scaleY = if (h > 0f) availH / h else Float.POSITIVE_INFINITY
            val raw = minOf(scaleX, scaleY)
            val scale = if (raw.isFinite() && raw > 0f) raw else 1f
            val offsetX = paddingPx + (availW - w * scale) / 2f - track.minX.toFloat() * scale
            val offsetY = paddingPx + (availH - h * scale) / 2f - track.minY.toFloat() * scale
            return TrackFit(scale, offsetX, offsetY, heightPx)
        }
    }
}

@Composable
fun RouteTrackCanvas(
    track: SimplifiedRoutePreview,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val markerColor = MaterialTheme.colorScheme.onSurface
    val markerFill = MaterialTheme.colorScheme.surface
    val background = MaterialTheme.colorScheme.surfaceContainerLowest

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .testTag("route_track_canvas")
            .semantics { this.contentDescription = contentDescription },
    ) {
        drawRect(color = background, size = size)
        val paddingPx = 20.dp.toPx()
        val fit = TrackFit.compute(track, size.width, size.height, paddingPx)
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        for (segment in track.segments) {
            if (segment.size >= 2) {
                val path = Path()
                segment.forEachIndexed { index, point ->
                    val x = fit.mapX(point.x)
                    val y = fit.mapY(point.y)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path = path, color = lineColor, style = stroke)
            } else if (segment.size == 1) {
                drawCircle(
                    color = lineColor,
                    radius = 2.dp.toPx(),
                    center = Offset(fit.mapX(segment[0].x), fit.mapY(segment[0].y)),
                )
            }
        }

        drawSquareMarker(fit, track.end, markerColor, markerFill)
        track.highest?.let { drawTriangleMarker(fit, it, markerColor, markerFill) }
        drawCircleMarker(fit, track.start, markerColor, markerFill)
    }
}

private fun DrawScope.drawCircleMarker(fit: TrackFit, p: ProjectedPoint, outline: Color, fill: Color) {
    val center = Offset(fit.mapX(p.x), fit.mapY(p.y))
    drawCircle(color = outline, radius = 6.dp.toPx(), center = center)
    drawCircle(color = fill, radius = 3.dp.toPx(), center = center)
}

private fun DrawScope.drawSquareMarker(fit: TrackFit, p: ProjectedPoint, outline: Color, fill: Color) {
    val center = Offset(fit.mapX(p.x), fit.mapY(p.y))
    val outer = 22.dp.toPx()
    val inner = 16.dp.toPx()
    drawRect(color = outline, topLeft = Offset(center.x - outer / 2, center.y - outer / 2), size = Size(outer, outer))
    drawRect(color = fill, topLeft = Offset(center.x - inner / 2, center.y - inner / 2), size = Size(inner, inner))
}

private fun DrawScope.drawTriangleMarker(fit: TrackFit, p: ProjectedPoint, outline: Color, fill: Color) {
    val cx = fit.mapX(p.x)
    val cy = fit.mapY(p.y)
    fun triangle(r: Float): Path = Path().apply {
        moveTo(cx, cy - r * 1.2f)
        lineTo(cx + r * 0.9f, cy + r * 0.6f)
        lineTo(cx - r * 0.9f, cy + r * 0.6f)
        close()
    }
    drawPath(triangle(10.dp.toPx()), outline)
    drawPath(triangle(6.dp.toPx()), fill)
}
