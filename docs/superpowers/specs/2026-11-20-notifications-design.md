# Notifications (level 3, part 2) - design

## Intent

Repzi only reaches the user while its screen is open. Two moments matter outside it: the rest between sets ends while the phone is locked or the app is minimized, and a person who stops training is not reminded. This part adds two notifications, both optional and off by default:

1. **Rest over**: a notification at the exact moment a rest countdown ends, if the app is not in the foreground.
2. **Inactivity reminder**: a notification when N days have passed without a finished workout. N is chosen by the user.

## Goals

- The rest notification arrives on time with the screen off, with the app minimized, and after the app is swiped away from recents (on stock Android).
- The inactivity reminder is configurable (number of days), fires once per lapse, and never nags.
- The Android 13+ notification permission is asked only when the user turns a notification on, never at launch.
- English and Spanish strings, like the rest of the app.

## Non-goals and anti-goals

- No foreground service and no live countdown in the notification. That is option B in the approaches below, kept as a future improvement to evaluate.
- No notification while the app is visible: in the foreground the countdown and the buzz already say it.
- No fixed-time workout reminders per weekday (the third idea that was discussed).
- Anti-goal: a reminder that repeats every day after the lapse started. One notice per lapse.
- Anti-goal: the app sending the user to a system settings screen as a precondition. Exact timing is a bonus, not a requirement.

## Approaches (rest notification)

- **A. Exact alarm (picked by the user).** `AlarmManager` fires a receiver at the end of the rest; the receiver posts the notification. Works even if the process is frozen or dead. Exact alarms are a special permission on Android 12+ that the user grants in system settings; without it the app falls back to an inexact alarm that can arrive late.
- **B. Foreground service during the workout.** Live countdown in the notification with +30 s / skip actions, most reliable. Needs the timer moved out of the ViewModel, a foreground service type and permissions. Future improvement.
- **C. WorkManager with a delay.** Rejected: no guarantee of the exact time.

The inactivity reminder has no real alternative: the app already uses WorkManager, and a daily check is enough.

## Design

### Settings (stored in DataStore, next to `restVibration`)

`SettingsRepository` gets `restNotification: Flow<Boolean>` (default false), `inactivityReminder: Flow<Boolean>` (default false), `inactivityDays: Flow<Int>` (default 3, range 1..14), each with a setter and, following the vibration precedent, one observe and one set use case per setting. A small separate interface `InactivityNoticeLog` (domain) stores the finish time of the last workout a notice was already sent for, in the same DataStore.

Settings screen: new section "Notifications" with a switch for each notification and a days stepper shown under the inactivity switch while it is on. Turning a switch on when the permission is missing (API 33+) asks for it; if denied the switch stays off and the row shows a line that links to the system notification settings of the app. The same line appears if a notification is on but the user later blocked it in the system. On API 31+, when the rest notification is on and exact alarms are not allowed, a line says that timing is approximate and opens the special-access screen (`ACTION_REQUEST_SCHEDULE_EXACT_ALARM`). Both hints refresh when the user comes back to the screen.

### New module `:core:notifications`

Same shape as `:core:network` (library + Hilt convention plugins). It owns channels, strings, the receiver, the worker and the posting code, so no feature module touches `NotificationManager`.

- Domain contract (in `:core:domain`): `RestReminderScheduler { schedule(endsAt: Instant); cancel() }`.
- Channels created on app start: `rest` (importance high, vibration on) and `inactivity` (importance default). Their user-visible names are strings in en/es.
- Tapping either notification brings the app back (launch intent). If the task is alive the user returns to the workout in progress; otherwise they land on the first screen.

### Rest notification

- `SessionViewModel` takes the scheduler. Ticking a set with a rest calls `schedule(restEndsAt)` only when the setting is on, reading it at that moment. `adjustRest` reschedules; `stopRest` (skip), `finish`, `discard` and `close` cancel.
- The ViewModel does not cancel at the natural end of the countdown and not in `onCleared`: the ViewModel keeps counting in the background and would cancel the alarm it should let fire, and swiping the app away clears it too.
- `AlarmManagerRestReminderScheduler` uses `setExactAndAllowWhileIdle` when `canScheduleExactAlarms()` (or API below 31), otherwise `setAndAllowWhileIdle`. One `PendingIntent` with a fixed request code, so schedule replaces and cancel removes.
- `RestReminderReceiver` (Hilt entry point, `goAsync`) posts "Rest is over - time for your next set" unless: the setting is now off, notifications are blocked, or the app is visible (`ProcessLifecycleOwner` state at least STARTED).
- Manifest: `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`.

### Inactivity reminder

- A unique periodic work `inactivity-check`, every 24 hours, first run delayed to the next 18:00 local time, scheduled on every app start with `KEEP` (like the catalog sync). It always exists; the worker reads the settings when it runs and does nothing if the reminder is off.
- Rule as a pure function `InactivityRule.shouldNotify(now, lastWorkoutAt, days, lastNoticedFor)`: false if there is no finished workout yet; true when `now - lastWorkoutAt` is at least `days` days and `lastNoticedFor != lastWorkoutAt`. After sending, the worker records `lastWorkoutAt`. Training again changes `lastWorkoutAt`, which rearms the reminder.
- Text: "Time to train - it has been N days since your last workout" (plural strings; N is the whole days elapsed).
- The last workout comes from `SessionRepository.observeSessions()` (largest `finishedAt`); no new query.

## Constraints

- minSdk 26, target 37: every API-level difference above (permission 33, exact alarms 31) is guarded.
- Notification code reads `Clock` from Hilt like the rest of the app, so rules are testable.
- No network, no new storage engine: DataStore for settings and the notice log.

## Tests

- Unit: `InactivityRule` (no workouts, below and at the threshold, already noticed, rearmed after a new workout, different `days`); `SessionViewModel` with a fake scheduler (schedules when on, not when off, reschedules on adjust, cancels on skip/finish/discard/close, does not cancel at the natural end); settings use cases and `DataStoreSettingsRepository` (defaults, range clamp of days); `SettingsViewModel`.
- Instrumented: the scheduler leaves exactly one pending alarm and cancel removes it; the receiver posts when the app is in the background and does not post when the setting is off or the app is visible; the worker posts once and not twice for the same lapse; `SettingsScreenTest` for the new rows and hints.
- Manual on the emulator: rest of a few seconds with the app minimized and with the screen off.

## Left to the builder

Names of the use cases and classes beyond the ones above, the exact copy of the strings, the stepper widget, and how the hint rows detect the system state, as long as they refresh on return to the screen.

## Calls made without being asked (check these)

- Default off for both notifications; permission asked when a switch is turned on.
- Default 3 days, range 1 to 14; check once a day around 18:00; one notice per lapse; none before the first workout.
- No notification while the app is visible, decided by the receiver and not by cancelling.
- Tapping the notification only brings the app back; it does not deep-link into the routine.
- New module `:core:notifications` instead of putting this in `:core:data`.
- The inactivity worker is always scheduled and the setting is read when it runs.
- The existing in-app buzz is left unchanged. `SessionScreen` collects `RestFinished` with a `LaunchedEffect` that is not tied to the lifecycle, so with the app minimized (screen still composed) it is consumed when the rest ends and tries to buzz while the notification arrives. That may feel like two vibrations, or one if Android ignores background vibration. It is not verified; check it in the manual test and, if it is noticeable, skip the in-app buzz when the app is not visible and the notification is on.
