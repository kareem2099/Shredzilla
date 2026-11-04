package com.FreeRave.shredzilla.ui.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.theme.glass

/**
 * Shell for pushed settings pages (no bottom bar): glass top bar with back, scrollable body and
 * an optional [bottomBar] (e.g. a Save button) pinned above the navigation bar.
 */
@Composable
fun GlassSubpageScaffold(
    title: String,
    onBack: () -> Unit,
    eyebrow: String? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GlassTopBar(title = title, eyebrow = eyebrow, onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp)
                .then(if (bottomBar == null) Modifier.navigationBarsPadding() else Modifier),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
        if (bottomBar != null) {
            Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) { bottomBar() }
        }
    }
}

/** Titled group of [GlassSettingsRow]s on one frosted card. */
@Composable
fun GlassSettingsGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = MaterialTheme.glass
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.contentMuted,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )
        }
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            content = content
        )
    }
}

/**
 * One tappable settings line: tinted icon badge, title/subtitle, an optional current [value]
 * and a chevron. Pass [tint] = palette.error for destructive actions.
 */
@Composable
fun GlassSettingsRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    value: String? = null,
    tint: Color = MaterialTheme.glass.accent,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    val palette = MaterialTheme.glass
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconBadge(painter = rememberVectorPainter(icon), size = 42.dp, tint = tint)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (tint == palette.error) palette.error else palette.content
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
            }
        }
        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.contentMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        trailing?.invoke()
        if (showChevron) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.contentMuted)
        }
    }
}

/** Small muted explainer under a group, e.g. what a setting does or doesn't change. */
@Composable
fun GlassFootnote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.glass.contentMuted,
        modifier = modifier.padding(horizontal = 8.dp)
    )
}
