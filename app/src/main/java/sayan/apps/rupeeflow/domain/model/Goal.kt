package sayan.apps.rupeeflow.domain.model

data class Goal(
    val id: Long,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: Long?,
    val icon: String = "🎯",
    val progress: Float = if (targetAmount > 0) (currentAmount / targetAmount).toFloat() else 0f
)
