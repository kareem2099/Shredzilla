package com.FreeRave.shredzilla.screens.pro

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Blender
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.widget.Toast
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay
import com.FreeRave.shredzilla.pro.AdBlock
import com.FreeRave.shredzilla.pro.AdHours
import com.FreeRave.shredzilla.pro.AdTimeBank
import com.FreeRave.shredzilla.pro.ProCatalog
import com.FreeRave.shredzilla.pro.ProCrown
import com.FreeRave.shredzilla.pro.ProGold
import com.FreeRave.shredzilla.pro.ProPill
import com.FreeRave.shredzilla.pro.ProSource
import com.FreeRave.shredzilla.pro.RewardedAds
import com.FreeRave.shredzilla.pro.SupportTip
import com.FreeRave.shredzilla.pro.findActivity
import com.FreeRave.shredzilla.pro.formatRemaining
import com.FreeRave.shredzilla.pro.rememberTickingNow
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassDefaults
import com.FreeRave.shredzilla.ui.glass.GlassFootnote
import com.FreeRave.shredzilla.ui.glass.GlassPrimaryButton
import com.FreeRave.shredzilla.ui.glass.GlassSubpageScaffold
import com.FreeRave.shredzilla.ui.glass.glassSurface
import com.FreeRave.shredzilla.ui.theme.glass

private data class ProFeature(val icon: ImageVector, val title: String, val text: String)

private val proFeatures = listOf(
    ProFeature(Icons.Filled.Psychology, "Next-set coach", "Suggests your next weight and reps from your last session"),
    ProFeature(Icons.Filled.CompareArrows, "Last-time comparison", "\"+2 reps\", \"+1 plate\" and last session's sets in workout mode"),
    ProFeature(Icons.Filled.Straighten, "Body insights", "Trend charts and what your weight and waist changes mean"),
    ProFeature(Icons.Filled.AutoGraph, "Advanced stats", "Sets per muscle each week, estimated 1RM and volume trends"),
    ProFeature(Icons.Filled.Palette, "Accent themes", "Inferno, Ocean, Gold and Volt on top of your profile colors"),
    ProFeature(Icons.Filled.Share, "Share card styles", "Midnight and Accent looks for your workout summaries"),
)

/**
 * The Pro page: buy Pro for life, earn Pro hours with optional ads, or send a tip.
 * Everything that's free stays free; this page is the only place ads ever appear.
 */
@Composable
fun ProScreen(
    proSource: ProSource,
    adTimeBank: AdTimeBank,
    supporterTips: Int,
    prices: Map<String, String>,
    storeAvailable: Boolean,
    onBuy: (productId: String) -> Unit,
    onRestore: () -> Unit,
    /** Firebase uid the server credits the ad hour to. */
    userId: String?,
    onBack: () -> Unit,
) {
    val palette = MaterialTheme.glass
    val now = rememberTickingNow()
    val context = LocalContext.current
    LaunchedEffect(proSource) { if (proSource != ProSource.LIFETIME) RewardedAds.preload(context) }

    GlassSubpageScaffold(title = "DotShredzilla Pro", onBack = onBack) {
        HeroCard(proSource, adTimeBank.remainingMillis(now), supporterTips)

        GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("What Pro adds", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
            proFeatures.forEach { FeatureRow(it, unlocked = proSource != ProSource.NONE) }
            Text(
                "Logging, lists, programs, workout mode, supersets, the rest timer, plate calculator, records, streaks and history stay free. No ads anywhere unless you choose to watch one here.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.contentMuted
            )
        }

        if (proSource == ProSource.LIFETIME) {
            OwnedCard()
        } else {
            LifetimeCard(price = prices[ProCatalog.PRO_LIFETIME] ?: ProCatalog.PRO_FALLBACK_PRICE, storeAvailable, onBuy = { onBuy(ProCatalog.PRO_LIFETIME) })
            AdTimeCard(adTimeBank, now, userId)
        }

        TipsCard(prices, onBuy)

        Text(
            "Restore purchases",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = palette.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onRestore)
                .padding(12.dp)
        )
        GlassFootnote(
            "Payments are handled by Google Play. Pro for life belongs to your Google Play account and comes back on new devices. " +
                "Ad hours are confirmed with Google and added to your DotShredzilla account by our server. They count down in real time, also while the app is closed, with up to ${ProCatalog.MAX_ADS_PER_DAY} ads in any 24 hours. " +
                "Tips are optional thank-yous and don't unlock Pro."
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HeroCard(source: ProSource, remainingMillis: Long, supporterTips: Int) {
    val palette = MaterialTheme.glass
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(palette)
            .background(Brush.verticalGradient(listOf(ProGold[1].copy(alpha = 0.16f), Color.Transparent)))
            .padding(vertical = 26.dp, horizontal = 20.dp)
    ) {
        ProCrown()
        Text(
            when (source) {
                ProSource.LIFETIME -> "You're Pro for life"
                ProSource.AD_TIME -> "Pro is on"
                ProSource.NONE -> "Train smarter with Pro"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = palette.content,
            textAlign = TextAlign.Center
        )
        when (source) {
            ProSource.AD_TIME -> AnimatedContent(targetState = formatRemaining(remainingMillis), label = "proLeft") { left ->
                Text("$left left", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = ProGold[2])
            }
            ProSource.LIFETIME -> Text("Every Pro feature, now and in future updates.", style = MaterialTheme.typography.bodyMedium, color = palette.contentMuted, textAlign = TextAlign.Center)
            ProSource.NONE -> Text("A coach in your pocket, insights from your body data and stats that show where you're growing.", style = MaterialTheme.typography.bodyMedium, color = palette.contentMuted, textAlign = TextAlign.Center)
        }
        if (supporterTips > 0) SupporterBadge()
    }
}

@Composable
fun SupporterBadge(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFFF4F7B).copy(alpha = 0.16f))
            .border(1.dp, Color(0xFFFF4F7B).copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color(0xFFFF4F7B), modifier = Modifier.size(14.dp))
        Text("Supporter", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFF4F7B))
    }
}

