package com.FreeRave.shredzilla.auth

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import com.google.firebase.firestore.Source

/**
 * Shared helper for fetching user-related Firestore data.
 * Decouples Firestore data fetching from specific Auth providers (Email/Google).
 */
object UserFirestoreHelper {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * The user's profile: the device cache first (instant, works offline), then the server with a
     * time limit so a weak gym connection can't hang startup. Null when the account has no profile
     * yet; failure only when neither cache nor server answered.
     */
    suspend fun getUserData(userId: String): Result<Map<String, Any>?> {
        val doc = db.collection("users").document(userId)
        runCatching { doc.get(Source.CACHE).await() }.getOrNull()
            ?.takeIf { it.exists() }
            ?.let { return Result.success(it.data) }
        return try {
            val snapshot = withTimeout(SERVER_TIMEOUT_MS) { doc.get(Source.SERVER).await() }
            Result.success(if (snapshot.exists()) snapshot.data else null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private const val SERVER_TIMEOUT_MS = 8_000L
}
