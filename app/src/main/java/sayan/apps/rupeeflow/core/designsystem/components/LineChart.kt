package sayan.apps.rupeeflow.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

data class LineChartPoint(
    val x: Float,
    val y: Double
)

@Composable
fun LineChart(
    points: List<LineChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF10B981)
) {
    if (points.isEmpty()) return

    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(points) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        val width = size.width
        val height = size.height
        
        val maxAmount = points.maxOf { it.y }.toFloat().coerceAtLeast(1f)
        val minAmount = 0f // Start from zero for better context
        
        val path = Path()
        val fillPath = Path()
        
        points.forEachIndexed { index, point ->
            val xPos = (index.toFloat() / (points.size - 1).coerceAtLeast(1)) * width
            val yPos = height - ((point.y.toFloat() - minAmount) / (maxAmount - minAmount)) * height
            
            if (index == 0) {
                path.moveTo(xPos, yPos)
                fillPath.moveTo(xPos, yPos)
            } else {
                path.lineTo(xPos, yPos)
                fillPath.lineTo(xPos, yPos)
            }
        }

        // Apply progress for animation
        val pathMeasure = android.graphics.PathMeasure(path.asAndroidPath(), false)
        val length = pathMeasure.length
        val androidAnimatedPath = android.graphics.Path()
        pathMeasure.getSegment(0f, length * animatedProgress.value, androidAnimatedPath, true)
        
        drawPath(
            path = androidAnimatedPath.asComposePath(),
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Gradient under the line (clip it to animated path length)
        val animatedFillPath = android.graphics.Path()
        pathMeasure.getSegment(0f, length * animatedProgress.value, animatedFillPath, true)
        animatedFillPath.lineTo((points.size - 1).toFloat() / (points.size - 1).coerceAtLeast(1) * width * animatedProgress.value, height) // This is wrong
        
        // Let's do it better: use the already constructed fillPath but clip it
        drawContext.canvas.save()
        drawContext.canvas.clipRect(0f, 0f, width * animatedProgress.value, height)
        
        fillPath.lineTo(width, height)
        fillPath.lineTo(0f, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.2f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )
        drawContext.canvas.restore()
    }
}
