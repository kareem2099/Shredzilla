# Shredzilla 🦖🏋️‍♂️

Shredzilla is a high-performance Android workout tracking application engineered with a focus on modern architecture, persistent state management, and seamless real-time data synchronization. Built natively using **Kotlin**, **Jetpack Compose**, and **Firebase**, it is designed to replace your notebook with a blazing-fast, crash-resistant experience tailored for serious weightlifters.

## 🔥 Key Features (v1.1.0)

* **Bulletproof Architecture (State Hoisting):** Critical Firebase operations (Account Deletion, Username Updates) are safely hoisted into the `MainViewModel`. This ensures resilient background processing that completely survives Configuration Changes and Process Death without crashing the UI.
* **Smart Memory Management:** Employs a rigorous `ListenerRegistration` caching system that actively severs and sweeps "Zombie" listeners and locally orphaned profile images to guarantee zero memory leaks upon user exits.
* **Modern Authentication Flow:** Multi-provider support utilizing `FirebaseEmailPasswordAuth` and `FirebaseGoogleAuth` wrapped within a secure, multi-step Jetpack Compose Onboarding Flow.
* **Real-time Synchronization:** Built on Firestore listeners that dynamically refresh state metrics—like dynamic midnight date resolutions for "Today's Sets"—keeping UX entirely reactive.
* **Production-Grade UX:** Leverages dynamic Dark/Light themes, `rememberSaveable` to protect inputs, and beautiful Material 3 Design paradigms throughout the hierarchy.

## 🛠 Tech Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **Design Pattern:** MVVM (Model-View-ViewModel)
* **Backend:** Firebase Authentication & Cloud Firestore
* **Coroutines/Flow:** Complete use of asynchronous Kotlin Coroutines for safe background networking and UI StateFlows.

## 🚀 Getting Started

To run Shredzilla locally:

1. **Clone the repository:**
   ```bash
   git clone https://github.com/kareem2099/Shredzilla.git
   ```
2. **Setup Firebase:**
   - Create a new project in the Firebase Console.
   - Register an Android App using your package name (e.g., `com.FreeRave.shredzilla`).
   - Download the generated `google-services.json` file.
   - Drop the `google-services.json` file directly into the `app/` directory of the cloned project.
   - Enable **Cloud Firestore** and **Authentication** (Email/Password, Google).
3. **Build & Run:**
   - Open the project in **Android Studio**.
   - Sync Gradle.
   - Build and run on an Android Emulator or physical device (Android API 26+ recommended).

## 🗺 Roadmap (v1.2.0 Preview)
We are currently focusing on the core workout experience:
* **Today Screen Engine:** Zero-latency active-rest timers and Offline-First synchronization caching allowing you to log workouts flawlessly in gym dead-zones.
* **Analytics Rendering:** Offloading heavy Total Volume computations to `Dispatchers.Default` threads to render massive Set Graphs flawlessly at 60 FPS.
* *(See `todo_list(1.2.0).json` for the complete milestone).*

## 🤝 Contributing
Contributions, issues, and feature requests are welcome! Feel free to check the [issues page].
If you want to contribute, please read the `CONTRIBUTING.md` file for details on our code of conduct, and the process for submitting pull requests to us.

## 📄 License
This project is licensed under the MIT License - see the LICENSE file for details.
