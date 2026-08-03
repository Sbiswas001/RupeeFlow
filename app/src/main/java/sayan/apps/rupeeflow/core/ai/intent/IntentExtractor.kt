package sayan.apps.rupeeflow.core.ai.intent

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import sayan.apps.rupeeflow.core.ai.backend.AiBackend
import sayan.apps.rupeeflow.core.ai.prompt.PromptTemplates
import sayan.apps.rupeeflow.core.ai.session.ChatMessage
import sayan.apps.rupeeflow.core.ai.session.Role
import javax.inject.Inject

class IntentExtractor @Inject constructor(
    private val backend: AiBackend
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun extract(userText: String, history: List<String> = emptyList()): IntentParseResult {
        val prompt = "${PromptTemplates.intent}\n\nUser request: $userText"
        
        val response = backend.generate(
            messages = listOf(ChatMessage(Role.USER, prompt))
        )
        
        return parseResponse(response)
    }

    private fun parseResponse(response: String): IntentParseResult {
        return try {
            // Very basic parsing for now. In a real app, we'd use Moshi more robustly.
            val cleaned = response.trim().removeSurrounding("```json", "```").trim()
            val map = moshi.adapter(Map::class.java).fromJson(cleaned) as? Map<String, Any>
            val intentName = map?.get("intent") as? String
            
            val intent: FinanceIntent = when (intentName) {
                "GetMonthlySummary" -> FinanceIntent.GetMonthlySummary
                "GetBudgetStatus" -> FinanceIntent.GetBudgetStatus
                "DetectRecurringPayments" -> FinanceIntent.DetectRecurringPayments()
                "SearchTransactions" -> FinanceIntent.SearchTransactions(map["query"] as? String ?: "")
                else -> FinanceIntent.Unknown
            }
            
            IntentParseResult(intent, 1.0f, response)
        } catch (e: Exception) {
            IntentParseResult(FinanceIntent.Unknown, 0.0f, response)
        }
    }
}
