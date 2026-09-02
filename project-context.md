# Lexicon Depths — Project Context

> **Read this first.** It maps features to files so you can navigate without searching.
> Design rationale lives in [`project-idea.md`](project-idea.md). This file is the *map*; that file is the *why*.

**Status legend used throughout:** ✅ exists · 🚧 in progress · ⬜ planned, file does not exist yet

Do not assume a 🚧 or ⬜ path is on disk. Check before importing it.

---

## 1. What this is

An Android vocabulary-learning game for intermediate Vietnamese ESL learners. Word puzzles (spelling, cloze, definition-matching, collocation) are reskinned as roguelike dungeon combat, and an SM-2 spaced-repetition scheduler decides which words appear, disguised as difficulty tuning.

The core bet: spaced repetition works but flashcard apps are boring and people quit. The roguelike wrapper supplies motivation without changing the underlying pedagogy.

One rule overrides everything else in the codebase: **dying in a run never rolls back what the player learned.** See §5.

---

## 2. Stack and constraints

| | |
|---|---|
| Language | **Java 17.** Kotlin is not used. Do not add the Kotlin plugin without asking. |
| UI | **XML layouts + ViewBinding.** Jetpack Compose is not available — it cannot be called from Java. |
| Orientation | Portrait-locked. This is why there is no ViewModel layer. |
| Navigation | One `Activity` per screen, `Intent` extras between them. No Fragments, no Navigation Component. |
| DI | **None.** `App.java` holds the database, prefs, and executor as fields. |
| Storage | Room for vocabulary and runs. `SharedPreferences` for settings. |
| Network | `HttpURLConnection` + `org.json`, both in the SDK. See `content/MapApi.java` and `game/question/gen/DatamuseAffixKeySource.java`. |
| Async | `ExecutorService` from `App`, plus Room `LiveData` return types. |

### Binding constraint

**[`.claude/skills/minimal-app-design/SKILL.md`](.claude/skills/minimal-app-design/SKILL.md) governs all Android code. Read it before writing any.**

It bans, explicitly: DI frameworks, repository layers, use-case layers, multiple modules, Clean Architecture, event buses, and adding a library when an SDK API does the job. When it conflicts with conventional Android advice, the skill wins.

### Approved dependencies

`appcompat` · `material` · `constraintlayout` · `room-runtime` · `room-compiler` · `lifecycle-livedata`

Adding anything else needs a written justification in the PR. Reach for the Android SDK first.

---

## 3. Where things live

```
E:\Project\CS426_Final\
├── android\                    ✅  Open THIS folder in Android Studio, not the repo root
│   └── app\src\
│       ├── main\java\com\lexicondepths\
│       │   ├── App.java        ✅  Application. Owns AppDatabase, Prefs, ExecutorService.
│       │   ├── Prefs.java      ✅  SharedPreferences wrapper.
│       │   ├── db\             ✅  Room only: entities, DAOs, Converters, AppDatabase.
│       │   ├── game\           ✅  Pure Java rules engine. NO Android imports. See §7.
│       │   │   ├── srs\        ✅  SM-2 scheduler.
│       │   │   ├── combat\     ✅  Damage + timer bonus math.
│       │   │   ├── run\        ✅  Dungeon generation and run state.
│       │   │   └── question\   ✅  Question models + the 11 shipped generators.
│       │   ├── content\        ✅  Loaders that read assets\ JSON into memory or Room.
│       │   └── ui\             ✅  One package per screen, plus ui\widget\ for shared views.
│       ├── main\assets\        ✅  words_seed.json, monsters.json, relics.json, fallback_map.json
│       ├── main\res\           ✅  layout\ values\ values-vi\ font\ drawable\ xml\
│       ├── debug\res\xml\      ✅  network_security_config.xml — cleartext, DEBUG BUILDS ONLY
│       └── test\java\          ✅  JUnit over game\ and content\. Runs without an emulator.
├── backend\                    ✅  Spring Boot DeepSeek proxy, 4 classes. See backend\README.md.
│                               Key comes from $DEEPSEEK_API_KEY — never a tracked file.
├── docs\                       ✅  Long-form notes that do not belong in code.
├── project-idea.md             ✅  Design doc. Rationale and game design.
├── project-context.md          ✅  This file.
└── .claude\skills\             ✅  Vendored third-party skills. Gitignored. minimal-app-design is binding.
```

### Two corrections to `project-idea.md`

That document predates the current stack. Where they disagree, this file wins:

1. **§10 specifies Kotlin + Jetpack Compose. Dropped.** The stack is Java + XML. Everything §8 describes as "~20 lines of Compose" is instead `ValueAnimator`, `Handler.postDelayed`, and XML drawables.
2. **§11 lists run structure as an open gap blocking the schema. It is now closed** — see §5 below. The schema is unblocked.

---

## 4. Feature → file index

The section that earns this file's existence. Find the feature, get the entry point.

### Learning engine

