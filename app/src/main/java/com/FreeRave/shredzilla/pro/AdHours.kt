package com.FreeRave.shredzilla.pro

import java.util.concurrent.TimeUnit

/**
 * Pro time banked from rewarded ads, as the server stored it on the user document. Only the
 * admobReward Cloud Function writes it, after AdMob confirms an ad was watched; the app reads it.
 */
data class AdTimeBank(
    val untilMillis: Long = 0L,
    /** When each recent ad was rewarded (server time), for the rolling 24-hour limit. */
    val adTimes: List<Long> = emptyList(),
) {
    fun remainingMillis(now: Long): Long = (untilMillis - now).coerceAtLeast(0L)
    fun isActive(now: Long): Boolean = untilMillis > now
    fun adsInWindow(now: Long): Int = adTimes.count { now - it < AdHours.WINDOW }
    /** When the oldest ad leaves the 24-hour window, i.e. when the next ad is allowed after the limit. */
    fun nextAdAllowedAt(now: Long): Long? = adTimes.filter { now - it < AdHours.WINDOW }.minOrNull()?.plus(AdHours.WINDOW)
}

/** Why another ad can't be watched right now. */
enum class AdBlock { DAILY_LIMIT, BANK_FULL }

/** The same limits the server enforces, so the app never offers an ad that wouldn't pay out. */
object AdHours {
    val WINDOW = TimeUnit.HOURS.toMillis(24)
    private val HOUR = TimeUnit.HOURS.toMillis(ProCatalog.HOURS_PER_AD.toLong())
    private val MAX_BANK = TimeUnit.HOURS.toMillis(ProCatalog.MAX_BANKED_HOURS.toLong())

    fun blockReason(bank: AdTimeBank, now: Long): AdBlock? = when {
        bank.adsInWindow(now) >= ProCatalog.MAX_ADS_PER_DAY -> AdBlock.DAILY_LIMIT
        bank.remainingMillis(now) + HOUR > MAX_BANK -> AdBlock.BANK_FULL
        else -> null
    }
}
