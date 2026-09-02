# Phase 4 — Polish

**Goal:** close every loop the first three phases left open. The vocabulary the player has been building becomes visible and legible on a stats screen, Marks become spendable, a daily reminder brings them back, a cold demo explains itself, the twelfth question type ships, and the schema stops being able to destroy the one thing §5 says it must never destroy.

**Phase 4 is the last phase.** Nothing follows it, so its "deferred" list is a *won't build* list, not a promise. Everything not in the task table below is out of scope permanently, and the phase ends with a report rather than a handoff.

**Milestone:** P4-2. The app has been recording every answer since P2-11 and reading none of it back. The stats screen is the first time a player can see what they have actually learned, which is the claim the whole project rests on.

---

## The two findings that shaped this phase

Both came out of reading the code rather than the design doc, and both change what a task can be.

### 1. The seed has no C1 or C2 words

`words_seed.json` is **76 A1 / 76 A2 / 76 B1 / 72 B2**, four topics, zero C1, zero C2.

`project-idea.md` §5 gates register/formality (question type 12) at C1+. With this word bank that type is unreachable — it could only ever fire on an AI-forged realm where the player happened to pick C1. A question type that cannot appear in a demo is not shipped, so **P4-10 gates it at B2+ instead.** The deviation from the design doc is deliberate and is recorded here rather than left as a silent inconsistency.

### 2. `fallbackToDestructiveMigration` is a permadeath-boundary bug waiting for a release

`App.java:29`:

```java
// No shipped installs to migrate yet — destructive fallback is the right size for a dev-phase schema bump.
db = Room.databaseBuilder(this, AppDatabase.class, "lexicon.db")
        .fallbackToDestructiveMigration()
        .build();
```

The comment was true when it was written and stops being true at submission. A destructive fallback drops every table — including `WordProgress`, which §5 of `project-context.md` names as the one thing a run ending must never touch. Upgrading the APK would do what losing a run is forbidden to do.

Phase 4 adds columns anyway (P4-8, P4-10), so the version number is getting bumped regardless. **P4-11 replaces the fallback with a real `Migration` and tests that `WordProgress` survives it.** It is the cheapest task in the phase that protects the most important invariant in the project.

---

## Task table

| ID | Task | Difficulty | Depends on | Unblocks | Track | Status |
|---|---|---|---|---|---|---|
| P4-1 | Stats snapshot contract + aggregate queries | high | P1-4, P2-1 | P4-2, P4-3, P4-5 | · | ⬜ |
| P4-2 | Vocabulary stats screen | medium | P4-1 | P4-4 | B | ⬜ |
| P4-3 | Achievement definitions | medium | P4-1 | P4-4 | A | ⬜ |
| P4-4 | Achievements on the stats screen | low | P4-2, P4-3 | — | B | ⬜ |
| P4-5 | Review reminder — channel, alarm, boot re-arm | medium | P4-1 | P4-6 | A | ⬜ |
| P4-6 | `POST_NOTIFICATIONS` runtime flow | medium | P4-5 | — | B | ⬜ |
| P4-7 | Onboarding carousel | medium | — | — | B | ⬜ |
| P4-8 | Marks purchase — persistence and consumption | medium | P2-9 | P4-9, P4-11 | A | ⬜ |
| P4-9 | Relic shop screen | medium | P4-8 | — | B | ⬜ |
| P4-10 | Register/formality question type | high | P2-1, P3-1 | P4-11 | A | ⬜ |
| P4-11 | Real Room migration, destructive fallback removed | medium | P4-8, P4-10 | P4-13 | A | ⬜ |
| P4-12 | Colorblind and accessibility audit | low | — | — | B | ⬜ |
| P4-13 | Edge-case hardening + `RealmImport` test | medium | P4-11 | P4-14 | · | ⬜ |
| P4-14 | Phase 4 report and test plan | low | everything | — | · | ⬜ |

**Critical path:** P4-10 → P4-11 → P4-13 → P4-14 — four hops, and the only chain that reaches the report. P4-1 → P4-2 → P4-4 is three and has slack; start P4-10 early anyway, since it is the phase's other `high`.

**Dependency graph:**

```
                        ┌── P4-2 ──┬── P4-4
P4-1 ───────────────────┼── P4-3 ──┘
                        └── P4-5 ── P4-6

P4-8 ── P4-9
  └──────────────┐
                 ├── P4-11 ── P4-13 ── P4-14
P4-10 ───────────┘

P4-7   ── independent
P4-12  ── independent
```

**Parallel split.** P4-1 is the only bottleneck, the same shape as P2-1 and P3-1 — one contract three consumers read, frozen before any of them is written. Like those two it is tagged `·`: one person writes it while the other reviews, and neither track starts its dependents until it is frozen.

