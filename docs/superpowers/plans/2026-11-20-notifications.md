# Notifications Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add two optional notifications to Repzi: one when a rest countdown ends while the app is not visible, and one after N days without a finished workout.

**Architecture:** A new `:core:notifications` module owns channels, the rest alarm (`AlarmManager` -> `BroadcastReceiver` -> notifier) and the daily inactivity check (WorkManager). `:core:domain` holds the contracts (`RestReminderScheduler`, `InactivityNoticeLog`, the three new settings) and the pure inactivity rule; `:core:data` stores them in the existing DataStore; `SessionViewModel` schedules and cancels the alarm; Settings gets a "Notifications" section.

**Tech Stack:** Kotlin, Hilt, WorkManager 2.12.0 (already in the catalog), `AlarmManager`, platform `Notification.Builder` (no new AndroidX core dependency), `androidx.lifecycle:lifecycle-process` (new), DataStore Preferences, Compose M3.

**Spec:** `docs/superpowers/specs/2026-11-20-notifications-design.md`

## Global Constraints

- minSdk 26, compileSdk/target 37. Guard by API level: `POST_NOTIFICATIONS` runtime permission on 33+, `canScheduleExactAlarms()` on 31+.
- Both notifications are off by default; `inactivityDays` default 3, range 1..14.
- Rest notification: exact alarm when allowed, otherwise `setAndAllowWhileIdle`; never posted when the app is visible, the setting is off or notifications are blocked.
- `SessionViewModel` cancels the alarm only from explicit user actions (skip, adjust to zero, finish, discard, back without input). Never at the natural end of the countdown and never in `onCleared`.
- Inactivity: unique periodic work `inactivity-check`, 24 h, first run at the next 18:00 local time, `KEEP`; one notice per lapse; none before the first finished workout.
- `Clock` is injected everywhere (`kotlin.time.Clock`); no `Clock.System` in logic.
- Strings in English (`values/`) and Spanish (`values-es/`). Code, comments and commit messages in English. Weight unit stays "kg" (not touched here).
- No foreground service, no new storage engine, no network.
- One commit per task, local commits are fine; **push only when the user asks**, one commit at a time. Commit messages end with the two attribution lines given in the session reminder.
- Gradle: run from PowerShell (`.\gradlew.bat ...`), write the output to a file and read it with Select-String; stop the emulator before JVM-only suites (`test`, `lintDebug`) and start it (AVD `Pixel_10`) for `connectedDebugAndroidTest`, which uninstalls the app and wipes its data.

## Review Focus

Input classes and failure modes the spec implies; each has a test in the task that owns the code:

1. The setting is turned off after an alarm was already scheduled: nothing is posted (Task 2).
2. The alarm fires while the app is visible: nothing is posted (Task 2).
3. Notifications are blocked or the permission was revoked when the alarm fires: no crash, nothing posted (Task 2).
4. The latest workout is deleted after a notice, or the clock is set back: no repeated or negative notice (Task 1).
5. A notice that could not be posted (blocked) is not recorded, so it is retried the next day (Task 4).

## Plan calls (refinements of the spec)

- The rule fires only when the latest workout is **newer** than the last one a notice was recorded for (`lastWorkoutAt > lastNoticedFor`), not merely different. Deleting the latest workout then cannot trigger a second notice for an older one. The spec said "different"; this is a stricter version of the same intent.
- `InactivityNotifier.notify` returns whether it posted, and the check records the notice only if it did.
- The receiver is a thin Hilt receiver; the logic lives in `RestReminderNotifier` so it can be tested without a Hilt application.

---

### Task 1: Settings, contracts and the inactivity rule

