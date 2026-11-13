# Repzi

[![CI](https://github.com/japl600/Repzi/actions/workflows/ci.yml/badge.svg)](https://github.com/japl600/Repzi/actions/workflows/ci.yml)

An Android app for logging gym workouts (predefined and custom routines). It is a practice project to review architecture and modern Android tooling.

## Stack

Kotlin, Coroutines and Flow, Jetpack Compose (Material 3, own teal/navy palette, no dynamic color), Hilt, Room, Navigation Compose, Gradle with convention plugins and a version catalog.

## Architecture

Clean Architecture modularized by layer and by feature. Screens use MVVM with a single UI state; the workout session screen uses MVI (intents, a pure reducer and one-shot effects).

```
app                      composition root (Application, Activity, NavHost)
core/
  domain                 pure Kotlin: models, repository interfaces, use cases
  data                   repository implementations (offline-first on top of Room)
  database               Room: entities, DAOs, database
  ui                     Material 3 theme and shared components
  testing                shared fakes and test rules
feature/
  exercises              exercise catalog filtered by muscle group (implemented)
  routines               create, edit, reorder and delete custom routines (implemented)
  session                log sets, reps and weight for a routine, built with MVI; the workout in progress is saved as a draft in Room and resumes after the app is closed (implemented)
  history                finished workouts, most recent first, with duration and volume (implemented)
  progress               max weight per session for an exercise as a Canvas line chart with date ranges, opened from Exercises or History (implemented)
build-logic/convention   convention plugins (repzi.android.*, repzi.jvm.library)
```

Allowed dependencies: `feature` → `core:domain` + `core:ui`; `core:data` → `core:domain` + `core:database`; `app` knows everything in order to build the Hilt graph. `core:domain` has no Android dependencies, so it can be moved to Kotlin Multiplatform later.

## Getting started

1. Open the project folder in Android Studio and wait for the Gradle sync (JDK 17 or newer; CI uses Temurin 21).
2. Run the `app` configuration on an emulator or device.
3. Tests: `./gradlew test` (unit tests) and `./gradlew connectedDebugAndroidTest` (Compose screen tests and Room DAOs/migration, needs an emulator or device).

## Roadmap

- **Level 1 (MVP):** exercise library, custom routines, workout session logging (sets, reps, weight), history.
- **Level 2:** predefined routines, rest timer, progress charts, search.
- **Level 3:** remote catalog with sync, WorkManager, notifications, widget (Glance).
- **Later:** Baseline Profiles and Macrobenchmark, screenshot tests, migrating `core:domain` and `core:data` to KMP.

## Measurements

| Measurement | Before | After | Change applied |
|---|---|---|---|
| Clean build | | | |
| Incremental build | | | |
| Cold start | | | |