- **Track A** (logic + data): P4-8 → P4-10 → P4-11, plus P4-3 and P4-5 whenever.
- **Track B** (UI + resources): P4-7 → P4-12 → P4-2 → P4-4 → P4-6 → P4-9. **P4-7 and P4-12 come first deliberately**: both are independent of P4-1, so Track B is never idle waiting on the contract.
- **Shared:** P4-13 and P4-14 are the closing tasks. One person writes, the other verifies — the exit checklist is the last thing that should be self-graded.

**Won't build.** Phase 4 is the last phase, so these are decisions, not deferrals:

- The optional CEFR placement quiz (`project-idea.md` §5, already marked a nice-to-have)
- The Hub daily challenge (§9)
- An `Achievement` table with unlock timestamps and unlock-time toasts — see P4-3 for the reasoning
- Register/formality at C1–C2, and any C1–C2 seed content
- Sharing realms between players, server-side persistence, sprite monsters, locales beyond `en` and `vi`

---

# Task detail

## P4-1 · Stats snapshot contract + aggregate queries ⬜

**Difficulty:** high · **Track:** · · **Depends on:** P1-4, P2-1 · **Unblocks:** P4-2, P4-3, P4-5

**Do this first and freeze it.** Three consumers want the same numbers: the stats screen (P4-2), the achievement predicates (P4-3), and the reminder's "N words due" text (P4-5). Computing them three times is how they end up disagreeing on screen, and a stats screen that contradicts an achievement is worse than no stats screen.

**Files:** `game/stats/{StatsSnapshot,TypeAccuracy,WeakWord}.java`, `content/StatsLoader.java`, `db/dao/{WordProgressDao,WordEventDao,RealmDao}.java` (extend)

### Where each piece lives, and why

The layout follows what the repo already does rather than adding a layer:

| Piece | Location | Why there |
|---|---|---|
| `StatsSnapshot`, `TypeAccuracy`, `WeakWord` | `game/stats/` | Plain Java value objects. Putting them in `game/` is what lets P4-3's predicates be plain-JUnit testable with no emulator. |
| The aggregate queries | the existing DAOs | A query belongs to the table it reads. `WordProgressDao` gets the mastery buckets, `WordEventDao` the accuracy aggregates. **No new DAO file.** |
| Assembly | `content/StatsLoader.java` | `content/` is already where loaders live — `SeedLoader`, `MonsterCatalog`, `RealmImport`. A reader of Room that returns plain objects is exactly that shape. |

> **The projections live in `game/`, not `db/`, and the direction matters.** Room maps a `SELECT` to any POJO with matching column names — no annotation required. So `TypeAccuracy` is a plain class in `game/stats/` and the DAO imports *it*, rather than the DAO exposing a nested class that `game/` would have to import back. The dependency points one way, and the §7 "`game/` imports no Android classes" check stays clean. `game/question/QuestionGenerator` already imports `db.entity.Word`, so `game/` depending on `db/` types is established; `db/` depending on `game/` types is the new direction and it is the one that keeps the constraint satisfiable.

### What the snapshot holds

**Mastery buckets** — four counts off `WordProgress`, using Anki's mature threshold of 21 days:

| Bucket | Definition |
|---|---|
| New | No `WordProgress` row yet |
| Learning | `interval < 21` days |
| Mastered | `interval >= 21` days |
| Due now | `dueAt <= now` |

The threshold is a named constant, not a literal in a query string, because it is the one number on this screen a grader might ask to justify.

**Per-question-type accuracy** — `SELECT questionType, AVG(ratio) AS avgRatio, COUNT(*) AS attempts FROM WordEvent GROUP BY questionType`.

This is the part of the task worth the "high" rating, and not because the SQL is hard. `WordEvent` has been recording every answer with its completion ratio since P2-11 and **nothing has ever read it back except Spoils.** Grouping it by type produces a sentence like *"your collocation accuracy is 45%"* — which is precisely the L1-interference weak spot `project-idea.md` §3 argues is this project's most defensible pedagogical claim. The data has been sitting there unread since Phase 2; this is the query that cashes it in.

**Weakest words** — `GROUP BY wordId ORDER BY AVG(ratio) ASC LIMIT 10`, joined to `Word` for the headword. Requires a minimum attempt count so a single unlucky answer does not top the list.

**Run record** — `streak`, `bestFloor`, `totalRuns`, `runsWon`, `marks`, straight off `Profile`.

> ⚠️ **`Profile` has no `runsWon`.** `SpoilsActivity:91` sets `streak = won ? streak + 1 : 0`, so a streak proves a *recent* win and nothing more — a later loss zeroes it and would retroactively re-lock a "win a run" achievement. Add `runsWon` as an `int` on `Profile`, incremented in the same `SpoilsActivity` transaction. It also gives the stats screen a win rate, which is the number a roguelike player actually wants.

