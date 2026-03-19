package com.FreeRave.shredzilla.screens.sets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.auth.FirebaseEmailPasswordAuth
import com.FreeRave.shredzilla.models.initialGlobalExerciseList // Import global list
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetsScreen(
    modifier: Modifier = Modifier,
    authManager: FirebaseEmailPasswordAuth // To fetch user data
) {
    var selectedExerciseNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Fetch selected exercises when the screen is composed
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val userId = authManager.getCurrentUser()?.uid
            if (userId != null) {
                val userDataResult = authManager.getUserData(userId)
                if (userDataResult.isSuccess) {
                    val userData = userDataResult.getOrNull()
                    @Suppress("UNCHECKED_CAST")
                    val exerciseIds = userData?.get("initialExercises") as? List<String> ?: emptyList()
                    selectedExerciseNames = exerciseIds.mapNotNull { id ->
                        initialGlobalExerciseList.find { it.id == id }?.name
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Selected Exercises") },
            text = {
                if (selectedExerciseNames.isNotEmpty()) {
                    LazyColumn {
                        items(selectedExerciseNames) { name ->
                            Text(name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    Text("No exercises selected yet.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Setgraph", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Handle settings click */ }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* TODO: Handle New List click */ }
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "...New List",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "New List",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (selectedExerciseNames.isNotEmpty()) showDialog = true }
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedExerciseNames.size.toString(), // Dynamic count
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Exercises",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = "Exercises",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SetsScreenDarkPreview() {
    ShredzillaTheme { // Removed darkTheme = true
        Surface {
            // Preview won't have authManager, so data fetching won't work.
            // Consider a preview-specific composable or mock data for better previews.
            SetsScreen(authManager = FirebaseEmailPasswordAuth()) // Dummy for preview
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
fun SetsScreenLightPreview() {
    ShredzillaTheme { // Removed darkTheme = false
        Surface {
            SetsScreen(authManager = FirebaseEmailPasswordAuth()) // Dummy for preview
        }
    }
}
