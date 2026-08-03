package sayan.apps.rupeeflow.core.ai.intent

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import sayan.apps.rupeeflow.core.ai.backend.AiBackend
import sayan.apps.rupeeflow.core.ai.session.ChatMessage

class IntentExtractorTest {

    private val mockBackend = object : AiBackend {
        var nextResponse: String = ""
        override suspend fun initialize() {}
        override suspend fun generate(messages: List<ChatMessage>, onToken: ((String) -> Unit)?): String = nextResponse
        override fun isInitialized(): Boolean = true
        override suspend fun release() {}
    }

    private val intentExtractor = IntentExtractor(mockBackend)

    @Test
    fun `extract MonthlySummary intent correctly`() = runBlocking {
        mockBackend.nextResponse = "{\"intent\": \"GetMonthlySummary\"}"
        val result = intentExtractor.extract("How much did I spend this month?")
        assertEquals(FinanceIntent.GetMonthlySummary, result.intent)
    }

    @Test
    fun `extract SearchTransactions intent with query`() = runBlocking {
        mockBackend.nextResponse = "{\"intent\": \"SearchTransactions\", \"query\": \"coffee\"}"
        val result = intentExtractor.extract("Show me coffee expenses")
        assertEquals(FinanceIntent.SearchTransactions("coffee"), result.intent)
    }

    @Test
    fun `handle Unknown intent gracefully`() = runBlocking {
        mockBackend.nextResponse = "invalid json"
        val result = intentExtractor.extract("something random")
        assertEquals(FinanceIntent.Unknown, result.intent)
    }
}
