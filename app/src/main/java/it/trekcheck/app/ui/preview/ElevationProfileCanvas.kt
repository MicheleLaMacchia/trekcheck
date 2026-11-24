package it.trekcheck.app.ui.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.trekcheck.app.domain.model.ElevationProfilePreview

/** Maps a profile sample to pixel space; flat profiles/zero length are centred without NaN. */
internal data class ProfileFit(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val minElevation: Double,
    val elevationRange: Double,
    val totalDistance: Double,
) {
    fun mapX(distance: Double): Float =
        if (totalDistance > 0.0) left + (distance / totalDistance).toFloat() * width else left + width / 2f

    fun mapY(elevation: Double): Float {
        if (!elevationRange.isFinite() || elevationRange <= 0.0) {
            return top + height / 2f
        }
        val delta = elevation - minElevation
        if (!delta.isFinite()) {
            return top + height / 2f
        }
        return top + height - (delta / elevationRange).toFloat() * height
    }

    companion object {
        fun compute(profile: ElevationProfilePreview, widthPx: Float, heightPx: Float, paddingPx: Float) =
            ProfileFit(
                left = paddingPx,
                top = paddingPx,
                width = (widthPx - 2f * paddingPx).coerceAtLeast(1f),
                height = (heightPx - 2f * paddingPx).coerceAtLeast(1f),
                minElevation = profile.minElevationMeters,
                elevationRange = profile.maxElevationMeters - profile.minElevationMeters,
                totalDistance = profile.totalDistanceMeters,
            )
    }
}

@Composable
fun ElevationProfileCanvas(
    profile: ElevationProfilePreview,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outline
    val fillColor = MaterialTheme.colorScheme.primaryContainer
    val background = MaterialTheme.colorScheme.surfaceContainerLowest

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .testTag("elevation_profile_canvas")
            .semantics { this.contentDescription = contentDescription },
    ) {
        drawRect(color = background, size = size)
        val fit = ProfileFit.compute(profile, size.width, size.height, 16.dp.toPx())
        val baseline = fit.top + fit.height

        drawLine(
            color = axisColor,
            start = Offset(fit.left, baseline),
            end = Offset(fit.left + fit.width, baseline),
            strokeWidth = 1.dp.toPx(),
        )

        val runs = profile.runs
        if (runs.isEmpty()) return@Canvas
        val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        for (run in runs) {
            if (run.size >= 2) {
                val line = Path()
                val area = Path()
                run.forEachIndexed { index, sample ->
                    val x = fit.mapX(sample.distanceMeters)
                    val y = fit.mapY(sample.elevationMeters)
                    if (index == 0) {
                        line.moveTo(x, y)
                        area.moveTo(x, baseline)
                        area.lineTo(x, y)
                    } else {
                        line.lineTo(x, y)
                        area.lineTo(x, y)
                    }
                }
                area.lineTo(fit.mapX(run.last().distanceMeters), baseline)
                area.close()
                drawPath(path = area, color = fillColor)
                drawPath(path = line, color = lineColor, style = stroke)
            } else if (run.size == 1) {
                drawCircle(
                    color = lineColor,
                    radius = 2.5.dp.toPx(),
                    center = Offset(fit.mapX(run[0].distanceMeters), fit.mapY(run[0].elevationMeters)),
                )
            }
        }
    }
}