**Files:**
- Modify: `core/domain/src/main/kotlin/com/japl/repzi/core/domain/repository/SettingsRepository.kt`
- Create: `core/domain/src/main/kotlin/com/japl/repzi/core/domain/repository/InactivityNoticeLog.kt`
- Create: `core/domain/src/main/kotlin/com/japl/repzi/core/domain/RestReminderScheduler.kt`
- Create: `core/domain/src/main/kotlin/com/japl/repzi/core/domain/InactivityRule.kt`
- Create (use cases, one file each, in `core/domain/.../usecase/`): `ObserveRestNotificationUseCase`, `SetRestNotificationUseCase`, `ObserveInactivityReminderUseCase`, `SetInactivityReminderUseCase`, `ObserveInactivityDaysUseCase`, `SetInactivityDaysUseCase`
- Modify: `core/data/src/main/kotlin/com/japl/repzi/core/data/repository/DataStoreSettingsRepository.kt`, `core/data/.../di/DataModule.kt`
- Create: `core/data/src/main/kotlin/com/japl/repzi/core/data/repository/DataStoreInactivityNoticeLog.kt`
- Modify: `core/testing/.../FakeSettingsRepository.kt`; Create: `core/testing/.../FakeRestReminderScheduler.kt`, `core/testing/.../FakeInactivityNoticeLog.kt`
- Test: `core/domain/src/test/kotlin/com/japl/repzi/core/domain/InactivityRuleTest.kt`, `core/data/src/test/kotlin/com/japl/repzi/core/data/repository/DataStoreSettingsRepositoryTest.kt` (extend), `core/data/src/test/kotlin/com/japl/repzi/core/data/repository/DataStoreInactivityNoticeLogTest.kt`

**Interfaces:**
- Produces:
  - `SettingsRepository` gains `val restNotification: Flow<Boolean>` (false), `val inactivityReminder: Flow<Boolean>` (false), `val inactivityDays: Flow<Int>` (3, always inside the range), `suspend fun setRestNotification(enabled: Boolean)`, `suspend fun setInactivityReminder(enabled: Boolean)`, `suspend fun setInactivityDays(days: Int)` (coerced into the range). Top level in the same file: `val INACTIVITY_DAYS_RANGE: IntRange = 1..14`, `const val DEFAULT_INACTIVITY_DAYS = 3`.
  - Use cases follow `SetRestVibrationUseCase`: observe ones are `operator fun invoke(): Flow<...>`, set ones are `suspend operator fun invoke(value)`.
  - `interface InactivityNoticeLog { suspend fun lastNoticeFor(): Instant?; suspend fun record(lastWorkoutAt: Instant) }` (`kotlin.time.Instant`).
  - `interface RestReminderScheduler { fun schedule(endsAt: Instant); fun cancel() }`.
  - `object InactivityRule { fun daysToReport(now: Instant, lastWorkoutAt: Instant?, days: Int, lastNoticedFor: Instant?): Int? }`: whole days elapsed since `lastWorkoutAt` when a notice is due, else null.
  - `FakeSettingsRepository(initialRestVibration = true, initialRestNotification = false, initialInactivityReminder = false, initialInactivityDays = DEFAULT_INACTIVITY_DAYS)`; `FakeRestReminderScheduler` with `val scheduled: List<Instant>`, `var cancelCount: Int`; `FakeInactivityNoticeLog(initial: Instant? = null)` with `val recorded: List<Instant>`.

- [ ] **Step 1: Write the failing tests**

`InactivityRuleTest` (days = 3 unless said; `last` is an `Instant`; `now = last + X`):
  - `noNoticeWithoutAFinishedWorkout`: `lastWorkoutAt = null` -> null.
  - `noNoticeBelowTheThreshold`: 2 days 23 h -> null.
  - `noticeAtExactlyTheThreshold`: 72 h -> 3.
  - `reportsTheWholeDaysElapsed`: 5 days 2 h -> 5.
  - `noNoticeTwiceForTheSameWorkout`: `lastNoticedFor == lastWorkoutAt`, 10 days -> null.
  - `noticeAgainAfterANewerWorkout`: `lastNoticedFor` one week before `lastWorkoutAt`, 4 days -> 4.
  - `noNoticeWhenTheLatestWorkoutWasDeleted`: `lastNoticedFor` newer than `lastWorkoutAt`, 10 days -> null.
  - `noNoticeWhenTheClockIsBehindTheWorkout`: `now` before `lastWorkoutAt` -> null.
  - `aOneDayThresholdReportsOneDay`: `days = 1`, 24 h -> 1.

`DataStoreSettingsRepositoryTest` (same helpers as the existing tests): `restNotificationIsOffByDefault`, `inactivityReminderIsOffByDefault`, `inactivityDaysIsThreeByDefault`, `newSettingsArePersisted` (set true/true/7 and read back), `inactivityDaysAreKeptInsideTheRange` (set 0 -> 1, set 99 -> 14), `corruptFileFallsBackToTheNewDefaults`.

