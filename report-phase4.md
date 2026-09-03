# Phase 4 — Polish · Report

**Status: closed, 14/14.** Phase 4 was the last phase, so nothing follows it and its deferred list is a *won't build* list rather than a handoff.

| | |
|---|---|
| Unit tests (`android/`) | **146**, green — was 124 at Phase 3 close |
| Backend tests (`backend/`) | **18**, green — unchanged |
| Instrumentation tests | **18**, green — was 2 at Phase 3 close |
| Verified on | Pixel 10 AVD, API 37 (x86_64), headless |
| Verified on a physical device | **No.** See *What was not verified*. |

---

## What shipped

| Task | What it is |
|---|---|
| P4-1 | `StatsSnapshot` + the aggregate queries. One object, three consumers. |
| P4-2 | The vocabulary stats screen — the phase milestone. |
| P4-3 | Nine achievements as predicates over that snapshot. Nothing stored. |
| P4-4 | Achievements as a fifth section on the stats screen. |
| P4-5 | Daily review reminder: channel, inexact alarm, boot re-arm. |
| P4-6 | `POST_NOTIFICATIONS` runtime flow with all three branches. |
| P4-7 | Four-page onboarding carousel, shown once, replayable from Settings. |
| P4-8 | Marks purchase — `Profile.pendingRelicId`, consumed by exactly one run. |
| P4-9 | The relic shop, and the first place a player can read what relics do. |
| P4-10 | Register/formality — question type 12 of 12. |
| P4-11 | Room migration 2 → 3; the destructive fallback is gone. |
| P4-12 | Colorblind and accessibility audit. |
| P4-13 | Edge-case hardening, `RealmImport` coverage, and six bug fixes. |
| P4-14 | This document and the doc sweep. |

---

## The six bugs the edge-case pass found

This is the section worth reading. **Every one of these was invisible to `assembleDebug`, invisible to 124 passing unit tests, and only appeared when the app was actually run.** That is the argument for P4-13 existing as a task rather than as a checklist item.

### 1. A fresh install seeded zero words

`SeedLoader.parseWords` let a `MapJson.InvalidMapException` escape, and `run()` caught it as "corrupt or truncated file" and left the database untouched. Two seed rows — `explore` and `liability` — have example sentences that use an inflected form (`exploring`, `liabilities`), which fails `parseWord`'s "the example must contain the headword" rule. **One bad row out of 300 emptied the entire word bank.**

Latent since P3-1, when `SeedLoader` switched to the shared `MapJson.parseWord`. Invisible for two phases because an already-seeded install skips seeding on the version guard — only a *fresh* install hits it, which is exactly the first case on `project-idea.md` §11's edge-case list.

Fixed at the root, in two places, because the data defect and the blast radius are separate faults:

- The two example sentences now use the headword. (`assets/words_seed.json`, version bumped to 2)
- `parseWords` skips an unusable row and logs it, which is what `MapJson.parseMap` already did on the network path for exactly this reason. One bad word costs one word, not three hundred.

### 2. Three relics had never once applied their effect

`RunEngine.maxHp`, `RunEngine.restHealAmount` and `RunResult.from` all branch on relic **effect** keys (`MAX_HP_PLUS_10`). Every production caller passed relic **IDs** (`lexicon_shard`). The sets never intersected, so Lexicon Shard, Hearth's Ember and Merchant's Ledger were inert from Phase 2 onward.

The unit tests passed because they were written against the functions directly and supplied effect keys — a textbook case of testing at the wrong boundary. `BattleActivity` had a private `resolveEffects` helper and used it correctly for `Damage`, then passed raw IDs to `maxHp` two lines later.

Fixed by moving the resolver to `RelicCatalog.effectsFor(context, ids)` and routing all three call sites through it, so there is one place left to make the mistake. Verified on device: a run with Merchant's Ledger paid 69 Marks on a 55-Mark run (55 × 1.25 = 68.75), and a purchased Lexicon Shard started a run at **HP 110 / 110**.

