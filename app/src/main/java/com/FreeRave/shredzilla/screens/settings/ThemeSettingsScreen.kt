package com.FreeRave.shredzilla.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.pro.LocalProAccess
import com.FreeRave.shredzilla.pro.ProPill
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.theme.AccentTheme
import com.FreeRave.shredzilla.ui.theme.glass
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.FreeRave.shredzilla.ui.glass.GlassFootnote
import com.FreeRave.shredzilla.ui.glass.GlassOptionCard
import com.FreeRave.shredzilla.ui.glass.GlassSubpageScaffold
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme

/** Stored by [name] in Firestore ("themePreferenceMale"/"themePreferenceFemale"), so keep names stable. */
enum class ThemeSetting(val displayName: String, val description: String, val icon: ImageVector) {
    LIGHT("Light", "Bright glass, easy in daylight", Icons.Filled.LightMode),
    DARK("Dark", "Deep glass, easy on the eyes", Icons.Filled.DarkMode),
    SYSTEM("System default", "Follow your phone's setting", Icons.Filled.BrightnessAuto)
}

/** Theme picker; applies instantly so the user sees the result while choosing. */
@Composable
fun ThemeSettingsScreen(
    onNavigateBack: () -> Unit,
    currentThemeSetting: ThemeSetting,
    onThemeSettingChange: (ThemeSetting) -> Unit,
    accentTheme: AccentTheme? = null,
    onAccentThemeChange: (AccentTheme?) -> Unit = {}
) {
    var selected by remember { mutableStateOf(currentThemeSetting) }
    val pro = LocalProAccess.current

    GlassSubpageScaffold(title = "Theme", eyebrow = "Settings", onBack = onNavigateBack) {
        ThemeSetting.entries.forEach { setting ->
            GlassOptionCard(
                icon = setting.icon,
                title = setting.displayName,
                subtitle = setting.description,
                selected = selected == setting,
                onClick = {
                    selected = setting
                    onThemeSettingChange(setting)
                }
            )
        }
        GlassFootnote("Light and dark follow the profile you picked during setup.")

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Accent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.glass.content)
                ProPill()
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                AccentSwatch(
                    label = "Profile",
                    colors = listOf(MaterialTheme.glass.accent, MaterialTheme.glass.accent),
                    selected = accentTheme == null || !pro.isPro,
                    locked = false,
                    onClick = { onAccentThemeChange(null) }
                )
                AccentTheme.entries.forEach { theme ->
                    AccentSwatch(
                        label = theme.title,
                        colors = theme.gradient,
                        selected = pro.isPro && accentTheme == theme,
                        locked = !pro.isPro,
                        onClick = { if (pro.isPro) onAccentThemeChange(theme) else pro.openPro() }
                    )
                }
            }
            Text(
                if (pro.isPro) "Recolors buttons, highlights and the background glow across the app."
                else "Pro recolors buttons, highlights and the background glow. Your pick is kept if Pro time runs out.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.glass.contentMuted
            )
        }
    }
}

@Composable
private fun AccentSwatch(label: String, colors: List<Color>, selected: Boolean, locked: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.glass
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .border(2.dp, if (selected) palette.content else Color.Transparent, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(colors))
        ) {
            when {
                selected -> Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(20.dp))
                locked -> Icon(Icons.Filled.Lock, contentDescription = "Pro", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(16.dp))
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) palette.content else palette.contentMuted)
    }
}

@Preview
@Composable
private fun ThemeSettingsScreenPreview() {
    ShredzillaTheme {
        ThemeSettingsScreen(onNavigateBack = {}, currentThemeSetting = ThemeSetting.SYSTEM, onThemeSettingChange = {})
    }
}
