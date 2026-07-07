package com.FreeRave.shredzilla.navigation

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.FreeRave.shredzilla.viewmodels.MainViewModel
import com.FreeRave.shredzilla.auth.FirebaseEmailPasswordAuth
import com.FreeRave.shredzilla.auth.FirebaseGoogleAuth
import com.FreeRave.shredzilla.composables.AppBottomNavigationBar
import com.FreeRave.shredzilla.models.ExerciseItem // Keep for CreateNewListScreen mapping
import com.FreeRave.shredzilla.models.ExerciseDisplayInfo
import com.FreeRave.shredzilla.screens.account.AccountScreen
import com.FreeRave.shredzilla.screens.account.UpdateUsernameScreen
import com.FreeRave.shredzilla.screens.exercises.AddExerciseScreen
import com.FreeRave.shredzilla.screens.exercises.ExerciseDetailScreen
import com.FreeRave.shredzilla.screens.exercises.ExercisesScreen
import com.FreeRave.shredzilla.screens.rewards.AdRewardsScreen
import com.FreeRave.shredzilla.screens.settings.*
import com.FreeRave.shredzilla.screens.sets.CreateNewListScreen
import com.FreeRave.shredzilla.screens.sets.ExerciseListDetailScreen
import com.FreeRave.shredzilla.screens.sets.SetGraphScreen
import com.FreeRave.shredzilla.screens.today.TodayScreen
import com.FreeRave.shredzilla.ui.theme.ThemeManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

