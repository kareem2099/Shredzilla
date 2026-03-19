package com.FreeRave.shredzilla.screens.account.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
// import androidx.compose.material.icons.filled.Delete // Or other delete icons if they work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ImageSourceSheetContent(
    onTakePhotoClick: () -> Unit,
    onChooseFromGalleryClick: () -> Unit,
    onRemovePictureClick: () -> Unit,
    onCancelClick: () -> Unit,
    showRemoveOption: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text(
            "Update Profile Picture",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        )
        
        ImageSourceOptionItem(
            text = "Take Photo",
            icon = Icons.Filled.CameraAlt,
            onClick = onTakePhotoClick
        )
        HorizontalDivider()
        ImageSourceOptionItem(
            text = "Choose from Gallery",
            icon = Icons.Filled.PhotoLibrary,
            onClick = onChooseFromGalleryClick
        )
        if (showRemoveOption) {
            HorizontalDivider()
            ImageSourceOptionItem(
                text = "Remove Picture",
                icon = null, // No icon for Remove Picture for now to avoid unresolved reference
                onClick = onRemovePictureClick
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onCancelClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Text("Cancel", color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

// ImageSourceOptionItem has been moved to AccountScreenComposables.kt
// Remove its definition from here.
