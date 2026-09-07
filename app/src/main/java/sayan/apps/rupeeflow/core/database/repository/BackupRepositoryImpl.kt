package sayan.apps.rupeeflow.core.database.repository

import android.content.Context
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import sayan.apps.rupeeflow.core.database.RupeeFlowDatabase
import sayan.apps.rupeeflow.core.util.Constants
import sayan.apps.rupeeflow.core.util.DriveServiceHelper
import sayan.apps.rupeeflow.domain.repository.BackupRepository
import sayan.apps.rupeeflow.domain.repository.BackupResult
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: RupeeFlowDatabase
) : BackupRepository {

    override fun backupToLocal(uri: Uri): Flow<BackupResult> = flow {
        emit(BackupResult.Loading)
        try {
            // 1. Checkpoint the database to ensure all WAL data is in the main .db file
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
                cursor.moveToFirst()
            }
            
            // 2. Get the database file
            val dbFile = context.getDatabasePath(Constants.DATABASE_NAME)
            if (!dbFile.exists()) {
                android.util.Log.e("BackupRepo", "Database file not found at ${dbFile.absolutePath}")
                emit(BackupResult.Error("Database file not found"))
                return@flow
            }
            
            android.util.Log.d("BackupRepo", "Backing up database. Size: ${dbFile.length()} bytes")
            
            // 3. Copy to the destination URI
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(dbFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            emit(BackupResult.Success)
        } catch (e: Exception) {
            emit(BackupResult.Error(e.message ?: "Unknown error occurred during backup"))
        }
    }.flowOn(Dispatchers.IO)

    override fun restoreFromLocal(uri: Uri): Flow<BackupResult> = flow {
        emit(BackupResult.Loading)
        try {
            // 1. Copy URI to temp file for validation and processing
            val tempFile = File(context.cacheDir, "temp_local_restore.db")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            // 2. Use the internal restore logic
            val result = restoreFromLocalInternal(tempFile)
            tempFile.delete()
            
            emit(result)
        } catch (e: Exception) {
            emit(BackupResult.Error(e.message ?: "Unknown error occurred during restore"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun backupToGoogleDrive(accessToken: String): BackupResult = withContext(Dispatchers.IO) {
        try {
            // 1. Checkpoint the database to ensure all WAL data is in the main .db file
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
                cursor.moveToFirst()
            }
            
            val requestInitializer = HttpRequestInitializer { request ->
                request.headers.authorization = "Bearer $accessToken"
            }
            
            val googleDriveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory(),
                requestInitializer
            ).setApplicationName("RupeeFlow").build()
            
            val driveHelper = DriveServiceHelper(googleDriveService)
            
            val dbFile = context.getDatabasePath(Constants.DATABASE_NAME)
            if (!dbFile.exists()) return@withContext BackupResult.Error("Database file not found")
            
            android.util.Log.d("BackupRepo", "Uploading database to Drive. Size: ${dbFile.length()} bytes")
            
            // 2. Upload
            val fileId = driveHelper.createFile(dbFile.absolutePath, "rupeeflow_backup.db")
            
            if (fileId != null) BackupResult.Success else BackupResult.Error("Failed to upload backup")
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "Google Drive backup failed")
        }
    }

    override suspend fun restoreFromGoogleDrive(accessToken: String): BackupResult = withContext(Dispatchers.IO) {
        try {
            val requestInitializer = HttpRequestInitializer { request ->
                request.headers.authorization = "Bearer $accessToken"
            }
            
            val googleDriveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory(),
                requestInitializer
            ).setApplicationName("RupeeFlow").build()
            
            val driveHelper = DriveServiceHelper(googleDriveService)
            
            // 1. Find file
            val fileId = driveHelper.findFile("rupeeflow_backup.db") ?: return@withContext BackupResult.Error("No backup found on Google Drive")
            
            // 2. Download to temp file
            val tempFile = File(context.cacheDir, "temp_restore.db")
            FileOutputStream(tempFile).use { 
                driveHelper.downloadFile(fileId, it)
            }
            
            // 3. Restore from local (temp file)
            val result = restoreFromLocalInternal(tempFile)
            tempFile.delete()
            
            result
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "Google Drive restore failed")
        }
    }

    override suspend fun deleteGoogleDriveBackup(accessToken: String): BackupResult = withContext(Dispatchers.IO) {
        try {
            val requestInitializer = HttpRequestInitializer { request ->
                request.headers.authorization = "Bearer $accessToken"
            }
            
            val googleDriveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory(),
                requestInitializer
            ).setApplicationName("RupeeFlow").build()
            
            val driveHelper = DriveServiceHelper(googleDriveService)
            
            val fileId = driveHelper.findFile("rupeeflow_backup.db") 
                ?: return@withContext BackupResult.Error("No backup found on Google Drive")
            
            driveHelper.deleteFile(fileId)
            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "Failed to delete Google Drive backup")
        }
    }

    private fun restoreFromLocalInternal(sourceFile: File): BackupResult {
        return try {
            android.util.Log.d("BackupRepo", "Restoring from file. Size: ${sourceFile.length()} bytes")
            if (sourceFile.length() < 1024) { // SQLite files are usually at least 1KB+
                 return BackupResult.Error("Backup file appears to be empty or invalid")
            }
            
            database.close()
            val dbFile = context.getDatabasePath(Constants.DATABASE_NAME)
            android.util.Log.d("BackupRepo", "Overwriting database at ${dbFile.absolutePath}")
            
            sourceFile.inputStream().use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            // Delete -wal and -shm files if they exist to prevent corruption from old journal
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()
            
            BackupResult.Success
        } catch (e: Exception) {
            android.util.Log.e("BackupRepo", "Restore failed", e)
            BackupResult.Error(e.message ?: "Restore failed")
        }
    }
}
