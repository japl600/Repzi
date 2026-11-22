# Home-screen widget (Glance), design

Status: waiting for approval. Date: 2026-11-22.

## Intent

Level 3, part 3 of the roadmap. One 2x2 home-screen widget that answers two questions without opening the app: "when did I last train?" and "what do I do today?". Tapping its button goes straight into the workout; if a workout is already in progress the button resumes it. It saves the taps a user spends in the gym and shows the app on the home screen of whoever reviews the project.

## Goals

- A single widget, `GlanceAppWidget`, 2x2 cells, in the app's teal/navy palette, English and Spanish.
- **Last workout** (read-only): routine name, how long ago, duration and volume in the user's weight unit.
- **Start button**: one button that opens the session screen of the routine "that is up next", or resumes the workout in progress.
- It stays correct: it changes when a workout is finished, deleted or abandoned, when routines change, when the weight unit changes, and its relative dates do not go stale for a day.
- The rule that picks the routine is a pure function with unit tests.

## Non-goals

- Weekly summary (option C of the brainstorm). It can be a second widget later; nothing here should block that (the widget module is the place for it, and the state is a plain data class).
- A live rest countdown (Glance cannot tick every second; it is tied to the deferred foreground-service idea).
- A configuration screen, several sizes or a routine list (the 4x2 variant was dropped by the user: 2x2 only).
- Widget previews for every locale or theme beyond one `previewLayout`.

## Anti-goals

- A widget that shows "Resume" for a workout that was already finished or discarded, or keeps showing one for a day after it expired.
- A widget that opens the app on the wrong screen, or leaves two copies of the activity in the back stack.
- Doing work when no widget is on the home screen beyond one cheap observer.

## Constraints

- Android only, `minSdk` 26. Glance `androidx.glance:glance-appwidget` 1.2.0 (stable; 1.3 is alpha) and `glance-material3` only if needed for theming; the builder must check it builds with AGP 9.5.1 / Kotlin 2.4.21 / compileSdk 37 and record the versions in `libs.versions.toml`.
- Module rules from the README: the new `:feature:widget` depends on `core:domain` and `core:ui` only. `app` builds the Hilt graph and starts the observer. `core:domain` stays pure Kotlin.
- Weights stay in kilograms in storage; the widget converts for display with the same helpers the rest of the app uses.
- Draft rule: a draft older than `GetWorkoutDraftUseCase.MAX_AGE` (6 h) is not a workout to resume.
- English in code, comments and docs; strings in `values/` and `values-es/`.

## Approaches considered

How the widget stays up to date:

1. **App-side observer that pushes updates (chosen).** At start, an observer collects a combined `HomeWidgetState` and calls `updateAll` on every distinct change. The system also refreshes it every 30 minutes (`updatePeriodMillis`, the platform minimum) so relative dates and the 6 h draft expiry do not go stale. Pros: it changes as soon as the user finishes or discards a workout; the choice rule is a pure function. Cons: one more piece started in `RepziApplication`; the observer only runs while the process is alive (the periodic refresh covers the rest).
2. **The widget reads Room in `provideGlance` only.** Less code, but Glance keeps that subscription only briefly, so finishing a workout while the widget's session is closed leaves old data until the next system refresh.

How the button opens the workout: a deep link `repzi://routine/{routineId}/session` on the existing session destination, started with Glance's `actionStartActivity` on `MainActivity`. Navigation Compose handles the URI and puts the session on top of the start destination, so Back returns to Exercises. Alternative (an intent extra read by the activity) was rejected: it would need hand-written routing next to a navigation graph that already supports deep links.

Which routine the button starts (decided with the user): **the one up next**. Routines are listed sorted by name, ignoring case (`ObserveRoutinesUseCase`). Find the routine whose name equals the last session's `routineName` and take the next one in that order, wrapping to the first. Sessions store only the routine name (a snapshot), not its id, so a renamed or deleted routine finds no match, and then the first routine is used. Rejected: repeat the last routine, always the first.

## Design

### Behavior

`HomeWidgetState` (in `:feature:widget`, plain data) has: the last session summary (name, finished time, duration, volume in kg) or null; the target: `Start(routineId, routineName)`, `Resume(routineId, routineName)` or `NoRoutines`; and the weight unit.

Pure function `homeWidgetState(sessions, routines, drafts, weightUnit, now)`:

- `sessions` newest first; the last session is `sessions.firstOrNull()`.
- A draft counts only if `now - startedAt <= 6 h`. If any counts, the target is `Resume` for the most recent one that still matches an existing routine (a draft whose routine was deleted is ignored).
- Otherwise, if there are no routines, `NoRoutines`.
- Otherwise `Start` with the routine after the last session's, by the rule above, or the first routine when there is no last session or no match.

