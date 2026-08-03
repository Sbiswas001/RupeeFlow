package sayan.apps.rupeeflow.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class DonutChartData(
    val amount: Double,
    val color: Color
)

@Composable
fun DonutChart(
    data: List<DonutChartData>,
    modifier: Modifier = Modifier,
    thickness: Dp = 30.dp,
    gapDegree: Float = 0f
) {
    val totalAmount = data.sumOf { it.amount }.toFloat()
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(200.dp)) {
            var startAngle = -90f
            
            data.forEach { item ->
                val sweepAngle = (item.amount.toFloat() / totalAmount) * 360f * animatedProgress.value
                
                drawArc(
                    color = item.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle - gapDegree,
                    useCenter = false,
                    style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round)
                )
                startAngle += sweepAngle
            }
        }
    }
}