| Feature | Entry point | Status |
|---|---|---|
| SM-2 scheduling, interval math | `game/srs/Sm2.java` | ✅ P1-7 |
| Self-rating grades (again/hard/good/easy) | `game/srs/ReviewGrade.java` | ✅ P1-7 |
| Due-queue query | `db/dao/WordDao.java` | ✅ P1-4 |
| Seeding the word bank on first launch | `content/SeedLoader.java` | ✅ P1-6 |
| Word bank content itself | `assets/words_seed.json` | ✅ P1-5 |

### Combat and runs

| Feature | Entry point | Status |
|---|---|---|
| Damage formula `base × (1 − ratio) × depth` | `game/combat/Damage.java` | ✅ P2-2 |
| Timer bonuses (bonus only, never penalty) | `game/combat/TimerBonus.java` | ✅ P2-2 |
| Floor/step/branch generation | `game/run/NodeGen.java` | ✅ P2-9 |
| Run state, HP, resume-after-kill | `game/run/RunEngine.java`, `game/run/RunState.java` | ✅ P2-9 |
| Relic effects (8 passives) | `game/combat/Damage.java`, `game/combat/TimerBonus.java`, `game/run/RunEngine.java` + `assets/relics.json` | ✅ P2-9 |
| Monster definitions and ASCII art | `assets/monsters.json`, `content/MonsterCatalog.java` | ✅ P2-8 |
| One-active-run guard | `game/run/RunEngine.java` (`startRun`) | ✅ P3-5 |

### AI map generation

Everything here routes through the proxy. **The DeepSeek key is never in the APK** — see §2 and `backend/README.md`.

| Feature | Entry point | Status |
|---|---|---|
| Generated-map JSON contract (and its validator, twice) | `content/MapJson.java`, `backend/.../MapValidator.java` | ✅ P3-1 |
| DeepSeek proxy — `/health`, `/generate-map` | `backend/src/main/java/com/lexicondepths/proxy/` | ✅ P3-2, P3-3 |
| Client HTTP call | `content/MapApi.java` | ✅ P3-4 |
| Cleartext policy — debug permits, release denies | `res/xml/network_security_config.xml` ×2 | ✅ P3-4 |
| Importing a generated map into Room | `content/RealmImport.java` | ✅ P3-5 |
| Forge UI, retry, offline fallback | `ui/library/LibraryActivity.java`, `assets/fallback_map.json` | ✅ P3-6 |
| Proxy base URL setting | `Prefs.mapApiBaseUrl()`, `ui/settings/SettingsActivity.java` | ✅ P3-9 |
| Affix answer key from Datamuse | `game/question/gen/DatamuseAffixKeySource.java` | ✅ P3-8 |

### Question types

All 11 shipped types implement `game/question/QuestionGenerator.java` and return a `QuestionResult` carrying a **completion ratio 0.0–1.0**. That single number is what lets a Wordle grid and an affix harvest feed the same damage formula.

| Group | File | Status |
|---|---|---|
| Contracts: `Question`, `Answer`, `QuestionResult`, `QuestionType` | `game/question/` | ✅ P2-1 |
| Definition→Word, Word→Definition, Synonym/Antonym | `game/question/gen/` | ✅ P2-3 |
| Word form, Cloze, Collocation | `game/question/gen/` | ✅ P2-4 |
| Anagram, Sentence scramble, Wordle, Affix harvest | `game/question/gen/` | ✅ P2-5 |
| Listening→Spelling (+ TTS wrapper) | `game/question/gen/`, `ui/widget/Speaker.java` | ✅ P2-6 |
| Register/formality (C1+) | — | deferred past Phase 2 |

### Screens

One `Activity` each, all in `ui/`.

| Screen | File | Status |
|---|---|---|
| Character Hub (launcher) | `ui/hub/HubActivity.java` | ✅ P1-9 |
| Settings | `ui/settings/SettingsActivity.java` | ✅ P1-8 |
| Practice / Flashcards | `ui/practice/PracticeActivity.java` | ✅ P1-12 |
| Realm select | `ui/realm/RealmSelectActivity.java` | ✅ P2-10 |
| Dungeon map | `ui/map/DungeonMapActivity.java` | ✅ P2-10 |
| Battle | `ui/battle/BattleActivity.java` | ✅ P2-11 |
| Reward (mid-run relic pick) | `ui/reward/RewardActivity.java` | ✅ P2-12 |
| Spoils (run-end recap) | `ui/reward/SpoilsActivity.java` | ✅ P2-12 |
| Vocabulary stats | `ui/stats/StatsActivity.java` | ⬜ Phase 4 |
| My Library + realm forge | `ui/library/LibraryActivity.java` | ✅ P3-6, P3-7 |

### Presentation

| Feature | Entry point | Status |
|---|---|---|
| Terminal theme, colors, mono font | `res/values/themes.xml`, `res/font/` | ✅ P1-3 |
| Typewriter, scramble, screen-shake, HP bar | `ui/widget/` | ✅ P1-11 |
| CRT scanline overlay | `res/drawable/scanlines.xml` | ✅ P1-11 |
| Question input views (MCQ, free-text, Wordle grid, ordering) | `ui/battle/view/QuestionView.java` + subclasses | ✅ P2-7 |
| Monster rendering (swappable for sprites later) | `ui/widget/MonsterRenderer.java` | ✅ P2-8 |
| English + Vietnamese strings | `res/values/strings.xml`, `res/values-vi/strings.xml` | ✅ P1-10 |
| Runtime locale switch | `AppCompatDelegate.setApplicationLocales` in Settings | ✅ P1-10 |