`DataStoreInactivityNoticeLogTest`: `startsWithoutANotice` (null), `recordedNoticeIsReadBack` (record `Instant.fromEpochMilliseconds(5_000)` -> same value).

- [ ] **Step 2: Run to verify they fail**

Run: `.\gradlew.bat :core:domain:test :core:data:test` (output to a file)
Expected: compilation fails on the missing `InactivityRule`, settings members and `DataStoreInactivityNoticeLog`.

- [ ] **Step 3: Implement**

  - Domain contracts, use cases and `InactivityRule` as in Interfaces. `daysToReport`: null if `lastWorkoutAt == null`; null if `lastNoticedFor != null && lastWorkoutAt <= lastNoticedFor`; `elapsed = now - lastWorkoutAt`; null if `elapsed < days.days`; else `elapsed.inWholeDays.toInt()`.
  - `DataStoreSettingsRepository`: keys `rest_notification`, `inactivity_reminder`, `inactivity_days`. Pull the existing `.catch { IOException -> emptyPreferences() }` into one private flow reused by every property. Days are coerced on read and on write.
  - `DataStoreInactivityNoticeLog`: `internal class` over the same `DataStore<Preferences>`, key `inactivity_noticed_for` (`longPreferencesKey`, epoch milliseconds), same corrupt-file handling; bind it in `DataModule`.
  - Fakes in `:core:testing` as in Interfaces; update existing callers only if the compiler asks.

- [ ] **Step 4: Run to verify they pass**

Run: `.\gradlew.bat :core:domain:test :core:data:test :core:testing:build`
Expected: BUILD SUCCESSFUL, the new tests included.

- [ ] **Step 5: Commit**

```bash
git add core/domain core/data core/testing
git commit -m "Add notification settings, the notice log and the inactivity rule"
```

---

### Task 2: `:core:notifications` with the rest alarm

**Files:**
- Modify: `settings.gradle.kts` (include `:core:notifications`), `gradle/libs.versions.toml` (`androidx-lifecycle-process = { module = "androidx.lifecycle:lifecycle-process", version.ref = "lifecycle" }`), `app/build.gradle.kts` (`implementation(projects.core.notifications)`), `app/src/main/kotlin/com/japl/repzi/RepziApplication.kt`
- Create: `core/notifications/build.gradle.kts`, `core/notifications/src/main/AndroidManifest.xml`
- Create (package `com.japl.repzi.core.notifications`, in `core/notifications/src/main/kotlin/...`): `NotificationChannels.kt`, `NotificationAccess.kt`, `AppVisibility.kt`, `AlarmManagerRestReminderScheduler.kt`, `RestReminderNotifier.kt`, `RestReminderReceiver.kt`, `NotificationsSetup.kt`, `di/NotificationsModule.kt`
- Create: `core/notifications/src/main/res/drawable/ic_notification.xml` (24 dp white vector of a stopwatch), `res/values/strings.xml`, `res/values-es/strings.xml`
- Test: `core/notifications/src/androidTest/kotlin/com/japl/repzi/core/notifications/` (`RestReminderNotifierTest.kt`, `AlarmManagerRestReminderSchedulerTest.kt`, `NotificationChannelsTest.kt`)

**Interfaces:**
- Consumes: `RestReminderScheduler`, `ObserveRestNotificationUseCase`, `FakeSettingsRepository` (Task 1).
- Produces:
  - `fun interface AppVisibility { fun isVisible(): Boolean }`; `ProcessAppVisibility` (Hilt-injected, `ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(STARTED)`).
  - `internal object NotificationChannels { const val REST = "rest"; const val INACTIVITY = "inactivity"; fun create(context: Context) }`: `rest` importance HIGH with vibration, `inactivity` importance DEFAULT; names and descriptions from strings.
  - `internal fun Context.canPostNotifications(): Boolean`: `NotificationManager.areNotificationsEnabled()` and, on 33+, the `POST_NOTIFICATIONS` permission granted.
  - `internal class RestReminderNotifier @Inject constructor(context, observeRestNotification, visibility: AppVisibility)` with `suspend fun onRestEnded()` and `companion const val NOTIFICATION_ID = 1`.
  - `class NotificationsSetup @Inject constructor(...)` with `fun start()`; here it only creates the channels (Task 4 adds the inactivity schedule).
  - Strings: `notification_channel_rest_name` "Rest timer", `notification_channel_rest_description` "Tells you when a rest between sets is over", `notification_channel_inactivity_name` "Workout reminders", `notification_channel_inactivity_description` "Reminds you to train after some days without a workout", `notification_rest_title` "Rest is over", `notification_rest_text` "Time for your next set." Spanish: "Descanso", "Te avisa cuando termina un descanso entre series", "Recordatorios de entrenamiento", "Te recuerda entrenar tras varios días sin hacerlo", "Descanso terminado", "Es hora de tu siguiente serie."

