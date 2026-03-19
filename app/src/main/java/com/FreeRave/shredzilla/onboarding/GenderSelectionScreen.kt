package com.FreeRave.shredzilla.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip // Re-add clip
// Removed: import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape // Re-add Shape
import androidx.compose.ui.graphics.Outline // Re-add Outline
import androidx.compose.ui.graphics.Path // Re-add Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity // Re-add LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density // Re-add Density
import androidx.compose.ui.unit.LayoutDirection // Re-add LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FreeRave.shredzilla.R
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
// Removed direct imports of DarkText and DarkAccentPink

// Custom Shape for Top-Left Triangle (for Female)
class TopLeftTriangleShape : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

// Custom Shape for Bottom-Right Triangle (for Male)
class BottomRightTriangleShape : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width, 0f) // Start at top-right
            lineTo(size.width, size.height) // Go to bottom-right
            lineTo(0f, size.height) // Go to bottom-left
            close() // This creates a bottom-right triangle if the implicit start is top-right,
                     // but to be explicit for a \ split:
                     // moveTo(width, 0) -> lineTo(width, height) -> lineTo(0, height) -> close() is bottom right
                     // For a \ split, Male is bottom-right, Female is top-left.
                     // Male: (0, height) -> (width, height) -> (width, 0) -> close()
                     // Female: (0,0) -> (width,0) -> (0, height) -> close()
        }
        // Corrected BottomRightTriangleShape for a '\' split
        val correctedPath = Path().apply {
            moveTo(0f, size.height) // Bottom-left
            lineTo(size.width, size.height) // Bottom-right
            lineTo(size.width, 0f) // Top-right
            close()
        }
        return Outline.Generic(correctedPath)
    }
}


@Composable
fun GenderSelectionScreen(onGenderSelected: (String) -> Unit) {
    var selectedGender by remember { mutableStateOf<String?>(null) }
    var showImagePopup by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Female Section (Top-Left Triangle)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(TopLeftTriangleShape()) // Female uses TopLeftTriangleShape
                .clickable {
                    selectedGender = "Female"
                    showImagePopup = true
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.female_choose),
                contentDescription = "Female Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Text(
                "FEMALE",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, // Changed to onPrimaryContainer for better contrast on image potentially
                modifier = Modifier
                    .align(Alignment.TopStart) // Align to TopStart
                    .padding(start = 32.dp, top = 32.dp) // Add some padding from the edges
            )
        }

        // Male Section (Bottom-Right Triangle)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(BottomRightTriangleShape()) // Male uses BottomRightTriangleShape
                .clickable {
                    selectedGender = "Male"
                    showImagePopup = true
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.male_choose),
                contentDescription = "Male Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Text(
                "MALE",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, // Changed to onPrimaryContainer for better contrast
                modifier = Modifier
                    .align(Alignment.BottomEnd) // Align to BottomEnd
                    .padding(end = 32.dp, bottom = 32.dp) // Add some padding from the edges
            )
        }
        // No explicit diagonal line needed, the clipped shapes form the boundary.

        if (showImagePopup && selectedGender != null) {
            AlertDialog(
                shape = MaterialTheme.shapes.medium, // Added shape parameter
                onDismissRequest = {
                    showImagePopup = false
                    selectedGender = null
                },
                title = { Text(text = "$selectedGender Selected") },
                text = { Text("You've selected $selectedGender. Continue?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showImagePopup = false
                            onGenderSelected(selectedGender!!)
                            selectedGender = null
                        },
                        modifier = Modifier.padding(start = 4.dp) // Add padding to push it slightly from center/other button
                    ) { Text("Yes, Continue") }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showImagePopup = false
                            selectedGender = null
                        },
                        modifier = Modifier.padding(end = 4.dp) // Add padding to push it slightly from center/other button
                    ) { Text("Cancel") }
                }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun GenderSelectionScreenDarkPreview() {
    // ThemeManager.currentGenderTheme = null // Or set to "Male" / "Female" to see specific gender colors
    // ThemeManager.themePreferenceMale = ThemeSetting.DARK // Example to force dark for male
    // ThemeManager.themePreferenceFemale = ThemeSetting.DARK // Example to force dark for female
    ShredzillaTheme {
        GenderSelectionScreen(onGenderSelected = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun GenderSelectionScreenLightPreview() {
    // ThemeManager.currentGenderTheme = null
    // ThemeManager.themePreferenceMale = ThemeSetting.LIGHT // Example to force light for male
    // ThemeManager.themePreferenceFemale = ThemeSetting.LIGHT // Example to force light for female
    ShredzillaTheme {
        GenderSelectionScreen(onGenderSelected = {})
    }
}
