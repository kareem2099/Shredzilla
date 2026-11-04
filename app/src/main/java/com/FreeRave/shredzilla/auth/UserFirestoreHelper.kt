package com.FreeRave.shredzilla.auth

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Shared helper for fetching user-related Firestore data.
 * Decouples Firestore data fetching from specific Auth providers (Email/Google).
 */
object UserFirestoreHelper {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

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
