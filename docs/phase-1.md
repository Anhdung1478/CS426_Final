# Phase 1 — Foundation

**Goal:** the app launches, seeds a word bank into Room, runs a working SM-2 scheduler, renders a terminal-styled UI in English and Vietnamese, and lets you review flashcards. No combat yet.

**Milestone:** P1-12 exercises the whole learning engine before a single monster exists. If the SM-2 math is wrong, it surfaces there rather than after the battle system is built on top of it.

**Prerequisites:** read [`.claude/skills/minimal-app-design/SKILL.md`](../.claude/skills/minimal-app-design/SKILL.md) — it is binding — and [`project-context.md`](../project-context.md) §2 and §7.

---

## Task table

| ID | Task | Difficulty | Depends on | Unblocks | Track | Status |
|---|---|---|---|---|---|---|
| P1-1 | Repo scaffolding + `project-context.md` | low | — | P1-2, P1-5 | · | ✅ done |
| P1-2 | Android project + Gradle config | medium | P1-1 | P1-3, P1-4, P1-8 | · | ✅ done |
| P1-3 | Terminal theme + Vietnamese font check | medium | P1-2 | P1-9, P1-11 | B | ✅ done |
| P1-4 | Room schema + permadeath test | high | P1-2 | P1-6, P1-7 | A | ✅ done |
| P1-5 | Seed content JSON | medium | P1-1 | P1-6 | A | ✅ done |
| P1-6 | `SeedLoader` | medium | P1-4, P1-5 | P2-1 | A | ✅ done |
| P1-7 | SM-2 scheduler + tests | high | P1-4 | P1-12 | A | ✅ done |
| P1-8 | `Prefs` + Settings screen | low | P1-2 | P1-10, P2-2 | B | ✅ done |
| P1-9 | Activity shells + Hub screen | medium | P1-3 | P1-10, P1-12 | B | ✅ done |
| P1-10 | en/vi localization | medium | P1-8, P1-9 | — | B | ✅ done |
| P1-11 | Terminal widget kit | medium | P1-3 | P1-12, P2-7 | B | ✅ done |
| P1-12 | Practice / Flashcards screen | medium | P1-7, P1-9, P1-11 | — | · | ✅ done |

**Critical path:** P1-1 → P1-2 → P1-4 → P1-7 → P1-12

**Dependency graph:**

```
P1-1 ──┬── P1-2 ──┬── P1-3 ──┬── P1-9 ──┬── P1-10
       │          │          │          │
       │          │          └── P1-11 ─┤
       │          │                     │
       │          ├── P1-4 ──┬── P1-6   │
       │          │          │          │
       │          │          └── P1-7 ──┤
       │          │                     │
       │          └── P1-8 ─────────────┴── P1-12
       │                                      │
       └── P1-5 ─────────────── P1-6 ─────────┘
```

**Parallel split.** After P1-2 lands, the two tracks never touch the same file:

- **Track A** (data + logic): P1-4 → P1-6 → P1-7. Owns `db/`, `game/srs/`, `content/`, `assets/`.
- **Track B** (UI + resources): P1-3 → P1-9 → P1-11 → P1-10, with P1-8 as filler. Owns `ui/`, `res/`.
- P1-5 is content authoring with no code dependency. Start it any time after P1-1.
- P1-2 and P1-12 are handoff points. One person does them while the other reviews.

---

# Task detail

## P1-1 · Repo scaffolding ✅ done

**Difficulty:** low · **Track:** · · **Depends on:** —

Git repo, folder skeleton, and the navigation doc.

**Delivered:** `.gitignore` (excludes `.claude/skills/`, ~60 MB of vendored clones), `android/` + `backend/` + `docs/`, `project-context.md`.

---

## P1-2 · Android project + Gradle config ✅ done

**Difficulty:** medium · **Track:** · · **Depends on:** P1-1 · **Unblocks:** P1-3, P1-4, P1-8

**Goal:** `gradlew.bat assembleDebug` produces an installable APK that launches to a blank portrait screen.

**Files**

```
android/settings.gradle
android/build.gradle
android/gradle.properties
android/gradle/wrapper/            gradle-wrapper.properties + jar
android/gradlew, gradlew.bat
android/app/build.gradle
android/app/src/main/AndroidManifest.xml
android/app/src/main/java/com/lexicondepths/App.java
android/app/src/main/java/com/lexicondepths/ui/hub/HubActivity.java
android/app/src/main/res/layout/activity_hub.xml
.gitattributes                     (repo root)
```

**Config**

