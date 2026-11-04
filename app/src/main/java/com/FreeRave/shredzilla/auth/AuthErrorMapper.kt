package com.FreeRave.shredzilla.auth

import com.google.firebase.auth.FirebaseAuthException

/**
 * Converts Firebase Auth exceptions into short, user-readable messages.
 *
 * Firebase throws [FirebaseAuthException] with machine-readable error codes
 * (e.g. "ERROR_EMAIL_ALREADY_IN_USE"). Raw messages from the SDK are either
 * too technical or expose internal details. This mapper converts them to
 * friendly one-liners suitable for Toasts / inline error labels.
 *
 * Usage:
 *   val msg = AuthErrorMapper.getMessage(exception)
 *   Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
 */
object AuthErrorMapper {

    fun getMessage(throwable: Throwable?): String {
        if (throwable == null) return "An unexpected error occurred."

        // FirebaseAuthException carries a typed error code — always prefer it
        if (throwable is FirebaseAuthException) {
            return when (throwable.errorCode) {
                // ── Login errors ──────────────────────────────────────────────
                "ERROR_INVALID_EMAIL"              -> "That doesn't look like a valid email address."
                "ERROR_USER_NOT_FOUND"             -> "No account found with this email."
                "ERROR_WRONG_PASSWORD"             -> "Incorrect password. Please try again."
                "ERROR_INVALID_CREDENTIAL"         -> "Incorrect email or password."   // New unified code (SDK ≥ 22.3)
                "ERROR_USER_DISABLED"              -> "This account has been disabled. Contact support."
                "ERROR_TOO_MANY_REQUESTS"          -> "Too many failed attempts. Please wait a moment and try again."
                "ERROR_OPERATION_NOT_ALLOWED"      -> "Email/password sign-in is not enabled. Contact support."
                "ERROR_USER_TOKEN_EXPIRED"         -> "Your session has expired. Please log in again."
                "ERROR_NETWORK_REQUEST_FAILED"     -> "Network error. Check your connection and try again."

                // ── Registration errors ───────────────────────────────────────
                "ERROR_EMAIL_ALREADY_IN_USE"       -> "An account with this email already exists. Try logging in."
                "ERROR_WEAK_PASSWORD"              -> "Password is too weak. Use at least 6 characters."
                "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                    "An account already exists with a different sign-in method."

                // ── Password reset errors ─────────────────────────────────────
                "ERROR_MISSING_EMAIL"              -> "Please enter your email address."

                // ── Fallback: log the raw code for debugging, show generic msg ─
                else -> "Something went wrong (${throwable.errorCode}). Please try again."
            }
        }

        // Non-Firebase exceptions (network, Firestore, etc.) — use a safe fallback
        val msg = throwable.message ?: ""
        return when {
            msg.startsWith(ACCOUNT_SETUP_FAILED) ->
                "We couldn't finish creating your account. Check your connection and try again."
            msg.contains("network", ignoreCase = true) ||
            msg.contains("offline", ignoreCase = true) ||
            msg.contains("timeout", ignoreCase = true) ||
            msg.contains("unable to resolve host", ignoreCase = true) ->
                "Network error. Check your connection and try again."
            msg.isNotBlank() -> msg   // pass through clean messages we wrote ourselves
            else -> "An unexpected error occurred. Please try again."
        }
    }
}
