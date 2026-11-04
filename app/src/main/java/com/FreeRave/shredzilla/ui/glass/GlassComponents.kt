package com.FreeRave.shredzilla.ui.glass

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.FreeRave.shredzilla.ui.theme.GlassPalette
import com.FreeRave.shredzilla.ui.theme.LocalGlassPalette
import com.FreeRave.shredzilla.ui.theme.glass

object GlassDefaults {
    val CardShape = RoundedCornerShape(28.dp)
    val ControlShape = RoundedCornerShape(16.dp)
    val CardPadding = PaddingValues(20.dp)
    val ButtonHeight = 54.dp
}

/** Frosted surface: translucent fill that fades downward + a light-catching gradient edge. */
fun Modifier.glassSurface(palette: GlassPalette, shape: Shape = GlassDefaults.CardShape): Modifier =
    this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(palette.fill.copy(alpha = palette.fill.alpha * 1.5f), palette.fill)
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(palette.border, palette.border.copy(alpha = palette.border.alpha * 0.25f))
            ),
            shape = shape
        )

/** Full-screen gradient with slowly drifting colour orbs — the backdrop every glass surface sits on. */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = MaterialTheme.glass
    val drift by rememberInfiniteTransition(label = "glassOrbs").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )
    val orbAlpha = if (palette.isDark) 0.45f else 0.70f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.backgroundTop, palette.backgroundBottom)))
            // `drift` is read only in the draw phase, so the animation redraws without recomposing
            .drawBehind {
                drawOrb(palette.orbPrimary.copy(alpha = orbAlpha), 0.10f + 0.12f * drift, 0.12f, 0.65f)
                drawOrb(palette.orbSecondary.copy(alpha = orbAlpha * 0.8f), 0.95f - 0.10f * drift, 0.50f, 0.55f)
                drawOrb(palette.orbPrimary.copy(alpha = orbAlpha * 0.6f), 0.25f, 0.92f - 0.06f * drift, 0.60f)
            },
        contentAlignment = contentAlignment,
        content = content
    )
}

/** Soft radial colour blob; positions and radius are fractions of the canvas width/height. */
fun DrawScope.drawOrb(color: Color, xFraction: Float, yFraction: Float, radiusFraction: Float) {
    val center = Offset(size.width * xFraction, size.height * yFraction)
    val radius = size.width * radiusFraction
    drawCircle(
        brush = Brush.radialGradient(listOf(color, Color.Transparent), center, radius),
        radius = radius,
        center = center
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassDefaults.CardShape,
    contentPadding: PaddingValues = GlassDefaults.CardPadding,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .glassSurface(MaterialTheme.glass, shape)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content
    )
}

/** Main call-to-action: accent gradient pill that turns into a spinner while [loading]. */
@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val palette = MaterialTheme.glass
    val active = enabled && !loading
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(GlassDefaults.ButtonHeight)
            .clip(GlassDefaults.ControlShape)
            .background(
                Brush.horizontalGradient(
                    palette.accentGradient.map { if (enabled) it else it.copy(alpha = 0.4f) }
                )
            )
            .clickable(enabled = active, role = Role.Button, onClick = onClick)
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = palette.onAccent,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = text,
                // Disabled text may carry a hint ("Give your list a name"), so keep it readable
                color = if (enabled) palette.onAccent else palette.content.copy(alpha = 0.75f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Circular glass badge with a soft accent glow behind the icon. */
@Composable
fun GlassIconBadge(
    painter: Painter,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    tint: Color = MaterialTheme.glass.accent
) {
    val palette = MaterialTheme.glass
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .drawBehind {
                drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = 0.45f), Color.Transparent)))
            }
            .glassSurface(palette, CircleShape)
    ) {
        Icon(painter = painter, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Thin divider with a centred label, e.g. "or". */
@Composable
fun GlassDividerWithText(text: String, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.glass
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = palette.border.copy(alpha = 0.3f))
        Text(
            text = text,
            color = palette.contentMuted,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = palette.border.copy(alpha = 0.3f))
    }
}