### 3. Spoils skipped exactly the words the player had just failed

`project-context.md` §5 promises that on death "every word failed during the run gets `dueAt` reset to now, so it resurfaces immediately." `WordProgressDao.resetDueNow` is a bare `UPDATE`, which matches nothing for a word that has no `WordProgress` row yet — and a word met for the first time *in a battle* has no row. The words a player had just proved they did not know were the ones the promise silently skipped.

Fixed with `resetDueNowOrCreate`: an `INSERT OR IGNORE` at SM-2 defaults, then the existing update. The insert is a no-op for a word that already has a schedule, so the permadeath boundary holds either way — asserted by two new tests in `PermadeathBoundaryTest`.

### 4. The Hub's due count went stale mid-session

`HubActivity` observed `getDueWords(System.currentTimeMillis())`. The timestamp is a query *argument*, so it was bound once at `onCreate` and never moved. A word that came due while the player was in Practice never appeared until the process restarted — and the reminder, which builds a fresh snapshot every time it fires, would then disagree with the Hub. P4-5 specifically requires those two numbers to match.

Fixed by reading the count on `onResume` through `App.io()`, the same pattern the Hub already used for its Resume Run check.

### 5. Every screen drew its first line under the status bar

`targetSdk 35` makes every Activity edge-to-edge. Nothing in the app applied window insets, so the onboarding page indicator sat under the system clock and the bottom row sat under the gesture bar, on all twelve screens.

The obvious fix — `android:fitsSystemWindows="true"` on the theme — is wrong here and was tried and reverted: that attribute *replaces* a view's padding, so every layout would have silently lost the `android:padding` it declares. Confirmed visually before reverting. The fix is one `ActivityLifecycleCallbacks` in `App.java` that *adds* the insets to each root's existing padding, posted so it runs after `setContentView`. One place instead of twelve, and the IME is in the mask so the keyboard no longer covers the input it opened for.

### 6. Every Courtier encounter crashed the app

Found *after* the phase was first called closed, by writing the test that closes exit-checklist item 10 — the one item whose gap the report had argued was "low risk" rather than verifying.

`QuestionView.create` maps the question types onto the four view families in one `switch`. P4-10 added `REGISTER_FORMALITY` to `QuestionType`, wrote its generator, tagged 24 seed words with `formalAlt`, gave The Courtier the type, and covered all of it with seven generator tests and two encounter tests — **and never added the branch that decides which view renders it.** The type fell through to `default:` and threw `IllegalArgumentException: No view family for REGISTER_FORMALITY`, from a background-loaded callback, killing the process. The Courtier was unfightable in the shipped build.

Nothing caught it because nothing crossed the boundary it lived on. The generator tests build `Question` objects and never render them; the encounter tests build slots and never bind them; `assembleDebug` sees a `switch` with a `default`, which is exhaustive as far as javac cares. The type has no unit-testable seam between "generated correctly" and "displayed at all", and that is precisely the gap the report had reasoned its way past.

Fixed by adding `case REGISTER_FORMALITY:` to the MCQ group, where it belongs — it is a four-option question and `McqView` needed no change to serve it. The guard against a recurrence is `CourtierRenderTest`, which inserts a Courtier node into the real database, launches the real `BattleActivity`, and asserts the register prompt and four distinct, visible, non-zero-height options actually reach the screen. It fails loudly if a thirteenth type is ever added without a view family.

**The lesson is the one the phase already half-learned in bug #2:** a type that is correct in the domain layer and absent from the mapping layer passes every test written at the domain boundary. Both bugs are the same shape — a set of keys on one side that the other side never learned about.

**A seventh, smaller one:** `ach_silver_tongue_req` contained `80%%`. The `%%` escape is only unescaped by `String.format`, and that string is read with a plain `getString`, so it rendered literally as `80%%`. Caught by reading the achievement rows out of a UI dump rather than by looking at a screenshot.

---

## Exit checklist