// Data classes (ExerciseDisplayInfo, UserExerciseList, RecordedSet) are now expected
// to be defined in UserDataManager.kt or a shared models package.
// If they were previously defined here, those definitions are removed.
// For this refactoring, we assume they are accessible via their original package
// or UserDataManager will provide access if they are moved there.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    mainNavController: NavHostController,
    firebaseEmailAuthManager: FirebaseEmailPasswordAuth,
    firebaseGoogleAuthManager: FirebaseGoogleAuth,
    mainViewModel: MainViewModel = viewModel()
) {
    val bottomBarNavController = rememberNavController()
    val context = LocalContext.current
    val db = Firebase.firestore // db instance for operations not yet moved to UserDataManager (like create/edit list)

    val userDataManager = mainViewModel.userDataManager
    val timerManager = mainViewModel.timerManager

    LaunchedEffect(userDataManager.userRestTimePreference) {
        timerManager.updateTotalSecondsFromPreference()
    }

    val currentUser = firebaseEmailAuthManager.getCurrentUser()

    LaunchedEffect(currentUser?.uid) {
        userDataManager.setupFirestoreListeners(currentUser)
        currentUser?.uid?.let { uid ->
            mainViewModel.loadHistoricalAnalytics(uid)
        }
    }

    val analyticsData by mainViewModel.analyticsGraphData.collectAsState()
    val activeDays = remember(analyticsData) {
        analyticsData.values.flatten().map { it.timestampDate }.toSet()
    }

    val searchQuery by mainViewModel.searchQuery.collectAsState()
    val debouncedSearchQuery by mainViewModel.debouncedSearchQuery.collectAsState()

    val exerciseDisplayList by remember(debouncedSearchQuery) {
        derivedStateOf {
            val query = debouncedSearchQuery.trim()
            val filteredList = if (query.isEmpty()) {
                userDataManager.masterExerciseList
            } else {
                userDataManager.masterExerciseList.filter { it.contains(query, ignoreCase = true) }
            }

            filteredList.map { exerciseName ->
                val exerciseId = exerciseName.lowercase().replace(" ", "_")
                ExerciseDisplayInfo(
                    id = exerciseId,
                    name = exerciseName,
                    lastPerformed = formatLastPerformedTimestamp(userDataManager.lastPerformedTimestampsMap[exerciseId])
                )
            }
        }
    }

    val recordSetAndStartTimerAction: (String, Int, Double, String) -> Unit = { exName, reps, weight, notes ->
        userDataManager.recordSet(exName, reps, weight, notes, currentUser)
        timerManager.startRestTimer()
    }

    val navBackStackEntry by bottomBarNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val screensWithoutBottomBar = listOf(
        AppRoutes.SETTINGS, AppRoutes.ACCOUNT_SETTINGS, AppRoutes.UPDATE_USERNAME,
        AppRoutes.ADD_EXERCISE, AppRoutes.EXERCISE_LIST_SCREEN, AppRoutes.UNIT_SETTINGS,
        AppRoutes.WORKOUT_REMINDERS, AppRoutes.DEFAULT_REST_TIME_SETTINGS, AppRoutes.THEME_SETTINGS,
        AppRoutes.CREATE_NEW_LIST, AppRoutes.EXERCISE_LIST_DETAIL, AppRoutes.EDIT_LIST_SCREEN,
        AppRoutes.EXERCISE_DETAIL
    )

    Scaffold(
        bottomBar = { if (currentRoute !in screensWithoutBottomBar) { AppBottomNavigationBar(navController = bottomBarNavController) } }
    ) { innerPadding ->
        NavHost(navController = bottomBarNavController, startDestination = BottomNavItem.Sets.route, modifier = Modifier.padding(innerPadding)) {
            composable(BottomNavItem.Today.route) {
                TodayScreen(
                    modifier = Modifier.fillMaxSize(),
                    activeDays = activeDays,
                    totalReps = userDataManager.todayTotalReps,
                    userDataManager.todayTotalSets,
                    userDataManager.todayUniqueExercises,
                    userDataManager.todayRecordedSetsList,
                    userDataManager.userUnitSystem,
                    onDateSelected = { selectedDate ->
                        currentUser?.uid?.let { uid ->
                            userDataManager.loadSetsForDate(uid, selectedDate)
                        }
                    },
                    onNavigateToExerciseDetail = { exerciseName ->
                        val exerciseId = exerciseName.lowercase().replace(" ", "_")
                        bottomBarNavController.navigate(AppRoutes.EXERCISE_DETAIL.replace("{exerciseId}", exerciseId))
                    }
                )
            }
            composable(BottomNavItem.Sets.route) {
                val isAnalyticsLoading by mainViewModel.isAnalyticsLoading.collectAsState()

                SetGraphScreen(
                    analyticsGraphData = analyticsData,
                    isAnalyticsLoading = isAnalyticsLoading,
                    exerciseCount = userDataManager.masterExerciseList.size,
                    userExerciseLists = userDataManager.userExerciseLists,
                    onNavigateToSettings = { bottomBarNavController.navigate(AppRoutes.SETTINGS) },
                    onNavigateToExerciseList = { bottomBarNavController.navigate(AppRoutes.EXERCISE_LIST_SCREEN) },
                    onNavigateToCreateNewList = { bottomBarNavController.navigate(AppRoutes.CREATE_NEW_LIST) },
                    onNavigateToExerciseListDetail = { listId, listName ->
                        bottomBarNavController.navigate(
                            AppRoutes.EXERCISE_LIST_DETAIL
                                .replace("{listId}", listId)
                                .replace("{listName}", listName)
                        )
                    },
                    isTimerRunning = timerManager.isTimerRunning,
                    timerRemainingSeconds = timerManager.timerRemainingSeconds,
                    timerTotalSeconds = timerManager.timerTotalSeconds,
                    onCloseTimer = { timerManager.stopRestTimer() }
                )
            }
            composable(AppRoutes.EXERCISE_LIST_SCREEN) {
                ExercisesScreen(
                    exercisesDisplayInfo = exerciseDisplayList,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { mainViewModel.updateSearchQuery(it) },
                    onNavigateToAddExercise = { bottomBarNavController.navigate(AppRoutes.ADD_EXERCISE) },
                    onNavigateBack = { bottomBarNavController.popBackStack() },
                    onDeleteExercise = { exerciseName -> userDataManager.deleteExerciseFromFirestore(exerciseName) },
                    onRecordExercise = recordSetAndStartTimerAction,
                    isTimerRunning = timerManager.isTimerRunning,
                    timerRemainingSeconds = timerManager.timerRemainingSeconds,
                    timerTotalSeconds = timerManager.timerTotalSeconds,
                    onCloseTimer = { timerManager.stopRestTimer() },
                    unitSystem = userDataManager.userUnitSystem,
                    navController = bottomBarNavController
                )
            }
            composable(AppRoutes.ADD_EXERCISE) {
                AddExerciseScreen(
                    userDataManager.masterExerciseList.toList(), // Pass a copy if AddExerciseScreen modifies it, or ensure it's read-only
                    { exerciseName -> userDataManager.addExerciseToCommonExercises(exerciseName) },
                    { bottomBarNavController.popBackStack() }
                ) { selectedExercises ->
                    selectedExercises.forEach { userDataManager.addExerciseToCommonExercises(it.name) }
                    bottomBarNavController.popBackStack()
                }
            }
            composable(AppRoutes.SETTINGS) {
                val currentGender = ThemeManager.currentGenderTheme ?: "Male"
                val effectiveThemeSetting = ThemeManager.getEffectiveThemeSetting(currentGender, userDataManager.themePreferenceMale, userDataManager.themePreferenceFemale)
                SettingsScreen(
                    onNavigateBack = { bottomBarNavController.popBackStack() },
                    onNavigateToAccount = { bottomBarNavController.navigate(AppRoutes.ACCOUNT_SETTINGS) },
                    onNavigateToUnitSettings = { bottomBarNavController.navigate(AppRoutes.UNIT_SETTINGS) },
                    onNavigateToWorkoutReminders = { bottomBarNavController.navigate(AppRoutes.WORKOUT_REMINDERS) },
                    onNavigateToDefaultRestTimeSettings = { bottomBarNavController.navigate(AppRoutes.DEFAULT_REST_TIME_SETTINGS) },
                    onNavigateToThemeSettings = { bottomBarNavController.navigate(AppRoutes.THEME_SETTINGS) },
                    onNavigateToAdRewards = { bottomBarNavController.navigate(AppRoutes.AD_REWARDS_SCREEN) },
                    currentUnitSystemDisplay = if (userDataManager.userUnitSystem == UnitSystem.METRIC) "Metric (kg/km)" else "Imperial (lbs/miles)",
                    currentWorkoutReminderDisplay = userDataManager.userWorkoutReminderSetting,
                    currentDefaultRestTimeDisplay = userDataManager.userRestTimePreference,
                    currentThemeDisplay = effectiveThemeSetting.displayName
                )
            }
            composable(AppRoutes.ACCOUNT_SETTINGS) {
                val user = firebaseEmailAuthManager.getCurrentUser()
                AccountScreen(
                    onNavigateBack = { bottomBarNavController.popBackStack() },
                    onSignOut = { firebaseEmailAuthManager.signOut(); firebaseGoogleAuthManager.signOut(); userDataManager.clearAllListeners(); ThemeManager.currentGenderTheme = null; mainNavController.navigate(AppRoutes.AUTH) { popUpTo(mainNavController.graph.startDestinationId) { inclusive = true }; launchSingleTop = true } },
                    onNavigateToUpdateUsername = { bottomBarNavController.navigate(AppRoutes.UPDATE_USERNAME) },
                    userEmail = user?.email,
                    userName = user?.displayName,
                    profileImageUrl = userDataManager.userProfileImageLocalPath,
                    isDeletingAccount = mainViewModel.isDeletingAccount,
                    onUpdateProfileImagePathInFirestore = { newPath -> userDataManager.updateProfileImagePathInFirestore(newPath, currentUser) },
                    onDeleteAccount = { onSuccess, onError ->
                        mainViewModel.deleteUserAccount(
                            onSuccess = {
                                userDataManager.clearAllListeners()
                                onSuccess()
                            },
                            onError = onError
                        )
                    }
                )
            }
            composable(AppRoutes.UPDATE_USERNAME) {
                val user = firebaseEmailAuthManager.getCurrentUser()
                UpdateUsernameScreen(
                    onNavigateBack = { bottomBarNavController.popBackStack() }, 
                    currentUsername = user?.displayName,
                    isUpdatingUsername = mainViewModel.isUpdatingUsername,
                    onUpdateUsername = { newUsername, onSuccess, onError ->
                        mainViewModel.updateUsername(newUsername, onSuccess, onError)
                    }
                )
            }
            composable(AppRoutes.UNIT_SETTINGS) {
                UnitSettingsScreen({ bottomBarNavController.popBackStack() }, userDataManager.userUnitSystem) { newUnitSystem ->
                    userDataManager.userUnitSystem = newUnitSystem
                    userDataManager.updateUserSetting(currentUser?.uid, "unitSystem", newUnitSystem.name)
                }
            }
            composable(AppRoutes.WORKOUT_REMINDERS) {
                WorkoutRemindersScreen({ bottomBarNavController.popBackStack() }, userDataManager.userWorkoutReminderSetting) { newSetting ->
                    userDataManager.userWorkoutReminderSetting = newSetting
                    userDataManager.updateUserSetting(currentUser?.uid, "workoutReminder", newSetting)
                }
            }
            composable(AppRoutes.DEFAULT_REST_TIME_SETTINGS) {
                DefaultRestTimeSettingsScreen(userDataManager.userRestTimePreference, { newPreference ->
                    userDataManager.userRestTimePreference = newPreference
                    userDataManager.updateUserSetting(currentUser?.uid, "restTimePreference", newPreference)
                    bottomBarNavController.popBackStack()
                }, { bottomBarNavController.popBackStack() })
            }
            composable(AppRoutes.THEME_SETTINGS) {
                val currentGender = ThemeManager.currentGenderTheme ?: "Male"
                val currentThemeForGender = if (currentGender == "Male") userDataManager.themePreferenceMale else userDataManager.themePreferenceFemale
                ThemeSettingsScreen(
                    onNavigateBack = { bottomBarNavController.popBackStack() },
                    currentThemeSetting = currentThemeForGender,
                    onThemeSettingChange = { newThemeSetting ->
                        val fieldToUpdate = if (currentGender == "Male") "themePreferenceMale" else "themePreferenceFemale"
                        if (currentGender == "Male") userDataManager.themePreferenceMale = newThemeSetting else userDataManager.themePreferenceFemale = newThemeSetting
                        ThemeManager.updateThemePreferenceForGender(currentGender, newThemeSetting)
                        userDataManager.updateUserSetting(currentUser?.uid, fieldToUpdate, newThemeSetting.name)
                    }
                )
            }
            composable(AppRoutes.CREATE_NEW_LIST) {
                val exercisesForSelection = userDataManager.masterExerciseList.map { name ->
                    ExerciseItem(id = name.lowercase().replace(" ", "_"), name = name, description = "", videoUrl = null, targetMuscles = emptyList(), equipmentNeeded = emptyList(), difficulty = "")
                }
                CreateNewListScreen(
                    allExercises = exercisesForSelection,
                    onNavigateBack = { bottomBarNavController.popBackStack() },
                    onSaveList = { _, listName, selectedExerciseIds ->
                        userDataManager.createNewList(listName, selectedExerciseIds, currentUser)
                        bottomBarNavController.popBackStack()
                    }
                )
            }
            composable(
                route = AppRoutes.EXERCISE_LIST_DETAIL,
                arguments = listOf(
                    androidx.navigation.navArgument("listId") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("listName") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val listId = backStackEntry.arguments?.getString("listId") ?: ""
                val listName = backStackEntry.arguments?.getString("listName") ?: "Workout List"

                val selectedList = userDataManager.userExerciseLists.find { it.id == listId }
                val exercisesForDetailScreen = selectedList?.exerciseIds?.mapNotNull { exerciseId ->
                    val exerciseName = userDataManager.masterExerciseList.find { name -> name.lowercase().replace(" ", "_") == exerciseId }
                    exerciseName?.let { name ->
                         ExerciseDisplayInfo(id = exerciseId, name = name, lastPerformed = formatLastPerformedTimestamp(userDataManager.lastPerformedTimestampsMap[exerciseId]))
                    }
                } ?: emptyList()

                ExerciseListDetailScreen(
                    listName = listName,
                    exercisesDisplayInfo = exercisesForDetailScreen,
                    onNavigateBack = { bottomBarNavController.popBackStack() },
                    onDeleteExerciseFromList = { exerciseNameToDelete, currentListId ->
                        val exerciseIdToDelete = exerciseNameToDelete.lowercase().replace(" ", "_")
                        userDataManager.removeExerciseFromList(currentListId, exerciseIdToDelete, currentUser)
                    },
                    onRecordExercise = recordSetAndStartTimerAction,
                    isTimerRunning = timerManager.isTimerRunning,
                    timerRemainingSeconds = timerManager.timerRemainingSeconds,
                    timerTotalSeconds = timerManager.timerTotalSeconds,
                    onCloseTimer = { timerManager.stopRestTimer() },
                    unitSystem = userDataManager.userUnitSystem,
                    listId = listId,
                    onNavigateToEditList = { id, _ -> // listName is not used here
                        bottomBarNavController.navigate(AppRoutes.EDIT_LIST_SCREEN.replace("{listId}", id))
                    },
                    navController = bottomBarNavController
                )
            }
            composable(
                route = AppRoutes.EDIT_LIST_SCREEN,
                arguments = listOf(androidx.navigation.navArgument("listId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val listIdToEdit = backStackEntry.arguments?.getString("listId")
                val listToEdit = userDataManager.userExerciseLists.find { it.id == listIdToEdit }

                val exercisesForSelection = userDataManager.masterExerciseList.map { name ->
                    ExerciseItem(id = name.lowercase().replace(" ", "_"), name = name, description = "", videoUrl = null, targetMuscles = emptyList(), equipmentNeeded = emptyList(), difficulty = "")
                }

                if (listToEdit != null) {
                    CreateNewListScreen(
                        allExercises = exercisesForSelection,
                        onNavigateBack = { bottomBarNavController.popBackStack() },
                        onSaveList = { existingListId, updatedListName, updatedExerciseIds ->
                            if (existingListId != null) {
                                userDataManager.updateList(existingListId, updatedListName, updatedExerciseIds, currentUser)
                            }
                            bottomBarNavController.popBackStack()
                        },
                        initialListId = listToEdit.id,
                        initialListName = listToEdit.name,
                        initialSelectedExerciseIds = listToEdit.exerciseIds
                    )
                } else {
                    Log.e("Navigation", "Attempted to edit non-existent list with ID: $listIdToEdit")
                    bottomBarNavController.popBackStack()
                }
            }
            composable(AppRoutes.AD_REWARDS_SCREEN) {
                AdRewardsScreen(onNavigateBack = { bottomBarNavController.popBackStack() })
            }
            composable(
                route = AppRoutes.EXERCISE_DETAIL,
                arguments = listOf(androidx.navigation.navArgument("exerciseId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val exerciseId = backStackEntry.arguments?.getString("exerciseId")
                ExerciseDetailScreen(
                    navController = bottomBarNavController,
                    exerciseId = exerciseId,
                    onRecordSet = recordSetAndStartTimerAction,
                    unitSystem = userDataManager.userUnitSystem,
                    onDeleteSet = { exName, docId -> userDataManager.deleteSet(exName, docId, currentUser) },
                    onUpdateSet = { exName, docId, reps, weight, notes -> userDataManager.updateSet(exName, docId, reps, weight, notes, currentUser) }
                )
            }
        }
    }
}

// This function might also be moved to a utils package or within UserDataManager if it's only used with its data
internal fun formatLastPerformedTimestamp(timestamp: Timestamp?): String? {
    if (timestamp == null) return null
    val today = Calendar.getInstance(); val lastPerformedDate = Calendar.getInstance().apply { time = timestamp.toDate() }
    today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0); today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0)
    lastPerformedDate.set(Calendar.HOUR_OF_DAY, 0); lastPerformedDate.set(Calendar.MINUTE, 0); lastPerformedDate.set(Calendar.SECOND, 0); lastPerformedDate.set(Calendar.MILLISECOND, 0)
    val diffInDays = TimeUnit.MILLISECONDS.toDays(today.timeInMillis - lastPerformedDate.timeInMillis).toInt()
    return when (diffInDays) { 0 -> "Today"; 1 -> "Yesterday"; else -> "$diffInDays days ago" }
}
