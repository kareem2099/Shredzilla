package com.FreeRave.shredzilla.auth

import android.app.Activity
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.FreeRave.shredzilla.R
import kotlinx.coroutines.tasks.await

class FirebaseGoogleAuth(private val activity: Activity) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val credentialManager: CredentialManager = CredentialManager.create(activity)

    private fun buildRequest(filterByAuthorizedAccounts: Boolean): GetCredentialRequest {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(activity.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(filterByAuthorizedAccounts) // auto-select only for returning users
            .build()

        val passwordOption = GetPasswordOption(
            isAutoSelectAllowed = filterByAuthorizedAccounts
        )

        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .addCredentialOption(passwordOption)
            .build()
    }

    /**
     * Signs in with Google, saved passwords, or passkeys using the Credential Manager bottom sheet.
     *
     * Strategy:
     *   1. Try with filterByAuthorizedAccounts=true  (returning users — fast, auto-selects if one account)
     *   2. If no saved credential found, retry with filterByAuthorizedAccounts=false (new users — full picker)
     *
     * Returns Result.failure with a user-readable message on any error.
     */
    suspend fun signInWithGoogle(): Result<FirebaseUser> {
        return try {
            val credentialResponse = try {
                // First try: returning user (fast path)
                credentialManager.getCredential(
                    context  = activity,
                    request  = buildRequest(filterByAuthorizedAccounts = true)
                )
            } catch (e: NoCredentialException) {
                // No saved credential — fall through to full account picker
                Log.d("GoogleAuth", "No authorized account found, showing full picker")
                credentialManager.getCredential(
                    context  = activity,
                    request  = buildRequest(filterByAuthorizedAccounts = false)
                )
            }

            val credential = credentialResponse.credential
            val firebaseUser = when {
                credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCredential).await().user
                        ?: throw Exception("Firebase user was null after Google sign-in.")
                }
                credential is PasswordCredential -> {
                    val email = credential.id
                    val password = credential.password
                    auth.signInWithEmailAndPassword(email, password).await().user
                        ?: throw Exception("Firebase user was null after password sign-in.")
                }
                else -> {
                    return Result.failure(Exception("Unexpected credential type received."))
                }
            }

            // Save / merge user data in Firestore
            try {
                val userMap = hashMapOf<String, Any?>().apply {
                    put("uid", firebaseUser.uid)
                    put("email", firebaseUser.email)
                    if (!firebaseUser.displayName.isNullOrEmpty()) {
                        put("name", firebaseUser.displayName)
                    }
                }
                db.collection("users").document(firebaseUser.uid)
                    .set(userMap, SetOptions.merge())
                    .await()
                Result.success(firebaseUser)
            }
 catch (e: Exception) {
                // Firestore failed — rollback auth state to avoid zombie session
                signOut()
                Result.failure(Exception("Database sync failed. Please try again.", e))
            }

        } catch (e: GetCredentialCancellationException) {
            // User dismissed the bottom sheet — not an error
            Result.failure(Exception("Sign-in cancelled."))
        } catch (e: Exception) {
            if (e is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                Result.failure(
                    Exception("An account already exists with this email. Please log in with your email and password.")
                )
            } else {
                Log.e("GoogleAuth", "signInWithGoogle failed", e)
                Result.failure(Exception("Google Sign-In failed. Please try again."))
            }
        }
    }

    suspend fun signOut() {
        auth.signOut()
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w("GoogleAuth", "clearCredentialState failed (non-fatal)", e)
        }
    }
}
