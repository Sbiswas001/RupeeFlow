package sayan.apps.rupeeflow.core.util

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialManagerHelper @Inject constructor(
    private val credentialManager: CredentialManager
) {

    suspend fun signInWithGoogle(activityContext: Context, filterByAuthorized: Boolean = false): GoogleIdTokenCredential? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorized)
            .setAutoSelectEnabled(filterByAuthorized)
            .setServerClientId("139982260832-7ilbj2oh5j5i0691lnt2ag8j8blh8tim.apps.googleusercontent.com")
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            
            Log.d("AuthHelpers", "Received credential type: ${credential.type}")
            
            when {
                credential is GoogleIdTokenCredential -> {
                    Log.d("AuthHelpers", "Credential is already GoogleIdTokenCredential")
                    credential
                }
                credential.type == "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL" -> {
                    try {
                        Log.d("AuthHelpers", "Parsing GoogleIdTokenCredential from data")
                        GoogleIdTokenCredential.createFrom(credential.data)
                    } catch (e: Exception) {
                        Log.e("AuthHelpers", "Failed to parse Google ID Token credential", e)
                        null
                    }
                }
                else -> {
                    Log.e("AuthHelpers", "Unexpected credential type: ${credential.type}")
                    null
                }
            }
        } catch (e: GetCredentialException) {
            Log.e("AuthHelpers", "Sign in failed", e)
            null
        }
    }

    suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("AuthHelpers", "Sign out failed", e)
        }
    }
}

@Singleton
class AuthorizationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val authorizationClient = Identity.getAuthorizationClient(context)

    fun getAuthorizationRequest(scopes: List<String>): AuthorizationRequest {
        val requestedScopes = scopes.map { Scope(it) }
        return AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            .build()
    }

    fun isAuthorized(scopes: List<String>): Boolean {
        // This is a simplified check. In practice, you might want to call authorize and check if it has resolution.
        // For Google Drive, we usually need to call authorize().
        return false 
    }

    suspend fun getAuthorizationResult(intentData: android.content.Intent?): AuthorizationResult {
        return authorizationClient.getAuthorizationResultFromIntent(intentData)
    }
}
