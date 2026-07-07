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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.FreeRave.shredzilla.auth.*
import com.FreeRave.shredzilla.onboarding.*
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.ThemeManager
import kotlinx.coroutines.launch

import androidx.lifecycle.viewmodel.compose.viewModel
import com.FreeRave.shredzilla.viewmodels.MainViewModel

@Composable
fun AppNavigationHost(
    navController: NavHostController,
    startDestination: String,
    firebaseEmailAuthManager: FirebaseEmailPasswordAuth,
    firebaseGoogleAuthManager: FirebaseGoogleAuth,
    googleSignInLauncher: ActivityResultLauncher<Intent>,
    mainViewModel: MainViewModel = viewModel()
) {
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    // ShredzillaTheme no longer takes genderTheme or darkTheme directly
    ShredzillaTheme { 
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            NavHost(navController = navController, startDestination = startDestination) {
                navigation(startDestination = AppRoutes.LOGIN, route = AppRoutes.AUTH) {
                    composable(AppRoutes.LOGIN) {
                        LoginScreen(
                            onLoginClick = { email, password ->
                                mainViewModel.viewModelScope.launch {
                                    isLoading = true
                                    val result = firebaseEmailAuthManager.signInUser(email, password)
                                    if (result.isSuccess) {
                                        val user = result.getOrNull()
                                        if (user != null) {
                                            val userDataResult = firebaseEmailAuthManager.getUserData(user.uid)
                                            val userData = userDataResult.getOrNull()
                                            ThemeManager.currentGenderTheme = userData?.get("gender") as? String

                                            val destination = when {
                                                userData == null -> AppRoutes.ONBOARDING
                                                userData.containsKey("weeklyGoalFrequency") -> AppRoutes.FITNESS_MAIN
                                                userData.containsKey("initialExercises") -> AppRoutes.WEEKLY_GOAL_SELECTION
                                                userData.containsKey("restTimePreference") -> AppRoutes.INITIAL_EXERCISES_SELECTION
                                                userData.containsKey("age") || userData.containsKey("height") || userData.containsKey("weight") -> AppRoutes.REST_TIME_SELECTION
                                                userData.containsKey("gender") -> AppRoutes.PHYSICAL_DETAILS
                                                else -> AppRoutes.ONBOARDING
                                            }
                                            navController.navigate(destination) {
                                                popUpTo(AppRoutes.AUTH) { inclusive = true }
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Login failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                    isLoading = false
                                }
                            },
                            onGoogleSignInClick = {
                                firebaseGoogleAuthManager.signInWithGoogle(googleSignInLauncher)
                            },
                            onNavigateToRegister = { navController.navigate(AppRoutes.REGISTER) },
                            onNavigateToForgotPassword = { navController.navigate(AppRoutes.FORGOT_PASSWORD) }
                        )
                    }
                    composable(AppRoutes.REGISTER) {
                        RegisterScreen(
                            onRegisterClick = { name, email, password, _ ->
                                mainViewModel.viewModelScope.launch {
                                    isLoading = true
                                    val result = firebaseEmailAuthManager.createUser(name, email, password)
                                    if (result.isSuccess) {
                                        ThemeManager.currentGenderTheme = null
                                        navController.navigate(AppRoutes.ONBOARDING) { popUpTo(AppRoutes.AUTH) { inclusive = true } }
                                    } else {
                                        Toast.makeText(context, "Registration failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                    isLoading = false
                                }
                            },
                            onNavigateToLogin = { navController.popBackStack() }
                        )
                    }
                    composable(AppRoutes.FORGOT_PASSWORD) {
                        var isEmailSent by remember { mutableStateOf(false) }
                        ForgotPasswordScreen(
                            onSendResetEmail = { email ->
                                mainViewModel.viewModelScope.launch {
                                    isLoading = true
                                    val result = firebaseEmailAuthManager.sendPasswordResetEmail(email)
                                    if (result.isSuccess) {
                                        isEmailSent = true
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Error: ${result.exceptionOrNull()?.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    isLoading = false
                                }
                            },
                            onNavigateBack = { navController.popBackStack() },
                            isLoading = isLoading,
                            isEmailSent = isEmailSent
                        )
                    }
                } // end auth navigation

                navigation(startDestination = AppRoutes.GENDER_SELECTION, route = AppRoutes.ONBOARDING) {
                    composable(AppRoutes.GENDER_SELECTION) {
                        GenderSelectionScreen { selectedGender ->
                            ThemeManager.currentGenderTheme = selectedGender
                            val userId = firebaseEmailAuthManager.getCurrentUser()?.uid
                            if (userId != null) {
                                mainViewModel.viewModelScope.launch {
                                    isLoading = true
                                    val userUpdates = hashMapOf<String, Any>("gender" to selectedGender)
                                    val result = firebaseEmailAuthManager.updateUserOnboardingData(userId, userUpdates)
                                    isLoading = false
                                    if (result.isSuccess) { navController.navigate(AppRoutes.PHYSICAL_DETAILS) }
                                    else { Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        }
                    }
                    composable(AppRoutes.PHYSICAL_DETAILS) {
                        PhysicalDetailsScreen { age, height, weight ->
                            val userId = firebaseEmailAuthManager.getCurrentUser()?.uid
                            if (userId != null) {
                                 mainViewModel.viewModelScope.launch {
                                    isLoading = true
                                    val details = hashMapOf<String, Any?>("age" to age.toIntOrNull(), "height" to height.toDoubleOrNull(), "weight" to weight.toDoubleOrNull())
                                    @Suppress("UNCHECKED_CAST")
                                    val validDetails = details.filterValues { it != null } as Map<String, Any>
                                    val result = firebaseEmailAuthManager.updateUserOnboardingData(userId, validDetails)
                                    isLoading = false
                                    if (result.isSuccess) { navController.navigate(AppRoutes.REST_TIME_SELECTION) }
                                    else { Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show() }
                                 }
                            }
                        }
                    }
                    composable(AppRoutes.REST_TIME_SELECTION) {
                        RestTimeScreen { selectedRestTime ->
                            val userId = firebaseEmailAuthManager.getCurrentUser()?.uid
                            if (userId != null) {
                                mainViewModel.viewModelScope.launch {
                                    isLoading = true
                                    val restTimeUpdate = hashMapOf<String, Any>("restTimePreference" to selectedRestTime)
                                    val result = firebaseEmailAuthManager.updateUserOnboardingData(userId, restTimeUpdate)
                                    isLoading = false
                                    if (result.isSuccess) { navController.navigate(AppRoutes.INITIAL_EXERCISES_SELECTION) }
                                    else { Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        }
                    }
                    composable(AppRoutes.INITIAL_EXERCISES_SELECTION) {
                        InitialExercisesScreen { selectedExerciseIds ->
                            val userId = firebaseEmailAuthManager.getCurrentUser()?.uid
                            if (userId != null) {
                                mainViewModel.viewModelScope.launch {
                                    isLoading = true
                                    val exerciseUpdate = hashMapOf<String, Any>("initialExercises" to selectedExerciseIds)
                                    val result = firebaseEmailAuthManager.updateUserOnboardingData(userId, exerciseUpdate)
                                    isLoading = false
                                    if (result.isSuccess) { navController.navigate(AppRoutes.WEEKLY_GOAL_SELECTION) }
                                    else { Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        }
                    }
                    composable(AppRoutes.WEEKLY_GOAL_SELECTION) {
                        WeeklyGoalScreen { selectedFrequency, nudgeDays ->
                            val userId = firebaseEmailAuthManager.getCurrentUser()?.uid
                            if (userId != null) {
                                mainViewModel.viewModelScope.launch {
                                    isLoading = true
                                    val weeklyGoalUpdate = hashMapOf<String, Any>(
                                        "weeklyGoalFrequency" to selectedFrequency,
                                        "nudgeDays" to nudgeDays
                                    )
                                    val result = firebaseEmailAuthManager.updateUserOnboardingData(userId, weeklyGoalUpdate)
                                    isLoading = false
                                    if (result.isSuccess) { navController.navigate(AppRoutes.FITNESS_MAIN) { popUpTo(AppRoutes.ONBOARDING) { inclusive = true } } }
                                    else { Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show() }
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
