# wger Exercise Catalog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Grow the exercise library from 15 built-ins to the ~920 non-cardio exercises of wger, synced weekly in the background, offline-first, with author and license shown per exercise.

**Architecture:** A new `:core:network` module (Retrofit + kotlinx-serialization) returns plain `Remote*` models. `core:data` runs a sync that compares each exercise's `last_update_global` with the stored value and writes only what changed into Room (new `exercise_translations` table, schema v7). A Hilt `CoroutineWorker` runs it weekly on an unmetered network. Names resolve at read time (seed resource, app language, English). A detail screen in `:feature:exercises` shows description, author and license.

**Tech Stack:** Retrofit 3.0.0, OkHttp 5.5.1, kotlinx-serialization-json 1.13.0, WorkManager 2.12.0, Hilt-Work 1.4.0, MockWebServer 5.5.1 (`mockwebserver3`), existing Room 2.8.5 / Hilt / Compose. Versions checked on Maven on 2026-11-19; the builder re-checks them when adding.

**Spec:** `docs/superpowers/specs/2026-11-19-wger-catalog-design.md`

## Global Constraints

- `core:domain` stays Android-free; `core:data` must not expose Retrofit/OkHttp types; only `:core:network` knows them.
- HTTPS only, base URL `https://wger.de/api/v2/`, `User-Agent: Repzi`, no API key, no logging interceptor in release.
- Only languages `en` (wger id 2) and `es` (wger id 4) leave `:core:network`. Page size 100. No `ordering` or `__gt` query parameters (the API ignores them).
- Category -> `MuscleGroup`: Abs=CORE, Arms=ARMS, Back=BACK, Calves=LEGS, Chest=CHEST, Legs=LEGS, Shoulders=SHOULDERS; Cardio and any unknown category are skipped.
- Sync only inserts exercises and inserts/updates translations. It never deletes, never touches `is_custom = 1` rows, and never changes `name` or `muscle_group` of a built-in (a row with a `seed_key`).
- Weekly periodic work, `NetworkType.UNMETERED`, exponential backoff, unique name `catalog-sync`, policy `KEEP`.
- Room schema 6 -> 7 with a tested migration; the new `schemas/.../7.json` is committed.
- Built-in -> wger ids (resolved from the live API on 2026-11-19): bench_press 73, incline_dumbbell_press 537, dumbbell_fly 238, pull_up 475, barbell_row 1698, deadlift 184, squat 615, leg_press 371, lunge 984, overhead_press 566, lateral_raise 348, biceps_curl 92, triceps_extension 50, plank 458, crunch 167.
- README, commits, code comments in English; UI strings in English with `values-es`. Kg label unchanged. One commit per task, local only; pushes are done by the user.
- Footer text (en): "Exercise data from wger.de, under CC-BY-SA, CC0 and other open licenses (see each exercise)"; es: "Datos de ejercicios de wger.de, con licencias CC-BY-SA, CC0 y otras abiertas (consulta cada ejercicio)". Detail note (en): "Shown as published on wger.de."; es: "Se muestra tal como se publicó en wger.de."

## Rulings (plan deviates from or refines the spec)

- **Licenses are resolved through `GET license/`.** Translations carry a license id, not a name; the sync fetches the small license list once per run. The spec did not mention this call.
- **No separate one-time first-run work.** A periodic work request runs as soon as its constraints are met, so the spec's extra one-time run would add a sync on every launch for no gain.
- **Built-ins are linked at the start of every sync run** (idempotent, 15 updates) using `SeedExercise.wgerId`, as the spec says "on the first sync".
- The footer wording names "CC-BY-SA, CC0 and other open licenses" because wger's license table also has CC-BY 4 and ODbL.

## Review Focus