- [ ] **Step 1: Write the failing instrumented tests**

Helpers: grant the permission with `InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(packageName, Manifest.permission.POST_NOTIFICATIONS)` (API 33+ only, `assumeTrue` otherwise); read posted notifications with `NotificationManager.activeNotifications`, polling up to 2 s; `@After` cancels all.

`RestReminderNotifierTest` (setting via `ObserveRestNotificationUseCase(FakeSettingsRepository(initialRestNotification = ...))`, visibility via a lambda):
  - `postsWhenTheSettingIsOnAndTheAppIsInTheBackground`: one notification with id 1, channel `rest`, title "Rest is over".
  - `postsNothingWhenTheSettingIsOffAtTheTimeOfTheAlarm`: construct with the setting on, flip it off through the fake repository, call `onRestEnded()` -> none (Review Focus 1).
  - `postsNothingWhenTheAppIsVisible` (Review Focus 2).
  - `postsNothingWhenNotificationsAreBlocked`: revoke the permission (`revokeRuntimePermission`, API 33+, `assumeTrue`), no exception, none posted (Review Focus 3).

`AlarmManagerRestReminderSchedulerTest`: `scheduleLeavesAPendingAlarm` (schedule one hour ahead; `PendingIntent.getBroadcast(context, REQUEST_CODE, intent, FLAG_NO_CREATE or FLAG_IMMUTABLE) != null`), `cancelRemovesThePendingAlarm`, `schedulingAgainReplacesTheAlarm` (schedule twice with different times, cancel once, none left).

`NotificationChannelsTest`: after `create`, `rest` exists with importance HIGH and vibration enabled, `inactivity` exists with importance DEFAULT; calling `create` twice changes nothing.

- [ ] **Step 2: Run to verify they fail**

Run: `.\gradlew.bat :core:notifications:connectedDebugAndroidTest` (emulator running)
Expected: fails to compile (module classes missing).

- [ ] **Step 3: Implement**

  - Module: `repzi.android.library` + `repzi.android.hilt`, namespace `com.japl.repzi.core.notifications`; `implementation(projects.core.domain)`, coroutines-android, `libs.androidx.lifecycle.process`; test deps as in `core/data` (`androidTestImplementation(projects.core.testing)`, ext-junit, runner, kotlin-test-junit, coroutines-test).
  - Manifest: `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `<receiver android:name=".RestReminderReceiver" android:exported="false" />`.
  - `AlarmManagerRestReminderScheduler`: explicit broadcast to `RestReminderReceiver`, request code constant `REQUEST_CODE = 1`, `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`. `schedule(endsAt)` uses `setExactAndAllowWhileIdle(RTC_WAKEUP, ...)` when `SDK_INT < 31 || canScheduleExactAlarms()`, else `setAndAllowWhileIdle`. `cancel()` cancels the alarm and the pending intent.
  - `RestReminderNotifier.onRestEnded()`: return unless `observeRestNotification().first()`; return if `visibility.isVisible()`; return unless `canPostNotifications()`; post on channel `rest` with `ic_notification`, the two strings, `setAutoCancel(true)`, category reminder, and a content intent that is the app launch intent (`packageManager.getLaunchIntentForPackage`, no content intent if null).
  - `RestReminderReceiver`: `@AndroidEntryPoint`, `@Inject lateinit var notifier`; `onReceive` calls `super.onReceive` first (Hilt requirement), then `goAsync()` and a `CoroutineScope(Dispatchers.Default)` that calls the notifier and finishes the pending result in `finally`.
  - `NotificationsModule`: `@Binds` `RestReminderScheduler` and `AppVisibility`.
  - `RepziApplication` injects `NotificationsSetup` and calls `start()` in `onCreate`.
  - Resolve any `ScheduleExactAlarm` / `MissingPermission` lint finding with the narrowest suppression and a one-line reason.

- [ ] **Step 4: Run to verify they pass**

Run: `.\gradlew.bat :core:notifications:connectedDebugAndroidTest`, then (emulator stopped) `.\gradlew.bat assembleDebug lintDebug`
Expected: all new tests pass; BUILD SUCCESSFUL; the merged manifest of `:app` contains both permissions and the receiver.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts gradle/libs.versions.toml app core/notifications
git commit -m "Add the rest-over notification with an exact alarm"
```