**Generated realm count** — one `COUNT(*) WHERE generated = 1` on `RealmDao`, for the Realm Forger achievement.

### The snapshot is a snapshot

`StatsSnapshot` is immutable and built once per screen open on `App.io()`. It is deliberately **not** `LiveData`: the stats screen is not a live dashboard, nothing changes while it is on screen, and eight observed queries would be eight main-thread hops for numbers that cannot move. The Hub's due count stays `LiveData` because that one *does* change under it.

**Done when**
- [ ] Every number the stats screen and the achievements show comes from one `StatsSnapshot`, built once
- [ ] `grep -rn "^import android" app/src/main/java/com/lexicondepths/game/` is still clean
- [ ] Mastery buckets partition correctly and sum to the total word count — asserted, not eyeballed
- [ ] `Profile.runsWon` increments in the same transaction as `streak`, and a loss does not decrement it
- [ ] A word with one attempt cannot appear in the weakest-words list

---

## P4-2 · Vocabulary stats screen ⬜

**Difficulty:** medium · **Track:** B · **Depends on:** P4-1

**This is the Phase 4 milestone.**

**Files:** `ui/stats/StatsActivity.java` (replace the placeholder), `res/layout/activity_stats.xml`, `res/values/strings.xml`, `res/values-vi/strings.xml`

`StatsActivity` currently inflates `activity_placeholder.xml` and sets a title. It is the last placeholder in the app.

Four sections, in this order — most legible first:

1. **Mastery** — the four buckets as counts plus a stacked proportion bar
2. **Where you are weak** — per-question-type accuracy, sorted worst first
3. **Words to revisit** — the ten weakest headwords with their accuracy
4. **Run record** — streak, best floor, runs won / runs played, Marks

Sorting section 2 **worst-first** is the whole point of the section. Best-first is a trophy case; worst-first is a study plan, and the app's argument is that it teaches.

### No chart library

A horizontal proportion bar is a `LinearLayout` whose children carry `layout_weight` — which is how `ui/widget/HpBar.java` already draws the HP bar. Nothing here needs a dependency, and §2's approved list does not include one.

Every bar pairs its color with its numeric label, per §7. A bar is never the only way to read a value.

### Empty state

A fresh install has no `WordEvent` rows at all, and a stats screen full of zeros and empty bars reads as broken rather than as new. Sections 2 and 3 collapse to a single line pointing at Practice or a run when there is not enough data yet.

**Done when**
- [ ] `activity_placeholder.xml` has no remaining users and is deleted
- [ ] Every string is in `strings.xml` and `values-vi/strings.xml`, none hardcoded
- [ ] A fresh install shows a readable invitation, not a wall of zeros
- [ ] Opening the screen does no Room work on the main thread
- [ ] Per-type accuracy is sorted worst-first

---

## P4-3 · Achievement definitions ⬜

**Difficulty:** medium · **Track:** A · **Depends on:** P4-1

**Files:** `game/stats/Achievements.java`, `test/java/com/lexicondepths/game/stats/AchievementsTest.java`

Each achievement is a name, a description, and a **predicate over a `StatsSnapshot`**. Nothing is stored. The list is recomputed whenever the stats screen opens.

| Achievement | Unlocks at |
|---|---|
| First Descent | `totalRuns >= 1` |
| Delver | `bestFloor >= 3` |
| Depth Conqueror | `runsWon >= 1` |
| Word Hoard | 150 words seen (any `WordProgress` row) |
| Lexicographer | 50 mastered words |
| Unbroken | `streak >= 7` |
| Realm Forger | at least one generated realm |
| Diligent | 200 questions answered |
| Silver Tongue | collocation accuracy ≥ 80% over ≥ 20 attempts |

Silver Tongue is the one with a reason beyond completeness: collocation is the hardest area for Vietnamese learners (`project-idea.md` §3) and the only achievement that rewards a *specific* skill rather than persistence.

### Derived, not stored — and what that costs

No `Achievement` table, no `unlockedAt` column, no unlock detection hooked into `SpoilsActivity` or `BattleActivity`.

What that buys: nine pure functions in `game/`, plain-JUnit tested, and zero new writes on any path that already works.

What it costs, stated plainly rather than discovered later: **no unlock timestamps, and no "achievement unlocked" moment.** A player who crosses 50 mastered words finds out the next time they open the stats screen. That is a real loss of game feel, and it is the right trade in the last phase — a table plus unlock-detection in two Activities that currently pass their tests is meaningful new risk to buy a toast.

> ⚠️ **Every predicate reads only `Profile`, `WordProgress`, `WordEvent`, and `Realm`** — never `Run`, `RunNode`, or `RunRelic`. Those four survive a run ending; the run-scoped three are wiped by `RunDao.clearRunState()`. A predicate over run-scoped data would silently re-lock an earned achievement the moment a run ended, which is the §5 boundary violated in a new place.

