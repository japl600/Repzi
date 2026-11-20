# Repzi

[![CI](https://github.com/japl600/Repzi/actions/workflows/ci.yml/badge.svg)](https://github.com/japl600/Repzi/actions/workflows/ci.yml)

An Android app for logging gym workouts (predefined and custom routines). It is a practice project to review architecture and modern Android tooling.

## Screenshots

<p>
  <img src="docs/screenshots/exercises.png" width="19%" alt="Exercise catalog filtered by muscle group">
  <img src="docs/screenshots/routines.png" width="19%" alt="Custom routines">
  <img src="docs/screenshots/session.png" width="19%" alt="Workout session in progress">
  <img src="docs/screenshots/history.png" width="19%" alt="Workout history">
  <img src="docs/screenshots/progress.png" width="19%" alt="Progress chart with metric selector">
</p>

Exercises, routines (your own and predefined ones you can add with a tap), a workout in progress (with the rest countdown), history and progress charts. The data shown is sample data.

## Stack

Kotlin, Coroutines and Flow, Jetpack Compose (Material 3, own teal/navy palette, no dynamic color), Hilt, Room, Preferences DataStore, Retrofit, OkHttp and kotlinx-serialization, WorkManager and AlarmManager, Navigation Compose, Gradle with convention plugins and a version catalog.

The UI is in English and Spanish. English is the default language and `values-es` holds the translation in each module; the built-in exercises are stored with a stable key (`exercises.seed_key`) and translated when shown, so their names follow the app language. Android 13+ offers a per-app language setting.

## Architecture

Clean Architecture modularized by layer and by feature. Screens use MVVM with a single UI state; the workout session screen uses MVI (intents, a pure reducer and one-shot effects).

```
app                      composition root (Application, Activity, NavHost)
core/
  domain                 pure Kotlin: models, repository interfaces, use cases
  data                   repository implementations (offline-first on top of Room) and the weekly catalog sync (WorkManager)
  database               Room: entities, DAOs, database
  network                Retrofit client for the wger exercise catalog (plain models, no HTTP types leak out)
  notifications          the rest-over notification (an exact alarm) and the daily inactivity reminder (WorkManager); channels, permission checks and the posting code live here
  ui                     Material 3 theme and shared components
  testing                shared fakes and test rules
feature/
  exercises              exercise catalog (the 15 built-ins plus ~900 exercises synced from wger) filtered by muscle group and searchable by name, ignoring case and accents, with a detail screen showing description, author and license (implemented)
  routines               create, edit, reorder and delete custom routines, with a rest time per exercise picked on a wheel; predefined routines (full body, push, pull, legs, core) are templates that are copied into your own routines (implemented)
  session                log sets, reps and weight for a routine, built with MVI; ticking a set starts the exercise's rest countdown (adjustable, with a buzz when it ends and an optional notification when the app is out of sight); the workout in progress is saved as a draft in Room and resumes after the app is closed (implemented)
  history                finished workouts, most recent first, with duration and volume (implemented)
  progress               per-exercise Canvas line chart of max weight, estimated 1RM, volume or reps with date ranges, opened from Exercises or History (implemented)
  settings               fourth tab: shortcut to the per-app language setting (Android 13+), rest timer vibration switch, notification switches and the days of the inactivity reminder, all stored in Preferences DataStore, open-source licenses (AboutLibraries), link to the source code and the app version (implemented)
build-logic/convention   convention plugins (repzi.android.*, repzi.jvm.library)
```

Allowed dependencies: `feature` → `core:domain` + `core:ui`; `core:data` → `core:domain` + `core:database` + `core:network`; `core:notifications` → `core:domain`; `app` knows everything in order to build the Hilt graph. `core:domain` has no Android dependencies, so it can be moved to Kotlin Multiplatform later.

## Data and licenses

The exercise catalog comes from [wger](https://wger.de), an open-source fitness project. Every exercise is published under a Creative Commons license that depends on the exercise (CC-BY-SA 3.0, CC-BY-SA 4.0 or CC0 in the Spanish and English data used here), and its author and license are shown on the exercise detail screen. The share-alike licenses apply to that content: the exercise texts stay under their license inside the app. The license of the app code is not affected. The catalog is downloaded once a week on an unmetered network (about 2.3 MB) and stored in Room, so the app works offline; the wger API cannot be filtered by date, so each exercise's `last_update_global` is compared with the stored one and only changes are written.

Open-source libraries are listed in the app under Settings, Licenses.

## Notifications

Both are optional and off by default; the Android 13+ permission is asked when a switch is turned on, not at launch.

- **Rest over.** Ticking a set sets an `AlarmManager` alarm for the end of the rest, so the notification arrives with the screen off or the app minimized, and the alarm survives the app being swiped away on stock Android. It is exact when the user allows exact alarms and approximate otherwise (Settings says so). It is not posted while the app is on screen, and some manufacturers' battery savers can still drop alarms.
- **Inactivity reminder.** A daily WorkManager check (around 18:00) notifies once when the chosen number of days (1 to 14, default 3) has passed since the last finished workout. It sends one notice per lapse and nothing before the first workout.

## Getting started

1. Open the project folder in Android Studio and wait for the Gradle sync (JDK 17 or newer; CI uses Temurin 21).
2. Run the `app` configuration on an emulator or device.
3. Tests: `./gradlew test` (unit tests) and `./gradlew connectedDebugAndroidTest` (Compose screen tests and Room DAOs/migration, needs an emulator or device).

## Roadmap

- **Level 1 (MVP):** exercise library, custom routines, workout session logging (sets, reps, weight), history.
- **Level 2:** predefined routines, rest timer, progress charts and exercise search (all done).
- **Level 3:** remote exercise catalog with background sync (done), notifications (done), widget (Glance).
- **Later:** a foreground service with a live rest countdown in the notification (evaluated, not built), Baseline Profiles and Macrobenchmark, screenshot tests, migrating `core:domain` and `core:data` to KMP.

## Measurements

| Measurement | Before | After | Change applied |
|---|---|---|---|
| Clean build | | | |
| Incremental build | | | |
| Cold start | | | |