- The sync dies midway (second page fails, process killed): rows already written stay valid, the next run finishes without duplicates (Task 3).
- Nothing the user owns changes: custom exercises, logged history, routines and the 15 built-ins' names and groups survive migration and sync (Tasks 2, 3).
- The app language changes: names follow it and fall back to English when a translation is missing (Task 5).
- The list with ~950 rows stays fast: the list query loads only names, not descriptions; lists use keys; the routine picker (already a `LazyColumn`) still works (Task 5).
- Descriptions are untrusted HTML: rendered without a WebView, and an exercise with no description, no author or an unknown license id shows a calm fallback instead of blank or crash (Tasks 3, 6).

## File Structure

- `core/network/` (new): `WgerApi`, DTOs, `WgerClient` + `RetrofitWgerClient`, `Remote*` models, `WgerException`, `NetworkModule`, manifest with `INTERNET`.
- `core/database/`: `ExerciseEntity` (+3 columns), `ExerciseTranslationEntity`, `ExerciseWithNames`, `ExerciseDao` additions, `MIGRATION_6_7`, `SeedExercises.wgerId`.
- `core/data/`: `sync/` (`CatalogStore`, `RoomCatalogStore`, `ExerciseCatalogSync`, `CatalogSyncRunner`, `CategoryMapping`), `work/` (`CatalogSyncWorker`, `WorkManagerCatalogSyncScheduler`), `ExerciseNames` (new resolution), mappers.
- `core/domain/`: `Exercise.fromCatalog`, `ExerciseDetail`, `ObserveExerciseDetailUseCase`, `CatalogSyncScheduler`.
- `feature/exercises/`: detail screen, ViewModel, navigation, list changes. `feature/settings/`: new row. `app/`: scheduler call, WorkManager config, nav wiring.

---

### Task 1: `:core:network` module and API client

**Files:**
- Modify: `settings.gradle.kts`, `gradle/libs.versions.toml`, root `build.gradle.kts` (serialization plugin `apply false`).
- Create: `core/network/build.gradle.kts` (`repzi.android.library`, `repzi.android.hilt`, serialization plugin; namespace `com.japl.repzi.core.network`), `core/network/src/main/AndroidManifest.xml` (`INTERNET`), sources under `core/network/src/main/kotlin/com/japl/repzi/core/network/`, tests and fixtures under `core/network/src/test/`.

**Interfaces:**
- Produces:
  - `data class RemoteExercise(val id: Long, val category: String, val updatedAt: String, val translations: List<RemoteTranslation>)`
  - `data class RemoteTranslation(val language: String, val name: String, val description: String, val author: String?, val licenseId: Int)` (`language` is `"en"` or `"es"`)
  - `data class RemoteLicense(val id: Int, val shortName: String, val url: String)`
  - `data class ExercisePage(val exercises: List<RemoteExercise>, val hasNext: Boolean)`
  - `interface WgerClient { suspend fun fetchPage(offset: Int): ExercisePage; suspend fun fetchLicenses(): List<RemoteLicense> }`
  - `class WgerException(message: String, val retryable: Boolean, cause: Throwable? = null) : Exception`
  - Hilt: `WgerClient` bound in `NetworkModule`.

- [ ] **Step 1: Write the failing tests** in `WgerClientTest` (MockWebServer `mockwebserver3`, client pointed at its URL) with fixture `exerciseinfo_page.json` (trimmed real response: an exercise with en, es and pt translations; a Cardio one; one with English only; `next` non-null) and `licenses.json`:
  - `keepsOnlyEnglishAndSpanishTranslations`
  - `hasNextFollowsTheNextLink`
  - `sendsLimit100AndTheOffsetAndTheRepziUserAgent`
  - `parsesLicenses`
  - `ioFailureIsRetryable`, `serverError500IsRetryable`, `http429IsRetryable`, `http404IsNotRetryable`, `malformedJsonIsNotRetryable`
