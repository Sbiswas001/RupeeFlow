package sayan.apps.rupeeflow.core.designsystem.components

import android.graphics.PathMeasure
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class LineChartPoint(
    val x: Float,
    val y: Double
)

@Composable
fun LineChart(
    points: List<LineChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color? = null,
    strokeWidth: Dp = 2.dp,
    increaseColor: Color = Color(0xFF1D4ED8),
    decreaseColor: Color = Color(0xFF991B1B),
    neutralColor: Color = Color.White.copy(alpha = 0.8f)
) {
    if (points.size < 2) return

    val sortedPoints = remember(points) { points.sortedBy { it.x } }
    val animatedProgress = remember(sortedPoints) { Animatable(0f) }

    LaunchedEffect(sortedPoints) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val startVal = sortedPoints.first().y
    val endVal = sortedPoints.last().y
    val overallTrendColor = lineColor ?: when {
        endVal > startVal -> increaseColor
        endVal < startVal -> decreaseColor
        else -> neutralColor
    }

    Canvas(modifier = modifier.fillMaxWidth().height(50.dp)) {
        val width = size.width
        val height = size.height

        val yValues = sortedPoints.map { it.y.toFloat() }
        val rawMin = yValues.minOrNull() ?: 0f
        val rawMax = yValues.maxOrNull() ?: 0f

        val range = rawMax - rawMin
        val padding = if (range == 0f) 1f else range * 0.1f
        val minY = rawMin - padding
        val maxY = rawMax + padding
        val span = maxY - minY

        val coords = sortedPoints.mapIndexed { index, point ->
            val xPos = (index.toFloat() / (sortedPoints.size - 1)).coerceAtLeast(0f) * width
            val yPos = if (span == 0f) {
                height / 2f
            } else {
                height - ((point.y.toFloat() - minY) / span) * height
            }
            Offset(xPos, yPos)
        }

        val path = Path()
        if (coords.isNotEmpty()) {
            path.moveTo(coords[0].x, coords[0].y)
            for (i in 0 until coords.size - 1) {
                val p1 = coords[i]
                val p2 = coords[i + 1]
                val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
                val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)
                path.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p2.x, p2.y)
            }
        }

        val pathMeasure = PathMeasure(path.asAndroidPath(), false)
        val length = pathMeasure.length
        val animatedPath = android.graphics.Path()
        pathMeasure.getSegment(0f, length * animatedProgress.value, animatedPath, true)

        drawPath(
            path = animatedPath.asComposePath(),
            color = overallTrendColor,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
