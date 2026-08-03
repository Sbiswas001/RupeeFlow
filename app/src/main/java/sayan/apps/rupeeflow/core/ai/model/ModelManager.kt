package sayan.apps.rupeeflow.core.ai.model

import kotlinx.coroutines.flow.StateFlow

interface ModelManager {
    val downloadProgress: StateFlow<Int>
    suspend fun isModelAvailable(): Boolean
    suspend fun downloadModel(wifiOnly: Boolean): Result<Unit>
    suspend fun verifyModel(): Boolean
    suspend fun deleteModel(): Result<Unit>
    suspend fun getStorageUsedBytes(): Long
}