- [ ] **Step 2: Run** `.\gradlew.bat :core:network:testDebugUnitTest` -> Expected: FAIL (module does not exist yet).
- [ ] **Step 3: Implement** the module: version-catalog entries (versions, libraries `retrofit`, `retrofit-kotlinx-serialization` (`converter-kotlinx-serialization`), `okhttp`, `kotlinx-serialization-json`, `okhttp-mockwebserver3`, plugin `kotlin-serialization`), `include(":core:network")`, `WgerApi` (`@GET("exerciseinfo/")` with `limit` and `offset`; `@GET("license/")`), DTOs with `ignoreUnknownKeys`, `RetrofitWgerClient` mapping DTOs to `Remote*` and `HttpException`/`IOException`/`SerializationException` to `WgerException` (IO, 5xx and 429 retryable; other 4xx and parse errors not), `NetworkModule` (OkHttp with User-Agent interceptor and 30 s timeouts, Retrofit, `Json`), `INTERNET` permission in the module manifest.
- [ ] **Step 4: Run** `.\gradlew.bat :core:network:testDebugUnitTest assembleDebug` -> Expected: PASS, BUILD SUCCESSFUL (the permission merges into the app).
- [ ] **Step 5: Commit** `Add the wger API client`.

---

### Task 2: Room v7 - catalog columns, translations table, DAO, built-in wger ids

**Files:**
- Modify: `ExerciseEntity.kt`, `ExerciseDao.kt`, `RepziDatabase.kt` (version 7, new entity), `Migrations.kt` (`MIGRATION_6_7`, add to `ALL_MIGRATIONS`), `SeedExercises.kt`, `DatabaseModule.kt` if needed, `MigrationTest.kt`, `ExerciseDaoTest.kt`.
- Create: `model/ExerciseTranslationEntity.kt`, `model/ExerciseWithNames.kt`, generated `schemas/.../7.json`.

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces (all in `com.japl.repzi.core.database`):
  - `ExerciseEntity` gains `remoteId: Long?` (`remote_id`, unique index), `source: String = "local"` (`defaultValue = "'local'"`, mirror how `draft_sets.done` declares its default), `remoteUpdatedAt: String?` (`remote_updated_at`).
  - `ExerciseTranslationEntity(exerciseId: Long, language: String, name: String, description: String, author: String?, licenseName: String?, licenseUrl: String?)`, primary key (`exercise_id`, `language`), foreign key to `exercises` with `ON DELETE CASCADE`.
  - `data class TranslatedName(val language: String, val name: String)` and `ExerciseWithNames(@Embedded exercise: ExerciseEntity, @Relation(... entity = ExerciseTranslationEntity::class, projection = ["language", "name"]) names: List<TranslatedName>)` (the projection keeps descriptions out of list queries).
  - `data class RemoteExerciseWrite(val remoteId: Long, val name: String, val muscleGroup: String, val updatedAt: String, val translations: List<TranslationWrite>)` and `data class TranslationWrite(val language: String, val name: String, val description: String, val author: String?, val licenseName: String?, val licenseUrl: String?)`.
  - `ExerciseDao`: `observeAll(): Flow<List<ExerciseWithNames>>`, `observeByMuscleGroup(muscleGroup: String): Flow<List<ExerciseWithNames>>` (replace the current entity-returning ones; `@Transaction`), `suspend fun remoteVersions(): List<RemoteVersion>` (`RemoteVersion(remoteId: Long, updatedAt: String?)`), `suspend fun linkBuiltIns(): Int` (sets `remote_id` and `source = 'wger'` on seeds whose `seed_key` has a `wgerId` and `remote_id IS NULL`), `suspend fun applyRemote(writes: List<RemoteExerciseWrite>)` (`@Transaction`: a row with that `remote_id` that is a built-in or already wger only gets `remote_updated_at` and its translations replaced; an unknown `remote_id` is inserted as `source = 'wger'`, `is_custom = 0`, `seed_key = null`), `observeDetailTranslations(exerciseId: Long): Flow<List<ExerciseTranslationEntity>>`, `suspend fun insert(exercise: ExerciseEntity): Long` kept for custom exercises.
  - `SeedExercise.wgerId: Long` filled from Global Constraints.

