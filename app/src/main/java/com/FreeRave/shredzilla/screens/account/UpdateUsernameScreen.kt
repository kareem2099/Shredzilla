package com.FreeRave.shredzilla.screens.account

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.auth.FirebaseEmailPasswordAuth // Assuming this has update logic or access to auth instance
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateUsernameScreen(
    onNavigateBack: () -> Unit,
    currentUsername: String?
) {
    var username by remember { mutableStateOf(currentUsername ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update Username") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (username.isBlank()) {
                        Toast.makeText(context, "Username cannot be empty.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                        if (firebaseUser == null) {
                            Toast.makeText(context, "Not logged in.", Toast.LENGTH_SHORT).show()
                            isLoading = false
                            return@launch
                        }

                        try {
                            // Update Firestore
                            Firebase.firestore.collection("users").document(firebaseUser.uid)
                                .update("name", username)
                                .await()

                            // Update Firebase Auth display name
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(username)
                                .build()
                            firebaseUser.updateProfile(profileUpdates).await()
                            
                            Toast.makeText(context, "Username updated successfully!", Toast.LENGTH_SHORT).show()
                            isLoading = false
                            onNavigateBack()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save Username")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UpdateUsernameScreenPreview() {
    ShredzillaTheme {
        UpdateUsernameScreen(onNavigateBack = {}, currentUsername = "Old Username")
    }
}
