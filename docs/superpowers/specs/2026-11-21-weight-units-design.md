# Weight units (kg / lb) - design

## Intent

Repzi shows every weight in kilograms. People who train in pounds have to convert in their head while logging and while reading their progress. This adds a setting that lets the user work in pounds, everywhere a weight is typed or shown.

## Goals

- A "Weight unit" setting with two choices, kg (default) and lb, in the Settings screen.
- The unit applies to what the user types in a workout, and to every weight shown in the workout, the history and the progress screens.
- Switching the unit never loses or distorts data: a workout logged as 135 lb reads 135 lb when the unit is lb and about 61.2 kg when it is kg.

## Non-goals and anti-goals

- No per-exercise unit and no mixing units on one screen.
- No migration of stored data and no new column: weights stay stored in kilograms (`weightKg`), the unit only changes how they are typed and shown.
- Anti-goal: rounding errors that make a lifted 135 lb come back as 134.99 lb.
- Not in scope: distances, body weight, plate calculators.

## Approaches

- **A. Setting, storage stays in kg, conversion at the edges (picked).** One conversion type in the domain, each screen converts when it shows or reads a number. No database change, switching is instant and lossless.
- **B. Store the unit with each workout.** Rejected: a history with mixed units is hard to chart and every query needs the unit.
- **C. Convert the stored data when the unit changes.** Rejected: destructive, drifts with repeated switching and is hard to undo.

## Design

- `WeightUnit` (domain): `KG`, `LB`; `fromKg(kg)` and `toKg(value)`, using 1 lb = 0.45359237 kg.
- `SettingsRepository.weightUnit: Flow<WeightUnit>` (default `KG`) and `setWeightUnit`, with one observe and one set use case, like the other settings. Stored in DataStore under `weight_unit` as the enum name; an unknown value falls back to `KG`.
- Display: `formatWeight(kg, unit)` in `:core:ui` converts and rounds to 2 decimals before using the existing `formatNumber`, so 61.2349 kg shows as 135 lb. A unit label (`kg`, `lb`) comes from string resources.
- Workout: `SessionState` carries the unit; the weight field label is the unit; what is typed is read in that unit and converted to kg when the workout is saved.
- History and progress: their state carries the unit; every weight and the volume are converted when shown. In progress the plotted series is converted, so the record, the change and the chart axis are in the chosen unit; the percent change does not depend on the unit.
- Settings: a two-option row in the General section.

## Calls made without being asked (check these)

- Default is kg, so nothing changes for existing users.
- The setting is a switch between two units, not a replacement of kg by lb.
- Values are rounded to 2 decimals only for display. Nothing stored is rounded.
- If the unit is changed while a workout is open, the numbers already typed are not converted: they are read in the new unit. Settings cannot be reached from inside a workout, so this needs the app to be left mid-workout.
- Spanish and English use the same unit labels (`kg`, `lb`).

## Tests

- Unit: `WeightUnit` conversions both ways and the round trip; DataStore default, persistence and unknown value; `SessionViewModel` saves a typed value in lb as kg; `HistoryViewModel` and `ProgressViewModel` expose the unit; the progress series, record and change in lb; `SettingsViewModel`.
- Instrumented: the Settings row, the label and the converted figures on the workout, history and progress screens.