**Done when**
- [ ] Every predicate has a unit test at the boundary — one below the threshold, one at it
- [ ] No predicate references a run-scoped table, asserted by inspection and stated in the report
- [ ] Ending a run cannot re-lock an unlocked achievement
- [ ] `Achievements` compiles and tests with no emulator

---

## P4-4 · Achievements on the stats screen ⬜

**Difficulty:** low · **Track:** B · **Depends on:** P4-2, P4-3

**Files:** `ui/stats/StatsActivity.java` (extend), `res/layout/activity_stats.xml` (extend), strings ×2

A fifth section on the stats screen, not a screen of its own. Nine rows is not a screen.

Locked and unlocked rows are distinguished by **glyph and text weight, not by color alone** — `[✓]` versus `[ ]`, per §7. Locked rows show their unlock condition, since a hidden requirement is not a goal.

**Done when**
- [ ] Locked/unlocked is readable in grayscale
- [ ] A locked achievement states what unlocks it
- [ ] Strings in both locales

---

## P4-5 · Review reminder — channel, alarm, boot re-arm ⬜

**Difficulty:** medium · **Track:** A · **Depends on:** P4-1

**Files:** `notify/{ReviewReminder,ReminderReceiver,BootReceiver}.java`, `AndroidManifest.xml`, `Prefs.java` (extend)

Three small classes in a new top-level `notify/` package. Not `ui/` — nothing here is a screen.

### `AlarmManager`, not `WorkManager`

`WorkManager` is not on §2's approved dependency list, and `minimal-app-design` bans adding a library where an SDK API does the job. `AlarmManager.setInexactRepeating` plus a `BroadcastReceiver` is that API.

**Inexact is a decision, not a shortcut.** Exact alarms need `SCHEDULE_EXACT_ALARM` on Android 12+ — a permission the Play Store scrutinises and which a daily vocabulary reminder has no claim to. Inexact lets the OS batch the wakeup, which is both the correct citizenship and one fewer permission to justify in the report.

### Behavioral rules

- The receiver reads the due count through **P4-1's snapshot** on `App.io()`, so the notification's number and the Hub's number cannot disagree.
- **A due count of zero posts nothing.** A notification reading "0 words due" is worse than silence — it trains the user to swipe the app's notifications away unread.
- Tapping the notification opens `PracticeActivity` directly, not the Hub. The reminder's whole purpose is the review session; making the user navigate to it wastes the intent it just earned.
- `BootReceiver` on `ACTION_BOOT_COMPLETED` re-arms the alarm, because `AlarmManager` schedules do not survive a reboot. Fifteen lines, and without them the feature quietly stops working after the first restart — the kind of bug that is invisible in a demo and obvious in use.
- Hour of day is stored in `Prefs`, default 20:00.

**Done when**
- [ ] With zero due words, no notification is posted
- [ ] Tapping the notification lands in Practice
- [ ] Rebooting the device (or `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED`) leaves the alarm armed
- [ ] Disabling reminders cancels the pending intent — verified with `adb shell dumpsys alarm`
- [ ] The notification's due count matches the Hub's

---

## P4-6 · `POST_NOTIFICATIONS` runtime flow ⬜

**Difficulty:** medium · **Track:** B · **Depends on:** P4-5

**Files:** `ui/settings/SettingsActivity.java` (extend), `res/layout/activity_settings.xml` (extend), strings ×2

`project-idea.md` §11 asks for this to be "built into the screen flow, not as an afterthought," and it is a stated device-integration rubric item. So it is a flow with three real branches, not a `requestPermissions` call.

### The request fires on the toggle, never at launch

A cold `POST_NOTIFICATIONS` prompt at first launch — before the user knows what the app is, let alone whether they want it to interrupt them — is what trains people to hit Deny reflexively. The request fires when the user turns **Daily review reminder** on in Settings, at the one moment the ask is self-explaining.

Three branches, all of which have to work:

| Situation | Behavior |
|---|---|
| API < 33 | No permission exists. The toggle simply works. |
| API 33+, granted | Schedule the alarm (P4-5). |
| API 33+, denied | The toggle snaps back off, with an inline line explaining notifications are blocked and a button to `ACTION_APP_NOTIFICATION_SETTINGS`. |

The denied branch distinguishes a first refusal from a permanent one via `shouldShowRequestPermissionRationale`, because re-prompting on a permanent denial does nothing at all — the dialog never appears, the toggle appears broken, and the only honest response is to send the user to system settings.

A toggle that silently fails to arm anything is worse than no toggle. The toggle's state must always reflect whether a notification will actually arrive, which means it also re-checks on `onResume` — the user may have granted or revoked the permission in system settings while the app was backgrounded.

`registerForActivityResult(ActivityResultContracts.RequestPermission())`, available through `appcompat`. No new dependency.

