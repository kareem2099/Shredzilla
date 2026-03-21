# Contributing to Shredzilla 🦖

First off, thank you for considering contributing to Shredzilla! It's people like you that make Shredzilla such a robust open-source workout tracker. 

## Where do I go from here?

If you've noticed a bug or have a feature request, make sure to check our [Issues](../../issues) tab first. If not already reported, please open a fresh issue describing the bug or feature request accurately.

## 🛠 Active Roadmap (v1.2.0)
If you're looking for where we need the most help right now, check out `todo_list(1.2.0).json`. We are actively seeking architectural refactoring assistance across:
1. **Today Screen:** Implementing sophisticated Offline-First synchronization caching to allow weightlifters to log intense active sets continuously through gym dead-zones without lag.
2. **Exercises Database:** Implementing LazyColumn pagination blocks for smooth frame-rates (60 FPS) and supercharging the Exercise Search Bar algorithm with rigorous debouncing techniques via Kotlin Flow.
3. **Analytics Processing:** Offloading heavyweight Total Volume summarization onto the ViewModel's asynchronous Dispatchers.Default threads.

## ⚙️ Pull Request Guidelines

1. **Fork the Repository:** Fork it on GitHub.
2. **Clone Locally:** `git clone https://github.com/kareem2099/Shredzilla.git`
3. **Create a Branch:** Create your feature or fix branch from `main`: `git checkout -b feature/amazing-feature` or `bugfix/issue-number`.
4. **Adhere to the Architecture:** 
   - **No UI Local States for Network Calls:** Follow our State Hoisting directives. Ensure that any Firestore transactions are executed exclusively via `MainViewModel` or a deeply abstracted Manager (ex: `UserDataManager`), never directly mapped to a `rememberCoroutineScope()` inside a Composable.
   - **Eliminate Memory Leaks:** If adding a Firebase Snapshot Listener, guarantee it is logged into the `ListenerRegistration` cache inside `UserDataManager` to be seamlessly handled by the `clearAllListeners()` wipe sequence.
   - **DRY Principle:** Reuse `updateUserSetting` for raw Firestore mutations instead of hardcoding instances. Use `convertToStorageWeight` and `convertToDisplayWeight` for Imperial calculations.
5. **Commit:** Ensure your commit messages are descriptive and conventional (e.g. `feat: Added pagination block for exercises list`).
6. **Push and PR:** Push your branch to your Fork, and open a PR against the `main` branch. 

## 🚨 Code Style
- We follow standard Kotlin formatting rules.
- Maintain Jetpack Compose declarative styling: heavily compartmentalize thick Composables into isolated functions to maintain UI reusability. 
- All PRs are subject to architectural review regarding device rotation endurance and process death survival mappings.

*Thank you for being part of the Shredzilla journey! Train hard, code clean.*
