package com.FreeRave.shredzilla.utils

object AdMobIds {
    /**
     * The real rewarded unit, in debug too: its server-side verification callback is what grants Pro
     * hours, and Google's shared test unit never calls our server. Debug builds register the device
     * as a test device (see MyApplication), so they still only get test ads.
     */
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-3186351063839089/2141699138"
}
