package sayan.apps.rupeeflow.core.ai.backend

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sayan.apps.rupeeflow.core.ai.metrics.AiMetricsCollector
import sayan.apps.rupeeflow.core.ai.session.ChatMessage
import sayan.apps.rupeeflow.core.ai.session.Role
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaPipeAiBackend @Inject constructor(
    @ApplicationContext private val context: Context,
    private val metricsCollector: AiMetricsCollector
) : AiBackend {

    private var llmInference: LlmInference? = null

    override suspend fun initialize() = withContext(Dispatchers.IO) {
        if (isInitialized()) return@withContext

        val startTime = System.currentTimeMillis()
        val modelPath = File(context.filesDir, "models/gemma-3-1b-it-int4.task").absolutePath
        if (!File(modelPath).exists()) {
            throw IllegalStateException("Model file not found at $modelPath")
        }

        val options = LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(2048)
            .build()

        llmInference = LlmInference.createFromOptions(context, options)
        metricsCollector.recordModelLoadTime(System.currentTimeMillis() - startTime)
    }

    override suspend fun generate(
        messages: List<ChatMessage>,
        onToken: ((String) -> Unit)?
    ): String = withContext(Dispatchers.Default) {
        val inference = llmInference ?: throw IllegalStateException("AI Backend not initialized")
        
        val prompt = buildPrompt(messages)
        val startTime = System.currentTimeMillis()
        var firstTokenTime = 0L
        
        // MediaPipe LlmInference currently supports synchronous generateResponse 
        // and asynchronous generateResponse for streaming.
        val response = if (onToken != null) {
            // Streaming implementation (simplified)
            // In a real app, we'd use the streaming API correctly
            val res = inference.generateResponse(prompt)
            firstTokenTime = System.currentTimeMillis() // Fake first token for mock
            onToken(res)
            res
        } else {
            inference.generateResponse(prompt)
        }
        
        val endTime = System.currentTimeMillis()
        if (firstTokenTime == 0L) firstTokenTime = endTime
        
        metricsCollector.recordInference(
            firstTokenMs = firstTokenTime - startTime,
            totalMs = endTime - startTime,
            tokens = response.split(" ").size // Estimate tokens
        )
        
        response
    }

    override fun isInitialized(): Boolean = llmInference != null

    override suspend fun release() {
        llmInference?.close()
        llmInference = null
    }

    private fun buildPrompt(messages: List<ChatMessage>): String {
        return messages.joinToString("\n") { message ->
            val rolePrefix = when (message.role) {
                Role.USER -> "<|user|>\n"
                Role.ASSISTANT -> "<|assistant|>\n"
                Role.TOOL -> "<|system|>\nFinancial Data: "
            }
            "$rolePrefix${message.content}<|end|>"
        } + "\n<|assistant|>\n"
    }
}
