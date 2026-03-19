package com.FreeRave.shredzilla.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.TimeUnit

object RewardedAdManager {
    private const val TAG = "RewardedAdManager"

    enum class AdType {
        TIMED_REWARD,
        SUPPORT_ONLY
    }

    // Ad Unit IDs are now managed in AdMobIds.kt
    private var rewardedAd: RewardedAd? = null
    private var isLoadingAd = false

    private const val PREFS_NAME = "AdFreePrefs"
    private const val KEY_ADS_WATCHED_COUNT = "ads_watched_count" // For timed rewards
    private const val KEY_AD_FREE_EXPIRY_TIME_MS = "ad_free_expiry_time_ms"
    private const val KEY_SUPPORT_ADS_WATCHED_TOTAL = "support_ads_watched_total" // For support ads

    // Define the reward tiers: Pair(adsRequired, durationInMilliseconds)
    internal val REWARD_TIERS = listOf(
        Pair(40, TimeUnit.HOURS.toMillis(2)),    // 40 ads for 2 hours
        Pair(30, TimeUnit.MINUTES.toMillis(90)), // 30 ads for 1.5 hours
        Pair(20, TimeUnit.HOURS.toMillis(1)),    // 20 ads for 1 hour
        Pair(10, TimeUnit.MINUTES.toMillis(30))  // 10 ads for 30 minutes
    ).sortedByDescending { it.first } // Ensure tiers are checked from highest to lowest adsRequired

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun loadRewardedAd(activity: Activity) {
        if (rewardedAd != null || isLoadingAd) {
            Log.d(TAG, "Ad already loaded or currently loading.")
            return
        }
        isLoadingAd = true
        val adRequest = AdRequest.Builder().build()
        // Using AdMobIds.CURRENT_REWARDED_AD_UNIT_ID
        RewardedAd.load(activity, AdMobIds.CURRENT_REWARDED_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "RewardedAd failed to load (using ID: ${AdMobIds.CURRENT_REWARDED_AD_UNIT_ID}): ${adError.message}")
                rewardedAd = null
                isLoadingAd = false
                // Optionally, show a toast or log the error
                 Handler(Looper.getMainLooper()).post {
                    Toast.makeText(activity, "Failed to load ad. Try again later.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "RewardedAd loaded successfully (using ID: ${AdMobIds.CURRENT_REWARDED_AD_UNIT_ID}).")
                rewardedAd = ad
                isLoadingAd = false
            }
        })
    }

    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: (RewardItem) -> Unit,
        onAdFailedToShow: (AdError) -> Unit,
        onAdDismissed: () -> Unit,
        adType: AdType = AdType.TIMED_REWARD // Default to timed reward for backward compatibility if any old calls exist
    ) {
        if (rewardedAd == null) {
            Log.w(TAG, "RewardedAd not loaded yet. Attempting to load.")
            loadRewardedAd(activity) // Try to load it if not available
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(activity, "Ad not ready. Please try again shortly.", Toast.LENGTH_SHORT).show()
            }
            onAdFailedToShow(AdError(0, "Ad not loaded", "com.google.android.gms.ads")) // Simulate an error
            return
        }

        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "RewardedAd showed full screen content.")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "RewardedAd failed to show full screen content: ${adError.message}")
                rewardedAd = null
                isLoadingAd = false
                onAdFailedToShow(adError)
                loadRewardedAd(activity) 
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "RewardedAd dismissed full screen content.")
                rewardedAd = null 
                isLoadingAd = false
                onAdDismissed()
                loadRewardedAd(activity) 
            }

             override fun onAdImpression() {
                Log.d(TAG, "RewardedAd impression recorded.")
            }
        }

        rewardedAd?.show(activity, OnUserEarnedRewardListener { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type} for adType: $adType")
            
            if (adType == AdType.TIMED_REWARD) {
                val currentCountBeforeIncrement = getPreferences(activity.applicationContext).getInt(KEY_ADS_WATCHED_COUNT, 0)
                handleTimedReward(activity.applicationContext, currentCountBeforeIncrement + 1)
            } else if (adType == AdType.SUPPORT_ONLY) {
                handleSupportOnlyReward(activity.applicationContext)
            }
            onUserEarnedReward(rewardItem)
        })
    }

    private fun handleSupportOnlyReward(context: Context) {
        val prefs = getPreferences(context)
        val currentSupportAds = prefs.getInt(KEY_SUPPORT_ADS_WATCHED_TOTAL, 0)
        prefs.edit().putInt(KEY_SUPPORT_ADS_WATCHED_TOTAL, currentSupportAds + 1).apply()
        Log.d(TAG, "Support ad watched. Total support ads: ${currentSupportAds + 1}")
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Thank you for supporting Shredzilla!", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleTimedReward(context: Context, newWatchCount: Int) {
        val prefs = getPreferences(context)
        var rewardGranted = false

        for (tier in REWARD_TIERS) { // REWARD_TIERS is sorted high to low
            if (newWatchCount >= tier.first) {
                val newExpiryTime = System.currentTimeMillis() + tier.second
                prefs.edit()
                    .putInt(KEY_ADS_WATCHED_COUNT, 0) // Reset count for timed rewards
                    .putLong(KEY_AD_FREE_EXPIRY_TIME_MS, newExpiryTime)
                    .apply()
                
                val durationHours = TimeUnit.MILLISECONDS.toHours(tier.second)
                val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(tier.second) % 60
                var durationStr = ""
                if (durationHours > 0) durationStr += "$durationHours hour(s) "
                if (durationMinutes > 0) durationStr += "$durationMinutes minute(s)"
                durationStr = durationStr.trim()

                Log.d(TAG, "Timed ad-free period granted: $durationStr. Expires at: $newExpiryTime. Ads watched was: $newWatchCount, reset to 0.")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "You've earned $durationStr ad-free!", Toast.LENGTH_LONG).show()
                }
                rewardGranted = true
                break 
            }
        }

        if (!rewardGranted) {
            prefs.edit().putInt(KEY_ADS_WATCHED_COUNT, newWatchCount).apply()
            Log.d(TAG, "Timed ad watched. Current count for timed reward: $newWatchCount")
            
            val nextTier = REWARD_TIERS.lastOrNull { newWatchCount < it.first } ?: REWARD_TIERS.last()
            val adsNeededForNext = nextTier.first - newWatchCount
            val nextDurationHours = TimeUnit.MILLISECONDS.toHours(nextTier.second)
            val nextDurationMinutes = TimeUnit.MILLISECONDS.toMinutes(nextTier.second) % 60
            var nextDurationStr = ""
            if (nextDurationHours > 0) nextDurationStr += "$nextDurationHours hour(s) "
            if (nextDurationMinutes > 0) nextDurationStr += "$nextDurationMinutes minute(s)"
            nextDurationStr = nextDurationStr.trim()

            Handler(Looper.getMainLooper()).post {
                if (adsNeededForNext > 0) {
                    Toast.makeText(context, "Ad watched! $adsNeededForNext more for $nextDurationStr ad-free.", Toast.LENGTH_SHORT).show()
                } else { 
                    Toast.makeText(context, "Ad watched! Current count: $newWatchCount", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Renamed from handleReward to handleTimedReward, original handleReward is now split.
    // private fun handleReward(context: Context, newWatchCount: Int) { ... }


    fun getAdsWatchedCount(context: Context): Int { // This refers to ads for TIMED rewards
        return getPreferences(context).getInt(KEY_ADS_WATCHED_COUNT, 0)
    }

    fun getAdFreeExpiryTimeMs(context: Context): Long {
        return getPreferences(context).getLong(KEY_AD_FREE_EXPIRY_TIME_MS, 0L)
    }

    fun isAdFree(context: Context): Boolean {
        val expiryTime = getAdFreeExpiryTimeMs(context)
        return System.currentTimeMillis() < expiryTime
    }

    // Returns the next tier goal (ads required, duration ms) for UI display
    // or null if all tiers are technically surpassed (though count resets)
    fun getNextRewardTierInfo(context: Context): Pair<Int, Long>? {
        val currentWatchCount = getAdsWatchedCount(context)
        // Find the lowest tier that the user hasn't reached yet
        return REWARD_TIERS.sortedBy { it.first }.find { currentWatchCount < it.first }
    }
    
    // Gets the lowest tier's ad requirement for initial display if no progress
    fun getLowestTierAdsRequired(): Int {
        return REWARD_TIERS.minByOrNull { it.first }?.first ?: 10 // Default to 10 if list is empty
    }


    fun getRemainingAdFreeTimeFormatted(context: Context): String {
        if (!isAdFree(context)) return "Not active"
        val remainingMs = getAdFreeExpiryTimeMs(context) - System.currentTimeMillis()
        if (remainingMs <= 0) {
            // Clear expiry if time is up, to ensure isAdFree reflects correctly next time
            // And also reset ad count if it wasn't reset by earning a reward
            // This part might be optional if ad count is always reset on reward
            // getPreferences(context).edit().putLong(KEY_AD_FREE_EXPIRY_TIME_MS, 0L).apply()
            return "Expired"
        }

        val hours = TimeUnit.MILLISECONDS.toHours(remainingMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMs) % 60
        return String.format("%02d:%02d:%02d remaining", hours, minutes, seconds)
    }
}