@Composable
private fun FeatureRow(feature: ProFeature, unlocked: Boolean) {
    val palette = MaterialTheme.glass
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(palette.accentGradient.map { it.copy(alpha = 0.22f) }))
        ) {
            Icon(feature.icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(feature.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = palette.content)
            Text(feature.text, style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
        }
        if (unlocked) Icon(Icons.Filled.CheckCircle, contentDescription = "Unlocked", tint = ProGold[1], modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun LifetimeCard(price: String, storeAvailable: Boolean, onBuy: () -> Unit) {
    val palette = MaterialTheme.glass
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(palette)
            .border(1.5.dp, Brush.linearGradient(ProGold), GlassDefaults.CardShape)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pro for life", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.content)
                    ProPill(text = "BEST")
                }
                Text("One payment. No subscription, no renewals.", style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
            }
            Text(price, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = palette.content)
        }
        GoldButton(text = "Get Pro for $price", onClick = onBuy)
        if (!storeAvailable) {
            Text("Connecting to Google Play…", style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
        }
    }
}

@Composable
private fun GoldButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(GlassDefaults.ButtonHeight)
            .clip(GlassDefaults.ControlShape)
            .background(Brush.horizontalGradient(ProGold))
            .clickable(onClick = onClick)
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2A1A00))
    }
}

@Composable
private fun OwnedCard() {
    val palette = MaterialTheme.glass
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ProGold[1], modifier = Modifier.size(28.dp))
            Column {
                Text("Pro for life is yours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
                Text("Thank you for backing DotShredzilla. No ads, nothing to renew.", style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
            }
        }
    }
}