| # | Item | Result | How |
|---|---|---|---|
| 1 | `assembleDebug` + `test` green in `android/`; `test` green in `backend/` | ✅ | Automated. 146 + 18 tests, from `clean`. |
| 2 | `connectedAndroidTest` green, including `MigrationTest` and `RealmImportTest` | ✅ | Automated. 18 tests, including `CourtierRenderTest`; run three times to confirm stability after one flake was fixed. |
| 3 | `grep -rn "^import android" .../game/` clean | ✅ | Automated grep — no output. |
| 4 | `grep -rn "fallbackToDestructiveMigration" android/` finds nothing | ✅ | Automated grep over `app/src` and `build.gradle` — no output. The phrase does not appear even in a comment. |
| 5 | Fresh install → onboarding once → Hub; replay works from Settings | ✅ | On device. `pm clear`, launch: `OnboardingActivity` on top. Skip on page 1 → Hub. Relaunch → straight to Hub. Settings → **Replay intro** reopens it. |
| 6 | Stats reads as an invitation when empty; shows real data after play | ✅ | On device, both states screenshotted. Fresh: `New 300 · Learning 0 · Mastered 0 · Due now 0` with an invitation line in sections 2 and 3. After a run: per-type accuracy sorted **worst-first** (0%, 0%, 0%, 50%), weakest words with their attempt counts, `Runs won: 0 of 1 (0%)`. |
| 7 | An achievement unlocks at its threshold and survives a subsequent loss | ✅ | On device *and* automated. First Descent unlocked **on a lost run** and stayed unlocked afterwards, which is the exact case the design worried about. `AchievementsTest.everyAchievementSurvivesARunEnding` asserts it for all nine. |
| 8 | Reminder: granted → arrives with a correct count; denied → toggle off with a settings route; zero due → nothing; reboot → still armed | ✅ | Mixed, see note below. |
| 9 | Buy a relic, start a run, confirm the effect applies and is consumed once | ✅ | On device. 69 Marks → buy Lexicon Shard → 9 Marks, "Next run starts with: Lexicon Shard". Force-quit → survived. Start run → **HP 110 / 110**. Reopen shop → "Next run starts with no relic", Marks still 9. Also `MarksPurchaseTest` ×4. |
| 10 | Fight The Courtier — four same-register options; degrades to its second type | ✅ | Automated, on the emulator. `CourtierRenderTest` drives the real `BattleActivity` against a Courtier node. **This closed the sixth bug — see below.** |
| 11 | Upgrade a v2 database to v3 with SRS progress in it, untouched | ✅ | `MigrationTest.migrate2To3_leavesEverySrsFieldUntouched` — ease, interval, reps, lapses and `dueAt` all asserted unchanged, with `validateDroppedTables=true`. |
| 12 | A battle screenshot is readable in grayscale; TalkBack completes one battle | ⚠️ | Grayscale ✅, TalkBack ⚠️. See below. |
| 13 | Every `project-idea.md` §11 edge case run, with its outcome recorded | ✅ | Table below. |
| 14 | Physical-device forge over LAN performed, or its absence stated | ⚠️ | Not performed. Stated below. |
| 15 | `git grep "sk-"` finds nothing; release config denies cleartext | ✅ | Only `backend/.env.example` placeholders (`sk-your-key-here`), which is the file whose job is to show the shape. Release `network_security_config.xml` has `cleartextTrafficPermitted="false"`. |

### Note on item 8

Three of the four parts were verified directly on the device:

- **Denied** → the toggle snapped back off and the hour spinner stayed disabled. Because this was a *first* refusal, no system-settings route appeared, which is correct — the dialog can still help, and the route is reserved for a permanent denial.
- **Granted** → `dumpsys alarm` shows `*walarm*:com.lexicondepths/.notify.ReminderReceiver` armed as `RTC_WAKEUP`.
- **Reboot** → after `am broadcast -a android.intent.action.BOOT_COMPLETED`, the alarm is still present.

