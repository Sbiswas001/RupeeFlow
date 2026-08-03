package sayan.apps.rupeeflow.core.ai.model

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompatibilityEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class CompatibilityResult(
        val isCompatible: Boolean,
        val androidVersionOk: Boolean,
        val ramOk: Boolean,
        val storageOk: Boolean,
        val gpuOk: Boolean,
        val totalRamGb: Double,
        val freeStorageGb: Double,
        val reason: String? = null
    )

    fun checkCompatibility(): CompatibilityResult {
        val reqs = ModelCatalog.defaultRequirements
        val minAndroidVersion = reqs.minAndroidSdk
        val minRamGb = reqs.minRamGb
        val minStorageGb = reqs.minStorageGb

        val androidVersionOk = Build.VERSION.SDK_INT >= minAndroidVersion
        
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)
        val totalRamGb = memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        val ramOk = totalRamGb >= minRamGb

        val stat = StatFs(context.filesDir.path)
        val freeStorageGb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024.0 * 1024.0 * 1024.0)
        val storageOk = freeStorageGb >= minStorageGb

        // Simple GPU check: assume true for now, can be refined with OpenGL checks
        val gpuOk = true 

        val isCompatible = androidVersionOk && ramOk && storageOk && gpuOk

        val reason = when {
            !androidVersionOk -> "Android 12 or higher required."
            !ramOk -> "Minimum 6GB RAM required (Device has ${String.format("%.1f", totalRamGb)}GB)."
            !storageOk -> "At least 1.4GB free storage required."
            else -> null
        }

        return CompatibilityResult(
            isCompatible = isCompatible,
            androidVersionOk = androidVersionOk,
            ramOk = ramOk,
            storageOk = storageOk,
            gpuOk = gpuOk,
            totalRamGb = totalRamGb,
            freeStorageGb = freeStorageGb,
            reason = reason
        )
    }
}