- [ ] **Step 1: Write the failing tests.**
  - `MigrationTest.migrate6To7KeepsExercisesAndRoutinesAndAddsCatalogColumns`: seed an exercise, a routine and a `routine_exercises` row at v6; after `MIGRATION_6_7`, the rows survive, `source = 'local'`, `remote_id` and `remote_updated_at` are null, `exercise_translations` exists and deleting the exercise cascades.
  - `ExerciseDaoTest`: `applyRemoteInsertsANewExerciseWithItsTranslations`; `applyRemoteWithANewTimestampReplacesTranslationsOfThatExerciseOnly`; `applyRemoteOnALinkedBuiltInKeepsItsNameAndGroup`; `applyRemoteDoesNotTouchCustomExercises`; `remoteVersionsListsOnlyRowsWithARemoteId`; `linkBuiltInsLinksSeedsOnceAndIsIdempotent`; `remoteIdIsUnique`; `observeAllReturnsNamesWithoutDescriptions`.
- [ ] **Step 2: Run** `.\gradlew.bat :core:database:assembleDebugAndroidTest` -> Expected: FAIL to compile (new members missing). Then start the emulator `Pixel_10` (not while Gradle builds) for the instrumented runs.
- [ ] **Step 3: Implement** the entities, DAO, `MIGRATION_6_7` (`ALTER TABLE` for the three columns, unique index, `CREATE TABLE exercise_translations`; SQL must match the exported schema exactly), database version 7, `SeedExercise.wgerId`. Keep `ExerciseSeedCallback` inserting the seeds unlinked (linking happens in sync).
- [ ] **Step 4: Run** `.\gradlew.bat :core:database:connectedDebugAndroidTest` -> Expected: PASS, including the old DAO and migration tests. Then, with the emulator stopped, `.\gradlew.bat assembleDebug assembleDebugAndroidTest test lintDebug` -> Expected: BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `Add the exercise catalog tables (Room v7)`.

> The DAO's list queries now return `ExerciseWithNames`, which would break `core:data`. So Step 3 also adds a minimal adapter in `OfflineFirstExerciseRepository` and `ExerciseNames` (map `ExerciseWithNames.exercise` through the existing `toDomain`, ignoring `names`) to keep this commit green. The real name resolution is Task 5.

---

### Task 3: Sync logic (JVM-testable, no Android)

**Files:**
- Modify: `core/data/build.gradle.kts` (`implementation(projects.core.network)`), `DataModule.kt`.
- Create under `core/data/src/main/kotlin/com/japl/repzi/core/data/sync/`: `CatalogStore.kt`, `RoomCatalogStore.kt`, `ExerciseCatalogSync.kt`, `CategoryMapping.kt`. Tests under `core/data/src/test/kotlin/com/japl/repzi/core/data/sync/` with `FakeWgerClient` and `FakeCatalogStore`.

**Interfaces:**
- Consumes: Task 1 `WgerClient`, `Remote*`, `WgerException`; Task 2 `ExerciseDao`, `RemoteExerciseWrite`, `TranslationWrite`, `RemoteVersion`.
- Produces:
  - `internal interface CatalogStore { suspend fun linkBuiltIns(); suspend fun remoteVersions(): Map<Long, String?>; suspend fun apply(writes: List<RemoteExerciseWrite>) }` and `RoomCatalogStore(dao)`.
  - `internal fun interface CatalogSyncRunner { suspend operator fun invoke(): Int }` (returns how many exercises were written; throws `WgerException`).
  - `internal class ExerciseCatalogSync @Inject constructor(client: WgerClient, store: CatalogStore) : CatalogSyncRunner`.
  - `internal fun muscleGroupOf(category: String): MuscleGroup?` (null = skip).

