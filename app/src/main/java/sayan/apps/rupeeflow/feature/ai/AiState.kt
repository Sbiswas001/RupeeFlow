package sayan.apps.rupeeflow.feature.ai

import sayan.apps.rupeeflow.core.ai.metrics.AiMetricsCollector
import sayan.apps.rupeeflow.core.ai.model.CompatibilityEngine

data class AiState(
    val status: AiStatus = AiStatus.UNKNOWN,
    val compatibilityResult: CompatibilityEngine.CompatibilityResult? = null,
    val metrics: AiMetricsCollector.AiMetrics = AiMetricsCollector.AiMetrics(),
    val downloadProgress: Int = 0,
    val messages: List<ChatMessageUi> = emptyList(),
    val isProcessing: Boolean = false,
    val error: String? = null,
    val wifiOnlyDownload: Boolean = true,
    val showPrivacyBanner: Boolean = true
) {
    // Helper properties for UI
    val isModelDownloaded: Boolean get() = status != AiStatus.NOT_DOWNLOADED && status != AiStatus.DOWNLOADING && status != AiStatus.INCOMPATIBLE && status != AiStatus.UNKNOWN
    val isDownloading: Boolean get() = status == AiStatus.DOWNLOADING
    val isAiReady: Boolean get() = status == AiStatus.READY
}
