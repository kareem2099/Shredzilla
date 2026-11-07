package com.FreeRave.shredzilla.pro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** Where the user's Pro comes from; lifetime wins over ad time. */
enum class ProSource { NONE, AD_TIME, LIFETIME }

data class ProAccess(
    val source: ProSource = ProSource.NONE,
    val isSupporter: Boolean = false,
    /** Opens the Pro page; locked features call this. */
    val openPro: () -> Unit = {},
) {
    val isPro: Boolean get() = source != ProSource.NONE
}

val LocalProAccess = compositionLocalOf { ProAccess() }

/**
 * Pro for the app root. Ad time is re-checked exactly when it runs out, so features lock again
 * on time without a per-second clock recomposing the whole app.
 */
@Composable
fun rememberProAccess(ownsLifetime: Boolean, bank: AdTimeBank, supporterTips: Int, openPro: () -> Unit): ProAccess {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(bank.untilMillis) {
        now = System.currentTimeMillis()
        val wait = bank.untilMillis - now
        if (wait > 0) {
            delay(wait + 250)
            now = System.currentTimeMillis()
        }
    }
    val source = when {
        ownsLifetime -> ProSource.LIFETIME
        bank.isActive(now) -> ProSource.AD_TIME
        else -> ProSource.NONE
    }
    val isSupporter = supporterTips > 0
    return remember(source, isSupporter, openPro) { ProAccess(source, isSupporter, openPro) }
}

/** A clock for countdowns on one screen, ticking on the second. */
@Composable
fun rememberTickingNow(periodMillis: Long = 1_000L): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(periodMillis) {
        while (true) {
            delay(periodMillis - System.currentTimeMillis() % periodMillis)
            now = System.currentTimeMillis()
        }
    }
    return now
}

/** "1h 05m 09s" / "42m 10s" for Pro time left. */
fun formatRemaining(millis: Long): String {
    val totalSeconds = millis / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%dh %02dm %02ds".format(h, m, s) else "%dm %02ds".format(m, s)
}