- [ ] **Step 1: Write the failing tests** (`ExerciseCatalogSyncTest`, `CategoryMappingTest`):
  - `linksBuiltInsBeforeDownloading`
  - `firstRunWritesEveryNonCardioExerciseAndSkipsCardio`
  - `mapsEachWgerCategoryToItsMuscleGroup`
  - `usesTheEnglishNameAsTheStoredName` (fallback to the first translation when English is missing)
  - `secondRunWithNothingChangedWritesNothing`
  - `aChangedTimestampRewritesOnlyThatExercise` (including a change that only touched a translation)
  - `resolvesLicenseNameAndUrlFromTheLicenseList` and `unknownLicenseIdLeavesLicenseFieldsNull`
  - `interruptedRunKeepsCommittedPagesAndTheNextRunWritesOnlyTheRest` (client fails on page 2, then recovers)
  - `anExerciseWithNoTranslationIsSkipped`
  - `wgerExceptionPropagatesToTheCaller`
- [ ] **Step 2: Run** `.\gradlew.bat :core:data:testDebugUnitTest` -> Expected: FAIL (classes missing).
- [ ] **Step 3: Implement** `ExerciseCatalogSync.invoke()`: `store.linkBuiltIns()`; `licenses = client.fetchLicenses().associateBy { it.id }`; `versions = store.remoteVersions()`; loop pages from offset 0 by 100 until `!hasNext`; per page keep exercises whose category maps to a group, with at least one translation, and whose `updatedAt != versions[id]`; call `store.apply` once per non-empty page and update the local `versions` map; sum the written count. Bind `CatalogStore` -> `RoomCatalogStore` and `CatalogSyncRunner` -> `ExerciseCatalogSync` in `DataModule`.
- [ ] **Step 4: Run** `.\gradlew.bat :core:data:testDebugUnitTest` -> Expected: PASS.
- [ ] **Step 5: Commit** `Add the exercise catalog sync`.

---

### Task 4: WorkManager worker and scheduling

**Files:**
- Modify: `gradle/libs.versions.toml` (work-runtime-ktx, work-testing, androidx hilt-work and its compiler), `core/data/build.gradle.kts` (work, hilt-work, `ksp` for `androidx.hilt:hilt-compiler`, `androidTestImplementation` test deps), `core/domain` (interface), `DataModule.kt`, `app/src/main/AndroidManifest.xml` (remove the default WorkManager initializer with `tools:node="remove"`), `RepziApplication.kt`.
- Create: `core/data/.../work/CatalogSyncWorker.kt`, `work/WorkManagerCatalogSyncScheduler.kt`, `core/domain/.../CatalogSyncScheduler.kt`, `core/data/src/androidTest/kotlin/.../work/CatalogSyncWorkerTest.kt` and `WorkManagerCatalogSyncSchedulerTest.kt`.

**Interfaces:**
- Consumes: Task 3 `CatalogSyncRunner`, `WgerException`.
- Produces: `interface CatalogSyncScheduler { fun schedule() }` in `core:domain`; `@HiltWorker class CatalogSyncWorker @AssistedInject constructor(@Assisted Context, @Assisted WorkerParameters, runner: CatalogSyncRunner) : CoroutineWorker`; `RepziApplication : Application(), Configuration.Provider` using `HiltWorkerFactory`, injecting `CatalogSyncScheduler` and calling `schedule()` in `onCreate`.

- [ ] **Step 1: Write the failing tests** (instrumented, `work-testing`):
  - `workerSucceedsWhenTheSyncSucceeds`
  - `workerRetriesWhenTheSyncFailsWithARetryableError`
  - `workerFailsWhenTheSyncFailsWithANonRetryableError`
  - `scheduleEnqueuesOneUniquePeriodicWorkOfSevenDaysThatNeedsAnUnmeteredNetwork`
  - `schedulingTwiceKeepsASingleWork`