/** Ad hours: a bank of up to 8 one-hour cells, filling as ads are watched and draining in real time. */
@Composable
private fun AdTimeCard(bank: AdTimeBank, now: Long, userId: String?) {
    val palette = MaterialTheme.glass
    val context = LocalContext.current
    val block = AdHours.blockReason(bank, now)
    val adsInWindow = bank.adsInWindow(now)
    var playing by remember { mutableStateOf(false) }
    // The server adds the hour after AdMob's callback; wait for the account to show it
    var awaitingFrom by remember { mutableStateOf<Long?>(null) }
    var slow by remember { mutableStateOf(false) }
    LaunchedEffect(bank.untilMillis, awaitingFrom) {
        val from = awaitingFrom ?: return@LaunchedEffect
        if (bank.untilMillis > from) {
            awaitingFrom = null
            slow = false
            Toast.makeText(context, "+${ProCatalog.HOURS_PER_AD} hour of Pro", Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(awaitingFrom) {
        if (awaitingFrom == null) return@LaunchedEffect
        delay(20_000)
        slow = true
    }
    val hoursBanked = bank.remainingMillis(now) / 3_600_000f

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = palette.accent, modifier = Modifier.size(30.dp))
            Column(Modifier.weight(1f)) {
                Text("Watch an ad, get an hour", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
                Text(
                    "Each ad adds ${ProCatalog.HOURS_PER_AD} hour of Pro. Bank up to ${ProCatalog.MAX_BANKED_HOURS} hours.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.contentMuted
                )
            }
        }
        TimeBankMeter(hoursBanked)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (hoursBanked > 0f) "${formatRemaining(bank.remainingMillis(now))} banked" else "Nothing banked yet",
                style = MaterialTheme.typography.labelLarge,
                color = palette.content,
                modifier = Modifier.weight(1f)
            )
            Text("$adsInWindow/${ProCatalog.MAX_ADS_PER_DAY} in 24h", style = MaterialTheme.typography.labelLarge, color = palette.contentMuted)
        }
        val adReady = RewardedAds.loadState == RewardedAds.LoadState.READY
        GlassPrimaryButton(
            text = when {
                awaitingFrom != null -> "Adding your hour…"
                block == AdBlock.DAILY_LIMIT -> bank.nextAdAllowedAt(now)
                    ?.let { "Next ad at ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it))}" }
                    ?: "Daily limit reached"
                block == AdBlock.BANK_FULL -> "Bank full. Enjoy your Pro time"
                RewardedAds.loadState == RewardedAds.LoadState.FAILED -> "No ad available. Tap to retry"
                !adReady -> "Loading ad…"
                else -> "Watch ad  ·  +${ProCatalog.HOURS_PER_AD} hour"
            },
            enabled = userId != null && awaitingFrom == null && block == null && (adReady || RewardedAds.loadState == RewardedAds.LoadState.FAILED),
            loading = playing || awaitingFrom != null,
            onClick = {
                if (!adReady) {
                    RewardedAds.preload(context)
                    return@GlassPrimaryButton
                }
                val activity = context.findActivity() ?: return@GlassPrimaryButton
                val uid = userId ?: return@GlassPrimaryButton
                playing = true
                val before = bank.untilMillis
                RewardedAds.show(activity, uid, onRewarded = { awaitingFrom = before }, onClosed = { playing = false })
            }
        )
        if (slow && awaitingFrom != null) {
            Text(
                "Google is still confirming the ad. Your hour will appear here as soon as it does, even if you leave this page.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.contentMuted
            )
        }
        if (RewardedAds.loadState == RewardedAds.LoadState.LOADING && block == null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = palette.contentMuted, modifier = Modifier.size(12.dp))
                Text("Getting an ad ready", style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
            }
        }
    }
}

@Composable
private fun TimeBankMeter(hoursBanked: Float) {
    val palette = MaterialTheme.glass
    val animated by animateFloatAsState(hoursBanked, tween(700), label = "bank")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(ProCatalog.MAX_BANKED_HOURS) { i ->
            val fill = (animated - i).coerceIn(0f, 1f)
            Box(
                Modifier
                    .weight(1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(palette.content.copy(alpha = 0.08f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fill)
                        .height(14.dp)
                        .background(Brush.horizontalGradient(ProGold))
                )
            }
        }
    }
}

@Composable
private fun TipsCard(prices: Map<String, String>, onBuy: (String) -> Unit) {
    val palette = MaterialTheme.glass
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Fuel the developer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color(0xFFFF4F7B), modifier = Modifier.size(18.dp))
        }
        Text(
            "DotShredzilla is built by one person. A tip says thanks, keeps updates coming and adds a Supporter badge to your profile. Tips don't unlock Pro.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.contentMuted
        )
        SupportTip.entries.forEach { tip ->
            TipRow(tip, price = prices[tip.productId] ?: tip.fallbackPrice, onClick = { onBuy(tip.productId) })
        }
    }
}

private val SupportTip.icon: ImageVector
    get() = when (this) {
        SupportTip.PRE_WORKOUT -> Icons.Filled.Bolt
        SupportTip.PROTEIN -> Icons.Filled.Blender
        SupportTip.CREATINE -> Icons.Filled.Inventory2
    }

private val SupportTip.tint: Color
    get() = when (this) {
        SupportTip.PRE_WORKOUT -> Color(0xFFFF7A45)
        SupportTip.PROTEIN -> Color(0xFF4FC3F7)
        SupportTip.CREATINE -> Color(0xFFB388FF)
    }

@Composable
private fun TipRow(tip: SupportTip, price: String, onClick: () -> Unit) {
    val palette = MaterialTheme.glass
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(palette, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(tip.tint.copy(alpha = 0.18f))
        ) {
            Icon(tip.icon, contentDescription = null, tint = tip.tint, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(tip.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = palette.content)
            Text(tip.blurb, style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
        }
        Text(
            price,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = palette.onAccent,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(palette.accentGradient))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