| Setting | Value |
|---|---|
| namespace / applicationId | `com.lexicondepths` |
| compileSdk / targetSdk | 35 |
| minSdk | 24 |
| Java source/target | 17 |
| `buildFeatures.viewBinding` | `true` |
| Orientation | `portrait` on every activity in the manifest |

**Dependencies — these six and nothing else:**

```gradle
implementation 'androidx.appcompat:appcompat:1.7.0'
implementation 'com.google.android.material:material:1.12.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.room:room-runtime:2.6.1'
annotationProcessor 'androidx.room:room-compiler:2.6.1'
implementation 'androidx.lifecycle:lifecycle-livedata:2.8.7'

testImplementation 'junit:junit:4.13.2'
```

`App.java` holds the singletons — this is the entire DI story:

```java
public class App extends Application {
    private static App instance;
    private AppDatabase db;          // wired in P1-4
    private Prefs prefs;             // wired in P1-8
    private ExecutorService io;

    public static App get() { return instance; }
    public ExecutorService io() { return io; }
}
```

Register it with `android:name=".App"` in the manifest.

**`.gitattributes`** — `gradlew` must stay LF or it breaks on macOS/Linux:

```
* text=auto
gradlew text eol=lf
*.bat text eol=crlf
```

**Done when**
- [ ] `gradlew.bat assembleDebug` succeeds
- [ ] `gradlew.bat test` succeeds (no tests yet, but the task must be wired)
- [ ] APK installs and launches to a blank Hub, locked portrait
- [ ] No dependency beyond the six above

---

## P1-3 · Terminal theme + Vietnamese font check ✅ done

**Difficulty:** medium · **Track:** B · **Depends on:** P1-2 · **Unblocks:** P1-9, P1-11

**Do the font check before anything else in this task.** Most retro and terminal fonts have no Vietnamese coverage, and the result is tofu boxes across the entire Vietnamese UI. Locale support is a graded requirement, so this fails loudly and late if skipped.

**Verification string** — render it and look at it before committing to a font:

```
Tiếng Việt — Cửa hàng đồ ăn
ế ộ ữ ẳ ằ ặ ỗ ự ơ ư đ
```

IBM Plex Mono and JetBrains Mono both cover Vietnamese and are open-licensed. If neither renders cleanly, fall back to mono for English game content and the system sans for Vietnamese UI chrome — that split reads as deliberate rather than broken.

**Files**

```
android/app/src/main/res/font/           the chosen .ttf files + font family XML
android/app/src/main/res/values/colors.xml
android/app/src/main/res/values/themes.xml
android/app/src/main/res/values/styles.xml
android/app/src/main/res/values/dimens.xml
```

**Palette** — name by role, not by colour, so a retheme is one file:

`bg` (near-black), `surface`, `fg` (primary text), `fg_dim`, `accent`, `success`, `warn`, `danger`, `hp_full`, `hp_low`.

**Text styles:** `TextAppearance.Lexicon.Title`, `.Body`, `.Mono`, `.Dim`.

Base the app theme on a `NoActionBar` Material3 parent. The terminal look wants a custom header, not the system action bar.

**Done when**
- [ ] The verification string renders with zero tofu boxes on a real device or emulator
- [ ] Every colour used anywhere is a token in `colors.xml`, never a literal hex in a layout
- [ ] The chosen font's license file is committed alongside it

---

## P1-4 · Room schema + permadeath test ✅ done

**Difficulty:** high · **Track:** A · **Depends on:** P1-2 · **Unblocks:** P1-6, P1-7

**Goal:** all nine entities, their DAOs, and the test that protects the project's central invariant.

**This task freezes the DAO signatures both tracks build against.** Once it merges, changing a method signature means telling the other person.

**Files**

```
db/AppDatabase.java
db/Converters.java
db/entity/{Word,WordProgress,Realm,RealmWord,Run,RunNode,RunRelic,WordEvent,Profile}.java
db/dao/{WordDao,WordProgressDao,RealmDao,RunDao,WordEventDao,ProfileDao}.java
app/src/androidTest/java/.../PermadeathBoundaryTest.java
```

**Entities** — full field lists are in [`project-context.md`](../project-context.md) §5.

`Converters` handles three shapes: `List<String>` ↔ CSV (used for synonyms, antonyms, collocations, forms), enum ↔ String, and `long` epoch millis for all dates. No child tables for the list fields — they are never queried by element, so CSV is correct and cheaper.

### ⚠️ The permadeath boundary

`RunDao.clearRunState(long runId)` is `@Transaction` and deletes from `Run`, `RunNode`, and `RunRelic` **only**. It must contain no reference to `WordProgress` in any form.