**Done when**
- [ ] Denying leaves the toggle off, never on-but-silent
- [ ] A permanent denial offers system settings instead of a dialog that never opens
- [ ] Granting the permission in system settings and returning updates the toggle
- [ ] The flow works on API < 33, where the permission does not exist
- [ ] `POST_NOTIFICATIONS` is declared in the manifest with a comment naming the API level that needs it (33+) — declaring it is harmless below 33, but an undocumented permission is the kind of thing a grader asks about

---

## P4-7 · Onboarding carousel ⬜

**Difficulty:** medium · **Track:** B · **Depends on:** —

**Files:** `ui/onboarding/OnboardingActivity.java`, `res/layout/activity_onboarding.xml`, `Prefs.java` (extend), `ui/hub/HubActivity.java` (extend), `ui/settings/SettingsActivity.java` (extend), strings ×2

Three unfamiliar mechanics stack in this app — Wordle letter feedback, roguelike node navigation, and a combat model where the monster has no health bar. `project-idea.md` §11 calls four tooltip screens the fix for a cold demo, and it is right: none of the three is guessable.

Four pages:

1. **What this is** — a vocabulary game wearing a dungeon; the words are the point
2. **The descent** — 3 floors × 4 steps, two nodes to pick from, HP carries across floors, **and dying never takes back what you learned**
3. **Combat** — monsters have no HP, they have question slots; damage is *inverse* to difficulty, so an easy miss hurts most; the timer only ever helps you
4. **Reading the grid** — the `✓ ~ ✗` glyphs, and where to go next

Page 2's last clause is there because it is the project's headline design claim, and a player who does not know it plays scared.

### No `ViewPager2`

Not on §2's approved list, and four pages of static text do not need a pager. One layout, a page index, and Next / Back / Skip swapping the text and the ASCII art — which is what `Typewriter` and `Scramble` from P1-11 already animate.

Gated on a `Prefs` flag and shown once before the Hub. **Settings gets a "Replay intro" entry** — small, and it means a demo can show onboarding on command instead of clearing app data in front of an audience.

**Done when**
- [ ] Shown exactly once on a fresh install, never again
- [ ] Skip on page 1 sets the flag, same as finishing
- [ ] Both locales, no hardcoded strings, no tofu boxes in Vietnamese
- [ ] Replay from Settings works and does not re-arm the first-launch gate
- [ ] Rotating (or a process kill) mid-carousel does not restart it from page 1

---

## P4-8 · Marks purchase — persistence and consumption ⬜

**Difficulty:** medium · **Track:** A · **Depends on:** P2-9 · **Unblocks:** P4-9, P4-11

**Files:** `db/entity/Profile.java`, `db/dao/ProfileDao.java` (extend), `game/run/RunEngine.java` (extend), `assets/relics.json`, `content/RelicCatalog.java` (extend), `test/java/com/lexicondepths/game/run/RunEngineTest.java` (extend)

### The gap

`SpoilsActivity:88` adds Marks to `Profile`. `HubActivity:90` displays them. **Nothing else in the codebase reads the field.** Marks are earned forever and spent never.

That is not just an unfinished feature — `project-context.md` §6 states "Marks are the permanent currency, spent at the Hub on starting bonuses," and that sentence is currently false. A grader who reads the design doc and then plays the app finds the discrepancy in about ninety seconds.

### The design: buy a starting relic

`Profile` gains one nullable `String pendingRelicId`. Buying deducts Marks and sets it, in one transaction. `RunEngine.startRun` inserts the matching `RunRelic` for the new run and clears the field.

**Every relic effect already exists** as a `switch` branch in `Damage`, `TimerBonus`, or `RunEngine` (P2-9, eight of them). So this adds a currency sink with **no new effect code and no new balance math** — the strongest argument for this design over permanent stat upgrades, which would need new branches in the damage path in the last phase of the project, with no playtesting budget left to catch a mistake.

Price is a `price` field in `relics.json`, not a constant in code. `RelicCatalog` already parses that file; a number that might need tuning during the demo belongs in data.

> `ponytail:` flat pricing to start. A run earns roughly 100–160 Marks (10/battle, 20/elite, 40/boss), so a uniform price near 60 makes a relic cost about half a run. Tiering by relic power is a `relics.json` edit if playtesting says the strong ones are underpriced.

### Two rules that have to hold

- **Deduction and assignment are one transaction.** Two writes means a crash between them either takes the Marks without granting the relic or the reverse. `ProfileDao` gets an `@Transaction` method; it does not get done from the Activity in two calls.
- **The purchase is consumed by exactly one run.** `startRun` clears `pendingRelicId` in the same transaction that inserts the `RunRelic`. A purchase that survives into a second run is a free relic every run forever.
- ⚠️ **`startRun` returns early when a run is already active** (the P3-5 guard). The purchase must **not** be consumed on that path — the player did not get a new run, so they must not lose the relic they paid for.

