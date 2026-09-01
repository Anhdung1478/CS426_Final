# Phase 1 Progress Report — P1-2 through P1-12 (complete)

**Date:** 2026-09-01
**Scope:** all twelve tasks in `docs/phase-1.md` (P1-1 was already done). Session 1 below covers the critical path (P1-2 → P1-4 → P1-7) plus Track A and the font/theme half of Track B. Session 2 covers the remaining five tasks — the rest of Track B plus the P1-12 milestone — closing out the phase.

All twelve tasks are implemented, built, and verified against their "Done when" checklists in `docs/phase-1.md`. `docs/plan.md` and `docs/phase-1.md` now reflect **12/12 — Phase 1 closed**, and Phase 2 is open.

---

# Session 1 — P1-2 through P1-7

---

## Environment note

The Android SDK (platforms `android-35`/`android-36.1`, build-tools, the `Pixel_10` emulator) was already installed on this machine, so verification below is against a real build and a real running emulator, not just a read of the source.

- Gradle wrapper: generated from the locally cached Gradle 8.9 distribution (`android/gradlew`, `android/gradlew.bat`, `android/gradle/wrapper/`).
- AGP: 8.5.2 (warns that it's untested past `compileSdk 34`, but compiles and links cleanly against `compileSdk 35`).
- `android/local.properties` (gitignored) points `sdk.dir` at the existing SDK.

---

## P1-2 — Android project + Gradle config

- `settings.gradle`, root `build.gradle`, `gradle.properties`, full Gradle wrapper.
- `app/build.gradle`: `com.lexicondepths`, compileSdk/targetSdk 35, minSdk 24, Java 17, `viewBinding` on, portrait-only. Dependencies are exactly the six approved ones plus the three `androidTest`-scoped ones from P1-4.
- `AndroidManifest.xml`, `App.java` (owns `AppDatabase` and the `ExecutorService`), `ui/hub/HubActivity.java` + `activity_hub.xml`.
- `.gitattributes` at repo root (LF for `gradlew`, CRLF for `.bat`).

**Verified:** `gradlew.bat assembleDebug` and `gradlew.bat test` both succeed. APK installs and launches on the `Pixel_10` emulator to a locked-portrait Hub screen (screenshot-verified below).

## P1-3 — Terminal theme + Vietnamese font check

- Font: **JetBrains Mono** (Regular/Italic/Bold), pulled from the official GitHub release, confirmed Vietnamese-covered. License (`OFL.txt`) committed at `android/licenses/JetBrainsMono-OFL.txt`.
- `res/font/mono_font_family.xml`, `res/values/{colors,styles,themes,dimens}.xml`. Every color is a role-named token (`bg`, `surface`, `fg`, `fg_dim`, `accent`, `success`, `warn`, `danger`, `hp_full`, `hp_low`) — no literal hex anywhere in a layout.
- `Theme.Lexicon` is based on `Theme.Material3.Dark.NoActionBar` — committed to the dark terminal look regardless of system light/dark setting, per the design doc's "fully committed, not a fallback" call.

**Verified:** installed the APK on the emulator and screenshotted the Hub. The verification string renders with **zero tofu boxes**:

```
Tiếng Việt — Cửa hàng đồ ăn
ế ộ ữ ẳ ằ ặ ỗ ự ơ ư đ
```

## P1-4 — Room schema + permadeath test

All nine entities (`Word`, `WordProgress`, `Realm`, `RealmWord`, `Run`, `RunNode`, `RunRelic`, `WordEvent`, `Profile`), six DAOs (`WordDao`, `WordProgressDao`, `RealmDao`, `RunDao`, `WordEventDao`, `ProfileDao`), `Converters` (CSV list converter + three enum converters), and `AppDatabase` (version 1, `exportSchema = true`, schema JSON committed at `android/app/schemas/com.lexicondepths.db.AppDatabase/1.json`).

`RunDao.clearRunState(long runId)` is `@Transaction`, deletes only from `Run`, `RunNode`, `RunRelic`, and contains no reference to `WordProgress`.

**Verified:** `PermadeathBoundaryTest` (instrumented, `androidTest`) passes on the `Pixel_10` emulator — inserts a `Word` + `WordProgress`, starts a run with nodes/relics/a `WordEvent`, calls `clearRunState()`, and asserts the run-scoped rows are gone while `WordProgress` is byte-for-byte identical (`ease`, `interval`, `reps`, `lapses`, `dueAt` all unchanged).

No DAO method returns a `Cursor` or takes a raw SQL string.

## P1-5 — Seed content JSON

- `assets/words_seed.json`: **300 words**, exactly 75 per topic (food/travel/business/emotions), balanced across CEFR (A1 76 / A2 76 / B1 76 / B2 72). Every word has `definition` and `example`. Collocation coverage 99.7%, forms coverage 86.7% (both comfortably over the 60% guidance). Vietnamese glosses use correct diacritics — this doubled as the font-check data in P1-3.
- `assets/monsters.json`: all 8 monsters from the design doc (Hydra/Void-eater/Mimic/Sphinx/Cipher/Twins/Echo/Chimera), each with its permanent `questionTypes` matching the Phase 2 `QuestionType` enum names exactly, plus ASCII art.
- `assets/relics.json`: all 8 relics with the exact `effect` keys from the design doc (`MAX_HP_PLUS_10` … `STRETCH_DAMAGE_HALVED`).

**Verified:** all three files parse as valid JSON (checked with a script and again implicitly by `SeedLoader`/`MonsterCatalog`/`RelicCatalog` successfully loading them on-device).

**Note on process:** the word-bank content was first handed to a background sub-agent; it self-reported writing `words_seed.json` plus a duplicate `SeedLoader`/`MonsterCatalog`/`RelicCatalog`, but on inspection it had not actually written anything to disk. I verified this (no file present) before trusting it, discarded that run, and authored the 300-word file directly instead — the version described above is mine, freshly validated.

## P1-6 — SeedLoader

- `content/SeedLoader.java`: guards on the JSON's `version` field against a `seed_version` key in `SharedPreferences` (not a boolean), reads `words_seed.json` via `org.json` off the main thread, inserts in one `db.runInTransaction`, and only then persists the new version. A parse failure logs and returns before any transaction starts, so a corrupt file can't leave a half-seeded database.
- `content/MonsterCatalog.java` / `content/RelicCatalog.java` (+`Monster.java`/`Relic.java`): parse once into memory, cached, never touch Room.
- Wired into `App.onCreate()` via `io.execute(() -> SeedLoader.run(this, db))`.

**Verified on-device:**
- Fresh install → pulled the Room DB (`lexicon.db` + WAL) → `SELECT COUNT(*) FROM Word` = **300**, all 4 topics present.
- Force-stopped and relaunched the app → word count still **300** (no re-seed, no duplication).
- No ANR/jank on cold start (seeding runs entirely on the executor thread).

## P1-7 — SM-2 scheduler + tests

- `game/srs/ReviewGrade.java` (`AGAIN`/`HARD`/`GOOD`/`EASY`), `game/srs/Sm2.java` — pure Java, `apply(WordProgress current, ReviewGrade grade, long nowMillis)` returns a new `WordProgress` rather than mutating the argument.
- Ease clamped to `[1.3, 2.7]`. Interval curve: `AGAIN` → reset to 0/reps 0/lapses+1; `HARD` → `max(1, interval×1.2)`; `GOOD` → 1 → 6 → `interval×ease`; `EASY` → same as `GOOD` then `×1.3`.
- `Sm2Test.java`: covers the 1→6→interval×ease curve, the ease floor and ceiling, `AGAIN` resetting reps while incrementing lapses, that `apply()` doesn't mutate its argument, and a hand-verified 10-review mixed-grade sequence.

**Verified:** `gradlew.bat test` passes all `Sm2Test` cases. `git grep -n "android\." -- "*/game/*"` returns nothing — zero Android imports in `game/`.

---

## Full verification run (session 1)

```
gradlew.bat test               → BUILD SUCCESSFUL   (Sm2Test, all cases)
gradlew.bat assembleDebug      → BUILD SUCCESSFUL   (app-debug.apk)
gradlew.bat connectedDebugAndroidTest → BUILD SUCCESSFUL (PermadeathBoundaryTest, 1/1 pass)
```

Plus manual, on-device checks: Hub screen screenshot (theme + Vietnamese font), seeded word count via a pulled copy of the Room database, and a force-stop/relaunch re-seed check.

## Known deviations / judgment calls (session 1)

- **`seed_version`** is stored directly via `SharedPreferences` inside `SeedLoader` (key `"seed_version"`) rather than through a `Prefs` wrapper, since `Prefs.java` is P1-8's deliverable and wasn't in scope yet. P1-8 should read/write the same underlying key so the two don't fight. *(Resolved in session 2 — see below.)*
- **AGP 8.5.2** prints a warning that it's untested above `compileSdk 34` but builds cleanly against 35; left as-is rather than bumping versions without a concrete reason, per the `minimal-app-design` skill's Gradle-discipline rule.
- The emulator (`Pixel_10`, AVD) launched for verification in this session was left running in case it's useful for continued work; it can be closed from Android Studio's Device Manager or `adb emu kill` at any time.

---

# Session 2 — P1-8 through P1-12 (Phase 1 closes)

**Date:** 2026-09-01
**Scope:** the remaining five tasks in `docs/phase-1.md` — the rest of Track B (`P1-8`, `P1-9`, `P1-10`, `P1-11`) plus the cross-track P1-12 milestone. All five are implemented, built, and verified on the same `Pixel_10` emulator left running from session 1.

## P1-8 · `Prefs` + Settings screen

- `Prefs.java`: typed getters/setters over one `SharedPreferences` file (`"lexicon_prefs"`) — the same file and key (`seed_version`) `SeedLoader` already wrote to, so the two don't fight, closing the deviation noted in session 1.
- Keys: `cefr_level` (default `B1`), `ui_locale` (`""` = system, else a BCP-47 tag), `timer_full_bonus_ms` (10000), `timer_partial_bonus_ms` (20000). All reads/writes are typed — no raw `getString`/`getInt` leaks past `Prefs`.
- `setTimerBonuses(full, partial)` clamps `partial = max(full, partial)` server-side in `Prefs` itself, not just in the UI, so the invariant holds regardless of caller.
- `ui/settings/SettingsActivity.java` + `activity_settings.xml`: a CEFR `Spinner` (A1–C2), a UI-language `Spinner` (System/English/Tiếng Việt), and two bounded `SeekBar`s for the timer windows. Changing CEFR also mirrors into the `Profile` row (see P1-9) so the Hub's live display and the Settings picker never disagree.
- `App.java` now owns a `Prefs` field alongside `AppDatabase`, matching the singleton pattern from `project-context.md` §2.

**Verified on-device:** set CEFR to B2 → confirmed the `Profile` row updated (`SELECT * FROM Profile` via a pulled DB showed `cefrLevel='B2'`) → force-stopped and relaunched → Settings still showed B2 and Hub still showed B2. Dragging the full-bonus slider above the partial-bonus slider pulls partial up with it (verified in code and by inspection); the reverse drag is rejected at the touch-input level in `onProgressChanged`.

## P1-9 · Activity shells + Hub screen

- Real Hub: `ui/hub/HubActivity.java` observes `ProfileDao.getProfile()` and `WordDao.getDueWords(now)` as `LiveData` directly (no ViewModel, per the no-ViewModel architecture rule in `project-context.md` §2) and renders CEFR level, streak, due-word count, and Marks live from Room.
- Eight new/updated activities, all portrait-locked and registered in the manifest: `SettingsActivity`, `PracticeActivity` (real), and six placeholder shells — `RealmSelectActivity`, `DungeonMapActivity`, `BattleActivity`, `RewardActivity`, `StatsActivity`, `LibraryActivity` — sharing one `activity_placeholder.xml` layout, each only setting its own title string. All are `exported="false"`; only `HubActivity` is launchable from outside the app.
- `App.java` gained `ensureProfile(db)`, called once after `SeedLoader.run()` on the executor thread: inserts a default `Profile` row (id=1, CEFR B1) if none exists yet, so the Hub always has a row to observe.

**Verified on-device:** fresh install → Hub shows `CEFR level: B1 / Streak: 0 / Words due: 0 / Marks: 0` and all five nav buttons (Practice, Realm Select, My Library, Stats, Settings). Opening a placeholder (Realm Select) and pressing the hardware back button returns focus to `HubActivity` (confirmed via `dumpsys window`). Reviewing cards in Practice changes the Hub's due-count and (for an `AGAIN` grade) streak-independent due-today figure on the next visit, proving the binding is live, not a one-time read.

## P1-10 · en/vi localization

- `res/values/strings.xml` and `res/values-vi/strings.xml`: every user-visible string introduced by P1-8/P1-9/P1-11/P1-12 has an entry in both files with an identical key set (28 keys). The stale `hub_placeholder` / `font_check_vi` strings from the P1-2/P1-3 placeholder Hub were deleted along with the placeholder layout they served.
- Locale switching goes through `AppCompatDelegate.setApplicationLocales()` in `SettingsActivity` — no manual `Configuration` override, no new dependency (the per-app-language APIs ship in `appcompat` 1.6.0+, already an approved dependency at 1.7.0).

**Verified on-device:** switched UI language to Tiếng Việt from Settings — the Settings screen itself recreated instantly in Vietnamese (`Cài đặt`, `Trình độ CEFR`, `Khung thưởng đầy đủ: 10000 ms`, …), and backing out to the already-existing `HubActivity` on the back stack showed it *also* recreated in Vietnamese (`Chuỗi ngày`, `Từ cần ôn`, `Điểm thưởng`, …) without being relaunched — confirming AppCompatDelegate recreates the whole back stack, not just the foreground activity. Opened the Realm Select placeholder in Vietnamese and confirmed its string (`Đang xây dựng — sẽ có trong giai đoạn sau.`) renders with zero tofu boxes, on the same JetBrains Mono font verified for coverage in P1-3.

## P1-11 · Terminal widget kit

- `ui/widget/Typewriter.java` and `Scramble.java`: static helpers, no custom `View`. Both use the target `TextView`'s own tag slot to hold a dedicated `Handler`, and `cancel()` calls `removeCallbacksAndMessages(null)` on it — one `Handler` per active effect, nothing shared, nothing leaked.
- `ui/widget/Shake.java`: static helper wrapping `ObjectAnimator` on `translationX`, same tag-based cancel pattern.
- `ui/widget/HpBar.java`: the one justified custom `View` — segmented terminal-style bar via `onDraw`, color swaps from `hp_full` to `hp_low` at a 30% threshold. Not wired into a screen yet (no HP exists before Phase 2's combat), but compiles and is ready for `BattleActivity`.
- `res/drawable/scanlines.xml` + `res/drawable-nodpi/scanline_tile.png`: a hand-built 1×4px RGBA PNG (72 bytes, generated with Python's stdlib `zlib`/`struct` — no image tool needed) tiled via `android:tileMode="repeat"`. Pure XML/resource, no code, per the plan's "no code at all" call.
- Wired `Typewriter` into `HubActivity`'s title (`onResume`/`onPause` start/cancel) and the scanline overlay into `activity_hub.xml` as a low-alpha `View` over the content, so both effects are demonstrably running rather than just present-but-unused.

**Bug caught and fixed during verification:** the first version started the typewriter effect once in `onCreate`. On the emulator, the screen locked milliseconds after cold start (before the first character posted), which fired `onPause` → `Typewriter.cancel()` and left the title permanently blank on every later resume — a real "Handler still posting into a paused Activity" class of bug, just inverted (cancelled too early instead of leaking). Fixed by moving `start()` into `onResume()` so it replays every time the screen becomes visible, confirmed by screenshot after force-stop/relaunch.

## P1-12 · Practice / Flashcards screen (the Phase 1 milestone)

- `ui/practice/PracticeActivity.java` + `activity_practice.xml`: pulls up to 20 cards via `WordDao.getQueue(now, cefrLevel, 20)` once per session (not `LiveData` — this screen is a static session queue, matching "no HP, no timer" simplicity). Front shows the headword; "Show answer" reveals definition, example, a Vietnamese gloss (only for A1/A2 words, per spec), and a Pronounce button; four rating buttons map straight to `ReviewGrade`.
- Rating a card runs `Sm2.apply(current-or-fresh WordProgress, grade, now)` on `App.io()` and upserts the result — the exact same `WordProgress` table and `Sm2` class Phase 2's battles will write through.
- `ui/widget/Speaker.java`: wraps `TextToSpeech`. `isReady()` is checked before showing the Pronounce button, so an `onInit` failure or missing voice data hides the button instead of crashing — never calls `speak()` unless ready. `shutdown()` is called from `PracticeActivity.onDestroy()`.
- An empty or exhausted queue shows a real message (`practice_empty` / `practice_done`) instead of a blank screen.

**Verified on-device (fresh install, 300-word seed, `WordProgress` table empty):**

1. Reviewed 5 distinct cards with 5 different grades (`AGAIN`, `HARD`, `GOOD`, `EASY`, `GOOD`) via precise `uiautomator dump`-measured taps (screen-coordinate guessing from screenshots proved unreliable once answer text length shifted the rating row's Y position card-to-card — worth flagging for anyone else automating UI taps on this screen). Pulled the live Room DB afterward:

   ```
   ingredient  ease=2.30  interval=0  reps=0  lapses=1  dueAt=<now>        (AGAIN)
   nutrition   ease=2.35  interval=1  reps=1  lapses=0  dueAt=<+1 day>     (HARD)
   appetite    ease=2.50  interval=1  reps=1  lapses=0  dueAt=<+1 day>     (GOOD)
   cuisine     ease=2.65  interval=1  reps=1  lapses=0  dueAt=<+1 day>     (EASY)
   portion     ease=2.50  interval=1  reps=1  lapses=0  dueAt=<+1 day>     (GOOD)
   ```

   All five `dueAt` values are distinct, and the ease deltas (−0.20 / −0.15 / 0 / +0.15) match `Sm2`'s documented curve exactly.
2. Force-stopped the app, reopened Hub → **Words due: 1** (only `ingredient`, correctly resurfaced by its `AGAIN` grade; the other four are due tomorrow and correctly absent). Reopened Practice → the queue's first card was `ingredient` again, proving the write landed in Room and the queue re-reads it rather than caching.
3. The Pronounce button appeared after `TextToSpeech` finished initializing, confirming `Speaker.isReady()` gates it correctly rather than assuming success.
4. No network code exists anywhere in the app (confirmed by inspection — `HttpURLConnection` isn't used until Phase 3's backend proxy), so "works in airplane mode except TTS" holds by construction.

## Full verification run (session 2)

```
gradlew.bat assembleDebug             → BUILD SUCCESSFUL  (app-debug.apk, all 12 tasks' code)
gradlew.bat test                      → BUILD SUCCESSFUL  (Sm2Test, all cases, unaffected)
gradlew.bat connectedDebugAndroidTest → BUILD SUCCESSFUL  (PermadeathBoundaryTest, 1/1 pass, unaffected)
```

Plus the on-device checks described per task above: Hub live data, Settings persistence and CEFR↔Profile mirroring, instant Vietnamese re-localization across the back stack, the widget kit's typewriter/scanline effects, and a full 5-card Practice session with a force-quit/reopen check.

## Known deviations / judgment calls (session 2)

- **CEFR lives in two places** (`Prefs.cefr_level` and `Profile.cefrLevel`) because `project-context.md`'s Prefs-key table and its Room-schema table both claim it, for different consumers (word selection/damage bands vs. the Hub's live display). Rather than pick one and leave the other doc wrong, `SettingsActivity` writes through to both on every change, so they can't drift. Worth collapsing to one source of truth if Phase 2 finds it awkward.
- **`HpBar`** exists and compiles but isn't wired into any Phase 1 screen — there's no HP concept before Phase 2's combat. This matches the task list (`ui/widget/` is P1-11's deliverable; wiring it into `BattleActivity` is P2-11's job).
- **Practice's queue is a one-shot list, not `LiveData`.** The screen has no timer and no combat, so re-querying Room reactively mid-session would add complexity for no player-visible benefit; the queue is re-pulled fresh every time the Activity is created, which is what the force-quit/reopen check exercises.
- The emulator (`Pixel_10`) was reinstalled mid-session after the app's data was cleared by an unrelated emulator hiccup (the installed package briefly disappeared — `adb shell pm list packages` came back empty and `am start` reported "Activity class does not exist"). This turned out to be useful: it forced a second, independent verification of the fresh-install seeding path (300 words, zero `WordProgress` rows) reported above.

---

## Phase 1 — closed

All 8 items on the exit checklist in `docs/phase-1.md` pass. `docs/plan.md` marks Phase 1 ✅ closed and Phase 2 🟢 open. Next up per `docs/plan.md`: `docs/phase-2.md`'s task table, starting with P2-1 (the question API — the second of the two frozen cross-track contracts).