@Composable
fun glassTextFieldColors(): TextFieldColors {
    val p = MaterialTheme.glass
    val fieldFill = p.fill.copy(alpha = if (p.isDark) 0.06f else 0.6f)
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = p.content,
        unfocusedTextColor = p.content.copy(alpha = 0.9f),
        disabledTextColor = p.content.copy(alpha = 0.38f),
        errorTextColor = p.content,
        focusedContainerColor = fieldFill,
        unfocusedContainerColor = fieldFill,
        disabledContainerColor = fieldFill,
        errorContainerColor = fieldFill,
        cursorColor = p.accent,
        errorCursorColor = p.error,
        focusedBorderColor = p.accent,
        unfocusedBorderColor = p.content.copy(alpha = 0.18f),
        disabledBorderColor = p.content.copy(alpha = 0.08f),
        errorBorderColor = p.error,
        focusedLabelColor = p.accent,
        unfocusedLabelColor = p.contentMuted,
        disabledLabelColor = p.content.copy(alpha = 0.3f),
        errorLabelColor = p.error,
        focusedLeadingIconColor = p.accent,
        unfocusedLeadingIconColor = p.contentMuted,
        disabledLeadingIconColor = p.content.copy(alpha = 0.3f),
        errorLeadingIconColor = p.error,
        focusedTrailingIconColor = p.accent,
        unfocusedTrailingIconColor = p.contentMuted,
        disabledTrailingIconColor = p.content.copy(alpha = 0.3f),
        errorTrailingIconColor = p.error,
        focusedSupportingTextColor = p.contentMuted,
        unfocusedSupportingTextColor = p.contentMuted,
        errorSupportingTextColor = p.error
    )
}

/**
 * Confirmation dialog on a frosted card. The card sits on an opaque palette gradient so text behind
 * the dimmed scrim can't bleed through.
 */
@Composable
fun GlassDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Cancel",
    icon: Painter? = null,
    destructive: Boolean = false,
    confirmLoading: Boolean = false
) {
    val palette = MaterialTheme.glass
    Dialog(onDismissRequest = { if (!confirmLoading) onDismiss() }) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(GlassDefaults.CardShape)
                .background(
                    Brush.verticalGradient(listOf(palette.backgroundTop, palette.backgroundBottom))
                ),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (icon != null) {
                GlassIconBadge(painter = icon, size = 64.dp, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            Text(
                text = title,
                color = palette.content,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = message,
                color = palette.contentMuted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            // Destructive actions swap the accent for the error colour so they can't be mistaken for "OK"
            CompositionLocalProvider(LocalGlassPalette provides if (destructive) palette.asDestructive() else palette) {
                GlassPrimaryButton(text = confirmText, onClick = onConfirm, loading = confirmLoading)
            }
            TextButton(
                onClick = onDismiss,
                enabled = !confirmLoading,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(dismissText, color = palette.contentMuted)
            }
        }
    }
}

private fun GlassPalette.asDestructive() = copy(
    accent = error,
    accentGradient = listOf(Color(0xFFD7263D), Color(0xFFFF5C6C)),
    onAccent = Color.White
)

/** Modal bottom sheet tinted with the palette so it matches the glass screens behind it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = MaterialTheme.glass
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.backgroundBottom,
        contentColor = palette.content,
        dragHandle = { BottomSheetDefaults.DragHandle(color = palette.content.copy(alpha = 0.3f)) }
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.content,
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                )
            }
            content()
        }
    }
}

@Composable
fun glassSwitchColors(): SwitchColors {
    val p = MaterialTheme.glass
    return SwitchDefaults.colors(
        checkedThumbColor = p.onAccent,
        checkedTrackColor = p.accent,
        checkedBorderColor = p.accent,
        uncheckedThumbColor = p.contentMuted,
        uncheckedTrackColor = p.fill,
        uncheckedBorderColor = p.content.copy(alpha = 0.25f)
    )
}

/** Pill search box with a clear button. */
@Composable
fun GlassSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.glass
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(50),
        placeholder = { Text(placeholder, color = palette.contentMuted) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Filled.Close, contentDescription = "Clear search") } }
        } else null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = glassTextFieldColors()
    )
}

/** Extended floating action button in the accent gradient. */
@Composable
fun GlassFab(icon: ImageVector, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.glass
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(palette.accentGradient))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = palette.onAccent)
        Spacer(Modifier.width(8.dp))
        Text(text, color = palette.onAccent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/** Small metric tile: icon, big animated value, label. */
@Composable
fun GlassStatTile(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.glass
    GlassCard(modifier = modifier, contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
        AnimatedContent(
            targetState = value,
            transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith fadeOut() },
            label = "${label}Stat"
        ) { shown ->
            Text(
                shown,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = palette.content,
                maxLines = 1
            )
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = palette.contentMuted, maxLines = 1)
    }
}
