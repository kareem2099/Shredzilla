# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned
- Real workout activity indicators on `DayPill` strip
- Per-exercise selector for the progress chart
- LazyColumn pagination for large exercise lists
- Screen transition animations
- `SavedStateHandle` persistence for analytics graph data

---

## [1.5.0] — Unreleased

### Added
- **Offline logging**: sets, edits and exercises save without internet and sync when the connection is back; an offline banner shows when there's no working connection.
- **Plates, bodyweight and per-side loads**: log stack machines by plate count, bodyweight work with optional added weight, and plate-loaded machines per side.
- **One side at a time**: left/right sets that alternate automatically, rest after both sides, and a pair counts as one set.
- **Workout mode**: guided sessions with targets, supersets, last session's sets and the change on every set, then a shareable summary.
- **History, records and sharing**: saved workouts, all-time personal records with a celebration banner, and a summary card to share as an image.
- **Set details**: warm-up, rest-pause, drop set and to-failure types (warm-ups stay out of counts, volume and records), plus tempo (lowering and hold seconds).
- **Progression help**: next-set suggestions, a barbell plate calculator and estimated 1RM for plate exercises.
- **Weekly goal and deload**: progress ring, streak, deload weeks, and a reminder after 6 hard weeks.
- **Body measurements**: right/left measurements with weekly or monthly reminders, trends, body weight and plain-language insights.
- **Exercise catalog** grouped by muscle, and drag-to-reorder exercises in a list.
- **App updates through Google Play**: in-app update prompts with background download, and a minimum supported version set from Firebase Remote Config.

### Changed
- Rest timer: lock-screen countdown, on-time alert with the screen off, vibration when rest ends, and tapping it returns to the app.
- New launcher icon.

### Removed
- The AppUpdater library and its JitPack repository; updates now come from Google Play directly.

---

## [1.4.0] — 2026-11-04

### Added
- **Glass design system** with its own palette for each profile (brand, male, female), each in light and dark, applied to every screen.
- **Redesigned onboarding**: 5 steps with a progress bar, ruler pickers for age/height/weight, and a live theme preview when choosing a profile.
- **Floating bottom bar** with Sets, Today and Settings tabs; edge-to-edge layout.
- **Ready-made programs**: Full Body, Upper/Lower, Push/Pull/Legs, Body Part, Glutes & Lower, Home. Beginner/Intermediate/Advanced levels suggest 4, 3–4 or 1–2 sets per exercise, shown as progress in each list.
- **Exercise detail** for any exercise, with 90-day history, edit/delete, heaviest set and estimated 1RM.
- **Calendar sheet** with workout-day dots and the week starting on the locale's first day.
- Delete whole lists, multi-select when adding exercises, live password-mismatch check on sign-up.

### Changed
- The exercise library is now **per user** (`users/{uid}/exercises`), with a one-time migration, plus Firestore security rules.
- Relicensed from MIT to the **Apache License 2.0**.

### Fixed
- Progress chart and exercise history were always empty, because day documents were never written.
- Signing out left the previous account's data visible to the next account.
- Account deletion left workout data behind; it now removes everything and checks for a recent login first.
- Email sign-ups now show the user's name; login no longer gets stuck when user data fails to load.
- Crash when reopening the app part-way through onboarding.
- Swipe-to-delete hid rows after cancelling; the "Recent" sort did nothing; the window panned under the status bar when the keyboard opened.

---

## [1.3.0] — 2026-07-07

### Added
- **Forgot Password Flow**: Added full password reset capability using Firebase Auth.
- **ForgotPasswordScreen**: Created a new password reset UI following the custom vertical gradient design theme, featuring an inline animated checkmark success state upon email dispatch.
- **IME Focus Chaining**: Connected custom `FocusRequester` paths across text fields to support fluid keyboard navigation (Login: Email -> Password -> Done; Register: Name -> Email -> Password -> Confirm -> Done).
- **Google Branding Compliance**: Redesigned the "Sign in with Google" button to fully align with official branding guidelines (pure white background, correct dimensions, original 4-color Google G icon).
- **Strict Accessibility Ratios**: Modified custom field colors to ensure all text and borders meet or exceed WCAG-AA contrast ratios (>4.5:1) over dark gradients.

