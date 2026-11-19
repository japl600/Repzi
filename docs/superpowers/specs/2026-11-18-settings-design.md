# Settings screen (v1) - design

## Intent

Repzi has no place for app-level options or for the legal notices that third-party code requires. The goal is a fourth bottom-bar tab, "Settings", that holds a few useful options and the licenses screen. It also gets the app ready for level 3: the wger content license (CC-BY-SA) will be listed there later.

It is a portfolio project, so the screen should look finished and follow the existing architecture (one feature module, MVVM with a single UI state, tests, one CI-checked commit per step).

## Goals

- A "Settings" tab with grouped rows (General, Help), a current value on the right where there is one, and the version at the bottom.
- Rows in v1:
  - **Language**: opens the system per-app language screen.
  - **Rest timer vibration**: on/off switch (today it always vibrates).
  - **Licenses**: open-source libraries used by the app.
  - **Source code**: opens the GitHub repository.
  - **Version**: version name and code (not clickable).
- The vibration preference is stored and read by the session screen.

## Non-goals

- Weight units (kg/lb) and data export. Both are future work; units touch the session, history and progress screens.
- Account, profile, notifications and anything else that needs a backend. Notifications come with level 3.
- Placeholder rows that do nothing.
- The wger attribution line. It is added when wger itself is added (level 3).

## Anti-goals

- A settings screen that looks like a mock-up because rows are inert.
- A new database migration for one boolean.

## Constraints

- `core:domain` stays Android-free: the preference is an interface there, implemented in `core:data`.
- `minSdk` 26: the per-app language screen only exists on Android 13+, so the Language row is shown only there.
- README and commits in English; the app UI in English with a `values-es` translation.

## Approaches considered (picked by the user on 2026-11-18)

1. **Where to store the switch.** DataStore Preferences (picked), because it is the current API and exposes a Flow like the rest of the app. Rejected: SharedPreferences (legacy API), a Room table (needs a migration for a boolean).
2. **License list.** AboutLibraries (picked): a Gradle plugin reads the dependencies and a Compose screen shows them; it is maintained and does not need Play Services. Rejected: Google's oss-licenses plugin (needs Play Services, little maintained), a hand-written list (goes stale).
3. **Where the code lives.** New module `:feature:settings` (picked), like the other features. Rejected: inside `:app` (breaks the structure shown in the README).

## Design

- **Module** `:feature:settings`, convention plugins like the other features. Depends on `core:domain` and `core:ui`. `app` adds the `SettingsRoute` tab to `TopLevelDestination` and the graph.
- **Domain**: `SettingsRepository` with `val restVibration: Flow<Boolean>` (default `true`) and `suspend fun setRestVibration(enabled: Boolean)`.
- **Data**: `DataStoreSettingsRepository` in `core:data`, one `preferencesDataStore`, provided by Hilt.
- **Settings screen**: `SettingsViewModel` exposes one `SettingsUiState(restVibration)`; the screen is stateless and receives the state and callbacks (same pattern as the other screens, so it can be tested without Hilt).
- **Session**: `SessionViewModel` reads `restVibration` when the countdown ends and only sends `SessionEffect.RestFinished` if it is on. This keeps the decision in the ViewModel, where it can be unit tested, and the screen keeps just doing the buzz. (My call: the alternative was to pass the flag in the effect.)
- **Language row**: `Settings.ACTION_APP_LOCALE_SETTINGS` with the package URI, shown only on API 33+. (My call.)
- **Version row**: `PackageManager` version name and code read in the screen, because `BuildConfig` is not available in a feature module. (My call.)
- **Licenses**: AboutLibraries plugin in the version catalog, applied to `:app` so it sees the whole dependency graph; the licenses screen is a destination inside `:feature:settings` that receives the library list from `app`. (Exact wiring is left to the builder.)

## Commits (one CI run each)

1. `:feature:settings` module, tab, Language, Source code and Version rows, string resources, unit and UI tests.
2. DataStore, `SettingsRepository`, the switch, and the session reading it (with tests for both states).
3. Licenses screen with AboutLibraries.

## Left to the builder

- Row visuals within Material 3 (list items with headers), icons, exact Spanish strings.
- Current AboutLibraries version and plugin setup (check at implementation time).
- Test names and fakes (a fake `SettingsRepository` goes in `core:testing`).
- README: add Settings to the module list and the screenshots if it looks good.
