package sayan.apps.rupeeflow.domain.repository

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface BackupRepository {
    fun backupToLocal(uri: Uri): Flow<BackupResult>
    fun restoreFromLocal(uri: Uri): Flow<BackupResult>
    
    // Google Drive
    suspend fun backupToGoogleDrive(accessToken: String): BackupResult
    suspend fun restoreFromGoogleDrive(accessToken: String): BackupResult
    suspend fun deleteGoogleDriveBackup(accessToken: String): BackupResult
}

sealed class BackupResult {
    object Success : BackupResult()
    data class Error(val message: String) : BackupResult()
    object Loading : BackupResult()
}