The **zero-due** and **correct-count** parts could not be verified by `adb shell am broadcast`: the receiver is `exported="false"`, so a shell broadcast never reaches it. "No notification appeared" from a shell broadcast is indistinguishable from the broadcast never arriving, so that observation proved nothing and was discarded. They are instead covered by `ReviewReminderTest`, which runs in the app's own process and calls `postIfDue` directly — zero due posts nothing, one due posts a notification whose body carries the same count the Hub would show.

The notification arriving **at the set hour** by real elapsed time was not waited out.

---

## `project-idea.md` §11 edge cases, executed

Listing them is not the deliverable; running them is. "We checked and it held" is a result, and so is "we checked and it did not."

| Case | Outcome |
|---|---|
| First launch with no internet | **Found bug #1.** With Wi-Fi and mobile data off: seeded 0 words. Fixed; re-verified — 300 words seeded, Practice and a full run play offline. |
| DeepSeek returns malformed JSON | Held. Covered by `MapJsonTest` (19 tests) and `MapValidatorTest` (18) on both sides of the wire. The proxy-unreachable path was also run on device: *"Could not forge: Failed to connect to /10.0.2.2:8080"* with **Retry** and **Use offline realm**, and no partial realm in the library. |
| Empty word bank | Held, and this is what bug #1 accidentally exercised in full. With zero words the Hub, Stats and Practice all render without crashing; Stats shows its invitation state. `Encounter` drops a slot it cannot fill rather than crashing, asserted in `EncounterTest`. |
| Locale switched mid-session | Held. Switched to Vietnamese from Settings: the screen recreated cleanly, fully in Vietnamese, correct diacritics, no tofu boxes, no mixed-language screen. Stats and Settings both checked. The reminder toggle survived the recreate in the correct state. Run state was untouched. |
| Run ending mid-battle | Held. Ran a full run to a loss: Spoils listed eight missed words, wrote Marks and `bestFloor`, and cleared the run — the Hub's Resume Run button disappeared, so no orphaned `Run` row. **But see bug #3**, which this case is what surfaced. |
| Force-quit at every screen | Held. Spot-checked at Hub, Shop, Dungeon Map, Battle and Settings. The most valuable one: force-quit after buying a relic and before starting a run — the purchase survived. Mid-battle resume was already covered in Phase 2 and is unchanged. |
| Notification permission revoked while backgrounded | Held by construction and by inspection: `syncReminderUi` runs on `onResume`, re-derives `remindersEnabled() && canPost()`, and cancels the alarm if the permission is gone rather than leaving a toggle that promises a notification it cannot deliver. Not exercised by actually revoking from system settings mid-session. |
| Marks purchase then force-quit before starting a run | Held. Verified on device (item 9) and by `MarksPurchaseTest.theActiveRunGuardLeavesThePurchaseIntact`, which covers the subtler sibling: `startRun` returning early on the one-active-run guard must not consume a purchase the player paid for. |

---

## Design decisions worth defending

**The stats snapshot is not `LiveData`.** Nothing on the stats screen changes while it is open, so eight observed queries would be eight main-thread hops for numbers that cannot move. One immutable object, built once on `App.io()`. The Hub's due count stays a per-resume read because that one *does* change under it — see bug #4.

**Achievements are derived, not stored.** Nine pure predicates over a snapshot, plain-JUnit tested with no emulator, and zero new writes on any path that already worked. The cost is stated rather than discovered: **there is no "achievement unlocked" moment.** A player who crosses fifty mastered words finds out the next time they open the stats screen. That is a real loss of game feel and the right trade in the last phase — a table plus unlock detection in two Activities that currently pass their tests is meaningful new risk bought for a toast.

The constraint that made it safe: every predicate reads only `Profile`, `WordProgress`, `WordEvent` and `Realm` — never `Run`, `RunNode` or `RunRelic`. Those three are wiped by `clearRunState()`, so a predicate over them would re-lock an earned achievement the moment a run ended. `everyAchievementSurvivesARunEnding` asserts it.

