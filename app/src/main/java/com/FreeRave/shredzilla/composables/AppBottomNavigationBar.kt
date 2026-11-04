package com.FreeRave.shredzilla.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.FreeRave.shredzilla.navigation.BottomNavItem
import com.FreeRave.shredzilla.navigation.bottomNavItems
import com.FreeRave.shredzilla.ui.glass.glassSurface
import com.FreeRave.shredzilla.ui.theme.glass

private val BarShape = RoundedCornerShape(50)

/** Floating frosted pill; the selected tab expands into an accent capsule with its label. */
@Composable
fun AppBottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.glass
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(64.dp)
            // Mostly opaque underlay keeps the bar readable over whatever scrolls behind it
            .background(palette.backgroundBottom.copy(alpha = 0.85f), BarShape)
            .glassSurface(palette, BarShape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        bottomNavItems.forEach { item ->
            NavTab(
                item = item,
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // Keep a single copy of each tab and restore its scroll/state on return
                            navController.graph.startDestinationRoute?.let { popUpTo(it) { saveState = true } }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavTab(item: BottomNavItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.glass
    val capsuleAlpha by animateFloatAsState(if (selected) 1f else 0f, label = "tabCapsule")
    val tint by animateColorAsState(if (selected) palette.onAccent else palette.contentMuted, label = "tabTint")

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(BarShape)
            .clickable(role = Role.Tab, onClick = onClick)
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = capsuleAlpha }
                .background(Brush.horizontalGradient(palette.accentGradient), BarShape)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(22.dp))
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row {
                    Spacer(Modifier.width(8.dp))
                    Text(item.label, color = tint, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
