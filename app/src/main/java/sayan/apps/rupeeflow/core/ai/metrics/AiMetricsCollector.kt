package sayan.apps.rupeeflow.core.ai.metrics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiMetricsCollector @Inject constructor() {
    
    data class AiMetrics(
        val firstTokenLatencyMs: Long = 0,
        val totalLatencyMs: Long = 0,
        val tokensPerSecond: Double = 0.0,
        val modelLoadTimeMs: Long = 0,
        val cacheHitCount: Int = 0,
        val cacheMissCount: Int = 0
    )

    private val _metrics = MutableStateFlow(AiMetrics())
    val metrics: StateFlow<AiMetrics> = _metrics.asStateFlow()

    fun recordModelLoadTime(timeMs: Long) {
        _metrics.update { it.copy(modelLoadTimeMs = timeMs) }
    }

    fun recordInference(firstTokenMs: Long, totalMs: Long, tokens: Int) {
        val tps = if (totalMs > 0) (tokens.toDouble() / (totalMs / 1000.0)) else 0.0
        _metrics.update { it.copy(
            firstTokenLatencyMs = firstTokenMs,
            totalLatencyMs = totalMs,
            tokensPerSecond = tps
        )}
    }

    fun recordCacheHit() {
        _metrics.update { it.copy(cacheHitCount = it.cacheHitCount + 1) }
    }

    fun recordCacheMiss() {
        _metrics.update { it.copy(cacheMissCount = it.cacheMissCount + 1) }
    }
}
