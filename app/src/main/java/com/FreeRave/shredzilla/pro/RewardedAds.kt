package com.FreeRave.shredzilla.pro

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.FreeRave.shredzilla.utils.AdMobIds
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions

/**
 * The only ads in the app: optional rewarded ads the user starts from the Pro page.
 * One ad is preloaded while the page is open, so the button plays it instantly.
 */
object RewardedAds {
    private const val TAG = "RewardedAds"

    enum class LoadState { IDLE, LOADING, READY, FAILED }

    var loadState by mutableStateOf(LoadState.IDLE)
        private set

    private var ad: RewardedAd? = null

    fun preload(context: Context) {
        if (ad != null || loadState == LoadState.LOADING) return
        loadState = LoadState.LOADING
        RewardedAd.load(context.applicationContext, AdMobIds.REWARDED_AD_UNIT_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(loaded: RewardedAd) {
                ad = loaded
                loadState = LoadState.READY
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                ad = null
                loadState = LoadState.FAILED
            }
        })
    }

    /**
     * Plays the preloaded ad for [userId] (the Firebase uid). AdMob then calls our server, which
     * checks Google's signature and banks the hour on that account. [onRewarded] runs when the ad
     * was watched to the end; [onClosed] always runs when it's gone.
     */
    fun show(activity: Activity, userId: String, onRewarded: () -> Unit, onClosed: () -> Unit) {
        val current = ad ?: run {
            preload(activity)
            onClosed()
            return
        }
        ad = null
        loadState = LoadState.IDLE
        current.setServerSideVerificationOptions(ServerSideVerificationOptions.Builder().setUserId(userId).build())
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                onClosed()
                preload(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded ad failed to show: ${error.message}")
                onClosed()
                preload(activity)
            }
        }
        current.show(activity) { onRewarded() }
    }
}
