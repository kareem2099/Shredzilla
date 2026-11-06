package com.FreeRave.shredzilla.update

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.glass.GlassBackground
import com.FreeRave.shredzilla.ui.glass.GlassDialog
import com.FreeRave.shredzilla.ui.glass.GlassIconBadge
import com.FreeRave.shredzilla.ui.glass.GlassPrimaryButton
import com.FreeRave.shredzilla.ui.glass.glassSurface
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.glass

/** Drawn above the whole app: a blocking screen for required updates, otherwise a prompt and download progress. */
@Composable
fun AppUpdateOverlay(controller: AppUpdateController) {
    val status = controller.status
    ShredzillaTheme {
        when (status) {
            UpdateStatus.Required -> UpdateRequiredScreen(controller.message, onUpdate = controller::startRequiredUpdate)
            is UpdateStatus.Available -> GlassDialog(
                title = "New version on Google Play",
                message = controller.message.ifBlank { "A new Shredzilla is ready with fixes and new features. It downloads in the background while you train." },
                confirmText = "Update",
                dismissText = "Later",
                icon = rememberVectorPainter(Icons.Filled.SystemUpdate),
                onConfirm = controller::startFlexibleUpdate,
                onDismiss = controller::postpone
            )
            is UpdateStatus.Downloading, UpdateStatus.ReadyToInstall -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                UpdateProgressPill(status, onRestart = controller::installDownloaded)
            }
            UpdateStatus.UpToDate -> Unit
        }
    }
}

/** No way around it but updating: the version is below the minimum set in Remote Config. */
@Composable
private fun UpdateRequiredScreen(message: String, onUpdate: () -> Unit) {
    val palette = MaterialTheme.glass
    // Swallows every touch so nothing underneath can be used
    GlassBackground(modifier = Modifier.clickable(enabled = true, onClick = {}), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            GlassIconBadge(painter = rememberVectorPainter(Icons.Filled.SystemUpdate))
            Text("Update required", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = palette.content)
            Text(
                message.ifBlank {
                    "This version of Shredzilla is no longer supported. Update from Google Play to keep logging and syncing your workouts. Your data is safe."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = palette.contentMuted,
                textAlign = TextAlign.Center
            )
            GlassPrimaryButton(text = "Update on Google Play", onClick = onUpdate)
        }
    }
}

@Composable
private fun UpdateProgressPill(status: UpdateStatus, onRestart: () -> Unit) {
    val palette = MaterialTheme.glass
    val ready = status == UpdateStatus.ReadyToInstall
    Row(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(bottom = 96.dp) // above the bottom navigation bar
            .glassSurface(palette, RoundedCornerShape(50))
            .clickable(enabled = ready, onClick = onRestart)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (ready) {
            Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = palette.accent)
            Text("Update ready · Tap to restart", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = palette.content)
        } else {
            CircularProgressIndicator(color = palette.accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            val percent = (status as? UpdateStatus.Downloading)?.percent ?: 0
            Text("Downloading update… $percent%", style = MaterialTheme.typography.labelLarge, color = palette.content)
        }
    }
}
