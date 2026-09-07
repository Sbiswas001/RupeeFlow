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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class BarChartData(
    val label: String,
    val value: Double,
    val color: Color = Color(0xFF10B981)
)

@Composable
fun BarChart(
    data: List<BarChartData>,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF10B981)
) {
    if (data.isEmpty()) return

    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        val width = size.width
        val height = size.height
        val barCount = data.size
        val maxAmount = data.maxOf { it.value }.toFloat().coerceAtLeast(1f)
        
        val spacing = 8.dp.toPx()
        val barWidth = (width - (spacing * (barCount + 1))) / barCount

        data.forEachIndexed { index, item ->
            val barHeight = (item.value.toFloat() / maxAmount) * height * animatedProgress.value
            val xPos = spacing + index * (barWidth + spacing)
            val yPos = height - barHeight

            drawRoundRect(
                color = item.color,
                topLeft = Offset(xPos, yPos),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}
