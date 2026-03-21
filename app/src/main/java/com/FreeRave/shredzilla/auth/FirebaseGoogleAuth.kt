package com.FreeRave.shredzilla.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.FreeRave.shredzilla.R
import kotlinx.coroutines.tasks.await

class FirebaseGoogleAuth(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()

    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    fun signInWithGoogle(launcher: ActivityResultLauncher<Intent>) {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    suspend fun handleGoogleSignInResult(intent: Intent?): Result<FirebaseUser> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            val account = task.getResult(ApiException::class.java)!!
            val idToken = account.idToken!!
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user!!

            try {
                // Save/update user details in Firestore
                val user = hashMapOf(
                    "uid" to firebaseUser.uid,
                    "name" to firebaseUser.displayName,
                    "email" to firebaseUser.email
                )
                // Use SetOptions.merge() to avoid overwriting existing onboarding data
                db.collection("users").document(firebaseUser.uid).set(user, com.google.firebase.firestore.SetOptions.merge()).await()

                Result.success(firebaseUser)
            } catch (e: Exception) {
                signOut() // Prevents zombie states if Firestore set() fails and logs out completely
                Result.failure(Exception("Database sync failed. Please try again.", e))
            }
        } catch (e: Exception) {
            if (e is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                Result.failure(Exception("An account already exists with this email using a password. Please login using your email and password.", e))
            } else {
                Result.failure(Exception("Google Sign-In failed or database sync interrupted. Please try again.", e))
            }
        }
    }

    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut() // Also sign out from Google
    }
}
