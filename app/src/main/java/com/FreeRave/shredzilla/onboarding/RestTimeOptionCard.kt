package com.FreeRave.shredzilla.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.theme.*

@Composable
fun RestTimeOptionCard(
    option: RestOption,
    isSelected: Boolean,
    onOptionSelected: () -> Unit
) {
    val currentGenderTheme = ThemeManager.currentGenderTheme
    val darkTheme = isSystemInDarkTheme()

    // Determine the option's highlight color based on option.id, gender theme, and light/dark mode
    val optionHighlightColor = when (currentGenderTheme) {
        "Male" -> when (option.id) {
            "1min" -> if (darkTheme) MaleOption1RedDark else MaleOption1RedLight
            "2min" -> if (darkTheme) MaleOption2OrangeDark else MaleOption2OrangeLight
            "5min" -> if (darkTheme) MaleOption3GreenDark else MaleOption3GreenLight
            else -> MaterialTheme.colorScheme.primary // Fallback for Male theme
        }
        "Female" -> when (option.id) {
            "1min" -> if (darkTheme) FemaleOption1PinkDark else FemaleOption1PinkLight
            "2min" -> if (darkTheme) FemaleOption2PurpleDark else FemaleOption2PurpleLight
            "5min" -> if (darkTheme) FemaleOption3TealDark else FemaleOption3TealLight
            else -> MaterialTheme.colorScheme.primary // Fallback for Female theme
        }
        else -> MaterialTheme.colorScheme.primary // Default theme fallback
    }

    val cardBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (darkTheme) 0.3f else 0.6f)
    val cardBorderColorIfSelected = optionHighlightColor
    val cardBorderColorIfNotSelected = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val cardBorderSize = if (isSelected) 2.dp else 1.dp

    val titleColor = MaterialTheme.colorScheme.onSurface
    val iconTintColor = if (isSelected) optionHighlightColor else MaterialTheme.colorScheme.onSurfaceVariant
    val radioButtonSelectedColor = optionHighlightColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onOptionSelected
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor // Keep background consistent or subtly tint if selected
            // containerColor = if (isSelected) optionHighlightColor.copy(alpha = 0.08f) else cardBackgroundColor // Alternative: subtle tint on selection
        ),
        border = BorderStroke(cardBorderSize, if (isSelected) cardBorderColorIfSelected else cardBorderColorIfNotSelected)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onOptionSelected,
                colors = RadioButtonDefaults.colors(
                    selectedColor = radioButtonSelectedColor,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    option.title, // e.g., "minute 1"
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor // Title color is standard
                )

                val descriptionAnnotatedString = buildAnnotatedString {
                    val descriptionParts = option.description.split("\n")
                    val highlightPhrase = when(option.id) {
                        "1min" -> "Quick Workouts"
                        "2min" -> "Build Muscle"
                        "5min" -> "Increase Strength"
                        else -> ""
                    }

                    descriptionParts.forEachIndexed { index, part ->
                        if (index > 0) append("\n")
                        if (isSelected && part.contains(highlightPhrase) && highlightPhrase.isNotEmpty()) {
                            val phraseIndex = part.indexOf(highlightPhrase)
                            if (phraseIndex != -1) {
                                append(part.substring(0, phraseIndex))
                                withStyle(style = SpanStyle(color = optionHighlightColor)) { // Highlight with option's specific color
                                    append(highlightPhrase)
                                }
                                append(part.substring(phraseIndex + highlightPhrase.length))
                            } else {
                                append(part)
                            }
                        } else {
                            append(part)
                        }
                    }
                }
                Text(
                    text = descriptionAnnotatedString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // Standard color for description
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = option.icon,
                contentDescription = option.title,
                modifier = Modifier.size(24.dp),
                tint = iconTintColor // Icon tint changes on selection to option's highlight color
            )
        }
    }
}