`PermadeathBoundaryTest` asserts it:

1. Insert a `Word` and a `WordProgress` with a known ease, interval, and due date.
2. Start a run, add nodes and relics, record a `WordEvent`.
3. Call `clearRunState()`.
4. Assert `Run`/`RunNode`/`RunRelic` rows are gone.
5. **Assert the `WordProgress` row is byte-for-byte identical.**

Room needs a `Context`, so this is an instrumented test in `androidTest`, not a JUnit test. Add these as `androidTestImplementation` only — they are test-scoped and never ship in the APK, which is why they do not count against the six-dependency budget:

```gradle
androidTestImplementation 'androidx.test.ext:junit:1.2.1'
androidTestImplementation 'androidx.test:runner:1.6.2'
androidTestImplementation 'androidx.room:room-testing:2.6.1'
```

Set `testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"`.

**Done when**
- [ ] `AppDatabase` version 1, `exportSchema = true`, schema JSON committed
- [ ] `App.java` builds the database once and exposes it
- [ ] `PermadeathBoundaryTest` passes on a device or emulator
- [ ] No DAO method returning a `Cursor` or taking a raw SQL string

---

## P1-5 · Seed content JSON ✅ done

**Difficulty:** medium · **Track:** A · **Depends on:** P1-1 · **Unblocks:** P1-6

**Goal:** the offline content the whole game runs on. No code dependency — this can start on day one, in parallel with everything.

**Files:** `android/app/src/main/assets/{words_seed.json, monsters.json, relics.json}`

### `words_seed.json`

Roughly 300 words across 4 topics (Food, Travel, Business, Emotions) and 4 CEFR bands (A1–B2). Draw from the Oxford 3000/5000 lists, which map to CEFR bands already — a defensible dataset beats hand-guessing difficulty.

```json
{
  "version": 1,
  "words": [
    {
      "headword": "decide",
      "cefr": "A2",
      "topic": "business",
      "pos": "verb",
      "definition": "to choose something after thinking about it",
      "example": "They decided to open a second branch.",
      "viGloss": "quyết định",
      "synonyms": ["choose", "determine"],
      "antonyms": ["hesitate"],
      "collocations": ["make a decision", "decide on", "decide against"],
      "forms": ["decides", "decided", "decision", "decisive"],
      "affixKey": null
    }
  ]
}
```

Every field carries a question type, so gaps disable types:

| Field | Feeds |
|---|---|
| `definition` | Definition→Word, Word→Definition |
| `example` | Cloze (blank the headword out) |
| `synonyms` / `antonyms` | Synonym/Antonym |
| `collocations` | Collocation — **§7 of the design doc calls this the sleeper hit.** Hardest area for Vietnamese learners, rarely gamified. Do not skimp here. |
| `forms` | Word form / morphology |
| `viGloss` | Bilingual scaffolding at A1–A2 |

Coverage rule: `definition` and `example` are mandatory on every word. At least 60% must carry `collocations` and `forms`.

### `monsters.json`

Eight monsters from §4 of the design doc. Question types per monster are **permanent** — same monster, same shape, every encounter, only the words change. That is what lets players read enemies and self-select which skills to drill.

```json
{
  "id": "hydra",
  "name": "Hydra",
  "questionTypes": ["AFFIX_HARVEST"],
  "slots": 3,
  "resists": [],
  "ascii": ["  /\\_/\\_/\\", "  ( o o o )", "   \\__|__/"]
}
```

Hydra→affix harvest, Void-eater→cloze, Mimic→word form, Sphinx→definition→word, Cipher→Wordle, Twins→synonym/antonym, Echo→listening, Chimera→collocation.

### `relics.json`

Eight passives. `effect` is a key that maps to a `switch` branch in Phase 2 — no scripting, no effect engine.

```json
{ "id": "lexicon_shard", "name": "Lexicon Shard",
  "desc": "+10 maximum health.", "effect": "MAX_HP_PLUS_10" }
```

Effects: `MAX_HP_PLUS_10`, `TIMER_PLUS_5S`, `FIRST_MISS_FREE`, `RATIO_FLOOR_20`, `DEPTH_MULT_MINUS_25`, `REST_HEALS_50`, `MARKS_PLUS_25`, `STRETCH_DAMAGE_HALVED`.

