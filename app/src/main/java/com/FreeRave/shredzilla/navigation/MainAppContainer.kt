package com.FreeRave.shredzilla.navigation

import com.FreeRave.shredzilla.utils.weeklyProgress
import com.FreeRave.shredzilla.models.SetType
import com.FreeRave.shredzilla.screens.body.BodyMeasurementsScreen
import com.FreeRave.shredzilla.screens.body.MeasurementEntryScreen
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
import com.FreeRave.shredzilla.models.workingSetCount
import com.FreeRave.shredzilla.models.completedSetCount
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
import com.FreeRave.shredzilla.utils.nextEntry
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
import com.FreeRave.shredzilla.screens.pro.ProScreen
import com.FreeRave.shredzilla.screens.badges.BadgesScreen
import com.FreeRave.shredzilla.notifications.PendingNavigation
import com.FreeRave.shredzilla.notifications.ReminderScheduler
import com.FreeRave.shredzilla.notifications.ReminderStore
import com.FreeRave.shredzilla.notifications.TrainingSnapshot
import com.FreeRave.shredzilla.notifications.rememberNotificationPermission
import com.FreeRave.shredzilla.screens.pro.SupportScreen
import com.FreeRave.shredzilla.screens.stats.StatsScreen
import com.FreeRave.shredzilla.pro.BillingEvent
import com.FreeRave.shredzilla.pro.LocalProAccess
import com.FreeRave.shredzilla.pro.findActivity
import com.FreeRave.shredzilla.pro.rememberProAccess
import com.FreeRave.shredzilla.ui.theme.AccentTheme
import com.FreeRave.shredzilla.ui.glass.GlassDialog
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
    val billing = mainViewModel.billing

    val proAccess = rememberProAccess(
        ownsLifetime = billing.ownsProLifetime,
        bank = userDataManager.adTimeBank,
        supporterTips = userDataManager.supporterTips,
        openPro = remember(bottomBarNavController) { { bottomBarNavController.navigate(AppRoutes.PRO) { launchSingleTop = true } } }
    )
    // Accent themes are Pro; when Pro ends the profile's own colors come back, and the choice is kept
    LaunchedEffect(proAccess.isPro, userDataManager.accentThemeKey) {
        ThemeManager.accentTheme = if (proAccess.isPro) AccentTheme.fromStored(userDataManager.accentThemeKey) else null
    }

    // Purchases can finish while the user is elsewhere (pending payments, restores), so listen app-wide
    var billingMessage by remember { mutableStateOf<Pair<String, String>?>(null) }
    LaunchedEffect(billing) {
        billing.events.collect { event ->
            billingMessage = when (event) {
                BillingEvent.ProUnlocked -> "Welcome to Pro" to "Every Pro feature is unlocked for life. Thank you for backing DotShredzilla!"
                is BillingEvent.TipReceived -> {
                    userDataManager.recordSupporterTip(firebaseEmailAuthManager.getCurrentUser())
                    "Thank you!" to "Your ${event.tip.title.lowercase()} keeps DotShredzilla growing. You're now a Supporter."
                }
                BillingEvent.Pending -> "Payment pending" to "Google Play will finish this purchase when the payment clears. It unlocks automatically."
                is BillingEvent.Failed -> "Purchase didn't complete" to event.message
            }
        }
    }
    fun buy(productId: String) {
        context.findActivity()?.let { billing.buy(it, productId) }
    }

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
        exerciseHistory.mapValues { (_, sets) -> sets.filter { it.dateKey == todayKey }.completedSetCount() }
    }

    // A training day is any day with a working set; warm-ups alone don't make a workout
    val weeklyProgress = remember(exerciseHistory, userDataManager.userWeeklyGoalDays, userDataManager.deloadWeeks) {
        val trainingDays = exerciseHistory.values.flatten().filter { it.setType != SetType.WARMUP }.map { it.dateKey }.toSet()
        weeklyProgress(trainingDays, userDataManager.userWeeklyGoalDays, userDataManager.deloadWeeks)
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

    val workout = mainViewModel.workout
    val badges = mainViewModel.badges
    val badgeCount = badges.statuses.filter { it.badge.category != com.FreeRave.shredzilla.badges.BadgeCategory.EVENTS || it.isEarned }
    val earnedBadgeEmojis = badges.statuses.filter { it.isEarned }
        .sortedWith(compareByDescending<com.FreeRave.shredzilla.badges.BadgeStatus> { it.badge.tier.ordinal }.thenByDescending { it.earnedAtMillis ?: 0L })
        .map { it.badge.emoji }

    // --- Notifications: permission, the on-device snapshot the daily check reads, and taps ---
    val reminderStore = remember { ReminderStore(context) }
    val notificationPermission = rememberNotificationPermission()
    var showNotificationExplainer by remember { mutableStateOf(false) }

    LaunchedEffect(userDataManager.notificationChoices) {
        reminderStore.saveChoices(userDataManager.notificationChoices)
        ReminderScheduler.syncDaily(context)
        ReminderScheduler.syncProEnding(context)
    }
    LaunchedEffect(exerciseHistory, trainingHistory.workoutLogs, userDataManager.userExerciseLists, userDataManager.userWeeklyGoalDays,
        userDataManager.deloadWeeks, trainingHistory.measurements, userDataManager.measurementCadence) {
        val lists = userDataManager.userExerciseLists
        val nextList = nextListAfter(trainingHistory.workoutLogs.firstOrNull()?.listName, lists)
        reminderStore.saveSnapshot(
            TrainingSnapshot(
                trainingDays = exerciseHistory.values.flatten().filter { it.setType != SetType.WARMUP }.map { it.dateKey }.toSet(),
                weeklyGoal = userDataManager.userWeeklyGoalDays,
                deloadWeeks = userDataManager.deloadWeeks,
                lastMeasurementMillis = trainingHistory.measurements.maxOfOrNull { it.takenAtMillis },
                measurementCadenceDays = userDataManager.measurementCadence.days,
                nextListId = nextList?.id,
                nextListName = nextList?.name,
            )
        )
    }
    LaunchedEffect(userDataManager.adTimeBank.untilMillis, billing.ownsProLifetime) {
        reminderStore.saveProTime(userDataManager.adTimeBank.untilMillis, billing.ownsProLifetime)
        ReminderScheduler.syncProEnding(context)
    }
    // The workout notification's Finish button: save it here, the workout screen then shows the summary
    LaunchedEffect(PendingNavigation.finishWorkout) {
        if (PendingNavigation.finishWorkout) {
            PendingNavigation.finishWorkout = false
            workout.finish()
        }
    }
    // A tapped notification's screen, once the app is up
    LaunchedEffect(PendingNavigation.route) {
        val route = PendingNavigation.route ?: return@LaunchedEffect
        PendingNavigation.route = null
        runCatching { bottomBarNavController.navigate(route) { launchSingleTop = true } }
            .onFailure { Log.w("Notifications", "Can't open $route", it) }
    }

    // The first rest is when the lock-screen timer matters, so that's when to ask for notifications
    fun afterRestStarted(rested: Boolean) {
        if (rested && !notificationPermission.granted && !reminderStore.permissionPrompted) showNotificationExplainer = true
    }

    fun logSet(exName: String, entry: SetEntry, startRest: Boolean = true) = afterRestStarted(workout.logSet(exName, entry, startRest))

    val recordSetAndStartTimerAction: (String, SetEntry) -> Unit = { exName, entry -> logSet(exName, entry) }

    // A new set starts from the last one logged, so machine exercises stay in plates and the load is
    // ready; the suggestion is the progression step from the last session
    fun prefillFor(exerciseName: String): SetPrefill = workout.prefillFor(exerciseName)

    val navBackStackEntry by bottomBarNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // Only the tab roots show the bar; every pushed page (settings sub-pages, details, editors) hides it
    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    // One shared backdrop for every tab, so switching tabs never flashes a new background.
    // Screens draw their own status-bar inset (top bars); the bottom bar handles the nav-bar inset.
    val isOnline = rememberIsOnline()
    CompositionLocalProvider(LocalProAccess provides proAccess) {
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
                        onNavigateToExerciseDetail = ::openExercise,
                        weeklyProgress = weeklyProgress,
                        showDeloadReminder = weeklyProgress.deloadDue &&
                            userDataManager.deloadReminderDismissedWeek != weeklyProgress.thisWeekKey,
                        onSetDeloadWeek = { isDeload ->
                            userDataManager.setDeloadWeek(weeklyProgress.thisWeekKey, isDeload, currentUser)
                        },
                        onDismissDeloadReminder = {
                            userDataManager.dismissDeloadReminder(weeklyProgress.thisWeekKey, currentUser)
                        },
                        workouts = trainingHistory.workoutLogs
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
                        onOpenStats = { bottomBarNavController.navigate(AppRoutes.STATS) },
                        onOpenBadges = { bottomBarNavController.navigate(AppRoutes.BADGES) },
                        badgeSummary = "${earnedBadgeEmojis.size} of ${badgeCount.size} earned" + earnedBadgeEmojis.take(4).joinToString("") { " $it" },
                        measurementCount = trainingHistory.measurements.size,
                        onOpenBody = { bottomBarNavController.navigate(AppRoutes.BODY_MEASUREMENTS) },
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
                        lengthUnit = userDataManager.userLengthUnit,
                        themeSetting = effectiveThemeSetting,
                        workoutReminderSetting = userDataManager.userWorkoutReminderSetting,
                        restTimePreference = userDataManager.userRestTimePreference,
                        onNavigateToAccount = { bottomBarNavController.navigate(AppRoutes.ACCOUNT_SETTINGS) },
                        onNavigateToUnitSettings = { bottomBarNavController.navigate(AppRoutes.UNIT_SETTINGS) },
                        onNavigateToWorkoutReminders = { bottomBarNavController.navigate(AppRoutes.WORKOUT_REMINDERS) },
                        onNavigateToDefaultRestTimeSettings = { bottomBarNavController.navigate(AppRoutes.DEFAULT_REST_TIME_SETTINGS) },
                        onNavigateToThemeSettings = { bottomBarNavController.navigate(AppRoutes.THEME_SETTINGS) },
                        proSource = proAccess.source,
                        proRemainingMillis = userDataManager.adTimeBank.remainingMillis(System.currentTimeMillis()),
                        isSupporter = proAccess.isSupporter,
                        onNavigateToPro = proAccess.openPro,
                        onNavigateToSupport = { bottomBarNavController.navigate(AppRoutes.SUPPORT) },
                        badgeEmojis = earnedBadgeEmojis.take(6),
                        badgeCountLabel = "${earnedBadgeEmojis.size}/${badgeCount.size}",
                        onNavigateToBadges = { bottomBarNavController.navigate(AppRoutes.BADGES) }
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
                    UnitSettingsScreen(
                        onNavigateBack = { bottomBarNavController.popBackStack() },
                        weightUnit = userDataManager.userUnitSystem,
                        lengthUnit = userDataManager.userLengthUnit,
                        onWeightUnitChange = { unit ->
                            userDataManager.userUnitSystem = unit
                            userDataManager.updateUserSetting(currentUser?.uid, "unitSystem", unit.name)
                        },
                        onLengthUnitChange = { unit ->
                            userDataManager.userLengthUnit = unit
                            userDataManager.updateUserSetting(currentUser?.uid, "lengthUnit", unit.storedValue)
                        }
                    )
                }
                composable(AppRoutes.WORKOUT_REMINDERS) {
                    NotificationSettingsScreen(
                        choices = userDataManager.notificationChoices,
                        hasWeeklyGoal = userDataManager.userWeeklyGoalDays != null,
                        measurementRhythm = userDataManager.measurementCadence.label,
                        onChange = { choices -> userDataManager.updateNotificationChoices(choices, currentUser) },
                        onNavigateBack = { bottomBarNavController.popBackStack() }
                    )
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
                        },
                        accentTheme = AccentTheme.fromStored(userDataManager.accentThemeKey),
                        onAccentThemeChange = { theme -> userDataManager.updateAccentTheme(theme?.storedValue, currentUser) }
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
                    val listId = backStackEntry.arguments?.getString("listId").orEmpty()
                    val workoutList = userDataManager.userExerciseLists.find { it.id == listId }
                    val listName = workoutList?.name ?: "Workout"
                    val listExercises = exercisesOf(workoutList)
                    val session = remember(listId) { workout.start(listId, listName, listExercises, targetSetsOf(workoutList)) }
                    // Keep the running session in step with edits to the list, after composition
                    SideEffect {
                        session.listName = listName
                        session.exercises = listExercises
                        session.targetSets = targetSetsOf(workoutList)
                    }
                    WorkoutScreen(
                        session = session,
                        history = exerciseHistory,
                        unitSystem = userDataManager.userUnitSystem,
                        isTimerRunning = timerManager.isTimerRunning,
                        timerRemainingSeconds = timerManager.timerRemainingSeconds,
                        timerTotalSeconds = timerManager.timerTotalSeconds,
                        onCloseTimer = { timerManager.stopRestTimer() },
                        prefillFor = ::prefillFor,
                        onRecordSet = { entry -> afterRestStarted(workout.logInWorkout(entry)) },
                        onFinishWorkout = { workout.finish() },
                        onLeave = {
                            workout.end()
                            bottomBarNavController.popBackStack()
                        }
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
                composable(AppRoutes.BODY_MEASUREMENTS) {
                    fun openEntry(id: String) = bottomBarNavController.navigate(AppRoutes.MEASUREMENT_ENTRY.replace("{measurementId}", id))
                    BodyMeasurementsScreen(
                        measurements = trainingHistory.measurements,
                        unitSystem = userDataManager.userUnitSystem,
                        lengthUnit = userDataManager.userLengthUnit,
                        cadence = userDataManager.measurementCadence,
                        onCadenceChange = { userDataManager.updateMeasurementCadence(it, currentUser) },
                        onNew = { openEntry("new") },
                        onOpen = ::openEntry,
                        onBack = { bottomBarNavController.popBackStack() }
                    )
                }
                composable(
                    route = AppRoutes.MEASUREMENT_ENTRY,
                    arguments = listOf(androidx.navigation.navArgument("measurementId") { type = androidx.navigation.NavType.StringType })
                ) { backStackEntry ->
                    val measurementId = backStackEntry.arguments?.getString("measurementId")
                    val existing = trainingHistory.measurements.find { it.id == measurementId }
                    MeasurementEntryScreen(
                        existing = existing,
                        // Hints come from the session before this one, or the latest when adding a new one
                        previous = trainingHistory.measurements.firstOrNull {
                            it.id != measurementId && (existing == null || it.takenAtMillis < existing.takenAtMillis)
                        },
                        unitSystem = userDataManager.userUnitSystem,
                        lengthUnit = userDataManager.userLengthUnit,
                        cadence = userDataManager.measurementCadence,
                        onSave = { measurement ->
                            trainingHistory.saveMeasurement(measurement)
                            bottomBarNavController.popBackStack()
                        },
                        onDelete = existing?.let { entry ->
                            {
                                trainingHistory.deleteMeasurement(entry.id)
                                bottomBarNavController.popBackStack()
                            }
                        },
                        onBack = { bottomBarNavController.popBackStack() }
                    )
                }
                composable(AppRoutes.STATS) {
                    StatsScreen(
                        history = exerciseHistory,
                        unitSystem = userDataManager.userUnitSystem,
                        exerciseName = ::exerciseNameFor,
                        onBack = { bottomBarNavController.popBackStack() },
                        workouts = trainingHistory.workoutLogs
                    )
                }
                composable(AppRoutes.PRO) {
                    ProScreen(
                        proSource = proAccess.source,
                        adTimeBank = userDataManager.adTimeBank,
                        supporterTips = userDataManager.supporterTips,
                        prices = billing.prices,
                        storeAvailable = billing.storeAvailable,
                        onBuy = ::buy,
                        onRestore = {
                            billing.restorePurchases()
                            Toast.makeText(context, "Checking Google Play for your purchases…", Toast.LENGTH_SHORT).show()
                        },
                        userId = currentUser?.uid,
                        onBack = { bottomBarNavController.popBackStack() },
                        onOpenSupport = { bottomBarNavController.navigate(AppRoutes.SUPPORT) { launchSingleTop = true } }
                    )
                }
                composable(AppRoutes.BADGES) {
                    BadgesScreen(
                        statuses = badges.statuses,
                        unitSystem = userDataManager.userUnitSystem,
                        onBack = { bottomBarNavController.popBackStack() }
                    )
                }
                composable(AppRoutes.SUPPORT) {
                    SupportScreen(
                        supporterTips = userDataManager.supporterTips,
                        prices = billing.prices,
                        onBuy = ::buy,
                        onOpenPro = proAccess.openPro,
                        onBack = { bottomBarNavController.popBackStack() }
                    )
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
        // A record first, then any badge it unlocked
        PrCelebrationBanner(
            celebration = workout.celebration ?: badges.celebration,
            onDismiss = { if (workout.celebration != null) workout.celebration = null else badges.dismissCelebration() },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
    if (showNotificationExplainer) {
        GlassDialog(
            title = "Turn on notifications?",
            message = "Then the rest timer shows on your lock screen and buzzes when it's time for your next set. You can also get the reminders you choose in Settings, Notifications.",
            confirmText = "Turn on",
            dismissText = "Not now",
            onConfirm = {
                showNotificationExplainer = false
                notificationPermission.request()
            },
            onDismiss = {
                showNotificationExplainer = false
                reminderStore.permissionPrompted = true
            }
        )
    }
    billingMessage?.let { (title, message) ->
        GlassDialog(
            title = title,
            message = message,
            confirmText = "Nice",
            onConfirm = { billingMessage = null },
            onDismiss = { billingMessage = null },
            dismissText = "Close"
        )
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

/**
 * The list to suggest after a workout on [lastListName]: the next day of the same split
 * ("Split · Back" after "Split · Chest"), wrapping around; the same list otherwise; the first
 * list when there's no workout yet. [lists] is in display order, which keeps split days in order.
 */
internal fun nextListAfter(lastListName: String?, lists: List<UserExerciseList>): UserExerciseList? {
    if (lastListName == null) return lists.firstOrNull()
    val last = lists.find { it.name == lastListName } ?: return lists.firstOrNull()
    val prefix = last.name.substringBefore(" · ", missingDelimiterValue = "").takeIf { it.isNotEmpty() } ?: return last
    val days = lists.filter { it.name.startsWith("$prefix · ") }
    return days.getOrNull((days.indexOf(last) + 1) % days.size)
}
