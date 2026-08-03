package sayan.apps.rupeeflow.core.ai.model

object ModelCatalog {
    val gemma3_1b = ModelConfig(
        id = "gemma-3-1b",
        displayName = "Gemma 3 1B",
        version = "1.0.0",
        fileName = "gemma-3-1b-it-int4.task",
        sizeBytes = 555 * 1024 * 1024,
        downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task"
    )

    data class HardwareRequirements(
        val minRamGb: Double,
        val recommendedRamGb: Double,
        val minStorageGb: Double,
        val minAndroidSdk: Int
    )

    val defaultRequirements = HardwareRequirements(
        minRamGb = 6.0,
        recommendedRamGb = 8.0,
        minStorageGb = 1.4,
        minAndroidSdk = 31 // Android 12
    )
}