- [ ] **Step 2: Run** `.\gradlew.bat :core:data:assembleDebugAndroidTest` -> Expected: FAIL to compile.
- [ ] **Step 3: Implement** the worker (`WgerException.retryable` -> `Result.retry()`, otherwise `Result.failure()`; `CancellationException` propagates), the scheduler (`enqueueUniquePeriodicWork("catalog-sync", KEEP, ...)`, 7 days, `UNMETERED`, exponential backoff, no extra one-time work), the Hilt binding, the Application configuration and the manifest change.
- [ ] **Step 4: Run** `.\gradlew.bat :core:data:connectedDebugAndroidTest assembleDebug` -> Expected: PASS. Then install and launch the app once and confirm with `adb shell dumpsys jobscheduler | grep -i repzi` (or the WorkManager database) that the work is scheduled and the app does not crash.
- [ ] **Step 5: Commit** `Schedule the weekly catalog sync with WorkManager`.

---

### Task 5: Names from translations and the larger list

**Files:**
- Modify: `core/domain/.../model/Exercise.kt` (`val fromCatalog: Boolean = false`), `ExerciseNames.kt` (new resolution), `ExerciseMappers.kt`, `OfflineFirstExerciseRepository.kt`, `FakeExerciseRepository.kt` (if needed), `ExerciseNamesTest.kt`, `ExerciseDaoTest.kt`, `feature/exercises` `ExercisesViewModel` and `ExercisesUiState` (`hasCatalogContent: Boolean`, computed from the group list before the text filter).

**Interfaces:**
- Consumes: Task 2 `ExerciseWithNames`, `TranslatedName`.
- Produces: `internal fun interface ExerciseNames { fun nameOf(exercise: ExerciseWithNames): String }` resolving, in order: the `seed_key` string resource; the translation whose language equals the app language (`resources.configuration.locales[0].language`); the `en` translation; `exercise.name`. `internal fun pickName(names: List<TranslatedName>, language: String, fallback: String): String` as the pure, unit-tested part. `Exercise.fromCatalog` = `source == "wger"`.

- [ ] **Step 1: Write the failing tests:** `pickNameUsesTheAppLanguageFirst`, `pickNameFallsBackToEnglish`, `pickNameFallsBackToTheStoredNameWhenThereAreNoTranslations`, `builtInKeepsItsResourceNameEvenWhenTranslationsExist`, `customExerciseShowsItsTypedName`, mapper `fromCatalogIsTrueOnlyForWgerRows`; ViewModel tests `hasCatalogContentIsTrueWhenAnyExerciseComesFromTheCatalog` and `hasCatalogContentIgnoresTheSearchText`.
- [ ] **Step 2: Run** `.\gradlew.bat :core:data:testDebugUnitTest :feature:exercises:testDebugUnitTest` -> Expected: FAIL.
- [ ] **Step 3: Implement** it; the repository keeps sorting with `Collator`; replace the Task 2 adapter. Check in `RoutineEditorScreen` that the picker (`availableExercises`) still renders through its `LazyColumn` with `key = { it.id }` (it does today) and leave it unchanged.
- [ ] **Step 4: Run** `.\gradlew.bat assembleDebug assembleDebugAndroidTest test lintDebug` -> Expected: PASS, BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `Resolve exercise names from translations`.

---

### Task 6: Detail screen, attribution footer, Settings row, README

**Files:**
- Modify: `core/domain/.../repository/ExerciseRepository.kt` (`fun observeDetail(id: Long): Flow<ExerciseDetail?>`), `OfflineFirstExerciseRepository.kt`, `FakeExerciseRepository.kt`, `feature/exercises` (`ExercisesScreen.kt`, `ExercisesNavigation.kt`, strings `values` and `values-es`), `feature/settings` (`SettingsScreen.kt`, `SettingsNavigation.kt`, strings), `app/.../RepziNavHost.kt`, `README.md`.
- Create: `core/domain/.../model/ExerciseDetail.kt`, `usecase/ObserveExerciseDetailUseCase.kt` (+ test), `feature/exercises/.../ExerciseDetailScreen.kt`, `ExerciseDetailViewModel.kt` (+ unit test), `ExerciseDetailUiState.kt`, tests `ExerciseDetailScreenTest`.

