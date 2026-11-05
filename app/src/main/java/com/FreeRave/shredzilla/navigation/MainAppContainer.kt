package com.FreeRave.shredzilla.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.FreeRave.shredzilla.screens.workout.WorkoutScreen
import com.FreeRave.shredzilla.models.UserExerciseList
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import com.FreeRave.shredzilla.composables.PrCelebration
import com.FreeRave.shredzilla.composables.PrCelebrationBanner
import com.FreeRave.shredzilla.utils.formatEntry
import com.FreeRave.shredzilla.utils.RecordCheck
import com.FreeRave.shredzilla.utils.checkRecord
import com.FreeRave.shredzilla.utils.displayToKg
import com.FreeRave.shredzilla.utils.recordsToBackfill
import com.FreeRave.shredzilla.models.RecordSet
import com.FreeRave.shredzilla.screens.history.PersonalRecordsScreen
import com.FreeRave.shredzilla.screens.history.WorkoutHistoryScreen
import com.FreeRave.shredzilla.screens.history.WorkoutLogScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import com.FreeRave.shredzilla.composables.OfflineBanner
import com.FreeRave.shredzilla.utils.rememberIsOnline
import com.FreeRave.shredzilla.models.SetEntry
import com.FreeRave.shredzilla.utils.toEntry
import com.FreeRave.shredzilla.utils.SetPrefill
import com.FreeRave.shredzilla.utils.suggestNextSet
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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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
    val trainingHistory = mainViewModel.trainingHistory

    LaunchedEffect(userDataManager.userRestTimePreference) {
        timerManager.updateTotalSecondsFromPreference()
    }

    val currentUser = firebaseEmailAuthManager.getCurrentUser()

    LaunchedEffect(currentUser?.uid) {
        userDataManager.setupFirestoreListeners(currentUser)
        trainingHistory.start(currentUser?.uid)
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

    fun exerciseNameFor(exerciseId: String): String =
        userDataManager.masterExerciseList.find { exerciseIdOf(it) == exerciseId }
            ?: initialGlobalExerciseList.find { it.id == exerciseId }?.name
            ?: exerciseId.split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

    // Records started with this feature, so older bests are filled in from the loaded history.
    // Only after the records snapshot arrived, or every best would look new.
    LaunchedEffect(exerciseHistory, trainingHistory.recordsLoaded) {
        if (!trainingHistory.recordsLoaded) return@LaunchedEffect
        exerciseHistory.forEach { (exerciseId, sets) ->
            recordsToBackfill(sets, trainingHistory.personalRecords[exerciseId]).forEach { (isPlates, record) ->
                trainingHistory.saveRecord(exerciseId, exerciseNameFor(exerciseId), isPlates, record)
            }
        }
    }

    // A list's exercises in its saved order; ids no longer in the library are skipped
    fun exercisesOf(list: UserExerciseList?): List<ExerciseDisplayInfo> =
        list?.exerciseIds.orEmpty().mapNotNull { exerciseId ->
            userDataManager.masterExerciseList.find { exerciseIdOf(it) == exerciseId }?.let(::displayInfoFor)
        }

    fun targetSetsOf(list: UserExerciseList?): IntRange? = list?.targetSetsMin?.let { min -> min..(list.targetSetsMax ?: min) }

    fun openExercise(exerciseName: String) {
        bottomBarNavController.navigate(AppRoutes.EXERCISE_DETAIL.replace("{exerciseId}", exerciseIdOf(exerciseName)))
    }

    // Any set change makes the progress chart and exercise history stale, so reload them
    fun refreshHistory() {
        currentUser?.uid?.let { mainViewModel.loadHistoricalAnalytics(it, force = true) }
    }

    var celebration by remember { mutableStateOf<PrCelebration?>(null) }

    fun logSet(exName: String, entry: SetEntry, startRest: Boolean = true) {
        // Checked against the all-time record and the history before this set lands in it
        val unitSystem = userDataManager.userUnitSystem
        val exerciseId = exerciseIdOf(exName)
        val check = checkRecord(exerciseHistory[exerciseId].orEmpty(), trainingHistory.personalRecords[exerciseId], entry, unitSystem)
        if (check != RecordCheck.NONE) {
            val load = entry.plates?.toDouble() ?: displayToKg(entry.weight, unitSystem)
            trainingHistory.saveRecord(exerciseId, exName, entry.plates != null, RecordSet(load, entry.reps, System.currentTimeMillis()))
        }
        if (check == RecordCheck.BEATEN) celebration = PrCelebration(exName, formatEntry(entry, unitSystem))
        userDataManager.recordSet(exName, entry, currentUser, onSaved = ::refreshHistory)
        if (startRest) timerManager.startRestTimer()
    }

    val recordSetAndStartTimerAction: (String, SetEntry) -> Unit = { exName, entry -> logSet(exName, entry) }

    // A new set starts from the last one logged, so machine exercises stay in plates and the load is
    // ready; the suggestion is the progression step from the last session
    fun prefillFor(exerciseName: String): SetPrefill {
        val history = exerciseHistory[exerciseIdOf(exerciseName)].orEmpty()
        val unitSystem = userDataManager.userUnitSystem
        return SetPrefill(
            initial = history.firstOrNull()?.toEntry(unitSystem)?.copy(notes = "") ?: SetEntry(),
            suggestion = suggestNextSet(history, unitSystem)
        )
    }

    val navBackStackEntry by bottomBarNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // Only the tab roots show the bar; every pushed page (settings sub-pages, details, editors) hides it
    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    // One shared backdrop for every tab, so switching tabs never flashes a new background.
    // Screens draw their own status-bar inset (top bars); the bottom bar handles the nav-bar inset.
    val isOnline = rememberIsOnline()
    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp),
            // Pushes content down instead of covering it; the banner takes over the status-bar inset
            topBar = {
                AnimatedVisibility(visible = !isOnline, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    OfflineBanner()
                }
            },
            bottomBar = { if (showBottomBar) AppBottomNavigationBar(navController = bottomBarNavController) }
        ) { innerPadding ->
            NavHost(
                navController = bottomBarNavController,
                startDestination = BottomNavItem.Sets.route,
                modifier = Modifier
                    .padding(innerPadding)
                    .then(if (isOnline) Modifier else Modifier.consumeWindowInsets(WindowInsets.statusBars))
            ) {
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
                        workoutCount = trainingHistory.workoutLogs.size,
                        recordCount = trainingHistory.personalRecords.size,
                        onOpenHistory = { bottomBarNavController.navigate(AppRoutes.WORKOUT_HISTORY) },
                        onOpenRecords = { bottomBarNavController.navigate(AppRoutes.PERSONAL_RECORDS) },
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
                        prefillFor = ::prefillFor,
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

                    ExerciseListDetailScreen(
                        // The route arg is the name at navigation time; prefer the live one so a rename shows
                        listName = selectedList?.name ?: listName,
                        exercises = exercisesOf(selectedList),
                        targetSets = targetSetsOf(selectedList),
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
                        prefillFor = ::prefillFor,
                        onRemoveExercise = { name -> userDataManager.removeExerciseFromList(listId, exerciseIdOf(name), currentUser) },
                        onReorderExercises = { ids -> userDataManager.updateListOrder(listId, ids, currentUser) },
                        onStartWorkout = { bottomBarNavController.navigate(AppRoutes.WORKOUT.replace("{listId}", listId)) }
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
                composable(
                    route = AppRoutes.WORKOUT,
                    arguments = listOf(androidx.navigation.navArgument("listId") { type = androidx.navigation.NavType.StringType })
                ) { backStackEntry ->
                    val workoutList = userDataManager.userExerciseLists.find { it.id == backStackEntry.arguments?.getString("listId") }
                    WorkoutScreen(
                        listName = workoutList?.name ?: "Workout",
                        exercises = exercisesOf(workoutList),
                        targetSets = targetSetsOf(workoutList),
                        history = exerciseHistory,
                        unitSystem = userDataManager.userUnitSystem,
                        isTimerRunning = timerManager.isTimerRunning,
                        timerRemainingSeconds = timerManager.timerRemainingSeconds,
                        timerTotalSeconds = timerManager.timerTotalSeconds,
                        onCloseTimer = { timerManager.stopRestTimer() },
                        prefillFor = ::prefillFor,
                        onRecordSet = ::logSet,
                        onSaveWorkout = trainingHistory::saveWorkout,
                        onFinish = { bottomBarNavController.popBackStack() }
                    )
                }
                composable(AppRoutes.WORKOUT_HISTORY) {
                    WorkoutHistoryScreen(
                        logs = trainingHistory.workoutLogs,
                        unitSystem = userDataManager.userUnitSystem,
                        onOpenLog = { id -> bottomBarNavController.navigate(AppRoutes.WORKOUT_LOG.replace("{logId}", id)) },
                        onBack = { bottomBarNavController.popBackStack() }
                    )
                }
                composable(
                    route = AppRoutes.WORKOUT_LOG,
                    arguments = listOf(androidx.navigation.navArgument("logId") { type = androidx.navigation.NavType.StringType })
                ) { backStackEntry ->
                    val logId = backStackEntry.arguments?.getString("logId")
                    WorkoutLogScreen(
                        log = trainingHistory.workoutLogs.find { it.id == logId },
                        unitSystem = userDataManager.userUnitSystem,
                        onDelete = {
                            logId?.let(trainingHistory::deleteWorkout)
                            bottomBarNavController.popBackStack()
                        },
                        onBack = { bottomBarNavController.popBackStack() }
                    )
                }
                composable(AppRoutes.PERSONAL_RECORDS) {
                    PersonalRecordsScreen(
                        records = trainingHistory.personalRecords.values.toList(),
                        unitSystem = userDataManager.userUnitSystem,
                        onOpenExercise = ::openExercise,
                        onDeleteRecord = trainingHistory::deleteRecord,
                        onBack = { bottomBarNavController.popBackStack() }
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
                        onRecordSet = { entry -> recordSetAndStartTimerAction(exerciseName, entry) },
                        onUpdateSet = { set, entry ->
                            userDataManager.updateSet(set.dateKey, set.docId, entry, currentUser, onDone = ::refreshHistory)
                        },
                        onDeleteSet = { set -> userDataManager.deleteSet(set.dateKey, set.docId, currentUser, onDone = ::refreshHistory) }
                    )
                }
            }
        }
        PrCelebrationBanner(
            celebration = celebration,
            onDismiss = { celebration = null },
            modifier = Modifier.align(Alignment.TopCenter)
        )
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