---

### Task 3: Schedule and cancel the alarm from the session

**Files:**
- Modify: `feature/session/src/main/kotlin/com/japl/repzi/feature/session/SessionViewModel.kt`, `feature/session/src/test/kotlin/com/japl/repzi/feature/session/SessionViewModelTest.kt`
- Test: `feature/session/src/test/kotlin/com/japl/repzi/feature/session/SessionRestViewModelTest.kt` (extend)

**Interfaces:**
- Consumes: `RestReminderScheduler`, `ObserveRestNotificationUseCase`, `FakeRestReminderScheduler`, `FakeSettingsRepository` (Task 1).
- Produces: `SessionViewModel` constructor gains `observeRestNotification: ObserveRestNotificationUseCase` and `restReminders: RestReminderScheduler`.

- [ ] **Step 1: Write the failing tests** in `SessionRestViewModelTest` (add the two new constructor arguments to `createViewModel`, with a `FakeRestReminderScheduler` field `reminders`, and to `SessionViewModelTest`):
  - `schedulesTheReminderAtTheEndOfTheRestWhenTheSettingIsOn`: setting on, tick the squat set (60 s) -> `reminders.scheduled == listOf(startOfRest + 60 s)`.
  - `schedulesNothingWhenTheSettingIsOff`.
  - `adjustingTheRestMovesTheReminder`: after +15 s the last scheduled instant is the old one + 15 s.
  - `skippingTheRestCancelsTheReminder`: `cancelCount == 1`.
  - `takingAwayAllTheRestCancelsTheReminder`: adjusting by -(everything left) -> `cancelCount == 1`.
  - `finishingTheWorkoutCancelsTheReminder`, `discardingTheWorkoutCancelsTheReminder`, `leavingWithoutInputCancelsTheReminder` (rest running, then the matching intent).
  - `theReminderIsNotCancelledWhenTheCountdownEndsInTheApp`: advance past the end -> `cancelCount == 0` (the alarm decides by visibility).
  - `aRestStoppedBeforeTheSettingWasReadIsNotScheduled`: start a rest and skip it before the scheduling coroutine runs -> `scheduled` is empty after both run (the stale end is not scheduled).

- [ ] **Step 2: Run to verify they fail**

Run: `.\gradlew.bat :feature:session:test`
Expected: compile error on the new constructor parameters.

- [ ] **Step 3: Implement** in `SessionViewModel`:
  - Inject the two new parameters.
  - `startRest` and `adjustRest` call a private `scheduleReminder(endsAt: Instant)`: launches in `viewModelScope`, reads `observeRestNotification().first()`, and schedules only if the setting is on **and** `restEndsAt == endsAt` still holds (the rest was not skipped or moved meanwhile).
  - `stopRest()` calls `restReminders.cancel()`. `finish()` (the `Saved` branch), `discard()` and `close()` call it too. Do not call it from `tickRest` at the natural end and do not add `onCleared`.

- [ ] **Step 4: Run to verify they pass**

Run: `.\gradlew.bat :feature:session:test`
Expected: PASS, all existing session tests included.

- [ ] **Step 5: Commit**

```bash
git add feature/session
git commit -m "Schedule the rest-over alarm from the session"
```

---

### Task 4: The inactivity reminder

