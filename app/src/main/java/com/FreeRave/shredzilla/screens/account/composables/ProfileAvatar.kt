package com.FreeRave.shredzilla.screens.account.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.ImageStorageUtils

/**
 * Round profile picture from the locally stored file, or the name's first letter on the accent
 * gradient when there's no photo. The bitmap is decoded once per path, not on every recomposition.
 */
@Composable
fun ProfileAvatar(imagePath: String?, name: String?, size: Dp, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.glass
    val bitmap = remember(imagePath) { imagePath?.let { ImageStorageUtils.loadImageFromPath(it)?.asImageBitmap() } }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(palette.accentGradient))
            .border(2.dp, palette.border, CircleShape)
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Text(
                text = name?.trim()?.firstOrNull()?.uppercase() ?: "?",
                color = palette.onAccent,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp
            )
        }
    }
}
