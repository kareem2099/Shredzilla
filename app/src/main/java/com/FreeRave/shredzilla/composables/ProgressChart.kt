package com.FreeRave.shredzilla.composables

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.viewmodels.MainViewModel

/**
 * Smooth volume trend: Y is scaled between the min and max session so small gains stay visible,
 * with a soft accent fill underneath and a dot on the latest session.
 */
@Composable
fun ProgressChartCanvas(dataPoints: List<MainViewModel.ChartDataPoint>, modifier: Modifier = Modifier) {
    if (dataPoints.size < 2) return
    val palette = MaterialTheme.glass
    val maxVolume = dataPoints.maxOf { it.totalVolume }
    val minVolume = dataPoints.minOf { it.totalVolume }
    val volumeRange = (maxVolume - minVolume).takeIf { it > 0.0 } ?: 1.0 // all-equal data draws a flat line

    Canvas(modifier = modifier) {
        val stepX = size.width / (dataPoints.size - 1)
        val points = dataPoints.mapIndexed { index, point ->
            val normalized = if (maxVolume == minVolume) 0.5f else ((point.totalVolume - minVolume) / volumeRange).toFloat()
            // Keep 10% headroom top and bottom so the stroke and dot never clip
            Offset(index * stepX, size.height * (0.9f - normalized * 0.8f))
        }

        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.zipWithNext { from, to ->
                val midX = (from.x + to.x) / 2
                cubicTo(midX, from.y, midX, to.y, to.x, to.y)
            }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(fill, Brush.verticalGradient(listOf(palette.accent.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(
            line,
            Brush.horizontalGradient(palette.accentGradient),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(palette.accent.copy(alpha = 0.3f), radius = 9.dp.toPx(), center = points.last())
        drawCircle(palette.accent, radius = 5.dp.toPx(), center = points.last())
    }
}
