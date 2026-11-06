package com.FreeRave.shredzilla.screens.account

import android.Manifest
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.screens.account.composables.ProfileAvatar
import com.FreeRave.shredzilla.ui.glass.GlassBottomSheet
import com.FreeRave.shredzilla.ui.glass.GlassDialog
import com.FreeRave.shredzilla.ui.glass.GlassSettingsGroup
import com.FreeRave.shredzilla.ui.glass.GlassSettingsRow
import com.FreeRave.shredzilla.ui.glass.GlassSubpageScaffold
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.ImageStorageUtils
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException

private enum class AccountDialog { NONE, REMOVE_PHOTO, DELETE_ACCOUNT }

/** Profile (name + photo), sign out and account deletion. The photo is stored on-device only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToUpdateUsername: () -> Unit,
    userEmail: String?,
    userName: String?,
    profileImageUrl: String?,
    isDeletingAccount: Boolean,
    onUpdateProfileImagePathInFirestore: (String?) -> Unit,
    onDeleteAccount: (() -> Unit, (Exception) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val palette = MaterialTheme.glass
    var imagePath by remember { mutableStateOf(profileImageUrl) }
    var showPhotoSheet by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf(AccountDialog.NONE) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(profileImageUrl) { imagePath = profileImageUrl }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    // Gallery and camera both end here: copy into app storage, drop the old file, sync the path
    fun usePhoto(uri: Uri, filePrefix: String) {
        val savedPath = ImageStorageUtils.saveImageToInternalStorage(context, uri, filePrefix)
        if (savedPath == null) {
            toast("Couldn't save that photo.")
            return
        }
        imagePath?.let(ImageStorageUtils::deleteImageFromInternalStorage)
        imagePath = savedPath
        onUpdateProfileImagePathInFirestore(savedPath)
        toast("Profile photo updated.")
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { usePhoto(it, "user_profile_gallery_") }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) usePhoto(uri, "user_profile_camera_")
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) return@rememberLauncherForActivityResult toast("Camera permission denied.")
        val photoFile = ImageStorageUtils.createImageFile(context, "user_profile_camera_")
            ?: return@rememberLauncherForActivityResult toast("Couldn't create a file for the photo.")
        ImageStorageUtils.getUriForFile(context, photoFile).also {
            pendingCameraUri = it
            cameraLauncher.launch(it)
        }
    }

    GlassSubpageScaffold(title = "Account", eyebrow = "Settings", onBack = onNavigateBack) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.clickable(role = Role.Button) { showPhotoSheet = true }) {
                ProfileAvatar(imagePath = imagePath, name = userName ?: userEmail, size = 108.dp)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(34.dp)
                        .background(palette.accent, CircleShape)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Change photo", tint = palette.onAccent, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                text = userName?.takeIf { it.isNotBlank() } ?: "Set your name",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = palette.content,
                modifier = Modifier.offset(y = 8.dp)
            )
            if (userEmail != null) {
                Text(userEmail, style = MaterialTheme.typography.bodyMedium, color = palette.contentMuted, modifier = Modifier.offset(y = 8.dp))
            }
        }

        GlassSettingsGroup(title = "Profile") {
            GlassSettingsRow(
                icon = Icons.Filled.Person,
                title = "Name",
                value = userName?.takeIf { it.isNotBlank() } ?: "Not set",
                onClick = onNavigateToUpdateUsername
            )
            GlassSettingsRow(icon = Icons.Filled.PhotoCamera, title = "Profile photo", onClick = { showPhotoSheet = true })
        }

        GlassSettingsGroup(title = "Session") {
            GlassSettingsRow(icon = Icons.AutoMirrored.Filled.Logout, title = "Sign out", onClick = onSignOut, showChevron = false)
        }

        GlassSettingsGroup(title = "Danger zone") {
            GlassSettingsRow(
                icon = Icons.Filled.DeleteForever,
                title = "Delete account",
                subtitle = "Permanently removes your account and data",
                tint = palette.error,
                onClick = { dialog = AccountDialog.DELETE_ACCOUNT },
                showChevron = false
            )
        }
    }

    if (showPhotoSheet) {
        GlassBottomSheet(title = "Profile photo", onDismiss = { showPhotoSheet = false }) {
            GlassSettingsRow(
                icon = Icons.Filled.PhotoCamera,
                title = "Take photo",
                showChevron = false,
                onClick = {
                    showPhotoSheet = false
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )
            GlassSettingsRow(
                icon = Icons.Filled.PhotoLibrary,
                title = "Choose from gallery",
                showChevron = false,
                onClick = {
                    showPhotoSheet = false
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            )
            if (imagePath != null) {
                GlassSettingsRow(
                    icon = Icons.Filled.DeleteOutline,
                    title = "Remove photo",
                    tint = palette.error,
                    showChevron = false,
                    onClick = {
                        showPhotoSheet = false
                        dialog = AccountDialog.REMOVE_PHOTO
                    }
                )
            }
        }
    }

    when (dialog) {
        AccountDialog.REMOVE_PHOTO -> GlassDialog(
            title = "Remove photo?",
            message = "Your profile will show your initial instead.",
            confirmText = "Remove",
            destructive = true,
            icon = rememberVectorPainter(Icons.Filled.DeleteOutline),
            onConfirm = {
                imagePath?.let(ImageStorageUtils::deleteImageFromInternalStorage)
                imagePath = null
                onUpdateProfileImagePathInFirestore(null)
                dialog = AccountDialog.NONE
                toast("Profile photo removed.")
            },
            onDismiss = { dialog = AccountDialog.NONE }
        )
        AccountDialog.DELETE_ACCOUNT -> GlassDialog(
            title = "Delete account?",
            message = "This permanently deletes your account and workout history. It can't be undone.",
            confirmText = "Delete forever",
            destructive = true,
            confirmLoading = isDeletingAccount,
            icon = rememberVectorPainter(Icons.Filled.DeleteForever),
            onConfirm = {
                onDeleteAccount(
                    {
                        toast("Account deleted.")
                        dialog = AccountDialog.NONE
                        onSignOut()
                    },
                    { e ->
                        Log.e("AccountScreen", "Error deleting account", e)
                        toast(
                            if (e is FirebaseAuthRecentLoginRequiredException) "For security, sign out and back in, then try again."
                            else "Couldn't delete account: ${e.message}"
                        )
                        dialog = AccountDialog.NONE
                    }
                )
            },
            onDismiss = { dialog = AccountDialog.NONE }
        )
        AccountDialog.NONE -> Unit
    }

    if (isDeletingAccount && dialog == AccountDialog.NONE) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(enabled = false, onClick = {}),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = palette.accent)
        }
    }
}

@Preview
@Composable
private fun AccountScreenPreview() {
    ShredzillaTheme {
        AccountScreen(
            onNavigateBack = {}, onSignOut = {}, onNavigateToUpdateUsername = {},
            userEmail = "alex@example.com", userName = "Alex Doe", profileImageUrl = null,
            isDeletingAccount = false, onUpdateProfileImagePathInFirestore = {}, onDeleteAccount = { _, _ -> }
        )
    }
}
