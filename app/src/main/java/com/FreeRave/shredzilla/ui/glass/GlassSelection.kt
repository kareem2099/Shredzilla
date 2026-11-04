package com.FreeRave.shredzilla.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.theme.glass

/**
 * Glass card that lights up with the accent colour when [selected].
 * Use [role] = [Role.Checkbox] for multi-select lists, [Role.RadioButton] for single choice.
 */
@Composable
fun GlassSelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.RadioButton,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable RowScope.() -> Unit
) {
    val palette = MaterialTheme.glass
    val tint by animateColorAsState(
        if (selected) palette.accent.copy(alpha = 0.14f) else Color.Transparent,
        label = "selectableTint"
    )
    val edge by animateColorAsState(
        if (selected) palette.accent else Color.Transparent,
        label = "selectableEdge"
    )
    Row(
        modifier = modifier
            .glassSurface(palette, shape)
            .background(tint)
            .border(1.5.dp, edge, shape)
            .selectable(selected = selected, role = role, onClick = onClick)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** Round check mark: filled accent circle when selected, faint ring otherwise. */
@Composable
fun GlassSelectionIndicator(selected: Boolean, modifier: Modifier = Modifier, size: Dp = 26.dp) {
    val palette = MaterialTheme.glass
    val checkScale by animateFloatAsState(if (selected) 1f else 0f, label = "checkScale")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.5.dp, if (selected) Color.Transparent else palette.content.copy(alpha = 0.3f), CircleShape)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .scale(checkScale)
                .background(Brush.linearGradient(palette.accentGradient), CircleShape)
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(size * 0.6f))
        }
    }
}

/** Small rounded label, e.g. "Popular" or a difficulty level. */
@Composable
fun GlassChip(text: String, modifier: Modifier = Modifier, highlighted: Boolean = false) {
    val palette = MaterialTheme.glass
    val shape = RoundedCornerShape(50)
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = if (highlighted) palette.onAccent else palette.content,
        modifier = modifier
            .clip(shape)
            .then(
                if (highlighted) Modifier.background(Brush.horizontalGradient(palette.accentGradient))
                else Modifier.glassSurface(palette, shape)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** Single-choice option with an icon, e.g. a unit system or theme. */
@Composable
fun GlassOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.glass
    GlassSelectableCard(selected = selected, onClick = onClick, modifier = modifier.fillMaxWidth()) {
        GlassIconBadge(painter = rememberVectorPainter(icon), size = 48.dp)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
        }
        Spacer(Modifier.width(12.dp))
        GlassSelectionIndicator(selected = selected)
    }
}

/**
 * Row of numbered circles for picking a small count. With [fillUpToSelected] every number up to
 * the selection lights up (a "how many" feel); otherwise only the chosen one does.
 */
@Composable
fun GlassNumberRow(
    range: IntRange,
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fillUpToSelected: Boolean = false
) {
    val palette = MaterialTheme.glass
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        range.forEach { number ->
            val filled = selected != null && if (fillUpToSelected) number <= selected else number == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .then(
                        if (filled) Modifier.background(Brush.linearGradient(palette.accentGradient))
                        else Modifier.glassSurface(palette, CircleShape)
                    )
                    .clickable(role = Role.Button) { onSelect(number) }
            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (filled) palette.onAccent else palette.contentMuted
                )
            }
        }
    }
}

/** Toggleable pill for filters and sort modes. */
@Composable
fun GlassFilterChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.glass
    val shape = RoundedCornerShape(50)
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = if (selected) palette.onAccent else palette.contentMuted,
        modifier = modifier
            .clip(shape)
            .then(
                if (selected) Modifier.background(Brush.horizontalGradient(palette.accentGradient))
                else Modifier.glassSurface(palette, shape)
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
