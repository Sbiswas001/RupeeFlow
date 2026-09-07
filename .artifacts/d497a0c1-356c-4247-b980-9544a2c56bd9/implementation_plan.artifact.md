# Fix "Offline" status for already downloaded AI model

The app incorrectly identifies the AI model as "Offline" (not downloaded) even when the file exists on disk. This is because `ModelManagerImpl.isModelAvailable` performs a mandatory ZIP header check (`PK` signature), but the Gemma model `.task` file used by MediaPipe is not necessarily a ZIP archive.

## Proposed Changes

### AI Core

#### [MODIFY] [ModelManagerImpl.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/ai/model/ModelManagerImpl.kt)

- Update `isModelAvailable()` to remove the ZIP header check.
- Implement a more robust check that verifies the file exists and its size matches the expected size defined in `ModelCatalog` (with a 1KB tolerance to account for potential file system block differences).

```kotlin
    override suspend fun isModelAvailable(): Boolean = withContext(Dispatchers.IO) {
        val model = ModelCatalog.activeModel
        val file = File(context.filesDir, "${ModelCatalog.MODEL_DIR}/${model.fileName}")

        if (!file.exists()) return@withContext false

        // Verify size matches expected size with small tolerance
        val sizeDiff = Math.abs(file.length() - model.sizeBytes)
        val isSizeCorrect = sizeDiff < 1024 // 1KB tolerance

        if (!isSizeCorrect) {
            Log.w(TAG, "Model file size mismatch. Expected: ${model.sizeBytes}, Actual: ${file.length()}")
        }

        isSizeCorrect
    }
```

## Verification Plan

### Manual Verification
1. Launch the app.
2. Navigate to the AI Assistant screen.
3. Verify that the UI transitions from "Checking compatibility..." to the Chat interface (or "Verifying" -> "Installing" -> Chat) instead of showing the "Download Model" button.
4. If the model was previously "stuck" in the Offline state, it should now be recognized immediately.
