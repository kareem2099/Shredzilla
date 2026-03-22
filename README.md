# Shredzilla 🦖🏋️

> A high-performance Android workout tracking application built for serious weightlifters who demand speed, reliability, and a crash-resistant experience.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-orange.svg)](https://firebase.google.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## Overview

Shredzilla replaces your workout notebook with a blazing-fast, offline-capable fitness tracker. It is engineered with a strict focus on **modern Android architecture**, **persistent state management**, and **seamless real-time data synchronization** — so your data is always safe, even in the deepest gym dead-zones.

---

## Features

### Core Workout Experience
- **Offline-First Set Logging** — Record sets instantly with zero latency. Firebase's native offline persistence silently syncs to the cloud once connectivity is restored.
- **Zero-Drift Rest Timer** — Delta-math anchored countdown timer (`endTime - currentTimeMillis`) guarantees atomic precision regardless of CPU load or background threading. Survives app minimization without losing a single second.
- **Debounced Exercise Search** — Kotlin `StateFlow` with a 300ms debounce prevents keystroke input lag and eliminates ANR risk during heavy list filtering.

### Architecture & Stability
- **Process Death Resilience** — `SavedStateHandle` + `StateFlow` preserves navigation destination across OS-level app kills. Users always return to exactly where they left off.
- **Configuration Change Safety** — `MainViewModel` owns `TimerManager` and `UserDataManager`, anchoring their lifecycle to `viewModelScope` — completely immune to screen rotations.
- **Zero Memory Leaks** — A strict `ListenerRegistration` cache in `UserDataManager` systematically severs all Firestore snapshot observers on logout and account deletion.
- **Atomic User Creation** — Firestore write failure during registration triggers an automatic Firebase Auth rollback, eliminating zombie accounts stuck in a broken state.

### Authentication
- **Multi-Provider Auth** — Supports Email/Password and Google Sign-In via Firebase Authentication.
- **Smart Onboarding Resume** — Detects exactly which onboarding step a user completed last and resumes from there on next launch.
- **Collision Detection** — Gracefully handles duplicate email errors across providers with a clear, actionable error message.

### Analytics & Data
- **Progress Chart** — Dynamic Y-axis clamping with gradient fill renders volume progression curves that accurately represent micro-improvements without visual distortion.
- **Historical Analytics** — Background computation on `Dispatchers.Default` aggregates up to 90 days of set data with session-level caching to prevent redundant Firestore reads.
- **Unit System Support** — Full Metric (kg) and Imperial (lbs) support with centralized `convertToStorageWeight` / `convertToDisplayWeight` helpers ensuring consistent storage in kg regardless of display preference.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose (Material 3) |
| Architecture | MVVM + State Hoisting |
| Backend | Firebase Authentication + Cloud Firestore |
| Async | Kotlin Coroutines + StateFlow |
| Ads | Google AdMob (App Open Ads + Rewarded Ads) |
| Notifications | NotificationCompat (Rest Timer) |
| Image Handling | Internal Storage + FileProvider |

---

## Getting Started

### Prerequisites
- Android Studio (latest stable)
- Android device or emulator running API 26+
- A Firebase project

### Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/kareem2099/Shredzilla.git
   cd Shredzilla
   ```

2. **Configure Firebase:**
   - Create a new project at [console.firebase.google.com](https://console.firebase.google.com)
   - Register an Android app using the package name `com.FreeRave.shredzilla`
   - Download `google-services.json` and place it in the `app/` directory
   - Enable **Cloud Firestore** and **Authentication** (Email/Password + Google providers)

3. **Configure signing (for Release builds):**
   Add the following to your `gradle.properties` (this file is `.gitignored` and never committed):
   ```properties
   SHREDZILLA_RELEASE_STORE_FILE=your_keystore.jks
   SHREDZILLA_RELEASE_STORE_PASSWORD=your_store_password
   SHREDZILLA_RELEASE_KEY_ALIAS=your_key_alias
   SHREDZILLA_RELEASE_KEY_PASSWORD=your_key_password
   ```

4. **Build & Run:**
   - Open the project in Android Studio
   - Sync Gradle
   - Run on a device or emulator

---

## Roadmap

### v1.2.0 — Core Workflow Stability *(Current)*
- [x] Offline-First Optimistic UI for set logging
- [x] Zero-Drift delta-math rest timer
- [x] Debounced exercise search with ViewModel StateFlow
- [x] Historical analytics with dynamic progress chart
- [x] Date-selectable Today screen

### v1.3.0 — UI Polish & Feature Completeness *(Planned)*
- [ ] Real workout activity indicators on the day strip
- [ ] Per-exercise chart selector
- [ ] LazyColumn pagination for large exercise lists
- [ ] Screen transition animations
- [ ] Analytics cache via `SavedStateHandle`

> See [`todo_list(v1.3.0).json`](todo_list(v1.3.0).json) for the complete milestone breakdown.

---

## Contributing

Contributions, issues, and feature requests are welcome.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m 'Add your feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

Please ensure your code follows the existing architecture patterns (MVVM, State Hoisting, ViewModel-owned coroutines) before submitting.

---

## Security

This repository does **not** contain:
- `google-services.json`
- `gradle.properties` (signing credentials)
- Any `.keystore` or `.jks` files

If you discover a security vulnerability, please open a private issue rather than a public one.

---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.