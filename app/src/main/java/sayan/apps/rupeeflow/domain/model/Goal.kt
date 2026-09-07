package sayan.apps.rupeeflow.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Goal(
    val id: Long,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: Long? = null,
    val icon: String = "🎯",
    val progress: Float = if (targetAmount > 0) (currentAmount / targetAmount).toFloat() else 0f,
    val remainingAmount: Double = (targetAmount - currentAmount).coerceAtLeast(0.0),
    val isCompleted: Boolean = targetAmount > 0 && currentAmount >= targetAmount
)
