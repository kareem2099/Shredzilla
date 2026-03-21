package com.FreeRave.shredzilla.screens.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.Manifest 
import android.util.Log 
import android.widget.Toast 
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase 
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory 
import android.net.Uri 
import androidx.activity.compose.rememberLauncherForActivityResult 
import androidx.activity.result.contract.ActivityResultContracts 
import com.FreeRave.shredzilla.R
import com.FreeRave.shredzilla.navigation.AppRoutes
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.utils.ImageStorageUtils 
import kotlinx.coroutines.launch
import com.FreeRave.shredzilla.screens.account.composables.AccountActionItem
import com.FreeRave.shredzilla.screens.account.composables.ImageSourceSheetContent 
import com.FreeRave.shredzilla.screens.account.composables.MainImageOptionsSheetContent


internal enum class ProfileImageSheetStage {
    NONE, 
    MAIN_OPTIONS, 
    SOURCE_SELECTION 
}

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentSheetStage by remember { mutableStateOf(ProfileImageSheetStage.NONE) }
    
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showRemoveImageConfirmDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var localProfileImagePath by remember { mutableStateOf<String?>(profileImageUrl) }
    var tempImageUriForCamera by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(profileImageUrl) {
        localProfileImagePath = profileImageUrl
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStorageUtils.saveImageToInternalStorage(context, uri, "user_profile_gallery_")
            if (savedPath != null) {
                localProfileImagePath?.let { ImageStorageUtils.deleteImageFromInternalStorage(it) }
                localProfileImagePath = savedPath
                onUpdateProfileImagePathInFirestore(savedPath)
                Toast.makeText(context, "Profile image updated.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to save image from gallery.", Toast.LENGTH_SHORT).show()
            }
        }
        scope.launch { sheetState.hide() }.invokeOnCompletion { currentSheetStage = ProfileImageSheetStage.NONE }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempImageUriForCamera?.let { uri ->
                val savedPath = ImageStorageUtils.saveImageToInternalStorage(context, uri, "user_profile_camera_")
                 if (savedPath != null) {
                    localProfileImagePath?.let { ImageStorageUtils.deleteImageFromInternalStorage(it) }
                    localProfileImagePath = savedPath
                    onUpdateProfileImagePathInFirestore(savedPath)
                    Toast.makeText(context, "Profile image updated.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to save image from camera.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
             Toast.makeText(context, "Failed to capture image.", Toast.LENGTH_SHORT).show()
        }
        tempImageUriForCamera = null 
        scope.launch { sheetState.hide() }.invokeOnCompletion { currentSheetStage = ProfileImageSheetStage.NONE }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val photoFile = ImageStorageUtils.createImageFile(context, "user_profile_camera_")
            if (photoFile != null) {
                tempImageUriForCamera = ImageStorageUtils.getUriForFile(context, photoFile)
                tempImageUriForCamera?.let { cameraLauncher.launch(it) }
            } else {
                Toast.makeText(context, "Could not create file for photo.", Toast.LENGTH_SHORT).show()
                scope.launch { sheetState.hide() }.invokeOnCompletion { currentSheetStage = ProfileImageSheetStage.NONE }
            }
        } else {
            Toast.makeText(context, "Camera permission denied.", Toast.LENGTH_SHORT).show()
            scope.launch { sheetState.hide() }.invokeOnCompletion { currentSheetStage = ProfileImageSheetStage.NONE }
        }
    }

    val galleryPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            galleryLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Storage permission denied.", Toast.LENGTH_SHORT).show()
            scope.launch { sheetState.hide() }.invokeOnCompletion { currentSheetStage = ProfileImageSheetStage.NONE }
        }
    }
    
    val painter = localProfileImagePath?.let { path ->
        ImageStorageUtils.loadImageFromPath(path)?.asImageBitmap()?.let {
            androidx.compose.ui.graphics.painter.BitmapPainter(it)
        }
    } ?: painterResource(id = R.drawable.male_choose) 

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ... (rest of the UI: Image, Text for username/email, Cards for actions) ...
            // This part remains the same as before
            Spacer(modifier = Modifier.height(16.dp))
            Image(painter = painter, contentDescription = "Profile Image", modifier = Modifier.size(100.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = userName ?: "Username not set", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = userEmail ?: "email@example.com", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))

            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))) {
                AccountActionItem(text = "Update Username", icon = if (userName.isNullOrEmpty() || userName == "Username not set") Icons.Filled.Info else null, onClick = onNavigateToUpdateUsername)
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                AccountActionItem(text = "Update Profile Image", icon = null) { currentSheetStage = ProfileImageSheetStage.MAIN_OPTIONS }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))) {
                AccountActionItem(text = "Delete Account", icon = null, textColor = MaterialTheme.colorScheme.error) { showDeleteConfirmDialog = true }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                AccountActionItem(text = "Sign Out", icon = null, textColor = MaterialTheme.colorScheme.primary, onClick = onSignOut)
            }
        }
    }

    if (currentSheetStage != ProfileImageSheetStage.NONE) {
        ModalBottomSheet(
            onDismissRequest = { currentSheetStage = ProfileImageSheetStage.NONE },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ) {
            when (currentSheetStage) {
                ProfileImageSheetStage.MAIN_OPTIONS -> {
                    MainImageOptionsSheetContent(
                        onChoosePictureClick = { currentSheetStage = ProfileImageSheetStage.SOURCE_SELECTION },
                        onRemovePictureClick = { 
                            scope.launch { sheetState.hide() }.invokeOnCompletion { 
                                currentSheetStage = ProfileImageSheetStage.NONE 
                                showRemoveImageConfirmDialog = true
                            }
                        },
                        onCancelClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { currentSheetStage = ProfileImageSheetStage.NONE } },
                        showRemoveOption = localProfileImagePath != null
                    )
                }
                ProfileImageSheetStage.SOURCE_SELECTION -> {
                    ImageSourceSheetContent(
                        onTakePhotoClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        onChooseFromGalleryClick = { galleryPermissionLauncher.launch(galleryPermission) },
                        onRemovePictureClick = {}, // This is now handled by MainImageOptionsSheetContent
                        onCancelClick = { currentSheetStage = ProfileImageSheetStage.MAIN_OPTIONS }, // Go back to main options
                        showRemoveOption = false // Remove option is in MainImageOptionsSheetContent
                    )
                }
                ProfileImageSheetStage.NONE -> {} // Should not happen if showProfileImageSheet is true
            }
        }
    }
    
    if (showRemoveImageConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveImageConfirmDialog = false },
            title = { Text("Remove Picture") },
            text = { Text("Are you sure you want to remove your profile picture?") },
            confirmButton = {
                TextButton(onClick = {
                    localProfileImagePath?.let { path ->
                        ImageStorageUtils.deleteImageFromInternalStorage(path)
                        onUpdateProfileImagePathInFirestore(null)
                        Toast.makeText(context, "Profile image removed.", Toast.LENGTH_SHORT).show()
                    }
                    localProfileImagePath = null
                    showRemoveImageConfirmDialog = false
                }) { Text("Yes, Remove") }
            },
            dismissButton = { TextButton(onClick = { showRemoveImageConfirmDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDeleteConfirmDialog) {
        // ... (Delete Account AlertDialog remains the same) ...
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) showDeleteConfirmDialog = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to permanently delete your account? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAccount(
                            { // onSuccess
                                Toast.makeText(context, "Account deleted successfully.", Toast.LENGTH_SHORT).show()
                                showDeleteConfirmDialog = false
                                onSignOut()
                            },
                            { e -> // onError
                                Toast.makeText(context, "Error deleting account: ${e.message}", Toast.LENGTH_LONG).show()
                                Log.e("AccountScreen", "Error deleting account", e)
                                if (e is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                                    Toast.makeText(context, "Please sign out and sign back in to delete your account.", Toast.LENGTH_LONG).show()
                                }
                                showDeleteConfirmDialog = false
                            }
                        )
                    },
                    enabled = !isDeletingAccount
                ) {
                    if (isDeletingAccount) { CircularProgressIndicator(modifier = Modifier.size(20.dp)) } 
                    else { Text("Yes, Delete", color = MaterialTheme.colorScheme.error) }
                }
            },
            dismissButton = { TextButton(onClick = { if (!isDeletingAccount) showDeleteConfirmDialog = false }) { Text("Cancel") } }
        )
    }

    if (isDeletingAccount) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(enabled = false, onClick = {}),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AccountScreenDarkPreview() {
    ShredzillaTheme { // Removed darkTheme = true
        AccountScreen(onNavigateBack = {}, onSignOut = {}, onNavigateToUpdateUsername = {}, userEmail = "kareem209907@gmail.com", userName = "Kareem Ehab", profileImageUrl = null, isDeletingAccount = false, onUpdateProfileImagePathInFirestore = {}, onDeleteAccount = { _, _ -> })
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
fun AccountScreenLightPreview() {
    ShredzillaTheme { // Removed darkTheme = false
        AccountScreen(onNavigateBack = {}, onSignOut = {}, onNavigateToUpdateUsername = {}, userEmail = "kareem209907@gmail.com", userName = null, profileImageUrl = null, isDeletingAccount = false, onUpdateProfileImagePathInFirestore = {}, onDeleteAccount = { _, _ -> })
    }
}