### Changed
- **Unified Auth Design Language**: Refactored `LoginScreen` and `RegisterScreen` with premium vertical gradients (`AuthBgTop`/`AuthBgBottom`) and a lightweight circular dumbbell container representing the brand logo.
- **Centralized Color Tokens**: Migrated all hardcoded hex colors into explicit tokens inside `Color.kt` for cleaner architecture and better code reuse.
- **Lazy Composable Theme Lookup**: Performance optimized `TextFormField` and `PasswordFormField` to avoid unnecessary ThemeManager checks during recompositions when custom colors are passed.
- **Loading State Lockouts**: Disabled text inputs (`enabled = !isLoading`) and guarded keyboard action hooks during active Firebase authentication requests to prevent double-submissions and race conditions.
- **Local Validation Consolidation**: Rewrote submission validation checks into unified single-source-of-truth functions (`attemptLogin()`, `attemptRegister()`), avoiding logic drift between keyboard Enter presses and button clicks.

---

## [1.2.0] — 2026-03-22

### Added
- **Offline-First Set Logging:** Implemented Optimistic UI updates — sets render instantly in the UI before the Firebase write completes. Firestore's native offline persistence queues the write and syncs silently upon reconnection.
- **Zero-Drift Rest Timer:** Re-architected the countdown loop using delta math (`targetEndTime - System.currentTimeMillis()`). The timer is now anchored to wall-clock time rather than coroutine tick intervals, eliminating all drift from CPU load and background threading.
- **Debounced Exercise Search:** Introduced a `MutableStateFlow` search query in `MainViewModel` with a 300ms `debounce` operator. Prevents keystroke latency and ANR risk during heavy list filtering.
- **Date-Selectable Today Screen:** Users can now tap any day on the horizontal strip or open the full-screen calendar to view sets logged on any past date. `UserDataManager.loadSetsForDate()` dynamically swaps the active Firestore listener.
- **Historical Analytics Engine:** `loadHistoricalAnalytics()` fetches up to 90 days of `dailyActivity` data, aggregates total volume per exercise per day on `Dispatchers.Default`, and caches the result in-session to prevent redundant reads.
- **Dynamic Progress Chart:** `ProgressChartCanvas` draws a gradient-filled line chart with dynamic Y-axis clamping. Handles edge cases including single data points, all-equal values, and zero-range division safely.

### Changed
- `MainViewModel` now owns both `TimerManager` and `UserDataManager`, anchoring their lifecycle to `viewModelScope`. Both survive screen rotation and background interruptions without state loss.
- `startDestination` navigation state migrated from a plain `mutableStateOf` in `MainActivity` to a `SavedStateHandle`-backed `StateFlow` in `MainViewModel`. Navigation destination now survives OS-level Process Death.
- `UserDataManager.setupFirestoreListeners()` now stores all `ListenerRegistration` handles in a managed list. `clearAllListeners()` removes them atomically on logout or account deletion.
- All onboarding Firestore writes consolidated behind `FirebaseEmailPasswordAuth.updateUserOnboardingData()`, replacing scattered direct `db.collection()` calls throughout the navigation graph.
- All settings Firestore writes consolidated behind `UserDataManager.updateUserSetting(userId, key, value)`, eliminating repetitive boilerplate across Unit, Theme, Reminder, and Rest Time settings screens.
- `AppNavigationHost` migrated from `activityContext.lifecycleScope` to `mainViewModel.viewModelScope` for all coroutine launches, preventing silent cancellation during configuration changes.