**Done when**
- [ ] Buying with insufficient Marks is refused, and no partial write occurs
- [ ] Marks deduction and `pendingRelicId` assignment are one transaction
- [ ] The relic appears in exactly one run and is gone from `Profile` afterwards
- [ ] `startRun` hitting the active-run guard leaves the purchase intact
- [ ] The relic's effect actually applies in that run — verified through the existing effect path, not just the `RunRelic` row

---

## P4-9 · Relic shop screen ⬜

**Difficulty:** medium · **Track:** B · **Depends on:** P4-8

**Files:** `ui/shop/ShopActivity.java`, `res/layout/activity_shop.xml`, `AndroidManifest.xml`, `ui/hub/HubActivity.java` (extend), strings ×2

Reached from the Hub, beside the Marks display that currently does nothing. Lists all eight relics from `relics.json`: name, description, price, and a buy button disabled when Marks are short. The currently-purchased relic shows as equipped for the next run, and buying a different one replaces it — with the price difference **not** refunded, stated on screen so it is a choice rather than a surprise.

Second reason this screen earns its place: it is where a player can **read what the eight relics do** before a reward screen (P2-12) asks them to pick one blind. Right now the only way to learn a relic is to take it.

**Done when**
- [ ] An unaffordable relic cannot be bought, and the reason is visible before the tap
- [ ] The equipped relic survives a force-quit
- [ ] Replacing a purchase is clearly a replacement, not an addition
- [ ] Descriptions come from `relics.json`, not duplicated into `strings.xml`

---

## P4-10 · Register/formality question type ⬜

**Difficulty:** high · **Track:** A · **Depends on:** P2-1, P3-1 · **Unblocks:** P4-11

**Files:** `game/question/QuestionType.java`, `game/question/gen/{RegisterFormalityGenerator,QuestionGenerators}.java`, `db/entity/Word.java`, `content/MapJson.java`, `assets/{words_seed.json,monsters.json}`, `backend/.../DeepSeekClient.java` (prompt), `test/java/com/lexicondepths/game/question/gen/RegisterFormalityGeneratorTest.java`

Type 12 of 12, deferred at Phase 2 close. Shipping it means every question type the design doc specifies exists.

### One nullable field, following `affixKey`

`QuestionGenerator.canGenerate(Word)` is word-driven, so this needs per-word data. There is already exactly this pattern in the repo: **`affixKey`, a nullable `Word` field serving exactly one question type, set on 14 of 300 seed words.** So `Word` gains a nullable `formalAlt` — the more formal equivalent of the headword (`kids` → `children`).

Two things fall out of reusing that pattern rather than inventing one:

**Gating needs no new code.** `canGenerate` returns false without `formalAlt`, and the type only appears via a monster that declares it. `Encounter` already handles a declared type with no eligible word. There is no CEFR check to write.

**Gated at B2+, not C1+.** See the findings section: the seed has no C1 or C2 words, so C1+ gating makes the type unreachable outside a forged C1 realm.

### Question shape

Prompt: the word's own `example` sentence as context, asking for the formal rewrite — *"Rewrite for a formal register: '…' — which word replaces 'kids'?"*

Options: `formalAlt` (correct) plus three distractors **drawn from other pool words' `formalAlt` values.** Distractors from the same field are at the same register level, which makes them genuinely tempting rather than obviously wrong. A distractor a learner can eliminate without knowing the answer teaches nothing.

`canGenerate` requires `formalAlt` non-blank and `example` containing the headword — the second is already a P3-1 validation rule, so forged words satisfy it by construction.

### ⚠️ Two failure modes to design against, not discover

**A monster declaring only this type can produce a zero-slot encounter.** `Encounter.pickSlot` drops a slot it cannot fill, and falls back only to the monster's *other* declared types. A monster declaring `REGISTER_FORMALITY` alone, in a topic whose pool has no `formalAlt` words, yields an encounter with zero slots. Therefore the new monster (**The Courtier**) declares `["REGISTER_FORMALITY", "SYNONYM_ANTONYM"]`, so the fallback always lands somewhere.

**Distractors can run short.** `canGenerate` sees one `Word` and cannot know how many others in the pool have `formalAlt`. Seed at least **five per topic** (≥20 total) so a 75-word topic pool always has four, and have `generate` pad from a small static list rather than emit a two-option MCQ.

### Both ends of the pipe

`MapJson.parseWord` reads `formalAlt` as nullable — the same treatment `affixKey` gets, so one parser still serves both the seed and the network path (P3-1). The DeepSeek system prompt gains the field as optional. The backend's `MapValidator` needs **no new fatal rule**: a missing `formalAlt` costs that word one question type, which is not a defect.