**Files:**
- Modify: `core/notifications/build.gradle.kts` (add `libs.androidx.work.runtime.ktx`, `libs.androidx.hilt.work`, `add("ksp", libs.androidx.hilt.compiler)`, `androidTestImplementation(libs.androidx.work.testing)`, `testImplementation(projects.core.testing)`, kotlin-test-junit, junit, coroutines-test), `core/notifications/.../NotificationsSetup.kt`, `core/notifications/.../di/NotificationsModule.kt`, both `strings.xml`
- Create (package `com.japl.repzi.core.notifications`): `InactivityNotifier.kt`, `NotificationManagerInactivityNotifier.kt`, `InactivityCheck.kt`, `InactivityCheckWorker.kt`, `InactivityScheduler.kt`
- Test: `core/notifications/src/test/kotlin/com/japl/repzi/core/notifications/InactivityCheckTest.kt`, `InactivitySchedulerTest.kt`; `core/notifications/src/androidTest/kotlin/com/japl/repzi/core/notifications/InactivityNotifierTest.kt`, `InactivityCheckWorkerTest.kt`

**Interfaces:**
- Consumes: `InactivityRule`, `InactivityNoticeLog`, `ObserveInactivityReminderUseCase`, `ObserveInactivityDaysUseCase`, `SessionRepository`, fakes (Task 1); `canPostNotifications`, `NotificationChannels` (Task 2).
- Produces:
  - `fun interface InactivityNotifier { fun notify(days: Int): Boolean }` (true when posted).
  - `internal class InactivityCheck @Inject constructor(sessions, observeReminder, observeDays, noticeLog, notifier, clock)` with `suspend operator fun invoke()`.
  - `internal fun delayUntilNextRun(now: ZonedDateTime, hour: Int = 18): java.time.Duration`; `internal class InactivityScheduler @Inject constructor(context)` with `fun schedule()`; work name constant `INACTIVITY_WORK_NAME = "inactivity-check"`.
  - Strings: `notification_inactivity_title` "Time to train"; plurals `notification_inactivity_text` one "It has been %d day since your last workout." / other "It has been %d days since your last workout."; Spanish "Es hora de entrenar" and "Han pasado %d día desde tu último entrenamiento." / "Han pasado %d días desde tu último entrenamiento."

- [ ] **Step 1: Write the failing tests**

`InactivityCheckTest` (JVM; fake sessions, settings, `FakeInactivityNoticeLog`, a recording notifier with a configurable result, a fake clock):
  - `doesNothingWhenTheReminderIsOff`; `doesNothingWithoutFinishedWorkouts`.
  - `notifiesWithTheWholeDaysElapsed`: last workout 5 days ago, days 3 -> notifier called with 5, log recorded the workout's `finishedAt`.
  - `usesTheLatestWorkoutOfSeveral`.
  - `notifiesOnceForTheSameLapse`: invoke twice -> one call.
  - `notifiesAgainAfterANewWorkout`.
  - `doesNotNotifyAgainWhenTheLatestWorkoutWasDeleted` (Review Focus 4).
  - `doesNotRecordANoticeThatCouldNotBePosted`: notifier returns false -> nothing recorded; a second invoke with a notifier that posts -> notifies (Review Focus 5).

`InactivitySchedulerTest` (JVM, `delayUntilNextRun` with `ZoneId.of("America/Lima")` and `ZoneId.of("Europe/Madrid")`): `beforeTheHourTodayIsTheSameDay` (10:00 -> 8 h), `afterTheHourIsTomorrow` (20:00 -> 22 h), `exactlyAtTheHourIsTomorrow` (18:00 -> 24 h), `springForwardDayIsStillTheNext18h` (the Madrid DST change day: the result matches `Duration.between(now, next18h)` computed with `ZonedDateTime`, not a fixed 24 h).

`InactivityNotifierTest` (instrumented): `postsOnTheInactivityChannelWithThePluralText` (days 1 and 5 produce "1 day" / "5 days"), returns true; `returnsFalseAndPostsNothingWhenNotificationsAreBlocked` (revoke on 33+, `assumeTrue`).

`InactivityCheckWorkerTest` (instrumented, `TestListenableWorkerBuilder` with a factory like `CatalogSyncWorkerTest`): `runsTheCheckAndSucceeds` with a real `InactivityCheck` over fakes -> `Result.success()` and one notice. Plus an instrumented check that `InactivityScheduler.schedule()` called twice leaves exactly one unique work `inactivity-check` in `ENQUEUED` (use `WorkManagerTestInitHelper`, like `WorkManagerCatalogSyncSchedulerTest`).

- [ ] **Step 2: Run to verify they fail**

Run: `.\gradlew.bat :core:notifications:test` and `.\gradlew.bat :core:notifications:connectedDebugAndroidTest`
Expected: compile errors on the missing classes.

