package sayan.apps.rupeeflow.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor() {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val KEY_ALIAS = "AppLockKey"
    private val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun getKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false) // We handle auth at AppLockManager level
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(data: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)
        
        // Format: IV_SIZE(1 byte) + IV + ENCRYPTED_DATA
        val combined = ByteArray(1 + iv.size + encryptedData.size)
        combined[0] = iv.size.toByte()
        System.arraycopy(iv, 0, combined, 1, iv.size)
        System.arraycopy(encryptedData, 0, combined, 1 + iv.size, encryptedData.size)
        
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(base64Data: String): ByteArray {
        val combined = Base64.decode(base64Data, Base64.NO_WRAP)
        val ivSize = combined[0].toInt()
        val iv = ByteArray(ivSize)
        System.arraycopy(combined, 1, iv, 0, ivSize)
        
        val encryptedDataSize = combined.size - 1 - ivSize
        val encryptedData = ByteArray(encryptedDataSize)
        System.arraycopy(combined, 1 + ivSize, encryptedData, 0, encryptedDataSize)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
        return cipher.doFinal(encryptedData)
    }
}
