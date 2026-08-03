package sayan.apps.rupeeflow.core.ai.model

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ModelManager {

    private val _downloadProgress = MutableStateFlow(0)
    override val downloadProgress: StateFlow<Int> = _downloadProgress

    override suspend fun isModelAvailable(): Boolean = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "models/gemma-3-1b-it-int4.task")
        file.exists() && file.length() > 1024 * 1024 // Must be > 1MB to be considered valid
    }

    override suspend fun downloadModel(wifiOnly: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val model = ModelCatalog.gemma3_1b
            val request = DownloadManager.Request(Uri.parse(model.downloadUrl))
                .setTitle("Downloading AI Model")
                .setDescription("Gemma 3 1B for RupeeFlow")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, "models", model.fileName)
                .setAllowedOverMetered(!wifiOnly)
                .setAllowedOverRoaming(false)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            // Start a coroutine to track progress
            var isDownloaded = false
            while (!isDownloaded) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

                    if (bytesTotal > 0) {
                        _downloadProgress.value = (bytesDownloaded * 100L / bytesTotal).toInt()
                    }

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        isDownloaded = true
                        
                        // Move file to internal storage where MediaPipe expects it
                        val downloadedFile = File(context.getExternalFilesDir("models"), model.fileName)
                        val internalDir = File(context.filesDir, "models")
                        if (!internalDir.exists()) internalDir.mkdirs()
                        val internalFile = File(internalDir, model.fileName)
                        
                        downloadedFile.copyTo(internalFile, overwrite = true)
                        downloadedFile.delete()
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        throw Exception("Download failed with status $status")
                    }
                }
                cursor.close()
                if (!isDownloaded) delay(500)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyModel(): Boolean = isModelAvailable()

    override suspend fun deleteModel(): Result<Unit> = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "models/gemma-3-1b-it-int4.task")
        if (file.exists() && file.delete()) {
            Result.success(Unit)
        } else {
            Result.success(Unit) // Succeed if already gone
        }
    }

    override suspend fun getStorageUsedBytes(): Long = withContext(Dispatchers.IO) {
        File(context.filesDir, "models/gemma-3-1b-it-int4.task").length()
    }
}
