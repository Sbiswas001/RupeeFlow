package sayan.apps.rupeeflow.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.core.ai.backend.AiManager
import sayan.apps.rupeeflow.core.ai.metrics.AiMetricsCollector
import sayan.apps.rupeeflow.core.ai.model.CompatibilityEngine
import sayan.apps.rupeeflow.core.ai.model.ModelManager
import javax.inject.Inject

@HiltViewModel
class AiViewModel @Inject constructor(
    private val aiManager: AiManager,
    private val modelManager: ModelManager,
    private val compatibilityEngine: CompatibilityEngine,
    private val metricsCollector: AiMetricsCollector
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiState())
    val uiState: StateFlow<AiState> = _uiState.asStateFlow()

    init {
        observeDownloadProgress()
        observeMetrics()
        performCompatibilityCheck()
    }

    private fun observeMetrics() {
        viewModelScope.launch {
            metricsCollector.metrics.collect { metrics ->
                _uiState.update { it.copy(metrics = metrics) }
            }
        }
    }

    private fun observeDownloadProgress() {
        viewModelScope.launch {
            modelManager.downloadProgress.collect { progress ->
                _uiState.update { it.copy(downloadProgress = progress) }
            }
        }
    }

    private fun performCompatibilityCheck() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = AiStatus.CHECKING_DEVICE) }
            val result = compatibilityEngine.checkCompatibility()
            
            if (result.isCompatible) {
                _uiState.update { it.copy(compatibilityResult = result) }
                checkModelStatus()
            } else {
                _uiState.update { it.copy(
                    status = AiStatus.INCOMPATIBLE,
                    compatibilityResult = result,
                    error = result.reason
                )}
            }
        }
    }

    private fun checkModelStatus() {
        viewModelScope.launch {
            val available = modelManager.isModelAvailable()
            if (available) {
                _uiState.update { it.copy(status = AiStatus.VERIFYING) }
                if (modelManager.verifyModel()) {
                    initializeAi()
                } else {
                    _uiState.update { it.copy(status = AiStatus.FAILED, error = "Model verification failed.") }
                }
            } else {
                _uiState.update { it.copy(status = AiStatus.NOT_DOWNLOADED) }
            }
        }
    }

    private fun initializeAi() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = AiStatus.INITIALIZING) }
            try {
                aiManager.initialize()
                _uiState.update { it.copy(status = AiStatus.READY) }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = AiStatus.FAILED, error = "AI Init Failed: ${e.message}") }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.status != AiStatus.READY) return

        val userMessage = ChatMessageUi(content = text, isFromUser = true)
        _uiState.update { it.copy(
            messages = it.messages + userMessage,
            isProcessing = true
        )}

        viewModelScope.launch {
            try {
                val response = aiManager.chat(text)
                val assistantMessage = ChatMessageUi(content = response, isFromUser = false)
                _uiState.update { it.copy(
                    messages = it.messages + assistantMessage,
                    isProcessing = false
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isProcessing = false,
                    error = "Chat Error: ${e.message}"
                )}
            }
        }
    }

    fun downloadModel() {
        _uiState.update { it.copy(status = AiStatus.DOWNLOADING) }
        viewModelScope.launch {
            val result = modelManager.downloadModel(_uiState.value.wifiOnlyDownload)
            if (result.isSuccess) {
                checkModelStatus()
            } else {
                _uiState.update { it.copy(status = AiStatus.FAILED, error = "Download Failed") }
            }
        }
    }

    fun startDemoMode() {
        _uiState.update { it.copy(status = AiStatus.READY, error = null) }
        initializeAi()
    }

    fun deleteModel() {
        viewModelScope.launch {
            modelManager.deleteModel()
            _uiState.update { it.copy(status = AiStatus.NOT_DOWNLOADED, error = null) }
        }
    }

    fun toggleWifiOnly(enabled: Boolean) {
        _uiState.update { it.copy(wifiOnlyDownload = enabled) }
    }

    fun dismissPrivacyBanner() {
        _uiState.update { it.copy(showPrivacyBanner = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
