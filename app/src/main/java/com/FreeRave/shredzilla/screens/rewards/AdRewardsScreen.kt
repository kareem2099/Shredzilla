package com.FreeRave.shredzilla.screens.rewards

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FreeRave.shredzilla.utils.RewardedAdManager
import java.util.concurrent.TimeUnit

data class RewardTierDisplay(
    val adsRequired: Int,
    val durationMillis: Long,
    val title: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdRewardsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalContext.current as? Activity

    var adsWatchedCount by remember { mutableStateOf(RewardedAdManager.getAdsWatchedCount(context)) }
    var adFreeTimeRemaining by remember { mutableStateOf(RewardedAdManager.getRemainingAdFreeTimeFormatted(context)) }
    var isAdFreeActive by remember { mutableStateOf(RewardedAdManager.isAdFree(context)) }
    var isAdLoadingOrShowing by remember { mutableStateOf(false) }

    val updateAdStatus = {
        adsWatchedCount = RewardedAdManager.getAdsWatchedCount(context)
        adFreeTimeRemaining = RewardedAdManager.getRemainingAdFreeTimeFormatted(context)
        isAdFreeActive = RewardedAdManager.isAdFree(context)
        // Ensure ad is loaded for next interaction
        activity?.let { RewardedAdManager.loadRewardedAd(it) }
    }

    LaunchedEffect(Unit, activity) {
        activity?.let {
            Log.d("AdRewardsScreen", "LaunchedEffect: Attempting to load rewarded ad.")
            RewardedAdManager.loadRewardedAd(it)
        }
        updateAdStatus() // Initial status update
    }

    val rewardTiersDisplay = remember {
        RewardedAdManager.REWARD_TIERS.sortedBy { it.first }.map { tier -> // Sort by ads ascending for display
            val hours = TimeUnit.MILLISECONDS.toHours(tier.second)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(tier.second) % 60
            var durationStr = ""
            if (hours > 0) durationStr += "$hours hr "
            if (minutes > 0) durationStr += "$minutes min"
            durationStr = durationStr.trim()
            RewardTierDisplay(
                adsRequired = tier.first,
                durationMillis = tier.second,
                title = "$durationStr Ad-Free",
                description = "Watch ${tier.first} ads"
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Earn Rewards") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Support the Dev",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Support Shredzilla!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Watch a few short ads to enjoy an ad-free experience and help keep Shredzilla running. Your support is greatly appreciated!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (isAdFreeActive) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Shield,
                                contentDescription = "Ad-Free Active",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Ad-Free Active!",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    "Time remaining: $adFreeTimeRemaining",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            } else {
                items(rewardTiersDisplay) { tier ->
                    val currentTierAdsRequired = tier.adsRequired
                    val adsNeededForThisTier = currentTierAdsRequired - adsWatchedCount

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (activity != null && !isAdLoadingOrShowing && adsNeededForThisTier > 0) {
                                isAdLoadingOrShowing = true
                                RewardedAdManager.showRewardedAd(
                                    activity = activity,
                                    onUserEarnedReward = {
                                        Log.d("AdRewardsScreen", "User earned reward: ${it.amount} ${it.type}")
                                        updateAdStatus()
                                        isAdLoadingOrShowing = false
                                    },
                                    onAdFailedToShow = { adError ->
                                        Log.e("AdRewardsScreen", "Ad failed to show: ${adError.message}")
                                        updateAdStatus()
                                        isAdLoadingOrShowing = false
                                    },
                                    onAdDismissed = {
                                        Log.d("AdRewardsScreen", "Ad dismissed.")
                                        updateAdStatus() // Status might change if reward was earned before dismissal
                                        isAdLoadingOrShowing = false
                                    }
                                )
                            } else if (adsNeededForThisTier <= 0) {
                                // This case means enough ads are watched for THIS tier or higher.
                                // The RewardedAdManager handles giving the highest eligible reward.
                                // We can still allow clicking to trigger the ad show, which will then process.
                                 if (activity != null && !isAdLoadingOrShowing) {
                                    isAdLoadingOrShowing = true
                                    RewardedAdManager.showRewardedAd(activity, { updateAdStatus(); isAdLoadingOrShowing = false }, { updateAdStatus(); isAdLoadingOrShowing = false }, { updateAdStatus(); isAdLoadingOrShowing = false })
                                 }
                            }
                        }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CardGiftcard, contentDescription = tier.title, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(tier.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(tier.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))

                            if (adsWatchedCount < currentTierAdsRequired) {
                                LinearProgressIndicator(
                                    progress = { adsWatchedCount.toFloat() / currentTierAdsRequired.toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Progress: $adsWatchedCount / $currentTierAdsRequired ads",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    "Goal reached! Watch ad to claim.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                     if (activity != null && !isAdLoadingOrShowing) {
                                        isAdLoadingOrShowing = true
                                        RewardedAdManager.showRewardedAd(
                                            activity = activity,
                                            onUserEarnedReward = { updateAdStatus(); isAdLoadingOrShowing = false; },
                                            onAdFailedToShow = { updateAdStatus(); isAdLoadingOrShowing = false; },
                                            onAdDismissed = { updateAdStatus(); isAdLoadingOrShowing = false; },
                                            adType = RewardedAdManager.AdType.TIMED_REWARD
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isAdLoadingOrShowing && activity != null
                            ) {
                                Icon(Icons.Filled.Videocam, contentDescription = "Watch Ad for Timed Reward")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (adsWatchedCount < currentTierAdsRequired) "Watch Ad ($adsNeededForThisTier left)" else "Claim Reward!")
                            }
                        }
                    }
                }
            }

            // Add the "Support Only" Ad Option if not ad-free
            if (!isAdFreeActive) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (activity != null && !isAdLoadingOrShowing) {
                                isAdLoadingOrShowing = true
                                RewardedAdManager.showRewardedAd(
                                    activity = activity,
                                    onUserEarnedReward = {
                                        // For support ads, the main reward is the "thank you" toast from manager.
                                        // We still update status in case other parts of UI depend on general ad states.
                                        updateAdStatus()
                                        isAdLoadingOrShowing = false
                                    },
                                    onAdFailedToShow = {
                                        updateAdStatus()
                                        isAdLoadingOrShowing = false
                                    },
                                    onAdDismissed = {
                                        updateAdStatus()
                                        isAdLoadingOrShowing = false
                                    },
                                    adType = RewardedAdManager.AdType.SUPPORT_ONLY
                                )
                            }
                        }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Favorite, contentDescription = "Support the Dev", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.error) // Using error color for "heart"
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Support Shredzilla", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Watch an ad to help support the development of Shredzilla. No timed reward, just pure appreciation!", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (activity != null && !isAdLoadingOrShowing) {
                                        isAdLoadingOrShowing = true
                                        RewardedAdManager.showRewardedAd(
                                            activity = activity,
                                            onUserEarnedReward = { updateAdStatus(); isAdLoadingOrShowing = false; },
                                            onAdFailedToShow = { updateAdStatus(); isAdLoadingOrShowing = false; },
                                            onAdDismissed = { updateAdStatus(); isAdLoadingOrShowing = false; },
                                            adType = RewardedAdManager.AdType.SUPPORT_ONLY
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isAdLoadingOrShowing && activity != null,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                            ) {
                                Icon(Icons.Filled.Videocam, contentDescription = "Watch Ad for Support")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Watch Ad to Support")
                            }
                        }
                    }
                }
            }
        }
    }
}