- [ ] **Step 3: Implement**
  - `InactivityCheck.invoke`: return if `!observeReminder().first()`; `last = sessions.observeSessions().first().maxOfOrNull { it.finishedAt }`; `report = InactivityRule.daysToReport(clock.now(), last, observeDays().first(), noticeLog.lastNoticeFor())`; if `report != null && notifier.notify(report)` then `noticeLog.record(last!!)`.
  - `NotificationManagerInactivityNotifier` (binds `InactivityNotifier`): false unless `canPostNotifications()`; posts id 2 on channel `inactivity`, plural text, launch content intent as in Task 2, returns true.
  - `InactivityCheckWorker`: `@HiltWorker` `CoroutineWorker` over `InactivityCheck`; `doWork` runs it and returns `Result.success()`.
  - `InactivityScheduler.schedule()`: `PeriodicWorkRequestBuilder<InactivityCheckWorker>(24, TimeUnit.HOURS).setInitialDelay(delayUntilNextRun(ZonedDateTime.now()))`, `enqueueUniquePeriodicWork(INACTIVITY_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)`.
  - `NotificationsSetup.start()` also calls `inactivityScheduler.schedule()`; bind the notifier in `NotificationsModule`.

- [ ] **Step 4: Run to verify they pass**

Run: `.\gradlew.bat :core:notifications:test`, then (emulator on) `.\gradlew.bat :core:notifications:connectedDebugAndroidTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/notifications
git commit -m "Add the inactivity reminder with a daily check"
```

---

### Task 5: The Notifications section in Settings

**Files:**
- Modify: `feature/settings/src/main/kotlin/com/japl/repzi/feature/settings/SettingsUiState.kt`, `SettingsViewModel.kt`, `SettingsScreen.kt`, `feature/settings/src/main/res/values/strings.xml`, `values-es/strings.xml`
- Test: `feature/settings/src/test/kotlin/com/japl/repzi/feature/settings/SettingsViewModelTest.kt` (extend), `feature/settings/src/androidTest/kotlin/com/japl/repzi/feature/settings/SettingsScreenTest.kt` (extend)

**Interfaces:**
- Consumes: the six use cases and `INACTIVITY_DAYS_RANGE`, `DEFAULT_INACTIVITY_DAYS` (Task 1).
- Produces:
  - `SettingsUiState(restVibration = true, restNotification = false, inactivityReminder = false, inactivityDays = DEFAULT_INACTIVITY_DAYS)`.
  - `SettingsViewModel` constructor gains the six use cases; new `onRestNotificationChange(Boolean)`, `onInactivityReminderChange(Boolean)`, `onInactivityDaysChange(Int)`.
  - `internal data class NotificationHints(val notificationsBlocked: Boolean, val exactTimingMissing: Boolean)`.
  - `SettingsScreen` gains `hints: NotificationHints`, `onRestNotificationChange`, `onInactivityReminderChange`, `onInactivityDaysChange: (Int) -> Unit`, `onOpenNotificationSettings`, `onOpenExactAlarmSettings`.
  - Strings: `settings_section_notifications` "Notifications", `settings_rest_notification` "Notify when a rest ends", `settings_inactivity_reminder` "Remind me when I stop training", `settings_inactivity_days` plurals ("After %d day" / "After %d days"), `settings_inactivity_days_less` "Fewer days", `settings_inactivity_days_more` "More days" (content descriptions), `settings_notifications_blocked` "Notifications are blocked for Repzi. Tap to change it in system settings.", `settings_exact_timing_missing` "The notification may arrive late. Tap to allow exact timing." Spanish: "Notificaciones", "Avisar cuando termine un descanso", "Recordarme si dejo de entrenar", "Tras %d día" / "Tras %d días", "Menos días", "Más días", "Las notificaciones están bloqueadas para Repzi. Toca para cambiarlo en los ajustes del sistema.", "El aviso puede llegar con retraso. Toca para permitir la hora exacta."

- [ ] **Step 1: Write the failing tests**

`SettingsViewModelTest` (update `createViewModel` with the new use cases): `newSettingsStartOff` (restNotification false, inactivityReminder false, days 3), `uiStateFollowsTheNewSettings`, `changingTheNotificationSwitchesWritesToTheRepository`, `changingTheDaysWritesToTheRepository`.

