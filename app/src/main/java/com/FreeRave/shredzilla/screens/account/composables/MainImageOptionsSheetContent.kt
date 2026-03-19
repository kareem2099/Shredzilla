package com.FreeRave.shredzilla.screens.account.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
// ImageSourceOptionItem is defined in AccountScreenComposables.kt, ensure it's imported if not in same package
// or defined here if this file is meant to be self-contained with its helpers.
// Assuming it's in AccountScreenComposables.kt and public/internal.
// No explicit import needed if they are in the same package 'com.FreeRave.shredzilla.screens.account.composables'
// and ImageSourceOptionItem is internal or public.
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun MainImageOptionsSheetContent(
    onChoosePictureClick: () -> Unit,
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
            "Profile Picture", // Or "Update Profile Picture"
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        )
        
        ImageSourceOptionItem( // Reusing ImageSourceOptionItem for consistency
            text = "Choose Picture",
            icon = Icons.Filled.Photo, // Generic picture icon
            onClick = onChoosePictureClick
        )
        if (showRemoveOption) {
            HorizontalDivider()
            ImageSourceOptionItem(
                text = "Remove Picture",
                icon = Icons.Filled.Delete, // Using standard Delete
                onClick = onRemovePictureClick
            )
        }
        HorizontalDivider()
        ImageSourceOptionItem(
            text = "Cancel",
            icon = Icons.Filled.Cancel, // Standard Cancel icon
            onClick = onCancelClick
        )
        // Or a Button for Cancel as before
        // Spacer(modifier = Modifier.height(16.dp))
        // Button(
        //     onClick = onCancelClick,
        //     modifier = Modifier.fillMaxWidth(),
        //     shape = RoundedCornerShape(8.dp),
        //     colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        // ) {
        //     Text("Cancel", color = MaterialTheme.colorScheme.onSecondaryContainer)
        // }
    }
}