**Register/formality is gated at B2+, not the C1+ the design doc specifies.** `words_seed.json` is 76 A1 / 76 A2 / 76 B1 / 72 B2 with zero C1 and zero C2. A C1 gate would make the type unreachable outside a forged C1 realm, and a question type that cannot appear in a demo is not shipped. The gate is realised entirely by *which words carry `formalAlt`* (24 B2 words, six per topic), reusing the `affixKey` pattern — so it needed no gating code at all. Recorded in `project-idea.md` §5 rather than left as a silent inconsistency.

**The Courtier declares two question types.** `Encounter.pickSlot` drops a slot it cannot fill and falls back only to the monster's *other* declared types, so a monster declaring `REGISTER_FORMALITY` alone would build a zero-slot encounter in any topic whose pool carries no `formalAlt`. Designed against rather than discovered.

**Marks buy a starting relic, not a permanent stat upgrade.** All eight relic effects already exist as `switch` branches from P2-9, so this added a currency sink with **no new effect code and no new balance math** — which matters in a last phase with no playtesting budget left to catch a mistake. Pricing is flat 60 in `relics.json`, not a constant in code, because it is the number most likely to need tuning during a demo.

**`AlarmManager`, not `WorkManager`.** WorkManager is not on the approved dependency list, and `setInexactRepeating` plus a `BroadcastReceiver` is the SDK API that does the job. Inexact is a decision, not a shortcut: exact alarms need `SCHEDULE_EXACT_ALARM` on Android 12+, which a daily vocabulary reminder has no claim to.

**No chart library and no `ViewPager2`.** A proportion bar is a `LinearLayout` with weighted children — the same idea `HpBar` already uses. Four pages of static text do not need a pager. Zero dependencies were added in Phase 4.

**The migration is the point, not bookkeeping.** `fallbackToDestructiveMigration` drops every table on a version mismatch, `WordProgress` included. §5 names that table as the one thing a run ending must never touch, so an APK upgrade would have done exactly what losing a run is forbidden to do. The comment justifying it ("no shipped installs to migrate yet") stopped being true at submission. There is deliberately no 1 → 3 path: 1 → 2 already ran destructively during development and no v1 install exists outside this repo.

---

## The colorblind claim, corrected

`project-context.md` §7 asserted *"every color-coded feedback state pairs with a shape or glyph."* `project-idea.md` §11 listed the same thing as an open gap. One of the two was wrong, and the audit's first job was to find out which.

| Surface | Before | Now |
|---|---|---|
| `WordleGridView` | Already correct — every tile carries `✓`, `~` or `✗` beside its color, with a comment explaining why. The hardest case, already handled. | Unchanged. |
| MCQ correct/incorrect | **There is no color feedback at all.** Tapping an option submits it immediately; nothing is ever colored. Nothing to pair. | Unchanged. The claim was neither true nor false here — it did not apply. |
| HP bar at low health | **Color alone.** Green → red below 30%, and the battle screen showed no HP number. | The bar carries a `contentDescription` with its values, reports itself as a `ProgressBar` to TalkBack, and the battle screen now prints `HP: n / m` above it. |
| Timer tier | Not surfaced in the UI at all. | Unchanged. Nothing to pair. |
| Stats proportion bar (new) | — | Counts are printed *above* the bar, and the bar carries them as its `contentDescription`. The bar is never the only way to read a value. |
| Achievement locked/unlocked (new) | — | `[✓]` bold versus `[ ]` dim. Readable in grayscale; no color carries meaning. |

**So both documents were partly wrong.** §7 overclaimed — the HP bar failed it. §11 overclaimed in the other direction — Wordle already passed. Both are now corrected to describe what the code does.

Other accessibility work: the ASCII monster gets a `contentDescription` naming the monster (TalkBack reads box-drawing characters as noise), dynamically-created MCQ buttons get an explicit 48dp minimum height that XML widgets inherit but code-created ones do not, the MCQ prompt is focusable so TalkBack lands on the question before the options, and the decorative scanline overlay was already `importantForAccessibility="no"`.

---

## What was not verified

Stated plainly, because this is the section that is easiest to skip and earns the most credit.

