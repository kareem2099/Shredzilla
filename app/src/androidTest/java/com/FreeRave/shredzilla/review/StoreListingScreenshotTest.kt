package com.FreeRave.shredzilla.review

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import com.FreeRave.shredzilla.composables.AppBottomNavigationBar
import com.FreeRave.shredzilla.models.*
import com.FreeRave.shredzilla.screens.body.BodyMeasurementsScreen
import com.FreeRave.shredzilla.screens.history.PersonalRecordsScreen
import com.FreeRave.shredzilla.screens.history.WorkoutLogScreen
import com.FreeRave.shredzilla.screens.settings.ThemeSetting
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.screens.sets.SetGraphScreen
import com.FreeRave.shredzilla.screens.today.TodayScreen
import com.FreeRave.shredzilla.screens.workout.WorkoutScreen
import com.FreeRave.shredzilla.ui.glass.GlassBackground
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.ThemeManager
import com.FreeRave.shredzilla.utils.*
import com.FreeRave.shredzilla.viewmodels.MainViewModel
import com.google.firebase.Timestamp
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.Date

/** Actual production screen composables, populated only with local demonstration fixtures. */
@RunWith(Parameterized::class)
class StoreListingScreenshotTest(private val page: String, private val tablet: Boolean) {
    @get:Rule val compose = createComposeRule()

    @Before fun requireCaptureApp() {
        assumeTrue("Run with -PstoreListingCapture=true", InstrumentationRegistry.getInstrumentation() is StoreCaptureRunner)
        check(InstrumentationRegistry.getInstrumentation().targetContext.packageName.endsWith(".storecapture"))
        ThemeManager.currentGenderTheme = "Male"
        ThemeManager.themePreferenceMale = if (page in listOf("records", "body")) ThemeSetting.LIGHT else ThemeSetting.DARK
    }

