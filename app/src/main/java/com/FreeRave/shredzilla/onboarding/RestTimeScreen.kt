package com.FreeRave.shredzilla.onboarding

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.onboarding.components.OnboardingScaffold
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.glass

private const val REST_TIME_ARTICLE_URL = "https://www.aworkoutroutine.com/how-long-to-rest-between-sets-exercises/"

/** Step 3 — default rest between sets. The timer restarts after every logged set. */
@Composable
fun RestTimeScreen(
    onRestTimeSelected: (String) -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false
) {
    var selectedId by rememberSaveable { mutableStateOf(restOptions.first { it.isPopular }.id) }
    val palette = MaterialTheme.glass

    OnboardingScaffold(
        step = 3,
        title = "Rest between sets",
        subtitle = "We'll count down after every set and tell you when it's time to go again.",
        ctaText = "Continue",
        ctaLoading = isLoading,
        onBack = onBack,
        onCta = { onRestTimeSelected(selectedId) }
    ) {
        restOptions.forEach { option ->
            RestTimeOptionCard(
                option = option,
                isSelected = selectedId == option.id,
                onOptionSelected = { selectedId = option.id }
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = buildAnnotatedString {
                append("The right rest depends on your goal. ")
                withLink(
                    LinkAnnotation.Url(
                        REST_TIME_ARTICLE_URL,
                        TextLinkStyles(SpanStyle(color = palette.accent, fontWeight = FontWeight.SemiBold))
                    )
                ) { append("Learn more") }
            },
            style = MaterialTheme.typography.bodySmall,
            color = palette.contentMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun RestTimeScreenPreview() {
    ShredzillaTheme {
        RestTimeScreen(onRestTimeSelected = {}, onBack = {})
    }
}
