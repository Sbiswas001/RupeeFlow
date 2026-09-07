package sayan.apps.rupeeflow.core.util

import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class DriveServiceHelper(private val driveService: Drive) {

    suspend fun createFile(filePath: String, fileName: String): String? = withContext(Dispatchers.IO) {
        val content = FileContent("application/octet-stream", java.io.File(filePath))
        
        // Search if file already exists
        val existingFileId = findFile(fileName)
        
        return@withContext if (existingFileId != null) {
            // Update existing file: Don't include 'parents' in metadata
            val updateMetadata = File().apply {
                name = fileName
            }
            driveService.files().update(existingFileId, updateMetadata, content).execute().id
        } else {
            // Create new file
            val createMetadata = File().apply {
                name = fileName
                parents = listOf("appDataFolder")
            }
            driveService.files().create(createMetadata, content).execute().id
        }
    }

    suspend fun findFile(fileName: String): String? = withContext(Dispatchers.IO) {
        val result = driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$fileName'")
            .setFields("files(id, name)")
            .execute()
        
        return@withContext result.files.firstOrNull()?.id
    }

    suspend fun downloadFile(fileId: String, outputStream: OutputStream) = withContext(Dispatchers.IO) {
        driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
    }

    suspend fun deleteFile(fileId: String) = withContext(Dispatchers.IO) {
        driveService.files().delete(fileId).execute()
    }
    
    suspend fun listBackups(): List<File> = withContext(Dispatchers.IO) {
        val result = driveService.files().list()
            .setSpaces("appDataFolder")
            .setFields("files(id, name, createdTime, modifiedTime)")
            .execute()
        return@withContext result.files ?: emptyList()
    }
}
