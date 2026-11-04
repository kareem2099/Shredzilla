package com.FreeRave.shredzilla.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.glass.GlassBackground
import com.FreeRave.shredzilla.ui.glass.GlassIconButton
import com.FreeRave.shredzilla.ui.glass.GlassPrimaryButton
import com.FreeRave.shredzilla.ui.theme.glass

const val ONBOARDING_STEP_COUNT = 5

/**
 * Shared shell for every onboarding step: back button + progress, headline, scrollable body and a
 * call-to-action pinned to the bottom so it never scrolls away.
 */
@Composable
fun OnboardingScaffold(
    step: Int,
    title: String,
    subtitle: String,
    ctaText: String,
    onCta: () -> Unit,
    onBack: (() -> Unit)? = null,
    ctaEnabled: Boolean = true,
    ctaLoading: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = MaterialTheme.glass
    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp)
        ) {
            OnboardingTopBar(step = step, onBack = onBack)
            Spacer(Modifier.height(28.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = palette.content
            )
            Spacer(Modifier.height(8.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyLarge, color = palette.contentMuted)
            Spacer(Modifier.height(28.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content
            )
            Spacer(Modifier.height(16.dp))
            GlassPrimaryButton(text = ctaText, onClick = onCta, enabled = ctaEnabled, loading = ctaLoading)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OnboardingTopBar(step: Int, onBack: (() -> Unit)?) {
    val palette = MaterialTheme.glass
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (onBack != null) {
            GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
        } else {
            Spacer(Modifier.size(44.dp)) // keeps the progress bar aligned on step 1
        }
        StepProgress(step = step, modifier = Modifier.weight(1f))
        Text(
            text = "$step/$ONBOARDING_STEP_COUNT",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = palette.contentMuted
        )
    }
}

/** Segmented bar; completed and current steps fill with the accent gradient. */
@Composable
private fun StepProgress(step: Int, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.glass
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(ONBOARDING_STEP_COUNT) { index ->
            val reached = index < step
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        brush = if (reached) Brush.horizontalGradient(palette.accentGradient)
                        else SolidColor(palette.content.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}
