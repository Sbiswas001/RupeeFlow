package sayan.apps.rupeeflow.feature.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import sayan.apps.rupeeflow.core.security.AppLockState

@Composable
fun AppLockScreen(
    viewModel: AppLockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    LaunchedEffect(uiState.isBiometricEnabled, uiState.isBiometricAvailable) {
        if (uiState.isBiometricEnabled && uiState.isBiometricAvailable && uiState.lockState == AppLockState.LOCKED) {
            activity?.let {
                showBiometricPrompt(it) {
                    viewModel.onBiometricSuccess()
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = Color(0xFF7C3AED),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "RupeeFlow Locked",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (uiState.lockState == AppLockState.COOLDOWN) {
                Text(
                    text = "Too many attempts. Try again in ${uiState.cooldownRemaining / 1000}s",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = if (uiState.error != null) uiState.error!! else "Enter your PIN to unlock",
                    color = if (uiState.error != null) Color.Red else Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) { index ->
                    val filled = index < uiState.pin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (filled) Color(0xFF7C3AED) else Color(0xFF1F2937))
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Number Pad
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val numbers = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf(null, "0", "BACK")
                )

                numbers.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { item ->
                            when (item) {
                                null -> {
                                    if (uiState.isBiometricEnabled && uiState.isBiometricAvailable) {
                                        IconButton(
                                            onClick = {
                                                activity?.let {
                                                    showBiometricPrompt(it) {
                                                        viewModel.onBiometricSuccess()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(64.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Fingerprint,
                                                contentDescription = "Biometric",
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(64.dp))
                                    }
                                }
                                "BACK" -> {
                                    IconButton(
                                        onClick = { viewModel.onDelete() },
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Backspace,
                                            contentDescription = "Delete",
                                            tint = Color.White
                                        )
                                    }
                                }
                                else -> {
                                    NumberButton(
                                        number = item,
                                        onClick = { viewModel.onPinChar(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { viewModel.onUnlock() },
                enabled = uiState.pin.length >= 4 && uiState.lockState != AppLockState.COOLDOWN,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C3AED),
                    disabledContainerColor = Color(0xFF1F2937)
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Unlock", fontWeight = FontWeight.Bold)
            }
            
            TextButton(
                onClick = { /* Show Forgot PIN Dialog */ },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Forgot PIN?", color = Color.Gray)
            }
        }
    }
}

@Composable
fun NumberButton(
    number: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        color = Color(0xFF111827)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = number,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                color = Color.White
            )
        }
    }
}

private fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock RupeeFlow")
        .setSubtitle("Use your biometric credential")
        .setNegativeButtonText("Use PIN")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .build()

    biometricPrompt.authenticate(promptInfo)
}
