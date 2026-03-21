# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-03-21

### Added
- **Multi-step Onboarding Flow:** Beautiful Jetpack Compose screens for new users to set `Gender`, and physical details (`Height`, `Weight`, `Experience Level`).
- **State Hoisted Architecture:** Dedicated `MainViewModel` engines to govern critical user mutations (`updateUsername`, `deleteUserAccount`) completely independent of the Compose UI lifecycle map.
- **Dynamic Midnight Resolver:** Extracted the "Today's Sets" listener `snapshotListener` off of strict App Initialization static dates, converting it to a dynamic refreshable request via `setupTodaySetsListener(userId)`.
- **Zombie File Sweeper:** Programmed an automatic garbage collection wipe using `ImageStorageUtils.deleteImageFromInternalStorage()` upon Profile Picture resets and outright Account Deletion routines.
- **Conversion Engines:** Abstracted mathematically hardcoded calculations (`* 0.45359237`) into mathematically isolated helpers `convertToStorageWeight` (Lbs to Kg) and `convertToDisplayWeight` (Kg to Lbs).

### Changed
- Refactored `FitnessActivity.kt` entirely out of the repository structure; unified navigation behind a purely composable `MainAppContainer`.
- Replaced vulnerable generic `remember { mutableStateOf() }` text bindings in `UpdateUsernameScreen` with `rememberSaveable` to actively preserve typing strings amidst Device Orientations.
- Re-architected Account Deletion procedures with immutable ViewModel-based process indicators (`isDeletingAccount`).
- Unified repetitive single-line Firestore document updates behind a versatile generic handler `updateUserSetting(userId, key, value)`.
- Replaced monolithic single-file authentications with separated robust abstractions: `FirebaseEmailPasswordAuth` and `FirebaseGoogleAuth`.

### Fixed
- **Massive Memory Leaks:** Identified and cured eternal background snapshot observer bindings. Instantiated a strict `ListenerRegistration` cache within `UserDataManager` executing a systematic `.remove()` iteration triggered immediately on Account Logout.
- Repaired compilation syntax parameter mismatches concerning implicit `androidx.lifecycle.viewModelScope` bindings across the isolated navigation routers.
- Engine corrected Firebase singleton reference aliases (`FirebaseFirestore.getInstance()`) preventing arbitrary compiler failure on network dispatch.

## [1.0.0] - Initial Release

### Added
- Minimum Viable Product (MVP) core logic.
- Initial Android App Framework utilizing Kotlin and modern Jetpack Compose.
- Basic Exercise sets tracking mechanisms and authentication prototypes.