**Interfaces:**
- Consumes: Task 2 `observeDetailTranslations`; Task 5 `Exercise.fromCatalog`, `hasCatalogContent`.
- Produces:
  - `data class ExerciseDetail(val id: Long, val name: String, val muscleGroup: MuscleGroup, val isCustom: Boolean, val description: String?, val author: String?, val licenseName: String?, val licenseUrl: String?, val sourceUrl: String?)`. Description, author and license come from the translation in the app language, else English, else null; `sourceUrl` = `https://wger.de/en/exercise/{remoteId}/view/` for wger rows, null otherwise.
  - Route `exercises/{exerciseId}`; `fun NavGraphBuilder.exercisesScreen(navController: NavController, onOpenProgress: (Long) -> Unit)` registers the list and the detail. `SettingsScreen(..., onOpenExerciseData: () -> Unit, ...)`.
  - Detail screen (stateless `ExerciseDetailScreen(uiState, onBack, onOpenUrl)` plus `ExerciseDetailRoute`): name, muscle group, description via `AnnotatedString.fromHtml`, author, license name as a link to `licenseUrl`, source link, the "Shown as published on wger.de." note. A custom exercise shows no author block; a missing description, author or license shows nothing for that part, not a blank label.

- [ ] **Step 1: Write the failing tests:**
  - `ObserveExerciseDetailUseCaseTest`, repository/DAO test `detailUsesTheAppLanguageThenEnglishAndCustomHasNoAuthor`
  - `ExerciseDetailViewModelTest.loadsTheExerciseFromTheNavigationArgument`
  - `ExerciseDetailScreenTest`: `showsDescriptionAuthorLicenseAndSource`, `customExerciseHasNoAuthorBlock`, `missingLicenseHidesTheLicenseRow`, `licenseLinkOpensItsUrl`, `backButtonCallsBack`
  - `ExercisesScreenTest`: `footerIsShownWhenTheLibraryHasCatalogContent`, `footerIsHiddenOtherwise`, `tappingARowOpensItsDetail`
  - `SettingsScreenTest`: `exerciseDataRowOpensTheSource`
- [ ] **Step 2: Run** `.\gradlew.bat :feature:exercises:assembleDebugAndroidTest :feature:settings:assembleDebugAndroidTest test` -> Expected: FAIL.
- [ ] **Step 3: Implement** the domain parts, the screens, the footer as the last `LazyColumn` item (opens `https://wger.de`), rows made clickable, the Settings row in the Help group ("Exercise data (wger)" / "Datos de ejercicios (wger)"), nav wiring in `RepziNavHost`, and the README: module line for `network`, the stack line (Retrofit, OkHttp, kotlinx-serialization, WorkManager), the roadmap line, and a "Data and licenses" paragraph (wger content is CC-BY-SA 3.0/4.0, CC0 or other open licenses per exercise, shown on each exercise; the share-alike ones stay under their license; the app code license is not affected).
- [ ] **Step 4: Run** `.\gradlew.bat assembleDebug assembleDebugAndroidTest test lintDebug` -> Expected: PASS; then on the emulator `.\gradlew.bat :feature:exercises:connectedDebugAndroidTest :feature:settings:connectedDebugAndroidTest` -> Expected: PASS.
- [ ] **Step 5: Verify with the real API** on the emulator: install, wait for the sync, pull the database (`run-as` plus `sqlite3`) and check ~920 non-cardio rows with `source = 'wger'`, the 15 built-ins linked, then screenshot the list with the footer and a detail screen.
- [ ] **Step 6: Commit** `Add the exercise detail screen and the wger attribution`.

---

## After the last task

Final whole-branch review (fresh reviewer, most capable model), then the user's push flow (one commit at a time, CI checked on the last one). Not committed to the repo: add section 17 to `C:\Users\japl_\claude_memory\preguntas-entrevista-android.md` (Retrofit/kotlinx-serialization, WorkManager periodic work, sync by comparing versions, CC-BY-SA in a mobile app) and update the project memory.
