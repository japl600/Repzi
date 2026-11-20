# wger exercise catalog (level 3, part 1) - design

## Intent

Repzi ships with 15 built-in exercises. Level 3 of the roadmap is a remote catalog that keeps itself up to date. The goal is that the exercise library grows to the ~970 exercises of the wger project, works fully offline, and shows who wrote each exercise and under which license, which wger's CC-BY-SA terms require.

It is a portfolio project, so this part must show: a real network layer, background sync with WorkManager, a Room migration that keeps user data intact, and licensing handled correctly.

This is part 1 of level 3. Part 2 (notifications) and part 3 (Glance widget) get their own design later. Part 1 comes first because they build on the work scheduling and the larger catalog.

## Decisions already made (by the user)

- wger is the data source (not free-exercise-db). Decided 2026-11-18.
- **Sync the whole catalog into Room** (the user's option A, 2026-11-19), so the 15 built-in exercises and the wger ones live in one library.
- **Translations table** (the user's approach 1, 2026-11-19).
- **Full weekly pass, comparing each exercise's `last_update_global` with the stored one** (the user's option b, 2026-11-19), because the API cannot be trusted to order or filter by that field (see Constraints).

## Goals

- All wger exercises that have a Spanish or English name, except Cardio, are available in the exercise list, the routine editor, search and progress, with no change to those screens beyond a longer list.
- The names follow the app language: the language of the app, then English, then nothing (the exercise is not stored if it has neither).
- A background job keeps the catalog current: it runs about once a week on an unmetered network, writes only the exercises whose `last_update_global` changed, and retries with backoff on failure.
- The 15 built-in exercises keep their local ids, so routines, drafts, sessions and history are untouched. They are linked to their wger twin so they also get a description and attribution.
- Attribution: a footer line in the exercise list, and an exercise detail screen with description, author, license (with link), the source (wger.de) and a note that the text is shown as published.
- The app is fully usable offline and on first launch with no network (the 15 built-ins are there from the start).

## Non-goals

- Images and videos (Coil comes later), equipment and secondary muscles, a manual "Sync now" button, and deleting exercises that wger deleted.
- Cardio exercises: the app logs sets, reps and weight, which does not fit them.
- Languages other than Spanish and English (the app has only those two). Portuguese is the first candidate: wger has one `pt` language (id 7, no Brazil/Portugal split, text is mostly Brazilian) with 433 translations (44 % of the catalog); it also needs a `values-pt` for the whole app, so it is a later, separate change. The language list in `:core:network` is one line per language so adding it later does not touch the sync.
- Writing back to wger or any account.
- Exposing sync errors in the UI. A failed sync is silent; the old data stays.

## Anti-goals

- Blocking the UI or app start on the network.
- Losing or renaming anything the user created or logged because of a sync. Sync only inserts exercises and inserts or updates translations. It never deletes exercises, and it never touches custom exercises.
- Rendering wger HTML in a WebView, or trusting it blindly (it is user-contributed).
- A migration that takes the old database and loses the 15 built-ins' links.

## Constraints

- `core:domain` stays Android-free: new interfaces there, implementations in `core:data`.
- The network code is isolated in a new module `:core:network`, so `core:data` does not depend on Retrofit types and the API client can be tested alone with MockWebServer.
- HTTPS only. The only permission added is `INTERNET`. The `User-Agent` is the generic client name "Repzi".
- The wger API has no key and is public. Observed on 2026-11-19: `GET /api/v2/exerciseinfo/` returns 979 exercises; each carries all its translations in every language (the `language` filter does not filter the translations, so the client filters); `ordering=-last_update_global` and filters such as `last_update_global__gt` are silently ignored (checked 2026-11-19: the results stay unordered and the count stays 979), while `ordering=-last_update` works but does not change when only a translation is edited, so no server-side "changed since" is usable; a full pass is about 2.3 MB gzipped (10 pages of 100); author and license come per translation, and the licenses are mixed (in the Spanish and English translations: CC-BY-SA 4: 1,345, CC-BY-SA 3: 239, CC0: 45); 8 categories (Abs, Arms, Back, Calves, Cardio, Chest, Legs, Shoulders); language ids: English 2, Spanish 4 (33 languages in total).
- Category to our `MuscleGroup`: Abs -> CORE, Arms -> ARMS, Back -> BACK, Calves -> LEGS, Chest -> CHEST, Legs -> LEGS, Shoulders -> SHOULDERS, Cardio -> skipped.
- Room schema moves from v6 to v7 with a tested migration. `minSdk` 26.
- README and commits in English; app UI in English with `values-es`.
- Versions of Retrofit, OkHttp, kotlinx-serialization, WorkManager, Hilt-Work and MockWebServer are checked against Maven when the plan is written (latest stable), like with DataStore and AboutLibraries.

## Approaches considered

1. **What to do with the existing 15 exercises (picked by the user: sync everything).** Rejected: importing on demand (the library would be small, but search would need a connection and the sync would have no purpose) and keeping the built-ins apart from wger (the same exercise would appear twice).
2. **How to store it (picked by the user: translations table).** `exercise_translations` holds name, description and author per language, so changing the app language needs no new download. Rejected: a bundled snapshot (heavier repo and goes stale; the built-ins already give an offline first launch) and one translated name column (a language change would need a new download).
3. **How to find what changed (picked by the user: a full weekly pass).** The API cannot order or filter by `last_update_global`, so the client downloads every page and compares each exercise's value with the stored one. Rejected: sorting by `-last_update` and stopping at a watermark (misses translation-only edits) and doing that daily plus an occasional full pass (the most complex for little gain).

## Design

- **Network (`:core:network`).** `WgerApi` (Retrofit, kotlinx-serialization) with one call for a page of `exerciseinfo` (no ordering or filter parameters, since the API ignores them) and one for `license/` (translations carry a license id, so names and links are resolved from that small list), plus DTOs that keep only the fields used. Page size 100. Language ids are mapped to `"en"` and `"es"` here; other languages never leave this module.
- **Database (v7).**
  - `exercises` gains `remote_id INTEGER` (unique, null for custom and unlinked built-ins), `source TEXT NOT NULL` (`"local"` or `"wger"`; existing rows migrate to `"local"`) and `remote_updated_at TEXT` (the exercise's `last_update_global` as last synced).
  - New table `exercise_translations(exercise_id, language, name, description, author, license_name, license_url, PRIMARY KEY(exercise_id, language))` with a foreign key to `exercises` and cascade delete. License fields are per translation, because that is how wger provides them.
  - `ExerciseDao` gains: upsert by `remote_id`, `observe` with translations, `findByRemoteId`.
- **Link of the built-ins.** Each `SeedExercise` gets an optional `wgerId`, resolved once from the API when the plan is written and written into the code (an explicit id is more reliable than matching names, and testable). On the first sync the row with that `seed_key` receives `remote_id` and `source = "wger"`; its name keeps coming from string resources. A built-in with no match stays local.
- **Sync (`core:data`).** `ExerciseCatalogSync` downloads every page, filters out Cardio and exercises with neither language, and for each remaining exercise compares its `last_update_global` with the `remote_updated_at` stored in its row: unchanged ones are skipped, new and changed ones are upserted with their translations in one Room transaction per page. Each page commits on its own and an interrupted run simply repeats the pages not yet committed on the next run (upserts are idempotent), so there is no global watermark to keep.
- **Work (`core:data`).** A Hilt-aware `CoroutineWorker` runs the sync; unique periodic work (about every 7 days, `NetworkType.UNMETERED` because a pass is about 2.3 MB, exponential backoff, `KEEP` policy) is scheduled from the `Application`, plus a one-time run on first launch. IO errors return `retry`; parse errors on a page are logged and return `failure` for that run, never crash.
- **Names.** `ExerciseNames` resolves: `seed_key` string resource, then the translation for the app language, then English, then the stored name (custom exercises). The repository keeps sorting with `Collator` in memory, which is fine for ~1000 rows.
- **Domain.** `Exercise` gains the source and `remoteId`; a new `ExerciseDetail` (name, group, description, author, license name and url, source) and `ObserveExerciseDetailUseCase` follow the use-case pattern the app already has. `ExerciseRepository` gets `observeDetail(id)`; scheduling is behind a domain interface so features never see WorkManager.
- **UI.**
  - Exercise list: rows become tappable and open the detail; a footer line "Exercise data from wger.de, under CC-BY-SA, CC0 and other open licenses (see each exercise)" with the link shows whenever the library holds wger content. The footer names the licenses generically because they are mixed; the exact license of each exercise is on its detail screen and is the one that counts. Because rows are not clickable today, the plan checks how the routine editor's exercise picker uses the same list so a tap there still adds the exercise.
  - Detail screen (stateless screen plus route, like the others): description rendered from HTML with Compose's `AnnotatedString.fromHtml` (no WebView, no scripts), author, license link, source link and the "shown as published" note. Custom exercises show no author block.
  - Settings, Help group: a row "Exercise data (wger)" that opens wger.de (the licenses are listed per exercise, so there is no single license page).
- **README.** A "Data and licenses" paragraph: exercise content comes from wger under CC-BY-SA 3.0, CC-BY-SA 4.0 or CC0 depending on the exercise (each exercise shows its own); the share-alike ones stay under their license in the app, and the app code license is not affected. (My reading of share-alike, not legal advice.)

## Testing

- `WgerApi` against MockWebServer with a trimmed real response saved as a test fixture, including an exercise with no Spanish name and a Cardio one.
- A migration test v6 -> v7 (the existing `MigrationTest`): built-ins and their routines survive.
- DAO tests (instrumented) for upsert, translations and cascade.
- Sync tests with the API faked: first sync, a second run that writes nothing when nothing changed, a changed `last_update_global` (including a translation-only change) updates that exercise only, an interrupted run resumes without duplicates, built-in linking, custom exercises untouched.
- Name resolution unit tests for the fallback order.
- Worker test with `TestListenableWorkerBuilder`.
- Compose tests for the detail screen and the footer. A final check on the emulator with the real API.

## Commits (one CI run each)

1. `:core:network`: API, DTOs, `INTERNET` permission, MockWebServer tests.
2. Room v7: migration, entities, DAO, seed `wgerId`s, tests.
3. Sync logic and the built-in linking, with tests.
4. WorkManager worker and scheduling.
5. Names from translations and the larger list.
6. Detail screen, footer, Settings row and README.

## Left to the builder

Exact DTO fields, Gradle wiring of the new module, the Hilt-Work setup, the list item layout, the detail layout and the exact wording of the attribution lines, within the rules above.

## Calls I made (correct any of them)

- Cardio is skipped; images are out; no manual sync button.
- Explicit `wgerId` per built-in instead of name matching.
- The weekly pass is unmetered-only; the first run waits for Wi-Fi too (the built-ins work meanwhile).
- The footer and README name the licenses generically; each exercise's own license is on its detail screen.
- Portuguese is left for later.
- Exercises with neither Spanish nor English name are not stored.
- Deleted-on-wger exercises are not removed (a known limitation); the full weekly pass could detect them (an exercise missing from the response), but removing rows that routines or history may reference needs its own design.