---

## 5. Room schema at a glance

Nine entities in `db/`. Monsters and relics are deliberately **not** tables — they are static JSON in `assets/`, since they never change at runtime.

| Entity | Purpose |
|---|---|
| `Word` | The vocabulary bank. List fields (synonyms, collocations, forms) are CSV via a `TypeConverter`. |
| `WordProgress` | **The mastery table.** ease, interval, reps, lapses, dueAt. |
| `Realm` | A topic map. Phase 3 adds AI-generated ones. |
| `RealmWord` | Join table. |
| `Run` | One dungeon attempt: hp, floor, step, marks, seed. |
| `RunNode` | One node on the map. `slot` is 0 or 1 (the branch choice). |
| `RunRelic` | Relics held this run. |
| `WordEvent` | Every answer given: ratio, damage, timestamp. Feeds Spoils and stats. |
| `Profile` | Singleton row (id=1): CEFR level, marks, streak, best floor. |

### ⚠️ The permadeath boundary

This is the one invariant that must never break, and it is the thing a grader is most likely to probe.

- **Wiped when a run ends:** `Run`, `RunNode`, `RunRelic` — HP, floor, held relics, run currency, path taken.
- **Never touched by a run ending:** `WordProgress`, `Profile`, `WordEvent`.

Enforced mechanically: the run-clearing DAO method has no reference to `WordProgress` at all, and a unit test asserts it. Losing a battle is a game setback. It is never a loss of what the player learned.

The related mechanic runs the *opposite* direction: on death, every word failed during the run gets `dueAt` reset to now, so it resurfaces immediately. That is the **Spoils** system. A loss makes the next run easier, which is both good roguelike design and correct spaced-repetition practice.

---

## 6. Run structure

Kept small enough to fit a handful of columns.

- **3 floors × 4 steps.** At each step the player picks one of **2 nodes** — a two-column ladder, not a general graph.
- **Node types:** `BATTLE`, `ELITE`, `REST`, `TREASURE`, `BOSS`. Steps 1–3 roll weighted from `{BATTLE ×3, REST, TREASURE}`. Step 4 is `ELITE` on floors 1–2, `BOSS` on floor 3.
- **HP:** 100, carried across floors, no free heal between them. `REST` gives 30 HP or a due-word review.
- **Depth multiplier** on damage: 1.0 / 1.5 / 2.0 by floor.
- **Relics:** 8 passives, each a plain `switch` branch in `Damage.java` or `TimerBonus.java`. No effect engine.
- **Marks** are the permanent currency, spent at the Hub on starting bonuses.

### Damage is inverse to difficulty

Failing an *easy* question hurts more than failing a hard one. This models "you should have known that," and it stops stretch vocabulary from being a death sentence, so players keep attempting words above their level.

| Question vs. player's CEFR level | Base damage |
|---|---|
| Below level | 18–20 |
| At level | 10–12 |
| One band above | 5–6 |
| Two+ bands above | 2–3 |

---

## 7. Conventions

**`game/` imports no Android classes.** Not `Context`, not `Log`, not `TextUtils`. This is the only architectural rule worth enforcing, and it exists so the SRS scheduler, damage math, and run generator are testable with plain JUnit in `app/src/test/` — no emulator, no Robolectric. If a `game/` class needs a string resource or a clock, pass it in.

**Other rules:**

- User-visible text goes in `strings.xml`. Never hardcode it in a layout or an Activity.
- Room access never runs on the main thread. Use `App.executor()` or a `LiveData` return type.
- Comments explain *why*, not *what*. Keep them rare.
- Prefer standard widgets. Write a custom `View` only when a static helper genuinely cannot do the job.
- Every color-coded feedback state pairs with a shape or glyph (✓ / ~ / ✗) for colorblind readability.
- Build check before calling a task done: `cd android && gradlew.bat assembleDebug` then `gradlew.bat test`.

---

## 8. Where the plan lives

This file maps *code*. The plan maps *work*. They are separate on purpose — the plan changes every task, this file changes only when the code's shape does.

| Document | Contents |
|---|---|
| [`docs/plan.md`](docs/plan.md) | **Start here.** Which phase is open, progression, the two-track working agreement. |
| [`docs/phase-1.md`](docs/phase-1.md) | Phase 1 task table, dependency graph, and per-task detail. |
| [`docs/phase-2.md`](docs/phase-2.md) | Phase 2, same shape, now closed. |
| [`docs/phase-3.md`](docs/phase-3.md) | Phase 3, same shape, now closed. |

**Phases 1, 2 and 3 are closed** — 12/12, 12/12, 9/9. See [`docs/plan.md`](docs/plan.md) and the per-phase reports.

Phase 4 (polish) is open but not planned in detail. Writing `docs/phase-4.md` is the next planning step; it is the last phase, so nothing follows it.
