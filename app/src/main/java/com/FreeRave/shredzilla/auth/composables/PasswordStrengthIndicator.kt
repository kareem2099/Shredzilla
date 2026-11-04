package com.FreeRave.shredzilla.auth.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FreeRave.shredzilla.ui.theme.glass

// ── Password strength levels ──────────────────────────────────────────────────

enum class PasswordStrength(
    val label: String,
    val segments: Int,       // how many of the 4 bars to fill
    val color: Color
) {
    EMPTY   ("",           0, Color.Transparent),
    WEAK    ("Weak",       1, Color(0xFFEF5350)), // red-400
    FAIR    ("Fair",       2, Color(0xFFFFA726)), // orange-400
    STRONG  ("Strong",    3, Color(0xFF66BB6A)), // green-400
    VERY_STRONG("Very Strong", 4, Color(0xFF26C6DA)) // cyan-400
}

/**
 * Evaluates password strength on 4 criteria:
 *   1. Length ≥ 8
 *   2. Contains digit
 *   3. Contains uppercase letter
 *   4. Contains special character
 *
 * Each criterion adds one segment. Results map:
 *   0 criteria met → WEAK (1 bar — shows the indicator isn't empty)
 *   1 criterion   → WEAK
 *   2 criteria    → FAIR
 *   3 criteria    → STRONG
 *   4 criteria    → VERY_STRONG
 */
fun evaluatePasswordStrength(password: String): PasswordStrength {
    if (password.isEmpty()) return PasswordStrength.EMPTY
    var score = 0
    if (password.length >= 8)                             score++
    if (password.any { it.isDigit() })                    score++
    if (password.any { it.isUpperCase() })                score++
    if (password.any { !it.isLetterOrDigit() })           score++
    return when (score) {
        0, 1 -> PasswordStrength.WEAK
        2    -> PasswordStrength.FAIR
        3    -> PasswordStrength.STRONG
        else -> PasswordStrength.VERY_STRONG
    }
}

// ── Composable ────────────────────────────────────────────────────────────────

private const val TOTAL_SEGMENTS = 4

@Composable
fun PasswordStrengthIndicator(
    password: String,
    modifier: Modifier = Modifier
) {
    val strength = evaluatePasswordStrength(password)
    if (strength == PasswordStrength.EMPTY) return  // hide when field is empty
    val emptySegmentColor = MaterialTheme.glass.content.copy(alpha = 0.15f)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Segmented bar (4 segments) ─────────────────────────────────────
        repeat(TOTAL_SEGMENTS) { index ->
            val segmentColor by animateColorAsState(
                targetValue   = if (index < strength.segments) strength.color else emptySegmentColor,
                animationSpec = tween(durationMillis = 300),
                label         = "segment_$index"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(segmentColor)
            )
        }

        // ── Label ──────────────────────────────────────────────────────────
        Text(
            text       = strength.label,
            color      = strength.color,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier.padding(start = 6.dp)
        )
    }
}
