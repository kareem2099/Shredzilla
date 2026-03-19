package com.FreeRave.shredzilla.utils

// import com.FreeRave.shredzilla.BuildConfig // Reverted: BuildConfig causing issues

object AdMobIds {
    // Your actual AdMob Rewarded Ad Unit ID
    const val REWARDED_AD_UNIT_ID_ACTUAL = "ca-app-pub-3186351063839089/2141699138"

    // AdMob's official test Ad Unit ID for Rewarded Ads
    const val REWARDED_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/5224354917"

    // Variable to easily switch between test and actual IDs.
    // Set to TEST for development/debugging, change to ACTUAL for release.
    // Reverted to manual switching due to BuildConfig issues.
    val CURRENT_REWARDED_AD_UNIT_ID: String = REWARDED_AD_UNIT_ID_ACTUAL // Keeping this manual for now based on previous issues

    // App Open Ad Unit IDs
    const val APP_OPEN_AD_UNIT_ID_ACTUAL = "ca-app-pub-3186351063839089/3101666828"
    const val APP_OPEN_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/9257395921" // AdMob's official test ID

    // TODO: Fix BuildConfig visibility in Gradle setup to enable automatic switching.
    // For now, using actual ID. Manually switch to _TEST for debugging if BuildConfig issue persists.
    val CURRENT_APP_OPEN_AD_UNIT_ID: String = APP_OPEN_AD_UNIT_ID_ACTUAL


    // Note: Your AdMob App ID (ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy)
    // should remain in your AndroidManifest.xml file.
}
