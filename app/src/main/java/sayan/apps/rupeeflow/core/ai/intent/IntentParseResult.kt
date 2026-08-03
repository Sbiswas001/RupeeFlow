package sayan.apps.rupeeflow.core.ai.intent

data class IntentParseResult(
    val intent: FinanceIntent,
    val confidence: Float,
    val rawModelOutput: String? = null
)