`SettingsScreenTest` (extend `setScreen` with defaulted parameters for the new state, hints and callbacks): `notificationSwitchesCallBackWithTheNewValue` (both), `stepperIsHiddenWhileTheInactivityReminderIsOff`, `stepperShowsTheDays` (text "After 3 days"), `stepperLessAndMoreCallBackWithDaysMinusAndPlusOne`, `stepperLessIsDisabledAtOneAndMoreAtFourteen`, `blockedHintIsShownAndOpensTheSystemSettings`, `blockedHintIsHiddenWhenNothingIsBlocked`, `exactTimingHintIsShownAndOpensItsScreen`.

- [ ] **Step 2: Run to verify they fail**

Run: `.\gradlew.bat :feature:settings:test` (emulator stopped), then `:feature:settings:connectedDebugAndroidTest`
Expected: compile errors on the new parameters.

- [ ] **Step 3: Implement**
  - ViewModel: `uiState` combines the four settings flows (`combine`) into `SettingsUiState`; keep `WhileSubscribed(5_000)`.
  - Screen: a "Notifications" section between General and Help with the rest switch (`SwitchRow`), the inactivity switch, and, only while it is on, a stepper row (title plural, two `IconButton`s with the "less"/"more" content descriptions, disabled at `INACTIVITY_DAYS_RANGE` bounds). Hint rows (clickable `ListItem` with supporting text) appear under the switches according to `hints`.
  - `SettingsRoute`: computes `NotificationHints` from the system and refreshes it on `ON_RESUME` (`LifecycleEventEffect`): `notificationsBlocked = !(areNotificationsEnabled && (SDK < 33 || permission granted)) && (uiState.restNotification || uiState.inactivityReminder || permissionDenied)`, `exactTimingMissing = SDK >= 31 && uiState.restNotification && !alarmManager.canScheduleExactAlarms()`. It owns a `rememberLauncherForActivityResult(RequestPermission())`: turning a switch **on** with the permission missing (33+) stores the pending action and launches the request; granted runs it, denied clears it, sets `permissionDenied = true` and leaves the switch off; turning a switch off never asks. Hint clicks open `Settings.ACTION_APP_NOTIFICATION_SETTINGS` (with `EXTRA_APP_PACKAGE`) and `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` (with the package URI) through the existing `startActivitySafely`.

- [ ] **Step 4: Run to verify they pass**

Run: `.\gradlew.bat :feature:settings:test`, then (emulator on) `.\gradlew.bat :feature:settings:connectedDebugAndroidTest`
Expected: PASS, the existing Settings tests included.

- [ ] **Step 5: Commit**

```bash
git add feature/settings
git commit -m "Add the notification settings"
```

---

### Task 6: README, full verification and emulator check

**Files:**
- Modify: `README.md` (feature list, module list with `:core:notifications`, roadmap line for level 3: notifications done, widget pending; one line under future work: "Foreground service with a live rest countdown in the notification (evaluated, not built)")

- [ ] **Step 1: Update the README** as listed, in English, keeping its tone.

- [ ] **Step 2: Run the whole suite**

Run (emulator stopped): `.\gradlew.bat assembleDebug assembleDebugAndroidTest test lintDebug`; then (emulator on) `.\gradlew.bat :core:database:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest :core:notifications:connectedDebugAndroidTest :feature:settings:connectedDebugAndroidTest :feature:session:connectedDebugAndroidTest`
Expected: BUILD SUCCESSFUL, zero failures.

- [ ] **Step 3: Check on the emulator** with a debug install: in Settings turn on "Notify when a rest ends" (accept the permission), start a routine, set a 10 s rest, tick a set and press Home -> the notification arrives about 10 s later and tapping it returns to the workout; repeat with the screen off (`adb shell input keyevent 26`); repeat with the app open -> no notification. Deny the permission (`adb shell pm revoke com.japl.repzi android.permission.POST_NOTIFICATIONS`, reinstall state as needed) and turn the switch on -> it stays off and the blocked hint appears. Confirm the inactivity work exists (`adb shell dumpsys jobscheduler` lists the app's `SystemJobService` job). Note whether a double vibration is felt (not observable on the emulator).
  Expected: each behaviour as described; record what could not be verified.

- [ ] **Step 4: Commit**

```bash
git add README.md
git commit -m "Document the notifications"
```