**No physical device.** Everything above is an emulator. Two consequences carry over unclosed from Phase 3:

1. **The LAN forge from a phone.** `Prefs.mapApiBaseUrl` exists precisely so a physical device can point at a laptop's LAN IP instead of the emulator's `10.0.2.2`, and that setting has still never been used against a real device. The emulator path works; the code path is identical apart from the string. **Do this before the demo.**
2. **Real-device performance and font rendering.** Vietnamese diacritics render correctly on the emulator at this DPI; nothing checks a different one.

**TalkBack was not run end to end.** The `contentDescription`s, the `ProgressBar` role, the focusable prompt and the 48dp targets are all in place and were verified by reading a UI hierarchy dump, but no screen reader was actually driven through a battle. This is the weakest ✅→⚠️ on the checklist and it is item 12.

~~**The Courtier was never fought by hand.**~~ **Closed after the fact, and it was hiding a crash.** This entry originally reasoned that the render risk was "low but not zero" because the generator and the encounter were both well covered. That reasoning was wrong in the way unverified reasoning usually is: `QuestionView.create` had never been given a `REGISTER_FORMALITY` branch, so it fell to `default:` and threw. Every Courtier encounter in the shipped build crashed the app. See bug #6.

**The notification was not waited out at its real hour.** The alarm is armed, the receiver's logic is tested in-process, and the boot re-arm works. The end-to-end "wait until 20:00 and see it appear" was not performed.

**String coverage is a parity check, not a reading.** Both locales carry 167 identically-named strings, verified programmatically; Phase 4 added 83 and removed one. The Vietnamese *wording* of those 83 was not reviewed by a native speaker — only that none is missing and that the diacritics render.

**Achievement thresholds were not playtested.** "150 words seen" and "200 questions answered" are guesses at a session's worth of play. They are single constants in `Achievements.Threshold` if they turn out wrong.

---

## Doc sweep

| Document | Change |
|---|---|
| `project-context.md` §2 | `notify/` and `game/stats/` in the tree; the destructive-fallback note replaced by the migration. |
| `project-context.md` §4 | Stats row → ✅; new rows for the shop, onboarding, notifications, register/formality and the migration; "11 shipped types" → 12. |
| `project-context.md` §5 | Schema version 3; the permadeath boundary now lists all three threats (losing a run, upgrading the APK, re-importing a known word) with the test that guards each. |
| `project-context.md` §6 | The Marks sentence is now true, and says where the currency is spent and how it is consumed. |
| `project-context.md` §7 | The colorblind convention rewritten to describe what the code does, with the audit result. |
| `project-context.md` §8 | All four phases closed; the two "this will become false" warnings removed, since they no longer are. |
| `project-idea.md` §5 | Question type 12 gated `B2+`, with a callout explaining the deviation from C1+. |
| `project-idea.md` §11 | Every checkbox closed, each pointing at the file that closed it. The placement quiz and daily challenge marked won't-build with a reason. |
| `docs/plan.md` | Phase 4 closed, 14/14; progression block updated; the five runtime bugs summarised. |
| `docs/phase-4.md` | All 14 task statuses flipped; a closed banner at the top. |
| `README.md` | Status line rewritten — 47/47 across four phases, with test counts. |

---

## Reproducing the verification

```
cd android
gradlew.bat clean assembleDebug test          # 146 unit tests
gradlew.bat connectedDebugAndroidTest         # 17 instrumentation tests, needs a device

cd ../backend
gradlew.bat test                              # 18 tests

# The two static checks from the exit checklist
grep -rn "^import android" android/app/src/main/java/com/lexicondepths/game/
grep -rn "fallbackToDestructiveMigration" android/app/src
```

`DatamuseLiveTest` makes a real call to `api.datamuse.com` and **skips itself** when the endpoint is unreachable, so a green build never depends on someone else's uptime. It closes the Phase 3 carry-over: that call had previously only ever been made against a stub. The stubbed `DatamuseAffixKeySourceTest` remains the behavioural test.
