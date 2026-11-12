package com.FreeRave.shredzilla

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import java.security.MessageDigest

/** No forced ads anywhere: AdMob only serves the optional rewarded ads that earn Pro time. */
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        resetUnusableAuthKey()
        // Debug builds use the real ad unit, so the server-side reward callback can be tested end to
        // end, but always mark this device as a test device: AdMob then serves labelled test ads and
        // a developer's own views never count as real impressions.
        if (BuildConfig.AD_TEST_DEVICE) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder().setTestDeviceIds(listOf(adMobTestDeviceId())).build()
            )
        }
        MobileAds.initialize(this) {}
    }

    /**
     * Firebase Auth encrypts the saved sign-in with a key in the Android Keystore. A backup restore
     * or device transfer can bring back its keyset file without that key, and then no sign-in can be
     * saved: the app forgets the user every time it closes. When no sign-in is saved there is
     * nothing the old keyset protects, so it's removed before Firebase Auth starts and Firebase
     * creates a working one with the next sign-in.
     */
    private fun resetUnusableAuthKey() {
        val suffix = "W0RFRkFVTFRd+MTo1MDE0ODU1MzU4MzY6YW5kcm9pZDoxZjVhNDk4MjIwNGI0YjY1Mjc5Yzk5"
        val store = getSharedPreferences("com.google.firebase.auth.api.Store.$suffix", MODE_PRIVATE)
        if (store.contains("com.google.firebase.auth.FIREBASE_USER")) return
        val crypto = "com.google.firebase.auth.api.crypto.$suffix"
        if (getSharedPreferences(crypto, MODE_PRIVATE).all.isNotEmpty()) {
            deleteSharedPreferences(crypto)
            android.util.Log.i("MyApplication", "No saved sign-in; reset Firebase Auth's keyset")
        }
    }

    /** AdMob's test-device id: the MD5 of this app's ANDROID_ID, upper-case hex. */
    @SuppressLint("HardwareIds")
    private fun adMobTestDeviceId(): String {
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        return MessageDigest.getInstance("MD5").digest(androidId.toByteArray())
            .joinToString("") { "%02X".format(it) }
    }
}
