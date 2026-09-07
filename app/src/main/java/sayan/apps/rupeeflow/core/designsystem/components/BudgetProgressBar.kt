package sayan.apps.rupeeflow.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A custom progress bar for budgets that visually distinguishes between the budget limit
 * and any over-budget spending.
 *
 * - When [progress] <= 1.0 (on track): Displays a single bar in [baseColor] or [warningColor].
 * - When [progress] > 1.0 (over budget): Displays a two-color bar where the first segment
 *   ([baseColor]) represents the 100% budget allowance and the second segment ([overBudgetColor])
 *   represents the excess spending over budget, separated by a subtle vertical marker line.
 */
@Composable
fun BudgetProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    baseColor: Color = Color(0xFF10B981),
    warningColor: Color = Color(0xFFF59E0B),
    overBudgetColor: Color = Color(0xFFEF4444),
    trackColor: Color = Color.White.copy(alpha = 0.1f),
    height: Dp = 8.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
    ) {
        val width = size.width
        val heightPx = size.height
        val cornerRadius = CornerRadius(heightPx / 2, heightPx / 2)

        if (progress <= 1f) {
            // Draw background track
            drawRoundRect(
                color = trackColor,
                size = size,
                cornerRadius = cornerRadius
            )
            // Draw filled progress
            val filledWidth = width * progress.coerceIn(0f, 1f)
            if (filledWidth > 0f) {
                val fillColor = if (progress >= 0.8f) warningColor else baseColor
                drawRoundRect(
                    color = fillColor,
                    size = Size(filledWidth, heightPx),
                    cornerRadius = cornerRadius
                )
            }
        } else {
            // Over budget: two-tone progress bar representing total expenditure
            val budgetFraction = (1f / progress).coerceIn(0f, 1f)
            val budgetWidth = width * budgetFraction

            // Segment 1: Within budget allowance
            drawRect(
                color = baseColor,
                topLeft = Offset(0f, 0f),
                size = Size(budgetWidth, heightPx)
            )

            // Segment 2: Over budget excess
            drawRect(
                color = overBudgetColor,
                topLeft = Offset(budgetWidth, 0f),
                size = Size(width - budgetWidth, heightPx)
            )

            // Budget cutoff line separator
            drawLine(
                color = Color.Black.copy(alpha = 0.35f),
                start = Offset(budgetWidth, 0f),
                end = Offset(budgetWidth, heightPx),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Preview
@Composable
fun BudgetProgressBarPreview() {
    Surface(
        color = Color(0xFF181818),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("On track (14%):", color = Color.White)
            BudgetProgressBar(progress = 0.14f)

            Text("Approaching limit (85%):", color = Color.White)
            BudgetProgressBar(progress = 0.85f)

            Text("Over budget (196% - 51% budget / 49% overage):", color = Color.White)
            BudgetProgressBar(progress = 1.96f)
        }
    }
}