### Fixed
- **Stuck Notification Bug:** Timer running notification persisted after app restart if the process was killed while the timer was active. `NotificationUtils.cancelTimerRunningNotification()` is now called on `MainActivity.onCreate()` to clear any orphaned notifications on fresh launch.
- **SetGraphScreen invisible content:** Inner `LazyColumn` had no `weight(1f)` modifier, causing it to render with zero measured height inside a `fillMaxSize` Column.
- **Double `MobileAds` initialization:** Removed redundant `MobileAds.initialize()` call from `MainActivity`. Initialization is handled exclusively in `MyApplication`.
- **App Open Ad frequency:** Removed `showAdIfAvailable()` call from `onActivityResumed`. Ads now trigger exclusively from `ProcessLifecycleOwner.onStart()`, preventing ads from showing after permission dialogs and external intents.
- Resolved `Unresolved reference: viewModelScope` compilation errors in `AppNavigationHost` and `MainAppContainer` by adding the missing `androidx.lifecycle.viewModelScope` import.
- Resolved `Unresolved reference: firestore` in `MainViewModel.deleteUserAccount()` by replacing the incorrect fully-qualified alias with a proper `Firebase.firestore` instance.

---

## [1.1.0] — 2026-03-21

### Added
- **Multi-step Onboarding Flow:** New user screens for `Gender`, `Physical Details` (height, weight, age), `Rest Time Preference`, `Initial Exercises`, and `Weekly Goal`, each saving incrementally to Firestore with `SetOptions.merge()`.
- **MainViewModel Architecture:** Dedicated ViewModel to govern critical user mutations (`updateUsername`, `deleteUserAccount`) using `viewModelScope`, completely independent of the Compose UI lifecycle.
- **Dynamic Date Resolver:** `UserDataManager.loadSetsForDate()` replaces the static date initialization, enabling dynamic listener swapping for any calendar date.
- **Zombie File Sweeper:** Automatic local image cleanup via `ImageStorageUtils.deleteImageFromInternalStorage()` triggered on profile picture replacement and account deletion.
- **Weight Conversion Helpers:** `convertToStorageWeight()` and `convertToDisplayWeight()` centralize unit conversion logic, ensuring all weights are stored in kg regardless of user display preference.
- **Atomic User Registration:** Firestore write failure during `createUser()` now triggers `firebaseUser.delete().await()` to roll back the Firebase Auth record, preventing zombie accounts.
- **Google Auth Collision Handling:** `FirebaseAuthUserCollisionException` is caught and surfaced with a clear message directing users to log in with their existing password.

### Changed
- Removed `FitnessActivity.kt` entirely. Navigation unified behind a composable `MainAppContainer` with a Jetpack Navigation `NavHost`.
- `UpdateUsernameScreen` text fields migrated from `remember` to `rememberSaveable` to preserve input across device rotations.
- `AccountScreen.isDeletingAccount` extracted from local Composable state into a ViewModel-backed `mutableStateOf`, preventing the loading indicator from disappearing on rotation.
- Replaced repetitive Firestore update calls across settings screens with the centralized `updateUserSetting(userId, key, value)` helper.
- Separated authentication logic into `FirebaseEmailPasswordAuth` and `FirebaseGoogleAuth` for single-responsibility and testability.

### Fixed
- **Memory Leaks:** Firestore snapshot listeners were never removed on logout. Introduced a `ListenerRegistration` list with systematic `.remove()` calls via `clearAllListeners()`.
- Fixed `viewModelScope` import errors causing compilation failures in navigation router files.
- Fixed `FirebaseFirestore.getInstance()` reference errors in `MainViewModel` by using the correct `Firebase.firestore` KTX extension.

---

## [1.0.0] — Initial Release

### Added
- Minimum Viable Product (MVP) core logic
- Android app framework using Kotlin and Jetpack Compose
- Basic exercise set tracking and Firebase Authentication prototype