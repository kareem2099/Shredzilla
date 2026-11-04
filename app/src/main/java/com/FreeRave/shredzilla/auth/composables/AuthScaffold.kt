package com.FreeRave.shredzilla.auth.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.R
import com.FreeRave.shredzilla.ui.glass.GlassBackground
import com.FreeRave.shredzilla.ui.glass.GlassIconBadge
import com.FreeRave.shredzilla.ui.theme.glass

/** Shared shell for Login / Register / Forgot Password: glass backdrop, brand header, scrollable body. */
@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    icon: Painter = painterResource(R.drawable.ic_dumbbell),
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = MaterialTheme.glass
    GlassBackground {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    // Min height = viewport so short content stays vertically centred but can still scroll
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                GlassIconBadge(painter = icon)
                Spacer(Modifier.height(20.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.content,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.contentMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                content()
            }
        }
    }
}

/** "Don't have an account? **Register**" style footer that switches between auth screens. */
@Composable
fun AuthSwitchPrompt(
    prompt: String,
    action: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val palette = MaterialTheme.glass
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(prompt, color = palette.contentMuted, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onClick, enabled = enabled) {
            Text(action, color = palette.accent, fontWeight = FontWeight.Bold)
        }
    }
}
