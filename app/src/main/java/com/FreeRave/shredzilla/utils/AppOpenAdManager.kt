package com.FreeRave.shredzilla.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.FreeRave.shredzilla.utils.AdMobIds // Added import
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

class AppOpenAdManager(private val context: Context) {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    var isShowingAd = false
        private set

    // Determine the screen orientation from activity.
    private var currentActivity: Activity? = null

    // Keep track of the time an ad was loaded to ensure it's not shown if too old.
    private var loadTime: Long = 0

    companion object {
        private const val LOG_TAG = "AppOpenAdManager"
        // AdMob's recommendation: Ad references expire after four hours.
        private const val AD_EXPIRY_DURATION_MS: Long = 4 * 60 * 60 * 1000 
    }

    fun loadAd(activity: Activity) {
        currentActivity = activity
        if (isLoadingAd || isAdAvailable()) {
            Log.d(LOG_TAG, "Ad is already loading or already available.")
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            AdMobIds.CURRENT_APP_OPEN_AD_UNIT_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    Log.d(LOG_TAG, "App Open Ad loaded.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    appOpenAd = null
                    Log.e(LOG_TAG, "App Open Ad failed to load: ${loadAdError.message} (Code: ${loadAdError.code})")
                    // Optionally, you could try to load another ad here or schedule a retry.
                }
            }
        )
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanFourHoursAgo()
    }

    private fun wasLoadTimeLessThanFourHoursAgo(): Boolean {
        val dateDifference: Long = Date().time - loadTime
        return dateDifference < AD_EXPIRY_DURATION_MS
    }

    fun showAdIfAvailable(activity: Activity, onShowFullScreenContent: () -> Unit = {}) {
        currentActivity = activity // Update current activity

        if (isShowingAd) {
            Log.d(LOG_TAG, "An ad is already showing.")
            return
        }

        if (!isAdAvailable()) {
            Log.d(LOG_TAG, "No ad available or ad is too old. Attempting to load a new one.")
            loadAd(activity) // Load a new ad if current one is not available or expired
            return
        }
        
        // Ensure currentActivity is not null before showing
        currentActivity?.let {
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Set the reference to null so isAdAvailable() returns false.
                    appOpenAd = null
                    isShowingAd = false
                    Log.d(LOG_TAG, "App Open Ad dismissed.")
                    // Load the next ad.
                    loadAd(it) 
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowingAd = false
                    Log.e(LOG_TAG, "App Open Ad failed to show: ${adError.message}")
                     // Load the next ad.
                    loadAd(it)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(LOG_TAG, "App Open Ad showed full screen content.")
                    isShowingAd = true
                    onShowFullScreenContent()
                }
            }
            appOpenAd?.show(it)
        } ?: run {
            Log.e(LOG_TAG, "Cannot show ad: currentActivity is null. Trying to load ad again.")
            loadAd(activity) // Try to load if activity was null
        }
    }
}
