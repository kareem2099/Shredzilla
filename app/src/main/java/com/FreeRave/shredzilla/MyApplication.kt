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

    /** AdMob's test-device id: the MD5 of this app's ANDROID_ID, upper-case hex. */
    @SuppressLint("HardwareIds")
    private fun adMobTestDeviceId(): String {
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        return MessageDigest.getInstance("MD5").digest(androidId.toByteArray())
            .joinToString("") { "%02X".format(it) }
    }
}