**Done when**
- [ ] `canGenerate` is false for a word without `formalAlt`, and true with one
- [ ] Distractors are all `formalAlt` values, never a mix of registers
- [ ] A pool with fewer than three other `formalAlt` words still yields a four-option question
- [ ] The Courtier never produces a zero-slot encounter, asserted in `EncounterTest`
- [ ] A forged realm carrying `formalAlt` generates this type; one without it degrades to the monster's second type
- [ ] `MapJson` parses `formalAlt` from both `words_seed.json` and a generated map, in the same code path

---

## P4-11 · Real Room migration, destructive fallback removed ⬜

**Difficulty:** medium · **Track:** A · **Depends on:** P4-8, P4-10 · **Unblocks:** P4-13

**Files:** `App.java`, `db/AppDatabase.java`, `androidTest/java/com/lexicondepths/MigrationTest.java`

Depends on P4-8 and P4-10 because it needs the final column list: `Profile.pendingRelicId`, `Profile.runsWon`, `Word.formalAlt`. Version 2 → 3, three `ALTER TABLE … ADD COLUMN` statements, and `fallbackToDestructiveMigration()` deleted from `App.java`.

### Why this is not bookkeeping

The current fallback drops every table on a version mismatch — **including `WordProgress`.** §5 of `project-context.md` names that table as the one thing a run ending must never touch, and enforces it with a test. But an APK upgrade would destroy it wholesale, doing exactly what losing a run is forbidden to do. The justifying comment ("no shipped installs to migrate yet") stops being true the moment the project is submitted.

The version is getting bumped by this phase regardless. Writing the migration is the difference between a boundary that is enforced and one that is enforced against only the threat we happened to think of first.

`exportSchema = true` is already set, so the v2 schema JSON is on disk to migrate from.

> A v1 install has no path to v3 and will not get one. v1 → v2 already ran destructively during development, there are no v1 installs outside this repo, and inventing a migration for a schema nobody is on is speculative work. 2 → 3 is the real one.

**Done when**
- [ ] `fallbackToDestructiveMigration` appears nowhere in the codebase
- [ ] `MigrationTest` migrates a v2 database with `WordProgress` rows to v3 and asserts **ease, interval, reps, lapses, and `dueAt` are unchanged**
- [ ] The three new columns exist after migration with correct defaults
- [ ] A fresh install on v3 still seeds correctly — the migration path and the create path both work

---

## P4-12 · Colorblind and accessibility audit ⬜

**Difficulty:** low · **Track:** B · **Depends on:** —

**Files:** wherever the audit finds a gap — expected: `ui/battle/view/McqView.java`, `ui/widget/{HpBar,AsciiMonsterRenderer}.java`, layouts

§7 of `project-context.md` claims *"every color-coded feedback state pairs with a shape or glyph (✓ / ~ / ✗) for colorblind readability."* This task's first job is to find out whether that is true, because it is written as a completed convention and `project-idea.md` §11 still lists it as an open gap. One of the two documents is wrong.

**Confirmed true so far:** `WordleGridView` does it properly — every tile carries `✓`, `~`, or `✗` alongside its color, with a comment explaining why. That is the hardest case and it is already handled.

**To verify:** MCQ correct/incorrect feedback, the HP bar at low health, timer-tier indication, and P4-2's own new bars.

Then the basics `project-idea.md` §11 groups with it: `contentDescription` on every icon-only control, 48dp minimum touch targets, a text alternative for the ASCII monster (TalkBack reads box-drawing characters as noise), and `android:importantForAccessibility="no"` on the decorative scanline overlay.

**Done when**
- [ ] Every color-coded state verified against the §7 claim, and the claim corrected in the doc if any case fails
- [ ] A screenshot of a battle in grayscale is fully readable
- [ ] TalkBack can complete one battle end to end
- [ ] No icon-only control lacks a `contentDescription`
- [ ] `project-idea.md` §11's colorblind checkbox is closed, or the reason it is not is written down

---

## P4-13 · Edge-case hardening + `RealmImport` test ⬜

**Difficulty:** medium · **Track:** · · **Depends on:** P4-11 · **Unblocks:** P4-14

**Files:** `androidTest/java/com/lexicondepths/{PermadeathBoundaryTest,RealmImportTest}.java`, plus fixes wherever the list below finds a crash

### `RealmImport` has never been tested automatically

`report-phase3.md` flags this explicitly: *"It touches Room, so it is covered by the on-device database inspection above rather than by an automated test. Adding it to `PermadeathBoundaryTest` would close this properly and is a reasonable Phase 4 item."*

It is also the highest-risk untested code in the project — it is the only path that writes to `Word` at runtime, and its seed-collision join is subtle enough that the Phase 3 report devotes a section to it. `androidTest`, three cases:

