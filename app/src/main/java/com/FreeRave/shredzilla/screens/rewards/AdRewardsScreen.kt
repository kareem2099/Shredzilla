package com.FreeRave.shredzilla.screens.rewards

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassChip
import com.FreeRave.shredzilla.ui.glass.GlassIconBadge
import com.FreeRave.shredzilla.ui.glass.GlassPrimaryButton
import com.FreeRave.shredzilla.ui.glass.GlassSelectionIndicator
import com.FreeRave.shredzilla.ui.glass.GlassSettingsGroup
import com.FreeRave.shredzilla.ui.glass.GlassSettingsRow
import com.FreeRave.shredzilla.ui.glass.GlassSubpageScaffold
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.RewardedAdManager
import java.util.concurrent.TimeUnit

/** A reward milestone: after [adsRequired] timed ads the user gets [durationMillis] without ads. */
private data class RewardTier(val adsRequired: Int, val durationMillis: Long)

private val rewardTiers = RewardedAdManager.REWARD_TIERS
    .map { (ads, duration) -> RewardTier(ads, duration) }
    .sortedBy { it.adsRequired }

/** 30 min, 1 hr, 1 hr 30 min… */
private fun durationLabel(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return listOfNotNull(hours.takeIf { it > 0 }?.let { "$it hr" }, minutes.takeIf { it > 0 }?.let { "$it min" })
        .joinToString(" ")
}

/** Rewarded ads: progress towards ad-free time, plus a no-reward "support" ad. */
@Composable
fun AdRewardsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val palette = MaterialTheme.glass

    var adsWatched by remember { mutableIntStateOf(RewardedAdManager.getAdsWatchedCount(context)) }
    var isAdFree by remember { mutableStateOf(RewardedAdManager.isAdFree(context)) }
    var adFreeRemaining by remember { mutableStateOf(RewardedAdManager.getRemainingAdFreeTimeFormatted(context)) }
    var isShowingAd by remember { mutableStateOf(false) }

    fun refresh() {
        adsWatched = RewardedAdManager.getAdsWatchedCount(context)
        isAdFree = RewardedAdManager.isAdFree(context)
        adFreeRemaining = RewardedAdManager.getRemainingAdFreeTimeFormatted(context)
        isShowingAd = false
        activity?.let(RewardedAdManager::loadRewardedAd) // queue the next ad
    }

    // Every outcome (reward, failure, dismissal) just refreshes the counters
    fun watchAd(type: RewardedAdManager.AdType) {
        if (activity == null || isShowingAd) return
        isShowingAd = true
        RewardedAdManager.showRewardedAd(
            activity = activity,
            onUserEarnedReward = { refresh() },
            onAdFailedToShow = { refresh() },
            onAdDismissed = { refresh() },
            adType = type
        )
    }

    LaunchedEffect(activity) { refresh() }

    val nextTier = rewardTiers.firstOrNull { adsWatched < it.adsRequired }

    GlassSubpageScaffold(title = "Earn rewards", eyebrow = "Settings", onBack = onNavigateBack) {
        GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassIconBadge(
                painter = rememberVectorPainter(Icons.Filled.VolunteerActivism),
                size = 64.dp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                "Support Shredzilla",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = palette.content,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                "Watch a few short ads to unlock ad-free time and help keep the app running.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.contentMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (isAdFree) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassIconBadge(painter = rememberVectorPainter(Icons.Filled.Shield), size = 52.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Ad-free is on", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
                        Text("$adFreeRemaining left", style = MaterialTheme.typography.bodyMedium, color = palette.contentMuted)
                    }
                }
            }
        } else {
            AdFreeProgressCard(
                adsWatched = adsWatched,
                nextTier = nextTier,
                isShowingAd = isShowingAd,
                canShowAds = activity != null,
                onWatchAd = { watchAd(RewardedAdManager.AdType.TIMED_REWARD) }
            )
            GlassSettingsGroup(title = "Just want to help?") {
                GlassSettingsRow(
                    icon = Icons.Filled.Favorite,
                    title = "Watch a support ad",
                    subtitle = "No reward, just a thank-you",
                    showChevron = false,
                    onClick = { watchAd(RewardedAdManager.AdType.SUPPORT_ONLY) }
                )
            }
        }
    }
}

@Composable
private fun AdFreeProgressCard(
    adsWatched: Int,
    nextTier: RewardTier?,
    isShowingAd: Boolean,
    canShowAds: Boolean,
    onWatchAd: () -> Unit
) {
    val palette = MaterialTheme.glass
    GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$adsWatched", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = palette.content)
            Spacer(Modifier.width(8.dp))
            Text("ads watched", style = MaterialTheme.typography.titleMedium, color = palette.contentMuted)
        }
        if (nextTier != null) {
            val progress = (adsWatched.toFloat() / nextTier.adsRequired).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(palette.content.copy(alpha = 0.12f), RoundedCornerShape(50))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(8.dp)
                        .background(Brush.horizontalGradient(palette.accentGradient), RoundedCornerShape(50))
                )
            }
            Text(
                "${nextTier.adsRequired - adsWatched} more for ${durationLabel(nextTier.durationMillis)} ad-free",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.contentMuted
            )
        }
        rewardTiers.forEach { tier ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassSelectionIndicator(selected = adsWatched >= tier.adsRequired, size = 22.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    "${tier.adsRequired} ads",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.content,
                    modifier = Modifier.weight(1f)
                )
                GlassChip(text = "${durationLabel(tier.durationMillis)} ad-free", highlighted = tier == nextTier)
            }
        }
        GlassPrimaryButton(text = "Watch an ad", onClick = onWatchAd, enabled = canShowAds, loading = isShowingAd)
    }
}
