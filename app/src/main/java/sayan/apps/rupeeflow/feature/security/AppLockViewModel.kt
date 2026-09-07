package sayan.apps.rupeeflow.feature.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.core.security.AppLockManager
import sayan.apps.rupeeflow.core.security.AppLockState
import sayan.apps.rupeeflow.domain.repository.UserPreferencesRepository
import javax.inject.Inject

data class AppLockUiState(
    val lockState: AppLockState = AppLockState.INITIALIZING,
    val pin: String = "",
    val error: String? = null,
    val cooldownRemaining: Long = 0,
    val isBiometricEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false
)

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appLockManager: AppLockManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppLockUiState())
    val uiState: StateFlow<AppLockUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    init {
        viewModelScope.launch {
            appLockManager.lockState.collect { state ->
                _uiState.update { 
                    it.copy(
                        lockState = state,
                        // Reset PIN and errors whenever the app becomes LOCKED
                        pin = if (state == AppLockState.LOCKED) "" else it.pin,
                        error = if (state == AppLockState.LOCKED) null else it.error
                    )
                }
                
                if (state == AppLockState.COOLDOWN) {
                    startCooldownTimer()
                } else {
                    cooldownJob?.cancel()
                    cooldownJob = null
                    _uiState.update { it.copy(cooldownRemaining = 0) }
                }
            }
        }
        
        viewModelScope.launch {
            userPreferencesRepository.userPreferences.collect { prefs ->
                val biometricManager = BiometricManager.from(context)
                val isAvailable = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
                
                _uiState.update { 
                    it.copy(
                        isBiometricEnabled = prefs.fingerprintUnlock,
                        isBiometricAvailable = isAvailable
                    ) 
                }
            }
        }
    }

    fun onPinChar(char: String) {
        if (_uiState.value.pin.length < 6) {
            val newPin = _uiState.value.pin + char
            _uiState.update { it.copy(pin = newPin, error = null) }
            
            // Auto-check when reaching 6 digits
            if (newPin.length == 6) {
                onUnlock(newPin)
            }
        }
    }

    fun onDelete() {
        if (_uiState.value.pin.isNotEmpty()) {
            _uiState.update { it.copy(pin = it.pin.dropLast(1), error = null) }
        }
    }

    fun onUnlock(pin: String = _uiState.value.pin) {
        if (_uiState.value.lockState == AppLockState.COOLDOWN) return

        viewModelScope.launch {
            val success = appLockManager.verifyPin(pin)
            _uiState.update { 
                it.copy(
                    pin = "", 
                    error = if (success) null else "Incorrect PIN"
                ) 
            }
        }
    }

    fun onBiometricSuccess() {
        viewModelScope.launch {
            appLockManager.unlockWithBiometric()
        }
    }

    private fun startCooldownTimer() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            while (true) {
                val remaining = appLockManager.getRemainingCooldownMillis()
                _uiState.update { it.copy(cooldownRemaining = remaining) }
                if (remaining <= 0) break
                delay(1000)
            }
        }
    }
}