**Done when**
- [ ] All three files parse as valid JSON
- [ ] ~300 words, every one with a definition and example
- [ ] Every monster's `questionTypes` values exist in the Phase 2 `QuestionType` enum
- [ ] Vietnamese glosses use correct diacritics (they double as P1-3's font test data)

---

## P1-6 · SeedLoader ✅ done

**Difficulty:** medium · **Track:** A · **Depends on:** P1-4, P1-5

**Goal:** first launch populates Room from `assets/`; later launches skip it.

**Files:** `content/SeedLoader.java`, `content/MonsterCatalog.java`, `content/RelicCatalog.java`

`SeedLoader` reads `words_seed.json` with `org.json` (in the SDK — do not add Gson), inserts in one transaction, and sets a `seed_version` key in `Prefs`. Guard on that version, not a boolean, so a later content update can re-seed.

`MonsterCatalog` and `RelicCatalog` parse into memory and stay there. They are not Room entities — the data never changes at runtime, so a table buys nothing.

Runs on `App.io()`. Never the main thread.

**Done when**
- [ ] First launch inserts ~300 words; second launch inserts zero
- [ ] Seeding does not block the UI thread (no ANR on a cold start)
- [ ] A corrupt or truncated JSON file logs and fails cleanly instead of leaving a half-seeded database

---

## P1-7 · SM-2 scheduler + tests ✅ done

**Difficulty:** high · **Track:** A · **Depends on:** P1-4 · **Unblocks:** P1-12

**Goal:** the learning engine. Pure Java, fully unit-tested, no Android imports.

**Files:** `game/srs/Sm2.java`, `game/srs/ReviewGrade.java`, `app/src/test/java/.../Sm2Test.java`

**Grades:** `AGAIN`, `HARD`, `GOOD`, `EASY` — matching the Anki self-rating in P1-12.

**Algorithm** (simplified SM-2; `ease` starts at 2.5, clamped to `[1.3, 2.7]`):

| Grade | Interval | Ease |
|---|---|---|
| `AGAIN` | reset to 0 (due now), `lapses++`, `reps = 0` | −0.20 |
| `HARD` | `max(1, interval × 1.2)` | −0.15 |
| `GOOD` | `reps == 0` → 1 day · `reps == 1` → 6 days · else `interval × ease` | unchanged |
| `EASY` | as `GOOD`, then `× 1.3` | +0.15 |

`dueAt = now + intervalDays`. Pass `now` in as a parameter rather than calling `System.currentTimeMillis()` inside — that is what makes the tests deterministic.

Signature stays pure:

```java
public static WordProgress apply(WordProgress current, ReviewGrade grade, long nowMillis);
```

`WordDao` gains the queue queries: due words ordered by `dueAt`, new words filtered by CEFR and topic, and a combined queue mixing both.

**Done when**
- [ ] `Sm2Test` covers: the 1 → 6 → interval×ease curve, ease floor at 1.3 and ceiling at 2.7, `AGAIN` resetting reps while incrementing lapses, and a full 10-review sequence
- [ ] Zero Android imports in `game/srs/` — tests run with `gradlew.bat test`, no emulator
- [ ] `apply()` returns a new object rather than mutating its argument

---

## P1-8 · Prefs + Settings screen ✅ done

**Difficulty:** low · **Track:** B · **Depends on:** P1-2 · **Unblocks:** P1-10, P2-2

**Files:** `Prefs.java`, `ui/settings/SettingsActivity.java`, `res/layout/activity_settings.xml`

| Key | Default | Used by |
|---|---|---|
| `cefr_level` | `B1` | word selection, damage bands |
| `ui_locale` | system | P1-10 |
| `timer_full_bonus_ms` | 10000 | P2-2 |
| `timer_partial_bonus_ms` | 20000 | P2-2 |
| `seed_version` | 0 | P1-6 |

Settings exposes CEFR as a picker, locale as a picker, and both timer thresholds as sliders. Typed getters only — no raw `getString` calls leaking out of `Prefs`.

**Done when**
- [x] Values survive a force-quit
- [x] Timer sliders are bounded so partial can never be less than full
- [x] Every label comes from `strings.xml`

---

## P1-9 · Activity shells + Hub screen ✅ done

**Difficulty:** medium · **Track:** B · **Depends on:** P1-3 · **Unblocks:** P1-10, P1-12

**Goal:** all eight screens exist and are reachable. Only the Hub is built for real.

Create the eight Activities listed in [`project-context.md`](../project-context.md) §4, register each in the manifest as portrait, and wire navigation with plain `Intent`s. Screens not yet implemented show a placeholder — but they must launch and back out cleanly.

The Hub is the launcher and gets real content: CEFR level, current streak, words due today, Marks balance, and entry buttons to Realm select, Practice, Library, Stats, and Settings.

**Done when**
- [x] All eight launch and the back stack behaves
- [x] The Hub reads live data from Room, not hardcoded numbers
- [x] Rotation is impossible (portrait lock holds)

---

## P1-10 · Localization ✅ done

**Difficulty:** medium · **Track:** B · **Depends on:** P1-8, P1-9

**Files:** `res/values/strings.xml`, `res/values-vi/strings.xml`

Two settings that must not be conflated:

- **UI locale** — menus, buttons, labels. Ships `en` + `vi`.
- **Target language** — English, fixed. That is the subject being taught.

Switch at runtime with `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("vi"))`. It persists across restarts on its own; do not hand-roll a `Configuration` override.

**Done when**
- [x] Zero hardcoded user-visible strings anywhere in `ui/`
- [x] Both files have identical key sets (a missing `vi` key silently falls back to English)
- [x] Switching locale updates every screen, including ones already on the back stack
- [x] No tofu boxes on any Vietnamese screen

---

## P1-11 · Terminal widget kit ✅ done

**Difficulty:** medium · **Track:** B · **Depends on:** P1-3 · **Unblocks:** P1-12, P2-7

**Goal:** the five signature animations from §8 of the design doc. These are what separate a deliberate terminal aesthetic from a lazy one.

**Prefer static helpers over custom `View` subclasses.** The `minimal-app-design` skill is explicit about this. Write a custom `View` only where a helper genuinely cannot do the job.

**Files:** `ui/widget/Typewriter.java`, `Scramble.java`, `Shake.java`, `HpBar.java`, `res/drawable/scanlines.xml`

| Effect | Implementation |
|---|---|
| Typewriter reveal | `Handler.postDelayed` appending one char at a time to a `TextView`. Static helper. |
| Scramble-then-resolve | Same loop, random glyphs settling to the target string. Static helper. |
| Screen shake on damage | `ObjectAnimator` on the root view's `translationX`. Static helper. |
| CRT scanlines | A repeating XML drawable over a `View`. No code at all. |
| HP bar | The only justified custom `View` — needs `onDraw` for the segmented terminal look. |

Every animation must be cancellable and must stop in `onPause`. A `Handler` still posting into a dead Activity leaks it.

**Done when**
- [x] All five run at 60fps on a mid-range device
- [x] Nothing keeps running after `onPause`
- [x] No `Activity` reference is held by a long-lived object

---

## P1-12 · Practice / Flashcards screen ✅ done

**Difficulty:** medium · **Track:** · · **Depends on:** P1-7, P1-9, P1-11

**This is the Phase 1 milestone.** It proves the whole learning engine end to end with no combat in the way.

**Files:** `ui/practice/PracticeActivity.java`, `res/layout/activity_practice.xml`, `ui/widget/Speaker.java`

Deliberately separate from the roguelike loop: no HP, no permadeath, no timer. Low stakes.

- Pulls the due-queue from `WordDao`
- Front: the word. Back: definition, example sentence, Vietnamese gloss at A1–A2, and a TTS pronunciation button
- Four self-rating buttons mapping straight to `ReviewGrade`
- Writes back through `Sm2.apply()` — **the same tables battles will use**

`Speaker.java` wraps Android `TextToSpeech`: initialise once, handle `onInit` failure and missing voice data by hiding the button rather than crashing. Phase 2 reuses it for listening questions.

Worth pointing out in the report: two very different UX modes, one shared learning-science engine.

**Done when**
- [x] Reviewing five cards with different ratings produces four different next-due dates
- [x] Force-quit and reopen — the queue reflects those ratings (proves the Room write landed)
- [x] Works fully in airplane mode except TTS
- [x] An empty queue shows a real "nothing due" state, not a blank screen or a crash

---

## Phase 1 exit checklist

Run all of this before opening Phase 2.

1. [x] `gradlew.bat assembleDebug` and `gradlew.bat test` both pass
2. [x] `PermadeathBoundaryTest` passes on a device
3. [x] Fresh install seeds ~300 words; second launch does not re-seed
4. [x] Settings → Vietnamese. Every string translates, **zero tofu boxes**
5. [x] Practice: review five cards, force-quit, reopen, confirm the queue changed
6. [x] Airplane mode: everything works except TTS
7. [x] No dependency added beyond the approved six plus the three `androidTest`-scoped ones
8. [x] `git grep -n "android\." -- "*/game/*"` returns nothing

**Phase 1 is closed — 12/12.** See [`report-phase1.md`](../report-phase1.md) for the full write-up.
