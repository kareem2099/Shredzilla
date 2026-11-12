# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned
- Translations: Arabic (right-to-left), Russian, German, French and Spanish, with an in-app language choice
- Real workout activity indicators on `DayPill` strip
- Per-exercise selector for the progress chart

---

## [1.7.1] — 2026-11-12

Released instead of 1.7.0, which was approved but never published; it includes everything in 1.7.0.

### Fixed
- **Signed out on every launch.** Android backup restored Firebase Auth's encrypted session without the Keystore key that opens it, so the session could never be read again. Those files are now excluded from cloud backup and device transfer, and an unreadable key is reset at startup so the next sign-in sticks.
- The app no longer signs you out when your profile can't be loaded (weak connection, slow start): it opens and syncs later. Only an account that was deleted or disabled is signed out.
- Google and email sign-in no longer wait for the server to save the profile, which hung on weak connections and then signed the user out. Whether onboarding is done is remembered on the device for faster starts.

### Changed
- **Founding member is exactly the first 100 accounts**, numbered #1 to #100 by sign-up order. A Cloud Function (`assignFoundingNumber`) gives the number when an account is created and numbers existing accounts once. It is stored in `founders/{uid}`, which members can read but not write, apart from the profile so no app version's profile write can touch it; a deleted account's number is removed and never reused. The badge shows "Founding member #N". Replaces the `founding_cutoff` date.
- Version name **1.7.1**, version code **12**.

---

## [1.7.0] — 2026-11-11

### Added
- **Badges, free for everyone.** 36 badges in milestones, training, strength, consistency, body and community: first set and workout, training days and sets, splits, supersets, one-sided sets, tempo, early-bird and night-owl sets, personal records, weight clubs (60–180 kg, or 135–405 lb), ten tonnes in a day, weekly-goal streaks, deloads, measurements, Founding member, Pro for life and Supporter tiers. A banner celebrates each new one; badges earned before are added quietly.
- **Event badges** defined in Remote Config (`badge_events`), with a live card, countdown and progress; earned event badges stay on the account after the event. `founding_cutoff` sets the last sign-up date for Founding member.
- **Live workout notification**: log the last set again, +1 rep, type a different set ("8" or "9 8"), move to the next exercise or finish, all from the lock screen, with the rest countdown inside. Four looks: Detailed and Compact (free), Rest focus and Progress bar (Pro; a Live Update chip on Android 16).
- **Notifications**: the permission is requested on the first rest; workout reminders after the days you choose (opening the next day of your split), a weekly-goal alert on the last chance to hit it, measurement reminders following your rhythm and a Pro-time-ending alert. A Notifications page has a switch for each, a reminder time and quiet hours (10 PM–8 AM).
- **Separate units** for weights (kg or lb) and body measurements (cm or in), also chosen during onboarding, defaulting to the phone's region.
- **Support the developer** has its own page; the Pro page lists the newest Pro features.

### Fixed
- Today numbered every set "1"; left and right now share a number (1, 1, 2, 3) and warm-ups show "W". Set details are on their own line instead of cut off.
- Workout mode moved on before the last right-side set; a one-sided set now counts only once both sides are logged.
- One-sided sets on per-side machines counted double volume.
- An exercise card's reps included warm-ups; the week chip wrapped onto two lines.
- Records: more reps at the same load is a new best, and the trophy marks the day's best set.

### Changed
- The camera permission is gone: "Take photo" uses the system camera app.
- Edge-to-edge: no deprecated status-bar color or full-screen splash in the themes.
- R8 rules no longer keep whole libraries; the release bundle is about 900 KB smaller.
- Version name **1.7.0**, version code **11**.

---

## [1.6.0] — 2026-11-07

### Added
- **DotShredzilla Pro.** Pro for life is a one-time Google Play purchase (`pro_lifetime`, $4.99), restored automatically on new devices and reinstalls.
- **Pro hours from ads.** One optional rewarded ad gives one hour of Pro. Hours stack up to 8 at a time, with at most 5 ads in any 24 hours. AdMob server-side verification calls the new `admobReward` Cloud Function, which checks Google's signature and banks the hour using server time; Firestore rules stop the app from writing Pro time itself.
- **Training time.** Today shows how long you trained (workouts by their measured length, other sets grouped into sessions by the gaps between them), with each session's start and end. Stats show time this week and a Time view of the last 8 weeks.
- **Supporter tips.** A pre-workout shot ($0.99), a protein scoop ($2.99) and a creatine tub ($4.99) are repeatable tips that add a Supporter badge. They don't unlock Pro.
- **Training stats (Pro).** Hard sets per muscle this week, compared with last week and the 10–20 set range; estimated 1RM trend per exercise; sets and volume over the last 8 weeks.
- **Accent themes (Pro).** Inferno, Ocean, Gold and Volt recolor accents, gradients and the background glow on top of the profile palette.
- **Share card styles (Pro).** Midnight and Accent looks for workout summaries.

### Fixed
- Today numbered every set "1"; sets are now numbered 1, 2, 3… per exercise and side, warm-ups show "W", and a left + right pair counts as one set.

### Changed
- The next-set coach, last-session comparison ("Last time" and "+2 reps") and body insights and trend charts are now Pro. Free Pro previews show what each feature does.
- Logging, lists, programs, workout mode, supersets, the rest timer, plate calculator, records, weekly goal, streaks, deload reminders, measurements, history and sharing stay free.
- Version name **1.6.0**, version code **10**.

### Removed
- The app-open ad and the old "Earn rewards" screen. The app shows no ads unless you choose to watch one on the Pro page.

---

## [1.5.1] — 2026-11-06

### Fixed
- Profile photos now use Android's system photo picker, with the AndroidX document-picker fallback on devices without a photo picker. The app only receives access to the selected image.
- Removed broad photo/shared-storage permissions and the gallery permission prompt; camera capture keeps its separate permission flow.
- Registration links now open DotShredzilla's own DotSuite terms and privacy pages, including the five-language selector.

### Changed
- Version name **1.5.1**, version code **9**. Compile/target SDK remains **37**, minimum SDK **29**.
- Supersedes the blocked version-code-8 upload. Old testing artifacts requesting broad photo access must also be replaced or deactivated in Play Console.

---

## [1.5.0] — 2026-11-06

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