### Screen (2x2)

```
┌───────────────────────┐
│ Last workout          │   "No workouts yet" when there are none
│ Push · 2 days ago     │   Today / Yesterday / N days ago
│ 52 min · 4,200 kg     │   kg or lb
│ [ ▶ Pull ]            │   "Resume Pull" when a draft exists
└───────────────────────┘
```

- `NoRoutines`: the button area reads "Create a routine" and opens the app (no deep link).
- Tapping anywhere except the button opens the app on its start screen.
- "Today / Yesterday / N days ago" counts calendar days in the device's time zone.

### Data

- New `DraftRepository.observeDrafts(): Flow<List<WorkoutDraftInfo>>` where `WorkoutDraftInfo(routineId, startedAt)` is a new domain model; Room query on `workout_drafts` only (`SELECT routine_id, started_at`). No migration, no change to the schema. Saving a draft deletes and reinserts the row, so the flow emits on every edit; `distinctUntilChanged` on the mapped state keeps those from reaching the widget.
- New use case `ObserveWorkoutDraftsUseCase` in `core:domain` (a thin wrapper over `observeDrafts`, like the other observe use cases). The state itself is a widget concern, so the combine lives in `:feature:widget` over `ObserveSessionsUseCase`, `ObserveRoutinesUseCase`, `ObserveWorkoutDraftsUseCase`, `ObserveWeightUnitUseCase` and a `Clock`.

### Update flow

- `HomeWidgetUpdater` (in `:feature:widget`, `start()` called from `RepziApplication.onCreate` next to `NotificationsSetup.start()`): on an application scope, collects the state, `distinctUntilChanged`, and calls `RepziWidget().updateAll(context)`. The updater takes a small `WidgetRefresher` interface so a test can count refreshes without Glance.
- `RepziWidgetReceiver : GlanceAppWidgetReceiver` (Hilt `@AndroidEntryPoint`) injects what `RepziWidget` needs; `provideGlance` computes the state once with `.first()`.
- `res/xml/repzi_widget_info.xml`: `targetCellWidth/Height="2"`, `updatePeriodMillis="1800000"`, a `previewLayout`, `description`, resizable.

### Navigation

- `sessionScreen` gets `deepLinks = listOf(navDeepLink { uriPattern = "repzi://routine/{routineId}/session" })`; `routineId` is the existing `LongType` argument.
- `MainActivity`: `launchMode="singleTop"` and a listener that passes a new intent to the nav controller, so tapping the button while the app is open does not stack a second activity. The builder must verify what Navigation 2.10.2 already does for new intents before adding the listener.
- The deep-link intent is explicit (package + `MainActivity`). Navigation Compose should match an explicit intent's data URI without a manifest intent filter, and the app should not expose a public `repzi://` scheme to other apps, so add no filter unless a test shows it is required.

### Errors and edge cases

- No routines, no sessions, routine deleted after the last session, only one routine, draft of a deleted routine, draft expired, unit switch, language switch: all in the pure function's tests or the Glance tests below.
- If reading fails in `provideGlance`, show the empty state instead of crashing the host.

### Tests

- **Unit (`:feature:widget`)**: `homeWidgetState` over the cases above (each rule line is one test); relative-day text around midnight and across a time zone; the updater refreshes once per distinct state and not on a draft edit that changes nothing; `observeDrafts` with the fake repository.
- **Instrumented**: the deep link opens the session of the right routine (Compose test with a `NavHost`); Room query for `observeDrafts`.
- **Glance**: composition tests with `glance-appwidget-testing` if it works with the pinned versions (state to text and button label, both languages); otherwise cover the mapping state-to-strings as a plain unit and say so.
- **By hand**: add the widget on the emulator and on the Pixel 6 Pro (never run `connectedDebugAndroidTest` there), finish a workout and watch it change, start one and watch "Resume" appear, tap the button from a cold start and with the app open.

### Docs

README: the widget in "What it does", a screenshot, and the roadmap line moves from "Next" to done. The second widget (weekly summary) stays in "Later".

## Left to the builder

- Exact Glance and `glance-material3` versions and their dependency lines; text and colors with `GlanceTheme` mapped to the palette (`#0E1B2C`, `#2DD4BF`, `#99F6E4`, `#CBD5E1`, `#94A3B8`).
- Hilt wiring of the receiver and the widget; where the application scope for the updater comes from.
- The exact formatting helpers for duration and volume (reuse `core:ui`'s `formatNumber` and the kg/lb conversion already used by history).
- Whether the intent filter is needed (see Navigation), and confirming that Back from the deep-linked session returns to Exercises.
