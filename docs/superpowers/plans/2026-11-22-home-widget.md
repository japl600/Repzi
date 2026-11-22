# Home-screen widget Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A 2x2 Glance widget that shows the last workout and a button that starts the routine that is up next, or resumes the workout in progress.

**Architecture:** A new `:feature:widget` module holds a pure function that turns sessions, routines, drafts and the weight unit into a `HomeWidgetState`, the Glance UI, and an observer that calls `updateAll` whenever that state changes. The button is a deep link `repzi://routine/{id}/session` on the existing session destination. Drafts get a new observable query (no schema change).

**Tech Stack:** Glance 1.2.0 (`glance-appwidget`), Room, Hilt, Navigation Compose 2.10.2, kotlinx-coroutines, Turbine.

**Spec:** `docs/superpowers/specs/2026-11-22-home-widget-design.md`

## Global Constraints

- Module rules: `:feature:widget` depends on `core:domain` and `core:ui` only (the `repzi.android.feature` plugin adds both). `core:domain` stays pure Kotlin. `app` depends on the new module and starts the updater.
- Weights are stored in kg; display with `formatWeight(kg, unit)` from `core:ui`.
- A draft older than `GetWorkoutDraftUseCase.MAX_AGE` (6 h) does not count; exactly 6 h still counts.
- Routines are ordered by name ignoring case (`ObserveRoutinesUseCase`); the last session's `routineName` is matched to a routine **ignoring case**.
- `minSdk` 26, `compileSdk` 37, AGP 9.5.1, Kotlin 2.4.21. Glance 1.2.0 goes in `gradle/libs.versions.toml`.
- English in code, comments, docs and commits; strings in `values/` and `values-es/`; weight label "kg".
- TDD: show the test failing before the implementation. Instrumented tests only on the emulator: `ANDROID_SERIAL=emulator-5554`, never on the physical phone (`connectedDebugAndroidTest` wipes the app's data). Start the emulator with `emulator -avd Pixel_10 -no-snapshot-load -no-audio` when a task needs it.
- Commits end with the two attribution lines from the session's system reminder.

## Review Focus

Inputs the spec implies but a happy-path test would miss; each has its test in the task named in brackets.

1. Two routines whose names match the last session's name (case aside): the next one after the first match is used, never a crash. [Task 3]
2. The last session belongs to the alphabetically last routine: the target wraps to the first. [Task 3]
3. A session finished at 23:59 and viewed at 00:01 reads "Yesterday", not "Today" (calendar days in the device's zone). [Task 3]
4. A bodyweight-only session (volume 0): the summary shows the duration and no "0 kg". [Task 5]
5. Sessions exist but every routine was deleted: the last workout still shows, the button says "Create a routine" and opens the app. [Task 3 and 5]

## File Structure

- `core/domain`: `model/WorkoutDraftInfo.kt` (new), `repository/DraftRepository.kt` (+`observeDrafts`), `usecase/WorkoutDraftUseCases.kt` (+`ObserveWorkoutDraftsUseCase`).
- `core/database`: `dao/WorkoutDraftDao.kt` (+`observeAll`), androidTest `WorkoutDraftDaoTest.kt`.
- `core/data`: `repository/OfflineFirstDraftRepository.kt`, `mapper/DraftMappers.kt`.
- `core/testing`: `FakeDraftRepository.kt` (observable).
- `core/ui`: `navigation/SessionDeepLink.kt` (new), unit test.
- `feature/session`: `SessionNavigation.kt` (deep link). `app`: `MainActivity.kt`, `AndroidManifest.xml`, `build.gradle.kts`, `RepziApplication.kt`.
- `feature/widget` (new module): `HomeWidgetState.kt`, `ObserveHomeWidgetState.kt`, `HomeWidgetUpdater.kt`, `RepziWidget.kt`, `HomeWidgetContent.kt`, `RepziWidgetReceiver.kt`, `WidgetModule.kt`, `res/xml/repzi_widget_info.xml`, `res/layout/widget_preview.xml`, `res/values{,-es}/strings.xml`, `AndroidManifest.xml`, tests.

---

### Task 1: Drafts can be observed

**Files:**
- Create: `core/domain/src/main/kotlin/com/japl/repzi/core/domain/model/WorkoutDraftInfo.kt`
- Modify: `DraftRepository.kt`, `WorkoutDraftUseCases.kt` (domain); `WorkoutDraftDao.kt`; `OfflineFirstDraftRepository.kt`, `DraftMappers.kt`; `FakeDraftRepository.kt`
- Test: `core/database/src/androidTest/.../WorkoutDraftDaoTest.kt`

**Interfaces:**
- Produces: `data class WorkoutDraftInfo(val routineId: Long, val startedAt: Instant)`; `DraftRepository.observeDrafts(): Flow<List<WorkoutDraftInfo>>`; `class ObserveWorkoutDraftsUseCase @Inject constructor(repository: DraftRepository) { operator fun invoke(): Flow<List<WorkoutDraftInfo>> }`; `WorkoutDraftDao.observeAll(): Flow<List<WorkoutDraftEntity>>`.

- [ ] **Step 1: Write the failing test** in `WorkoutDraftDaoTest`: `observeAllEmitsWhenADraftIsSavedAndDeleted`. Insert a routine, collect `draftDao.observeAll()` with Turbine or `first()` between operations; assert `emptyList()`, then one entity with the saved `routineId` and `startedAt` after `save(...)`, then `emptyList()` after `delete(routineId)`.
- [ ] **Step 2: Run it and see it fail (compile error: `observeAll` unresolved).** Command: `ANDROID_SERIAL=emulator-5554 ./gradlew :core:database:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.japl.repzi.core.database.WorkoutDraftDaoTest`. Expected: FAIL.
- [ ] **Step 3: Implement.** DAO: `@Query("SELECT * FROM workout_drafts") abstract fun observeAll(): Flow<List<WorkoutDraftEntity>>`. Domain: the model, the repository method and the use case above. Data: `observeDrafts()` maps entities with `Instant.fromEpochMilliseconds(startedAt)`, switching context like the other methods only if the DAO call needs it (Room flows already run off the main thread). `FakeDraftRepository`: hold the drafts in a `MutableStateFlow<Map<Long, WorkoutDraft>>` (keep the `drafts` property) and implement `observeDrafts()` as the mapped flow.
- [ ] **Step 4: Run the DAO test again.** Expected: PASS; then `./gradlew test` for the whole project. Expected: PASS.
- [ ] **Step 5: Commit** `feat(drafts): let the unfinished workouts be observed`.

---

### Task 2: The session destination has a deep link

**Files:**
- Create: `core/ui/src/main/kotlin/com/japl/repzi/core/ui/navigation/SessionDeepLink.kt`; Test: `core/ui/src/test/kotlin/com/japl/repzi/core/ui/navigation/SessionDeepLinkTest.kt`
- Modify: `feature/session/.../SessionNavigation.kt`, `app/.../MainActivity.kt`, `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: `const val SESSION_DEEP_LINK_PATTERN = "repzi://routine/{routineId}/session"`; `fun sessionDeepLink(routineId: Long): String` (the pattern with the id filled in). The placeholder name equals `ROUTINE_ID_ARG` ("routineId") in `feature:session`.

- [ ] **Step 1: Write the failing test** `SessionDeepLinkTest`: `sessionDeepLink(42)` equals `"repzi://routine/42/session"`, and the pattern contains `{routineId}` exactly once.
- [ ] **Step 2: Run it.** `./gradlew :core:ui:testDebugUnitTest`. Expected: FAIL (unresolved reference).
- [ ] **Step 3: Implement the two declarations; then in `sessionScreen` add `deepLinks = listOf(navDeepLink { uriPattern = SESSION_DEEP_LINK_PATTERN })` to the destination.** In `MainActivity` set `android:launchMode="singleTop"` in the manifest and make a new intent reach the nav controller (check first what Navigation 2.10.2 does on its own for a new intent in a Compose `NavHost`; add an `addOnNewIntentListener` that calls `navController.handleDeepLink(intent)` only if it does not). Add no `<intent-filter>` for `repzi://` unless step 5 shows it is required.
- [ ] **Step 4: Run the unit test again.** Expected: PASS.
- [ ] **Step 5: Verify by hand on the emulator.** Install with `ANDROID_SERIAL=emulator-5554 ./gradlew :app:installDebug`, create or find a routine id, then `adb -s emulator-5554 shell am start -n com.japl.repzi/.MainActivity -a android.intent.action.VIEW -d "repzi://routine/1/session"`. Expected: the session screen of that routine opens; Back goes to Exercises; running the command again while the app is open does not stack a second activity (`adb shell dumpsys activity activities | grep -c MainActivity` stays at 1). Record anything that differs and fix it here.
- [ ] **Step 6: Commit** `feat(session): open a workout from a deep link`.

---

### Task 3: The widget module and the rule for what to show

**Files:**
- Create: `feature/widget/build.gradle.kts` (plugin `libs.plugins.repzi.android.feature`, namespace `com.japl.repzi.feature.widget`, dependencies `libs.glance.appwidget`), `feature/widget/src/main/kotlin/com/japl/repzi/feature/widget/HomeWidgetState.kt`, `feature/widget/src/test/.../HomeWidgetStateTest.kt`
- Modify: `settings.gradle.kts` (`include(":feature:widget")`), `gradle/libs.versions.toml` (`glance = "1.2.0"`, `glance-appwidget` and, only if needed, `glance-material3`)

**Interfaces:**
- Produces, in `HomeWidgetState.kt`:
  - `data class HomeWidgetState(val lastWorkout: LastWorkout?, val target: WidgetTarget, val weightUnit: WeightUnit)`
  - `data class LastWorkout(val routineName: String, val finishedAt: Instant, val duration: Duration, val volumeKg: Double)`
  - `sealed interface WidgetTarget { data class Start(val routineId: Long, val routineName: String); data class Resume(val routineId: Long, val routineName: String); data object NoRoutines }`
  - `fun homeWidgetState(sessions: List<WorkoutSession>, routines: List<Routine>, drafts: List<WorkoutDraftInfo>, weightUnit: WeightUnit, now: Instant): HomeWidgetState` (sessions newest first, routines sorted by name ignoring case, as the use cases deliver them)
  - `fun daysAgo(from: Instant, now: Instant, zone: TimeZone): Int` (calendar days between the two dates in `zone`, never negative)

- [ ] **Step 1: Write the failing tests** in `HomeWidgetStateTest`. One test per rule, named for the rule, with these assertions:
  - `noRoutinesGivesNoRoutines`: empty routines, one session -> `target == NoRoutines` and `lastWorkout` is still filled in.
  - `noSessionsStartsTheFirstRoutine`: routines Pull, Push, no sessions -> `Start(Pull)`, `lastWorkout == null`.
  - `startsTheRoutineAfterTheLastOne`: sessions last = "Push", routines Legs, Pull, Push -> `Start(Legs)` (wraps); last = "Pull" -> `Start(Push)`.
  - `theLastSessionNameIsMatchedIgnoringCase`: session "push", routine "Push" at index 1 of 3 -> the routine at index 2.
  - `anUnknownLastRoutineStartsTheFirstOne`: session "Old name" -> `Start(first)`.
  - `twoRoutinesWithTheSameNameUseTheFirstMatch` (Review Focus 1): routines A, B(id 2), B(id 3), C; last = "B" -> `Start(id 3)`.
  - `aSingleRoutineIsItsOwnNext`.
  - `aRecentDraftWinsAndResumes`: draft 1 h old for routine 2 -> `Resume(2)` regardless of the rotation; `lastWorkout` unchanged.
  - `aDraftExactlySixHoursOldStillCountsAndOlderOnesDoNot`.
  - `theMostRecentDraftThatStillHasARoutineIsResumed`: three drafts, the newest one's routine deleted -> the second newest.
  - `aDraftOfADeletedRoutineIsIgnored`.
  - `theWeightUnitIsCarriedIntoTheState`.
  - `daysAgo`: same day -> 0; yesterday -> 1; **23:59 viewed at 00:01 -> 1** (Review Focus 3); two zones give the calendar difference of the given zone; a future `from` -> 0.
- [ ] **Step 2: Run them.** `./gradlew :feature:widget:testDebugUnitTest`. Expected: FAIL to compile (functions undefined).
- [ ] **Step 3: Implement** the types and the two functions; one line of approach: the next routine is `routines[(indexOfFirstMatch + 1) % size]`.
- [ ] **Step 4: Run the tests again.** Expected: PASS.
- [ ] **Step 5: Confirm Glance resolves and the module builds with the pinned toolchain:** `./gradlew :feature:widget:assembleDebug`. Expected: BUILD SUCCESSFUL; if 1.2.0 does not build with AGP 9.5.1 / Kotlin 2.4.21, record the version that does in the commit message.
- [ ] **Step 6: Commit** `feat(widget): decide what the home widget shows`.

---

### Task 4: The state flow and the updater

**Files:**
- Create: `feature/widget/src/main/kotlin/.../ObserveHomeWidgetState.kt`, `HomeWidgetUpdater.kt`, tests `ObserveHomeWidgetStateTest.kt`, `HomeWidgetUpdaterTest.kt`

**Interfaces:**
- Consumes: Task 3's `homeWidgetState` and types; `ObserveSessionsUseCase`, `ObserveRoutinesUseCase`, `ObserveWorkoutDraftsUseCase`, `ObserveWeightUnitUseCase` (all `operator fun invoke(): Flow<...>`), `kotlin.time.Clock`.
- Produces:
  - `class ObserveHomeWidgetState @Inject constructor(sessions: ObserveSessionsUseCase, routines: ObserveRoutinesUseCase, drafts: ObserveWorkoutDraftsUseCase, weightUnit: ObserveWeightUnitUseCase, clock: Clock) { operator fun invoke(): Flow<HomeWidgetState> }`
  - `fun interface WidgetRefresher { suspend fun refresh() }`
  - `@Singleton class HomeWidgetUpdater @Inject constructor(observeState: ObserveHomeWidgetState, refresher: WidgetRefresher) { fun start(scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)): Job }`: collects `observeState()` with `distinctUntilChanged()` and calls `refresher.refresh()` for each; a failing `refresh()` is logged (`Log.w`) and does not stop the collection (rethrow `CancellationException`).

- [ ] **Step 1: Write the failing tests** using `FakeSessionRepository`, `FakeRoutineRepository`, `FakeDraftRepository`, `FakeSettingsRepository` and a fixed `Clock`:
  - `ObserveHomeWidgetStateTest.recomputesWhenAnyInputChanges`: Turbine; initial state, then save a session -> new state, then save a draft -> `Resume`, then delete the draft -> `Start` again, then change the unit -> new unit.
  - `HomeWidgetUpdaterTest`: `refreshesOnceForTheFirstState`; `aDraftEditThatChangesNothingVisibleDoesNotRefresh` (save a draft, then save it again with different set text and the same `startedAt` -> still one extra refresh in total); `finishingAWorkoutRefreshes`; `aFailingRefreshDoesNotStopTheUpdates` (refresher throws on the first call, the second change still refreshes).
- [ ] **Step 2: Run them.** `./gradlew :feature:widget:testDebugUnitTest`. Expected: FAIL to compile.
- [ ] **Step 3: Implement both classes** (`combine` of the four flows mapped through `homeWidgetState(..., now = clock.now())`).
- [ ] **Step 4: Run the tests again.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(widget): push an update whenever the widget's state changes`.

---

### Task 5: The Glance screen

**Files:**
- Create: `feature/widget/src/main/kotlin/.../HomeWidgetContent.kt`, `feature/widget/src/main/res/values/strings.xml`, `values-es/strings.xml`; Test: `feature/widget/src/test/.../HomeWidgetContentTest.kt` (Glance's testing library if `glance-appwidget-testing` 1.2.0 builds with the toolchain: add it as `testImplementation`; if it does not, test the text mapping below as a plain unit and say so in the commit)

**Interfaces:**
- Consumes: `HomeWidgetState`, `daysAgo`, `formatWeight` and `formatNumber` from `core:ui`, `sessionDeepLink`.
- Produces: `@Composable fun HomeWidgetContent(state: HomeWidgetState, now: Instant, zone: TimeZone, openApp: Action, startRoutine: (Long) -> Action)` for the 2x2 layout, and `internal fun lastWorkoutText(...)` pieces that map state to the strings (so they are testable): title line "Last workout" / "No workouts yet"; line 2 "<routine> · Today|Yesterday|N days ago"; line 3 "<N> min" plus " · <volume> <unit>" only when the volume is above 0 (Review Focus 4); button label "Start <routine>", "Resume <routine>" or "Create a routine".
- Strings (en / es): `widget_last_workout` Last workout / Último entrenamiento; `widget_no_workouts` No workouts yet / Aún sin entrenamientos; `widget_today` Today / Hoy; `widget_yesterday` Yesterday / Ayer; plural `widget_days_ago` (`%d days ago` / `hace %d días`); `widget_minutes` (`%d min`); `widget_start` (`Start %1$s` / `Empezar %1$s`); `widget_resume` (`Resume %1$s` / `Reanudar %1$s`); `widget_create_routine` Create a routine / Crear rutina; `widget_description` for the picker.

- [ ] **Step 1: Write the failing tests:** for each of: no workouts and no routines; last workout with `Start`; with `Resume`; with `NoRoutines` but a last workout (Review Focus 5); volume 0 omits the volume (Review Focus 4); LB shows the converted value; yesterday and N days wording; Spanish strings for one case. Assert on the texts and on the button label.
- [ ] **Step 2: Run them.** `./gradlew :feature:widget:testDebugUnitTest`. Expected: FAIL to compile.
- [ ] **Step 3: Implement the composable and the strings.** Colors from the app palette via `GlanceTheme`/`ColorProvider` (`#0E1B2C` background, `#2DD4BF` accent button, text `#CBD5E1` and `#94A3B8`; light variants chosen by the builder so contrast stays readable); the whole widget background taps to `openApp`; the button taps to `startRoutine(id)` for `Start`/`Resume` and to `openApp` for `NoRoutines`.
- [ ] **Step 4: Run the tests again.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(widget): draw the 2x2 widget`.

---

### Task 6: Register the widget and start the updater

**Files:**
- Create: `feature/widget/src/main/kotlin/.../RepziWidget.kt` (`GlanceAppWidget`, `SizeMode.Single`), `RepziWidgetReceiver.kt`, `WidgetModule.kt` (Hilt: binds `WidgetRefresher` to `RepziWidget().updateAll(context)` with the application context), `feature/widget/src/main/res/xml/repzi_widget_info.xml`, `feature/widget/src/main/res/layout/widget_preview.xml`, `feature/widget/src/main/AndroidManifest.xml`
- Modify: `app/build.gradle.kts` (`implementation(projects.feature.widget)`), `RepziApplication.kt`

**Interfaces:**
- Consumes: Task 4's `HomeWidgetUpdater.start()` and `ObserveHomeWidgetState`, Task 5's `HomeWidgetContent`, Task 2's `sessionDeepLink`.
- Produces: the registered widget "Repzi" in the launcher picker; `RepziApplication.onCreate` calls `homeWidgetUpdater.start()` after `notificationsSetup.start()`.

- [ ] **Step 1: Implement.** `RepziWidget.provideGlance` gets `ObserveHomeWidgetState` and `Clock` through a Hilt `@EntryPoint` (`EntryPointAccessors.fromApplication`), reads the state once with `.first()` inside `try/catch` (on failure, render the empty state, rethrow `CancellationException`), and calls `provideContent { HomeWidgetContent(...) }`. Actions: `openApp` = `actionStartActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!)`; `startRoutine(id)` = `actionStartActivity` with that same launch intent plus `data = Uri.parse(sessionDeepLink(id))`. The receiver is `GlanceAppWidgetReceiver` returning `RepziWidget()`, declared in the module manifest with `android:exported="true"`, the `android.appwidget.action.APPWIDGET_UPDATE` filter and the `android.appwidget.provider` meta-data pointing to the info XML. Info XML: `targetCellWidth="2"`, `targetCellHeight="2"`, `minWidth="110dp"`, `minHeight="110dp"`, `resizeMode="horizontal|vertical"`, `updatePeriodMillis="1800000"`, `previewLayout`, `description="@string/widget_description"`, `widgetCategory="home_screen"`.
- [ ] **Step 2: Build everything.** `./gradlew assembleDebug assembleDebugAndroidTest test lintDebug`. Expected: BUILD SUCCESSFUL, no new lint errors.
- [ ] **Step 3: Verify on the emulator** (start it, install with `ANDROID_SERIAL=emulator-5554`). Add the widget from the launcher's picker (long-press the home screen, Widgets, Repzi). Expected, each checked and noted: (a) with no routines, "Create a routine"; (b) with routines but no workouts, "Start <first routine>"; (c) finish a workout in the app and go home: the widget now shows it and the next routine; (d) start a workout and leave without finishing: "Resume <routine>"; (e) tap the button from the home screen with the app closed and then with the app open: the session opens, no second activity, Back goes to Exercises; (f) change the weight unit in Settings: the volume changes; (g) switch the language to Spanish: texts follow after the next update. If the launcher cannot place the widget from adb, do it by hand through the emulator window.
- [ ] **Step 4: Commit** `feat(widget): register the home widget and keep it updated`.

---

### Task 7: README, screenshot and the final check

**Files:**
- Modify: `README.md`; Create: `docs/screenshots/widget.png`

- [ ] **Step 1: Capture the widget on the emulator** (demo mode clock 9:41 like the other screenshots, sample data) and crop to the home screen portion with the widget.
- [ ] **Step 2: README:** add the widget to "What it does", add the screenshot, move "home-screen widget" from "Next" to "Done" and keep the weekly-summary widget under "Later", add one line about the deep link and the push-updated state to "Technical highlights".
- [ ] **Step 3: Full check.** `./gradlew assembleDebug assembleDebugAndroidTest test lintDebug`, then the instrumented tests of the touched modules on the emulator. Expected: all green.
- [ ] **Step 4: Commit** `docs: show the home widget in the README`.