    @Test fun renderStoreAsset() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            val original = LocalConfiguration.current
            val width = if (tablet) 840 else original.screenWidthDp
            val height = if (tablet) 1120 else original.screenHeightDp
            val density = LocalDensity.current.density * original.screenWidthDp / width
            val config = Configuration(original).apply { screenWidthDp = width; screenHeightDp = height; fontScale = 1f }
            CompositionLocalProvider(LocalConfiguration provides config, LocalDensity provides Density(density, 1f),
                LocalLayoutDirection provides LayoutDirection.Ltr) {
                ShredzillaTheme(dynamicColor = false) {
                    Box(Modifier.requiredSize(width.dp, height.dp).testTag("store-capture")) {
                        GlassBackground { StorePage(page) }
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()
        val bitmap = compose.onNodeWithTag("store-capture").captureToImage().asAndroidBitmap()
        val directory = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        File(directory, "store-${if (tablet) "tablet" else "phone"}-$page.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0} tablet={1}") fun cases() =
            listOf("home", "workout", "today", "history", "records", "body").map { arrayOf<Any>(it, false) } +
                listOf("home", "workout", "history", "body").map { arrayOf<Any>(it, true) }
    }
}

private val sampleTime = System.currentTimeMillis()
private val day = 86_400_000L
private val unit = UnitSystem.METRIC
private val exercises = listOf("Bench Press", "Shoulder Press", "Chest Fly").map { ExerciseDisplayInfo(exerciseIdOf(it), it, "Last session") }
private val chart = (0..9).map { MainViewModel.ChartDataPoint(sampleTime - (9-it)*7*day, 900.0+it*10) }
private val samples = exercises.mapIndexed { index, exercise ->
    exercise.id to (0..2).map { set -> LoggedSet("example-$index-$set", dateKeyOf(sampleTime-3*day), exercise.id,
        10, listOf(60.0, 35.0, 15.0)[index], "", sampleTime-3*day+set*1000) }
}.toMap()
private val todaySets = exercises.flatMapIndexed { index, exercise -> (1..3).map { number ->
    RecordedSet(exercise.name, number, 10, listOf(60f,35f,15f)[index], timestamp=Timestamp(Date(sampleTime)), exerciseId=exercise.id)
} }
private val workoutLog = WorkoutLog("example-session", "Push day", sampleTime-day, 42*60_000L,
    9, 3300.0, exercises.mapIndexed { index, exercise ->
        WorkoutExerciseSummary(exercise.id, exercise.name, 3, SetLoad(listOf(60.0,35.0,15.0)[index],null,10))
    }, listOf(WorkoutPr("Bench Press",SetLoad(60.0,null,10))))

@Composable private fun StorePage(page: String) {
    if (page in listOf("home", "today")) {
        val nav = rememberNavController()
        Scaffold(containerColor=Color.Transparent, contentWindowInsets=WindowInsets(0.dp),
            bottomBar={ AppBottomNavigationBar(nav) }) { padding ->
            NavHost(nav, startDestination=if (page=="home") "sets" else "today_screen", modifier=Modifier.padding(padding)) {
                composable("sets") { HomePage() }
                composable("today_screen") {
                    TodayScreen(activeDays=setOf(startOfDay(),startOfDay(Date(sampleTime-2*day))), totalReps=90,
                        totalSets=9, uniqueExercises=3, recordedSetsToday=todaySets, unitSystem=unit,
                        onDateSelected={}, onNavigateToExerciseDetail={}, weeklyProgress=WeeklyProgress(weekKeyOf(sampleTime),2,3,3,3,false),
                        showDeloadReminder=false, onSetDeloadWeek={}, onDismissDeloadReminder={})
                }
            }
        }
        return
    }
    when(page) {
        "workout" -> WorkoutScreen("Push day",exercises,3..4,samples,unit,true,48,90,{},
            prefillFor={SetPrefill(SetEntry(reps=10,weight=60.0),SetSuggestion(SetEntry(11,60.0),SetEntry(10,60.0)))},
            onRecordSet={_,_,_->},onSaveWorkout={},onFinish={})
        "history" -> WorkoutLogScreen(workoutLog,unit,onDelete={},onBack={})
        "records" -> PersonalRecordsScreen(listOf(
            PersonalRecord("bench_press","Bench Press",RecordSet(60.0,10,sampleTime-day),null),
            PersonalRecord("squat","Squat",RecordSet(80.0,8,sampleTime-2*day),null),
            PersonalRecord("deadlift","Deadlift",RecordSet(95.0,5,sampleTime-4*day),null),
            PersonalRecord("cable_row","Cable Row",null,RecordSet(7.0,12,sampleTime-5*day))
        ),unit,onOpenExercise={},onDeleteRecord={},onBack={})
        "body" -> BodyMeasurementsScreen((0..3).map { index -> BodyMeasurement("example-$index",sampleTime-index*7*day,
            mapOf("arm_flexed_right" to 32.0-index*.2,"arm_flexed_left" to 31.5-index*.2,
                "chest" to 98.0-index*.2,"waist" to 80.0+index*.2),73.0,"Example reading") },unit,
            MeasurementCadence.WEEKLY,onCadenceChange={},onNew={},onOpen={},onBack={})
    }
}

@Composable private fun HomePage() {
    SetGraphScreen(mapOf("bench_press" to chart),false,24,
        listOf(UserExerciseList("push","Push day",exercises.map { it.id }),
            UserExerciseList("pull","Pull day",listOf("row","pull_up"))),"Alex",
        onNavigateToExerciseList={},onOpenPrograms={},recommendedProgramName="Push / Pull / Legs",workoutCount=12,recordCount=4,
        onOpenHistory={},onOpenRecords={},measurementCount=4,onOpenBody={},onNavigateToCreateNewList={},
        onNavigateToExerciseListDetail={_,_->},isTimerRunning=false,timerRemainingSeconds=0,timerTotalSeconds=90,onCloseTimer={})
}
