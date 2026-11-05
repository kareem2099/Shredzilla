package com.FreeRave.shredzilla.screens.exercises.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.glass.GlassChip
import com.FreeRave.shredzilla.ui.glass.GlassDefaults
import com.FreeRave.shredzilla.ui.glass.glassSurface
import com.FreeRave.shredzilla.ui.theme.glass

/**
 * Exercise row: tap to open, "+" to record a set, swipe towards the start to delete, and an
 * optional leading [dragHandle] for lists that can be reordered.
 * The swipe only *asks* to delete: the card springs back and [onDelete] shows the confirmation,
 * so cancelling never leaves an invisible row behind.
 */
@Composable
fun SwipableExerciseListItem(
    exerciseName: String,
    lastPerformed: String?,
    onDelete: () -> Unit,
    onRecord: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    swipeEnabled: Boolean = true,
    progress: String? = null,
    progressDone: Boolean = false,
    dragHandle: (@Composable () -> Unit)? = null
) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = swipeEnabled,
        backgroundContent = {
            // Only paint the red layer mid-swipe, otherwise it bleeds through the translucent card
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) DeleteBackground()
        }
    ) {
        ExerciseCard(
            name = exerciseName,
            lastPerformed = lastPerformed,
            progress = progress,
            progressDone = progressDone,
            onClick = onClick,
            onRecord = onRecord,
            dragHandle = dragHandle
        )
    }
}

@Composable
private fun DeleteBackground() {
    val palette = MaterialTheme.glass
    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = Modifier
            .fillMaxSize()
            .clip(GlassDefaults.CardShape)
            .background(palette.error.copy(alpha = 0.25f))
            .padding(horizontal = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Delete", color = palette.error, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = palette.error)
        }
    }
}

@Composable
private fun ExerciseCard(
    name: String,
    lastPerformed: String?,
    progress: String?,
    progressDone: Boolean,
    onClick: () -> Unit,
    onRecord: () -> Unit,
    dragHandle: (@Composable () -> Unit)?
) {
    val palette = MaterialTheme.glass
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(palette, GlassDefaults.CardShape)
            .clickable(onClick = onClick)
            .padding(start = if (dragHandle != null) 4.dp else 18.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dragHandle?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = lastPerformed?.let { "Last done ${it.replaceFirstChar(Char::lowercase)}" } ?: "Not done yet",
                style = MaterialTheme.typography.bodySmall,
                color = palette.contentMuted
            )
        }
        if (progress != null) {
            GlassChip(text = progress, highlighted = progressDone)
            Spacer(Modifier.width(10.dp))
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(palette.accentGradient))
                .clickable(role = Role.Button, onClick = onRecord)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Record a set of $name", tint = palette.onAccent)
        }
    }
}
