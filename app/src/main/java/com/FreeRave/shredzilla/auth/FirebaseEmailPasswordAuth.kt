package com.FreeRave.shredzilla.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseEmailPasswordAuth {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    internal val db: FirebaseFirestore = FirebaseFirestore.getInstance() // Made internal

    suspend fun createUser(name: String, email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                try {
                    // Save user details to Firestore
                    val user = hashMapOf(
                        "uid" to firebaseUser.uid,
                        "name" to name,
                        "email" to email
                    )
                    db.collection("users").document(firebaseUser.uid).set(user).await()
                    Result.success(firebaseUser)
                } catch (e: Exception) {
                    // If Firestore fails, rollback the Firebase Auth creation
                    try {
                        firebaseUser.delete().await()
                    } catch (deleteException: Exception) {
                        Log.e("Auth", "Rollback failed: ${deleteException.message}")
                    }
                    Result.failure(Exception("Failed to save user data. Please check your connection and try again.", e))
                }
            } else {
                Result.failure(Exception("Firebase user was null after creation."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInUser(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserOnboardingData(userId: String, data: Map<String, Any>): Result<Unit> {
        return try {
            db.collection("users").document(userId)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserData(userId: String): Result<Map<String, Any>?> {
        return try {
            val documentSnapshot = db.collection("users").document(userId).get().await()
            if (documentSnapshot.exists()) {
                Result.success(documentSnapshot.data)
            } else {
                Result.success(null) // User document doesn't exist
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
