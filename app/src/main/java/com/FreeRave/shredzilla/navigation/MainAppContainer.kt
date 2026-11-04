package com.FreeRave.shredzilla.navigation

import com.FreeRave.shredzilla.models.initialGlobalExerciseList
import com.FreeRave.shredzilla.models.exerciseIdOf
import android.util.Log
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.FreeRave.shredzilla.ui.glass.GlassBackground
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
import android.widget.Toast
import com.FreeRave.shredzilla.utils.dateKeyOf
import com.FreeRave.shredzilla.screens.programs.ProgramsScreen
import com.FreeRave.shredzilla.models.workoutPrograms
import com.FreeRave.shredzilla.models.recommendedProgramId

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

    val exerciseHistory by mainViewModel.exerciseHistory.collectAsState()
    val setsDoneToday = remember(exerciseHistory) {
        val todayKey = dateKeyOf(System.currentTimeMillis())
        exerciseHistory.mapValues { (_, sets) -> sets.count { it.dateKey == todayKey } }
    }

    val searchQuery by mainViewModel.searchQuery.collectAsState()
    val debouncedSearchQuery by mainViewModel.debouncedSearchQuery.collectAsState()

    // Same shape everywhere an exercise is listed: library, list detail
    fun displayInfoFor(exerciseName: String): ExerciseDisplayInfo {
        val id = exerciseIdOf(exerciseName)
        val lastPerformed = userDataManager.lastPerformedTimestampsMap[id]
        return ExerciseDisplayInfo(
            id = id,
            name = exerciseName,
            lastPerformed = formatLastPerformedTimestamp(lastPerformed),
            lastPerformedMillis = lastPerformed?.toDate()?.time
        )
    }

    val exerciseDisplayList by remember(debouncedSearchQuery) {
        derivedStateOf {
            val query = debouncedSearchQuery.trim()
            val filteredList = if (query.isEmpty()) {
                userDataManager.masterExerciseList
            } else {
                userDataManager.masterExerciseList.filter { it.contains(query, ignoreCase = true) }
            }

            filteredList.map(::displayInfoFor)
        }
    }

    fun openExercise(exerciseName: String) {
        bottomBarNavController.navigate(AppRoutes.EXERCISE_DETAIL.replace("{exerciseId}", exerciseIdOf(exerciseName)))
    }

    // Any set change makes the progress chart and exercise history stale, so reload them
    fun refreshHistory() {
        currentUser?.uid?.let { mainViewModel.loadHistoricalAnalytics(it, force = true) }
    }

    val recordSetAndStartTimerAction: (String, Int, Double, String) -> Unit = { exName, reps, weight, notes ->
        userDataManager.recordSet(exName, reps, weight, notes, currentUser, onSaved = ::refreshHistory)
        timerManager.startRestTimer()
    }

    val navBackStackEntry by bottomBarNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // Only the tab roots show the bar; every pushed page (settings sub-pages, details, editors) hides it
    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    // One shared backdrop for every tab, so switching tabs never flashes a new background.
    // Screens draw their own status-bar inset (top bars); the bottom bar handles the nav-bar inset.
    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp),
            bottomBar = { if (showBottomBar) AppBottomNavigationBar(navController = bottomBarNavController) }
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
                        onNavigateToExerciseDetail = ::openExercise
                    )
                }
                composable(BottomNavItem.Sets.route) {
                    val isAnalyticsLoading by mainViewModel.isAnalyticsLoading.collectAsState()

                    SetGraphScreen(
                        analyticsGraphData = analyticsData,
                        isAnalyticsLoading = isAnalyticsLoading,
                        exerciseCount = userDataManager.masterExerciseList.size,
                        userExerciseLists = userDataManager.userExerciseLists,
                        userName = currentUser?.displayName,
                        onNavigateToExerciseList = { bottomBarNavController.navigate(AppRoutes.EXERCISE_LIST_SCREEN) },
                        onOpenPrograms = { bottomBarNavController.navigate(AppRoutes.PROGRAMS) },
                        recommendedProgramName = recommendedProgramId(userDataManager.userWeeklyGoalDays)
                            ?.let { id -> workoutPrograms.first { it.id == id }.name },
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
                        onOpenExercise = ::openExercise
                    )
                }
                composable(AppRoutes.ADD_EXERCISE) {
                    AddExerciseScreen(
                        libraryExercises = userDataManager.masterExerciseList.toList(),
                        onCreateExercise = { name ->
                            userDataManager.addExerciseToCommonExercises(name)
                            bottomBarNavController.popBackStack()
                        },
                        onAddExercises = { names ->
                            names.forEach(userDataManager::addExerciseToCommonExercises)
                            bottomBarNavController.popBackStack()
                        },
                        onNavigateBack = { bottomBarNavController.popBackStack() }
                    )
                }
                composable(AppRoutes.SETTINGS) {
                    val currentGender = ThemeManager.currentGenderTheme ?: "Male"
                    val effectiveThemeSetting = ThemeManager.getEffectiveThemeSetting(currentGender, userDataManager.themePreferenceMale, userDataManager.themePreferenceFemale)
                    SettingsScreen(
                        userName = currentUser?.displayName,
                        userEmail = currentUser?.email,
                        profileImagePath = userDataManager.userProfileImageLocalPath,
                        unitSystem = userDataManager.userUnitSystem,
                        themeSetting = effectiveThemeSetting,
                        workoutReminderSetting = userDataManager.userWorkoutReminderSetting,
                        restTimePreference = userDataManager.userRestTimePreference,
                        onNavigateToAccount = { bottomBarNavController.navigate(AppRoutes.ACCOUNT_SETTINGS) },
                        onNavigateToUnitSettings = { bottomBarNavController.navigate(AppRoutes.UNIT_SETTINGS) },
                        onNavigateToWorkoutReminders = { bottomBarNavController.navigate(AppRoutes.WORKOUT_REMINDERS) },
                        onNavigateToDefaultRestTimeSettings = { bottomBarNavController.navigate(AppRoutes.DEFAULT_REST_TIME_SETTINGS) },
                        onNavigateToThemeSettings = { bottomBarNavController.navigate(AppRoutes.THEME_SETTINGS) },
                        onNavigateToAdRewards = { bottomBarNavController.navigate(AppRoutes.AD_REWARDS_SCREEN) }
                    )
                }
                composable(AppRoutes.ACCOUNT_SETTINGS) {
                    val user = firebaseEmailAuthManager.getCurrentUser()
                    AccountScreen(
                        onNavigateBack = { bottomBarNavController.popBackStack() },
                        onSignOut = {
                            val scope = kotlinx.coroutines.MainScope()
                            scope.launch {
                                firebaseEmailAuthManager.signOut()
                                firebaseGoogleAuthManager.signOut()
                                mainViewModel.clearSession() // the next account must not see this one's data
                                mainViewModel.updateStartDestination(AppRoutes.AUTH)
                                mainNavController.navigate(AppRoutes.AUTH) {
                                    popUpTo(mainNavController.graph.startDestinationId) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
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
                    CreateNewListScreen(
                        exerciseNames = userDataManager.masterExerciseList,
                        onNavigateBack = { bottomBarNavController.popBackStack() },
                        onSave = { listName, exerciseIds ->
                            userDataManager.createNewList(listName, exerciseIds, currentUser)
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
                        val exerciseName = userDataManager.masterExerciseList.find { name -> exerciseIdOf(name) == exerciseId }
                        exerciseName?.let(::displayInfoFor)
                    } ?: emptyList()

                    ExerciseListDetailScreen(
                        // The route arg is the name at navigation time; prefer the live one so a rename shows
                        listName = selectedList?.name ?: listName,
                        exercises = exercisesForDetailScreen,
                        targetSets = selectedList?.targetSetsMin?.let { min -> min..(selectedList.targetSetsMax ?: min) },
                        setsDoneToday = setsDoneToday,
                        unitSystem = userDataManager.userUnitSystem,
                        isTimerRunning = timerManager.isTimerRunning,
                        timerRemainingSeconds = timerManager.timerRemainingSeconds,
                        timerTotalSeconds = timerManager.timerTotalSeconds,
                        onCloseTimer = { timerManager.stopRestTimer() },
                        onNavigateBack = { bottomBarNavController.popBackStack() },
                        onEditList = { bottomBarNavController.navigate(AppRoutes.EDIT_LIST_SCREEN.replace("{listId}", listId)) },
                        onDeleteList = {
                            userDataManager.deleteList(listId, currentUser)
                            bottomBarNavController.popBackStack()
                        },
                        onOpenExercise = ::openExercise,
                        onRecordExercise = recordSetAndStartTimerAction,
                        onRemoveExercise = { name -> userDataManager.removeExerciseFromList(listId, exerciseIdOf(name), currentUser) }
                    )
                }
                composable(
                    route = AppRoutes.EDIT_LIST_SCREEN,
                    arguments = listOf(androidx.navigation.navArgument("listId") { type = androidx.navigation.NavType.StringType })
                ) { backStackEntry ->
                    val listIdToEdit = backStackEntry.arguments?.getString("listId")
                    val listToEdit = userDataManager.userExerciseLists.find { it.id == listIdToEdit }


                    if (listToEdit != null) {
                        CreateNewListScreen(
                            exerciseNames = userDataManager.masterExerciseList,
                            onNavigateBack = { bottomBarNavController.popBackStack() },
                            onSave = { listName, exerciseIds ->
                                userDataManager.updateList(listToEdit.id, listName, exerciseIds, currentUser)
                                bottomBarNavController.popBackStack()
                            },
                            isEditing = true,
                            initialName = listToEdit.name,
                            initialSelectedIds = listToEdit.exerciseIds
                        )
                    } else {
                        Log.e("Navigation", "Attempted to edit non-existent list with ID: $listIdToEdit")
                        bottomBarNavController.popBackStack()
                    }
                }
                composable(AppRoutes.PROGRAMS) {
                    ProgramsScreen(
                        recommendedProgramId = recommendedProgramId(userDataManager.userWeeklyGoalDays),
                        // A program counts as added while any of its day lists ("PPL · …") exists
                        addedProgramIds = workoutPrograms.filter { program ->
                            userDataManager.userExerciseLists.any { it.name.startsWith("${program.shortName} · ") }
                        }.map { it.id }.toSet(),
                        onAddProgram = { program, level ->
                            userDataManager.addProgram(program, level, currentUser)
                            Toast.makeText(context, "${program.name} added to your lists", Toast.LENGTH_SHORT).show()
                            bottomBarNavController.popBackStack()
                        },
                        onNavigateBack = { bottomBarNavController.popBackStack() }
                    )
                }
                composable(AppRoutes.AD_REWARDS_SCREEN) {
                    AdRewardsScreen(onNavigateBack = { bottomBarNavController.popBackStack() })
                }
                composable(
                    route = AppRoutes.EXERCISE_DETAIL,
                    arguments = listOf(androidx.navigation.navArgument("exerciseId") { type = androidx.navigation.NavType.StringType })
                ) { backStackEntry ->
                    val exerciseId = backStackEntry.arguments?.getString("exerciseId").orEmpty()
                    val builtIn = initialGlobalExerciseList.find { it.id == exerciseId }
                    // Library name first; fall back to the built-in name or a readable version of the id
                    val exerciseName = userDataManager.masterExerciseList.find { exerciseIdOf(it) == exerciseId }
                        ?: builtIn?.name
                        ?: exerciseId.split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
                    val isHistoryLoading by mainViewModel.isAnalyticsLoading.collectAsState()

                    ExerciseDetailScreen(
                        exerciseName = exerciseName,
                        info = builtIn,
                        history = exerciseHistory[exerciseId].orEmpty(),
                        volumeTrend = analyticsData[exerciseId].orEmpty(),
                        isLoading = isHistoryLoading,
                        unitSystem = userDataManager.userUnitSystem,
                        onBack = { bottomBarNavController.popBackStack() },
                        onRecordSet = { reps, weight, notes -> recordSetAndStartTimerAction(exerciseName, reps, weight, notes) },
                        onUpdateSet = { set, reps, weight, notes ->
                            userDataManager.updateSet(set.dateKey, set.docId, reps, weight, notes, currentUser, onDone = ::refreshHistory)
                        },
                        onDeleteSet = { set -> userDataManager.deleteSet(set.dateKey, set.docId, currentUser, onDone = ::refreshHistory) }
                    )
                }
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