- A map of entirely new words produces a realm with the full count
- A map of entirely pre-existing words also produces a realm with the full count, and **their `WordProgress` rows are byte-identical afterwards**
- A failure partway through leaves no `Realm` row at all

### The `project-idea.md` §11 list, executed

That document asks for a "test plan / edge-case list for the report" and notes it is often explicitly on grading rubrics. Listing them is not the deliverable — running them is:

| Case | What must happen |
|---|---|
| First launch with no internet | Seeds, plays, reviews. Only forging fails, and readably. |
| DeepSeek returns malformed JSON | The P3-1 validator rejects it; no partial realm; a readable error. |
| Empty word bank | No crash, no empty encounter. A monster with zero eligible words is handled. |
| Locale switched mid-session | Recreates cleanly; no mixed-language screen; no lost run state. |
| Run ending mid-battle | The P2-12 path; no orphaned `Run` rows, no leaked nodes. |
| Force-quit at every screen | Every screen resumes or returns to a valid state. |
| Notification permission revoked while backgrounded | P4-6's toggle re-checks on resume. |
| Marks purchase then force-quit before starting a run | The purchase survives; it is not double-consumed. |

Each one either passes or produces a fix in this task. Whichever it is gets recorded in P4-14 — including the ones that found nothing, since "we checked and it held" is a result.

**Done when**
- [ ] `RealmImport` has automated coverage for the collision join and the rollback
- [ ] Every case above has been run on a device or emulator, with its outcome written down
- [ ] Any crash found is fixed at its root, not guarded at the call site
- [ ] `gradlew.bat test` and `gradlew.bat connectedAndroidTest` both green

---

## P4-14 · Phase 4 report and test plan ⬜

**Difficulty:** low · **Track:** · · **Depends on:** everything

**Files:** `report-phase4.md`, `docs/plan.md`, `project-context.md`, `README.md`

Same shape as the three reports before it: test counts, the exit checklist with per-item evidence, the design decisions worth defending, and — the section that earns the most credit and is easiest to skip — **what was not verified, stated plainly.**

Two items carry over unverified from Phase 3 and must be either closed or restated:

- **The physical-device LAN forge.** No physical device was available at Phase 3 close. `report-phase3.md` says "do this before the demo."
- **A live Datamuse Hydra fight.** Unit-tested against a stub; the real `api.datamuse.com` call has never been made from a device.

Then the doc sweep, because three documents currently describe a project that will no longer exist:

- `project-context.md` §4 — Stats row to ✅; new rows for the shop, onboarding, notifications, and register/formality
- `project-context.md` §2 — `notify/` in the tree; the migration replacing the destructive-fallback note
- `project-context.md` §6 — the Marks sentence becomes true, or is corrected
- `project-idea.md` §11 — close the checkboxes Phase 4 actually closed; mark the placement quiz and daily challenge as won't-build
- `docs/plan.md` — Phase 4 closed, progression block updated
- `README.md` — status line

**Done when**
- [ ] Every exit-checklist item below has evidence or an explicit "not verified" with a reason
- [ ] The two Phase 3 carry-overs are closed or restated
- [ ] No document still describes a Phase 4 that has not happened
- [ ] `project-context.md` §6's Marks claim matches the code

---

## Phase 4 exit checklist

1. `gradlew.bat assembleDebug` and `gradlew.bat test` green in `android/`; `gradlew.bat test` green in `backend/`
2. `gradlew.bat connectedAndroidTest` green, including `MigrationTest` and `RealmImportTest`
3. `grep -rn "^import android" app/src/main/java/com/lexicondepths/game/` clean
4. `grep -rn "fallbackToDestructiveMigration" android/` finds nothing
5. Fresh install → onboarding shows once → Hub. Reinstall-free replay works from Settings
6. Stats screen on a fresh install reads as an invitation; after a run and a Practice session it shows real buckets, per-type accuracy sorted worst-first, and weakest words
7. Achievement unlocks at its threshold, and **survives a subsequent run loss**
8. Reminder toggle: granted → notification arrives at the set hour with a correct count; denied → toggle off with a system-settings route; zero due → nothing posted; reboot → still armed
9. Buy a relic with Marks, start a run, confirm its effect applies and the purchase is consumed exactly once
10. Fight The Courtier — a register question appears with four same-register options; in a topic with no `formalAlt` words the fallback type appears instead and the encounter still has its full slots
11. Upgrade a v2 database to v3 with SRS progress in it, and confirm ease/interval/reps/lapses/`dueAt` are untouched
12. A battle screenshot is fully readable in grayscale; TalkBack completes one battle
13. Every `project-idea.md` §11 edge case run, with its outcome recorded
14. Physical-device forge over LAN performed, or its absence stated in the report
15. `git grep "sk-"` still finds nothing; release `network_security_config.xml` still denies cleartext

See `report-phase4.md` for how each item was verified and which are manual/on-device versus automated.
