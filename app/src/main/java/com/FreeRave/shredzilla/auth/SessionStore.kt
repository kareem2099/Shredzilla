package com.FreeRave.shredzilla.auth

import android.content.Context
import androidx.core.content.edit

/**
 * What startup needs to know without the network: whether this account finished onboarding and
 * its profile (gender theme). Firebase Auth keeps the sign-in itself on the device; this keeps the
 * app from asking Firestore before it can open, so a signed-in user goes straight in, offline too.
 */
class SessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun isOnboarded(uid: String): Boolean = prefs.getBoolean("onboarded_$uid", false)

    fun gender(uid: String): String? = prefs.getString("gender_$uid", null)

    fun remember(uid: String, onboarded: Boolean, gender: String?) = prefs.edit {
        putBoolean("onboarded_$uid", onboarded)
        if (gender != null) putString("gender_$uid", gender) else remove("gender_$uid")
    }

    fun clear() = prefs.edit { clear() }
}
