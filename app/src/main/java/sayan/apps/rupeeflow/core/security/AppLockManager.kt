package sayan.apps.rupeeflow.core.security

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.domain.model.LockTimeout
import sayan.apps.rupeeflow.domain.model.UserPreferences
import sayan.apps.rupeeflow.domain.repository.UserPreferencesRepository
import java.security.SecureRandom
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.withContext

enum class AppLockState {
    INITIALIZING,
    LOCKED,
    AUTHENTICATING,
    UNLOCKED,
    COOLDOWN
}

@Singleton
class AppLockManager @Inject constructor(
    private val cryptoManager: CryptoManager,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    
    private val _lockState = MutableStateFlow(AppLockState.INITIALIZING)
    val lockState: StateFlow<AppLockState> = _lockState.asStateFlow()

    private var lastBackgroundTime: Long = 0

    private val userPreferences = userPreferencesRepository.userPreferences.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = UserPreferences()
    )

    private val PBKDF2_ITERATIONS = 20000
    private val PBKDF2_KEY_LENGTH = 256
    private val VERSION = 1

    private var hasInitializedLockState = false

    init {
        scope.launch {
            userPreferences.collect { prefs ->
                val firstLoad = !hasInitializedLockState
                hasInitializedLockState = true
                
                if (!prefs.appLock) {
                    _lockState.value = AppLockState.UNLOCKED
                } else if (firstLoad) {
                    // On first load/app start, if lock is enabled, we check for cooldown then set state
                    if (System.currentTimeMillis() < prefs.cooldownEndTimeMillis) {
                        _lockState.value = AppLockState.COOLDOWN
                    } else {
                        _lockState.value = AppLockState.LOCKED
                    }
                }
            }
        }
    }

    fun onAppForegrounded() {
        // If we haven't even loaded preferences yet, don't attempt to change state.
        // The init block's collector will handle the state transition upon first load.
        if (!hasInitializedLockState || _lockState.value == AppLockState.INITIALIZING) return

        val prefs = userPreferences.value
        if (!prefs.appLock) {
            _lockState.value = AppLockState.UNLOCKED
            return
        }

        val currentState = _lockState.value
        if (currentState == AppLockState.UNLOCKED) {
            val elapsed = SystemClock.elapsedRealtime() - lastBackgroundTime
            // If timeout is IMMEDIATE, or if enough time has passed since last backgrounded
            if (prefs.lockTimeout == LockTimeout.IMMEDIATE || (lastBackgroundTime > 0 && elapsed >= prefs.lockTimeout.durationMillis)) {
                _lockState.value = AppLockState.LOCKED
            }
        } else if (currentState != AppLockState.COOLDOWN && currentState != AppLockState.AUTHENTICATING) {
             _lockState.value = AppLockState.LOCKED
        }
        
        checkCooldown()
    }

    fun onAppPaused() {
        handleAppBackgrounded()
    }

    fun onAppBackgrounded() {
        handleAppBackgrounded()
    }

    private fun handleAppBackgrounded() {
        lastBackgroundTime = SystemClock.elapsedRealtime()
        val prefs = userPreferences.value
        if (prefs.appLock && prefs.lockTimeout == LockTimeout.IMMEDIATE) {
            _lockState.value = AppLockState.LOCKED
        }
    }

    fun onScreenOff() {
        handleAppBackgrounded()
    }

    fun lock() {
        if (userPreferences.value.appLock) {
            _lockState.value = AppLockState.LOCKED
        }
    }

    fun setAuthenticating() {
        if (_lockState.value == AppLockState.LOCKED) {
            _lockState.value = AppLockState.AUTHENTICATING
        }
    }

    private fun checkCooldown() {
        val prefs = userPreferences.value
        if (System.currentTimeMillis() < prefs.cooldownEndTimeMillis) {
            _lockState.value = AppLockState.COOLDOWN
        } else if (_lockState.value == AppLockState.COOLDOWN) {
            _lockState.value = AppLockState.LOCKED
        }
    }

    fun getRemainingCooldownMillis(): Long {
        return (userPreferences.value.cooldownEndTimeMillis - System.currentTimeMillis()).coerceAtLeast(0)
    }

    suspend fun createPin(pin: String) {
        val salt = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val hash = deriveHash(pin, salt)
        val encryptedHash = cryptoManager.encrypt(hash)
        val saltBase64 = android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP)
        
        // Material format: VERSION|SALT|ENCRYPTED_HASH
        val material = "$VERSION|$saltBase64|$encryptedHash"
        userPreferencesRepository.updateEncryptedPinMaterial(material)
        userPreferencesRepository.updateAppLock(true)
        _lockState.value = AppLockState.UNLOCKED
    }

    suspend fun verifyPin(pin: String): Boolean {
        checkCooldown()
        val prefs = userPreferences.value
        if (_lockState.value == AppLockState.COOLDOWN) return false

        val material = prefs.encryptedPinMaterial ?: return false
        val parts = material.split("|")
        if (parts.size < 3) return false
        
        val version = parts[0].toInt()
        val salt = android.util.Base64.decode(parts[1], android.util.Base64.NO_WRAP)
        val encryptedHash = parts[2]
        
        val storedHash = cryptoManager.decrypt(encryptedHash)
        val derivedHash = deriveHash(pin, salt)
        
        val success = MessageDigestEquals(storedHash, derivedHash)
        
        if (success) {
            userPreferencesRepository.updateFailedAttempts(0)
            _lockState.value = AppLockState.UNLOCKED
        } else {
            val newFailedAttempts = prefs.failedAttempts + 1
            userPreferencesRepository.updateFailedAttempts(newFailedAttempts)
            if (newFailedAttempts >= 5) {
                val cooldownSec = when (newFailedAttempts) {
                    5 -> 30
                    6 -> 60
                    7 -> 300
                    else -> 1800
                }
                val endTime = System.currentTimeMillis() + (cooldownSec * 1000L)
                userPreferencesRepository.updateCooldownEndTimeMillis(endTime)
                _lockState.value = AppLockState.COOLDOWN
            }
        }
        return success
    }

    suspend fun unlockWithBiometric() {
        userPreferencesRepository.updateFailedAttempts(0)
        userPreferencesRepository.updateCooldownEndTimeMillis(0)
        _lockState.value = AppLockState.UNLOCKED
    }

    private suspend fun deriveHash(pin: String, salt: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        factory.generateSecret(spec).encoded
    }

    private fun MessageDigestEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }

    suspend fun disableAppLock() {
        userPreferencesRepository.updateAppLock(false)
        userPreferencesRepository.updateEncryptedPinMaterial(null)
        userPreferencesRepository.updateFailedAttempts(0)
        userPreferencesRepository.updateCooldownEndTimeMillis(0)
        _lockState.value = AppLockState.UNLOCKED
    }
}
