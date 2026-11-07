package com.FreeRave.shredzilla.pro

/**
 * Google Play in-app products. The ids must match the products created in Play Console;
 * prices shown in the app come from Play (localized), the fallbacks only fill the gap
 * before they load.
 */
object ProCatalog {
    /** One-time, non-consumable: Pro for life. */
    const val PRO_LIFETIME = "pro_lifetime"
    const val PRO_FALLBACK_PRICE = "$4.99"

    /** Rewarded ads: one ad buys one hour of Pro, banked up to [MAX_BANKED_HOURS], at most [MAX_ADS_PER_DAY] a day. */
    const val HOURS_PER_AD = 1
    const val MAX_BANKED_HOURS = 8
    const val MAX_ADS_PER_DAY = 5

    val allProductIds: List<String> get() = listOf(PRO_LIFETIME) + SupportTip.entries.map { it.productId }
}

/** Consumable "buy me a…" tips. They say thank you and add a supporter badge; they don't unlock Pro. */
enum class SupportTip(val productId: String, val title: String, val blurb: String, val fallbackPrice: String) {
    PRE_WORKOUT("tip_pre_workout_shot", "Pre-workout shot", "A quick boost for the next update", "$0.99"),
    PROTEIN("tip_protein_scoop", "Protein scoop", "Keeps the features growing", "$2.99"),
    CREATINE("tip_creatine_tub", "Creatine tub", "A month of fuel for DotShredzilla", "$4.99");

    companion object {
        fun forProduct(productId: String): SupportTip? = entries.find { it.productId == productId }
    }
}
