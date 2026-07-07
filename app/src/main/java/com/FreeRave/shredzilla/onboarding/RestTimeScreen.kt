package com.FreeRave.shredzilla.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FreeRave.shredzilla.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestTimeScreen(onRestTimeSelected: (String) -> Unit) {
    var selectedOptionId by remember { mutableStateOf(restOptions.first().id) }

    // Button scale animation
    var btnPressed by remember { mutableStateOf(false) }
    val btnScale by animateFloatAsState(
        targetValue = if (btnPressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "btnScale"
    )

    // Match the auth screens styling
    val btnGradient = Brush.horizontalGradient(
        listOf(AuthBtnGradientStart, AuthBtnGradientEnd)
    )

    // Reusing the general primary/accent colour
    val screenAccent = AuthBtnGradientEnd

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AuthBgTop, AuthBgBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Glowing Icon ───────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Purple40.copy(alpha = 0.22f))
                    .border(1.dp, screenAccent.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.AvTimer,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Set a Rest Time",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "With a rest timer, Shredzilla will let you know\nwhen you're ready to start your next set.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.60f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "The timer will reset after every set.",
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.45f)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // ── Options list ──────────────────────────────────────────────────
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                restOptions.forEach { option ->
                    RestTimeOptionCard(
                        option = option,
                        isSelected = selectedOptionId == option.id,
                        onOptionSelected = { selectedOptionId = option.id }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f).heightIn(min = 32.dp))

            // ── Educational Link (WCAG AA Compliant contrast) ──────────────────
            val annotatedString = buildAnnotatedString {
                append("Optimal rest time depends on many factors. ")
                val linkUrl = "https://www.aworkoutroutine.com/how-long-to-rest-between-sets-exercises/"
                val linkStyle = SpanStyle(
                    color = screenAccent,
                    fontWeight = FontWeight.Bold
                )
                withLink(LinkAnnotation.Url(url = linkUrl)) {
                    withStyle(style = linkStyle) {
                        append("Tap here")
                    }
                }
                append(" to learn more.")
            }

            Text(
                text = annotatedString,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.60f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Next Button ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .scale(btnScale)
                    .clip(RoundedCornerShape(14.dp))
                    .background(btnGradient)
            ) {
                Button(
                    onClick = { onRestTimeSelected(selectedOptionId) },
                    modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    elevation = null
                ) {
                    Text(
                        text = "Next →",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RestTimeScreenPreview() {
    ThemeManager.currentGenderTheme = "Male"
    ShredzillaTheme {
        RestTimeScreen {}
    }
}
