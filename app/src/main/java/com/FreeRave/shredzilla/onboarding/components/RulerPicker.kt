package com.FreeRave.shredzilla.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.theme.glass
import kotlin.math.abs

private val TickSpacing = 12.dp
private val RulerHeight = 56.dp

/**
 * Horizontal ruler that snaps to whole numbers. The tick under the centre line is the value;
 * every change ticks the haptics like a physical dial.
 */
@Composable
fun RulerPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.glass
    val haptics = LocalHapticFeedback.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (value - range.first).coerceIn(0, range.count() - 1))

    val centredIndex by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val centre = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - centre) }?.index
        }
    }
    LaunchedEffect(centredIndex) {
        val index = centredIndex ?: return@LaunchedEffect
        val newValue = range.first + index
        if (newValue != value) {
            currentOnValueChange(newValue)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(RulerHeight)) {
        // Side padding lets the first and last ticks reach the centre line
        val sidePadding = (maxWidth - TickSpacing) / 2
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(horizontal = sidePadding),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .fadeEdges()
        ) {
            items(range.count()) { index ->
                RulerTick(number = range.first + index)
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(4.dp)
                .height(44.dp)
                .background(Brush.verticalGradient(palette.accentGradient), RoundedCornerShape(50))
        )
    }
}

@Composable
private fun RulerTick(number: Int) {
    val palette = MaterialTheme.glass
    val isMajor = number % 10 == 0
    val isMid = number % 5 == 0
    Column(
        modifier = Modifier.width(TickSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (isMajor) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = palette.contentMuted,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.wrapContentWidth(unbounded = true)
            )
        }
        Box(
            modifier = Modifier
                .width(if (isMajor) 2.dp else 1.5.dp)
                .height(
                    when {
                        isMajor -> 32.dp
                        isMid -> 22.dp
                        else -> 14.dp
                    }
                )
                .background(
                    palette.content.copy(alpha = if (isMajor) 0.8f else 0.35f),
                    RoundedCornerShape(50)
                )
        )
    }
}

/** Fades ticks out towards both ends so the ruler looks like it curves away. */
private fun Modifier.fadeEdges() = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.2f to Color.Black,
                0.8f to Color.Black,
                1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }
