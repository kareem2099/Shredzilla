package com.FreeRave.shredzilla

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.FreeRave.shredzilla.utils.AppOpenAdManager
import com.google.android.gms.ads.MobileAds
import com.FreeRave.shredzilla.SplashActivity // Import SplashActivity

class MyApplication : Application(), Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private lateinit var appOpenAdManager: AppOpenAdManager
    private var currentActivity: Activity? = null

    companion object {
        private const val LOG_TAG = "MyApplication"
    }

    override fun onCreate() {
        super<Application>.onCreate()
        registerActivityLifecycleCallbacks(this)
        MobileAds.initialize(this) {}
        // Ensure "androidx.lifecycle:lifecycle-process:$LIFECYCLE_VERSION" is in app/build.gradle.kts
        ProcessLifecycleOwner.get().lifecycle.addObserver(this) 
        appOpenAdManager = AppOpenAdManager(this)
        Log.d(LOG_TAG, "MyApplication onCreate: AppOpenAdManager initialized.")
    }

    /**
     * DefaultLifecycleObserver method that is invoked when the App comes to foreground.
     */
    override fun onStart(owner: LifecycleOwner) {
        // The super.onStart(owner) call is intentionally removed.
        // It's an interface method from DefaultLifecycleObserver, and Application class
        // does not have a matching onStart to call.
        currentActivity?.let { activity ->
            val activityClassName = activity.javaClass.name
            if (activity is SplashActivity) {
                Log.d(LOG_TAG, "App came to foreground, current activity is SplashActivity. Ad will not be shown now. Pre-loading ad for next screen.")
                appOpenAdManager.loadAd(activity) 
            } else if (activityClassName != "com.google.android.gms.ads.AdActivity" && activityClassName != "com.google.android.gms.auth.api.signin.internal.SignInHubActivity") {
                Log.d(LOG_TAG, "App came to foreground with ${activity.localClassName} (not Splash/Ad/SignIn). Attempting to show App Open Ad via onStart (process lifecycle).")
                appOpenAdManager.showAdIfAvailable(activity)
            } else {
                Log.d(LOG_TAG, "App came to foreground, but current activity is AdActivity or SignInHubActivity (${activity.localClassName}). No ad attempt via onStart.")
            }
        } ?: Log.d(LOG_TAG, "App came to foreground (process lifecycle), but currentActivity is null.")
    }

    /**
     * ActivityLifecycleCallback methods.
     */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (!appOpenAdManager.isShowingAd) {
            currentActivity = activity
            Log.d(LOG_TAG, "Activity started: ${activity.localClassName}. CurrentActivity set.")
        } else {
            Log.d(LOG_TAG, "Activity started: ${activity.localClassName}, but an ad is already showing.")
        }
    }

    override fun onActivityResumed(activity: Activity) {
        // currentActivity is updated in onActivityStarted.
        // This callback is crucial for showing the ad after SplashActivity finishes,
        // or when any non-Splash, non-Ad, non-SignIn activity resumes.
        val activityClassName = activity.javaClass.name
        if (activity !is SplashActivity && activityClassName != "com.google.android.gms.ads.AdActivity" && activityClassName != "com.google.android.gms.auth.api.signin.internal.SignInHubActivity") {
            Log.d(LOG_TAG, "Activity resumed: ${activity.localClassName} (not Splash/Ad/SignIn). Attempting to show App Open Ad via onActivityResumed.")
            appOpenAdManager.showAdIfAvailable(activity)
        } else if (activity is SplashActivity) {
            Log.d(LOG_TAG, "Activity resumed: SplashActivity. No ad shown by onActivityResumed.")
        } else { // It's an AdActivity or SignInHubActivity
            Log.d(LOG_TAG, "Activity resumed: AdActivity or SignInHubActivity (${activity.localClassName}). No ad shown by onActivityResumed.")
        }
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null 
            Log.d(LOG_TAG, "Activity destroyed: ${activity.localClassName}. CurrentActivity cleared.")
        }
    }
}
