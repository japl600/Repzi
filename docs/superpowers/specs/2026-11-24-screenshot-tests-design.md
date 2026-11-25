# Screenshot tests, design

Status: approved by the user in conversation (tool, scope, variants). Date: 2026-11-24.

## Intent

The instrumented tests check that the text is there, not that the screen looks right. The widget changed its layout twice in one session and no test noticed. Screenshot tests keep a reference image of each screen state and fail when the pixels change, on the JVM, with no emulator, in the CI job that already runs `./gradlew test`.

## Goals

- A reference image per relevant state of each stateless `*Screen` composable, in light and dark theme, in English.
- `@PreviewTest` previews that also work as normal Android Studio previews.
- A fast CI step that fails on any unintended visual change and uploads the diff report.
- A short README section on how to update the references.

## Non-goals

- Replacing the behavior tests (taps, navigation, alarms).
- The Glance widget (so the scope is 8 screens in 7 modules). Glance composables are not regular Compose, so the preview engine cannot render them; the widget stays covered by its Glance unit tests and the README screenshot.
- Spanish, font-scale or every theme x language x size combination.
- Pixel-perfect equality across machines if the engine renders differently on CI (see Risks).

## Anti-goals

- References that change on every run (real clocks, random data, animations).
- A big set of images nobody reviews: each reference must be small enough to read in a PR diff.
- A new heavy test dependency that drags in Robolectric or Paparazzi.

## Constraints

- AGP 9.5.1, Kotlin 2.4.21, Compose BOM 2026.09.00, JDK 21. The test-suite integration needs AGP 9.5.0-alpha03+ and engine `0.0.1-alpha17` (`alpha16`, the one the docs name, fails to render with a missing `R` class) (the old `com.android.compose.screenshot` plugin is unmaintained: do not use it).
- Features are `com.android.library` modules (convention plugin `repzi.android.feature`); the composables are `internal`, so the previews must see internal declarations of their own module.
- English in code, comments, docs and commits.

## Approaches considered

1. **Google's Compose Preview Screenshot Testing through AGP test suites (chosen by the user).** Official, no emulator, previews are reusable in Android Studio. Alpha engine, and the docs do not show library modules or `internal` visibility.
2. **Roborazzi.** Mature, reuses Compose test code, but adds Robolectric and its AGP 9.5 / Kotlin 2.4 support is unconfirmed. Fallback if 1 fails the spike.
3. **Paparazzi.** Fast, separate task, same unconfirmed compatibility. Not considered further.

## Design

### Build

- Root `gradle.properties`: `android.experimental.enableScreenshotTest=true` and `android.experimental.testSuiteSupport=true`.
- A `screenshotTests.create("screenshotTest")` suite per feature module, with the engine version and its two dependencies (`ui-tooling`, `screenshot-validation-api`). To avoid repeating it in eight modules, the suite is configured once in the feature convention plugin (`build-logic`), with the engine version in `libs.versions.toml`.
- Sources in `{module}/src/screenshotTest/kotlin/`; references in `{module}/src/screenshotTestDefaultDebug/reference/` and committed to Git.

### Previews

- One file per screen, `XScreenScreenshots.kt`, with `@PreviewTest @Preview` functions that wrap the screen in `RepziTheme` and feed it fixed fake data. Light and dark come from a multi-preview annotation (`uiMode` night yes/no), declared once in `core:ui`'s test-only area or in the suite itself (decided by the spike).
- Fixed data only: invented names, numbers and a fixed `Instant`; no `Clock`, no `Locale` change. The test JVM runs in English.

### Coverage (about 40 images: 8 screens x states x 2 themes, fewer where a state does not exist)

| Screen | States |
|---|---|
| Exercises | list with data, empty, filtered by muscle group |
| ExerciseDetail | with steps, author and license |
| Routines | empty (with predefined routines), with own routines |
| RoutineEditor | new, with exercises |
| Session | sets not done, one set done with the rest timer running |
| History | with sessions, empty |
| Progress | chart with data, no data |
| Settings | defaults, notifications on |

The Licenses screen is a library screen with generated data and is left out.

### Workflow

- Update: `gradlew updateDebugScreenshotTestDefaultTestSuite` per module when a visual change is intended; review the PNG diff in the PR.
- Validate: `gradlew testDebugScreenshotTestDefaultTestSuite`. One root-level task or script runs them all.
- CI: a step in job 1 ("Build y tests unitarios") after `./gradlew assembleDebug test`; on failure it uploads `**/build/reports/tests/*ScreenshotTest*` as an artifact.

## Spike first (the riskiest part)

Before the whole set: one screen (Settings) in one theme, in one feature module, to confirm in this order: the suite configures with AGP 9.5.1 and Kotlin 2.4.21 in a library module; the preview sees `internal` `SettingsScreen`; record then validate passes, and validate fails after a deliberate visual change; the CI runner renders the same pixels as this PC. If a point fails and has no cheap fix, stop and go back to the user with Roborazzi as the fallback.

## Risks

1. **Compatibility** of the alpha engine with AGP 9.5.1 / Kotlin 2.4.21 or with library modules. Settled by the spike.
2. **`internal` visibility** from the custom source set. If not visible: make the previews live in `src/main` (they would ship in the APK, so only as a last resort) or expose the screens with `@VisibleForTesting`-style opt-in; the spike decides.
3. **Rendering differs between this PC and Linux CI** (fonts, anti-aliasing). Mitigation: generate references from the CI's output if needed, or use an image-difference threshold if the suite supports one.
4. **Maintenance**: every visual change needs updated references. That is the point, and the README says how.

## Left to the builder

- Exact engine version line and where the multi-preview annotation lives.
- The fake data for each state; keep it as small as the state allows.
- The CI artifact paths once the real report directory is known.

## Spike result (Settings, light theme, 2026-11-24)

- It works in a library module with AGP 9.5.1 and Kotlin 2.4.21, and the preview sees the `internal` screen with no change to `main`. Record then validate pass; a visual change fails validation (even a 0.01% difference).
- Engine `0.0.1-alpha17` is needed (see Constraints).
- Each module with a suite needs `unitTests.isIncludeAndroidResources = true` and its host unit tests enabled for every variant (`androidComponents.beforeVariants`); without the second one the plugin fails at configuration time. That makes `test` also run `testReleaseUnitTest`, so CI runs `./gradlew assembleDebug test -x testReleaseUnitTest` (not `testDebugUnitTest`, which the pure Kotlin modules do not have).
- Still unconfirmed: that the CI runner renders the same pixels as the developer's PC. The first push with only the Settings module checks it.
