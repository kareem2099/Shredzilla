package com.FreeRave.shredzilla.navigation

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Import LocalContext
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.FreeRave.shredzilla.FitnessAppScreen // Import FitnessAppScreen
import com.FreeRave.shredzilla.auth.*
import com.FreeRave.shredzilla.composables.AppBottomNavigationBar
import com.FreeRave.shredzilla.onboarding.*
import com.FreeRave.shredzilla.screens.sets.SetGraphScreen // Import the new SetGraphScreen
import com.FreeRave.shredzilla.screens.exercises.AddExerciseScreen
import com.FreeRave.shredzilla.screens.exercises.ExercisesScreen
import com.FreeRave.shredzilla.screens.today.TodayScreen
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.ThemeManager
import com.FreeRave.shredzilla.utils.NotificationUtils // Import NotificationUtils
import kotlinx.coroutines.launch

@Composable
fun AppNavigationHost(
    navController: NavHostController,
    startDestination: String,
    firebaseEmailAuthManager: FirebaseEmailPasswordAuth,
    firebaseGoogleAuthManager: FirebaseGoogleAuth,
    googleSignInLauncher: ActivityResultLauncher<Intent>,
    activityContext: ComponentActivity
) {
    var isLoading by remember { mutableStateOf(false) }

    // ShredzillaTheme no longer takes genderTheme or darkTheme directly
    ShredzillaTheme { 
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            NavHost(navController = navController, startDestination = startDestination) {
                navigation(startDestination = AppRoutes.LOGIN, route = AppRoutes.AUTH) {
                    composable(AppRoutes.LOGIN) {
                        LoginScreen(
                            onLoginClick = { email, password ->
                                activityContext.lifecycleScope.launch {
                                    isLoading = true
                                    val result = firebaseEmailAuthManager.signInUser(email, password)
                                    if (result.isSuccess) {
                                        val user = result.getOrNull()
                                        if (user != null) {
                                            val userDataResult = firebaseEmailAuthManager.getUserData(user.uid)
                                            ThemeManager.currentGenderTheme = (userDataResult.getOrNull()?.get("gender") as? String)
                                            // Check all onboarding fields
                                            if (userDataResult.isSuccess &&
                                                userDataResult.getOrNull()?.containsKey("gender") == true &&
                                                userDataResult.getOrNull()?.containsKey("age") == true &&
                                                userDataResult.getOrNull()?.containsKey("restTimePreference") == true &&
                                                userDataResult.getOrNull()?.containsKey("initialExercises") == true &&
                                                userDataResult.getOrNull()?.containsKey("weeklyGoalFrequency") == true) {
                                                navController.navigate(AppRoutes.FITNESS_MAIN) { popUpTo(AppRoutes.AUTH) { inclusive = true } }
                                            } else {
                                                navController.navigate(AppRoutes.ONBOARDING) { popUpTo(AppRoutes.AUTH) { inclusive = true } }
                                            }
                                        }
                                    } else {
                                        Toast.makeText(activityContext, "Login failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                    isLoading = false
                                }
                            },
                            onGoogleSignInClick = {
                                firebaseGoogleAuthManager.signInWithGoogle(googleSignInLauncher)
                            },
                            onNavigateToRegister = { navController.navigate(AppRoutes.REGISTER) }
                        )
                    }
                    composable(AppRoutes.REGISTER) {
                        RegisterScreen(
                            onRegisterClick = { name, email, password, _ ->
                                activityContext.lifecycleScope.launch {
                                    isLoading = true
                                    val result = firebaseEmailAuthManager.createUser(name, email, password)
                                    if (result.isSuccess) {
                                        ThemeManager.currentGenderTheme = null
                                        navController.navigate(AppRoutes.ONBOARDING) { popUpTo(AppRoutes.AUTH) { inclusive = true } }
                                    } else {
                                        Toast.makeText(activityContext, "Registration failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                    isLoading = false
                                }
                            },
                            onNavigateToLogin = { navController.popBackStack() }
                        )
                    }
                }

                navigation(startDestination = AppRoutes.GENDER_SELECTION, route = AppRoutes.ONBOARDING) {
                    composable(AppRoutes.GENDER_SELECTION) {
                        GenderSelectionScreen { selectedGender ->
                            ThemeManager.currentGenderTheme = selectedGender
                            val userId = firebaseEmailAuthManager.getCurrentUser()?.uid
                            if (userId != null) {
                                activityContext.lifecycleScope.launch {
                                    isLoading = true
                                    val userUpdates = hashMapOf<String, Any>("gender" to selectedGender)
                                    firebaseEmailAuthManager.db.collection("users").document(userId)
                                        .set(userUpdates, com.google.firebase.firestore.SetOptions.merge())
                                        .addOnSuccessListener { isLoading = false; navController.navigate(AppRoutes.PHYSICAL_DETAILS) }
                                        .addOnFailureListener { e -> isLoading = false; Toast.makeText(activityContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        }
                    }
                    composable(AppRoutes.PHYSICAL_DETAILS) {
                        PhysicalDetailsScreen { age, height, weight ->
                            val userId = firebaseEmailAuthManager.getCurrentUser()?.uid
                            if (userId != null) {
                                 activityContext.lifecycleScope.launch {
                                    isLoading = true
                                    val details = hashMapOf<String, Any?>("age" to age.toIntOrNull(), "height" to height.toDoubleOrNull(), "weight" to weight.toDoubleOrNull())
                                    val validDetails = details.filterValues { it != null }
                                    firebaseEmailAuthManager.db.collection("users").document(userId)
                                        .set(validDetails, com.google.firebase.firestore.SetOptions.merge())
                                        .addOnSuccessListener { isLoading = false; navController.navigate(AppRoutes.REST_TIME_SELECTION) }
                                        .addOnFailureListener { e -> isLoading = false; Toast.makeText(activityContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                                 }
                            }
                        }
                    }
                    composable(AppRoutes.REST_TIME_SELECTION) {
                        RestTimeScreen { selectedRestTime ->
                            val userId = firebaseEmailAuthManager.getCurrentUser()?.uid
                            if (userId != null) {
                                activityContext.lifecycleScope.launch {
                                    isLoading = true
                                    val restTimeUpdate = hashMapOf<String, Any>("restTimePreference" to selectedRestTime)
                                    firebaseEmailAuthManager.db.collection("users").document(userId)
                                        .set(restTimeUpdate, com.google.firebase.firestore.SetOptions.merge())
                                        .addOnSuccessListener { isLoading = false; navController.navigate(AppRoutes.INITIAL_EXERCISES_SELECTION) }
                                        .addOnFailureListener { e -> isLoading = false; Toast.makeText(activityContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        }
                    }
                    composable(AppRoutes.INITIAL_EXERCISES_SELECTION) {
                        InitialExercisesScreen { selectedExerciseIds ->
                            val userId = firebaseEmailAuthManager.getCurrentUser()?.uid
                            if (userId != null) {
                                activityContext.lifecycleScope.launch {
                                    isLoading = true
                                    val exerciseUpdate = hashMapOf<String, Any>("initialExercises" to selectedExerciseIds)
                                    firebaseEmailAuthManager.db.collection("users").document(userId)
                                        .set(exerciseUpdate, com.google.firebase.firestore.SetOptions.merge())
                                        .addOnSuccessListener { isLoading = false; navController.navigate(AppRoutes.WEEKLY_GOAL_SELECTION) }
                                        .addOnFailureListener { e -> isLoading = false; Toast.makeText(activityContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        }
                    }
                    composable(AppRoutes.WEEKLY_GOAL_SELECTION) {
                        WeeklyGoalScreen { selectedFrequency, nudgeDays ->
                            val userId = firebaseEmailAuthManager.getCurrentUser()?.uid
                            if (userId != null) {
                                activityContext.lifecycleScope.launch {
                                    isLoading = true
                                    val weeklyGoalUpdate = hashMapOf<String, Any>(
                                        "weeklyGoalFrequency" to selectedFrequency,
                                        "nudgeDays" to nudgeDays
                                    )
                                    firebaseEmailAuthManager.db.collection("users").document(userId)
                                        .set(weeklyGoalUpdate, com.google.firebase.firestore.SetOptions.merge())
                                        .addOnSuccessListener { isLoading = false; navController.navigate(AppRoutes.FITNESS_MAIN) { popUpTo(AppRoutes.ONBOARDING) { inclusive = true } } }
                                        .addOnFailureListener { e -> isLoading = false; Toast.makeText(activityContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        }
                    }
                }

                composable(AppRoutes.FITNESS_MAIN) {
                    MainAppContainer(
                        mainNavController = navController,
                        firebaseEmailAuthManager = firebaseEmailAuthManager, // Explicitly matching param name
                        firebaseGoogleAuthManager = firebaseGoogleAuthManager // Explicitly matching param name
                    )
                }
            }
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

// MainAppScaffold has been moved to MainAppContainer.kt
// Ensure MainAppContainer is imported and used in AppNavigationHost where MainAppScaffold was.
