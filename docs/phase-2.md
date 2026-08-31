# Phase 2 — The playable run

**Goal:** pick a topic, walk a branching 3-floor dungeon, fight monsters by answering vocabulary questions, take damage on failures, and finish the run either dead or victorious — with every answer feeding the same SRS tables the flashcard mode uses.

**Blocked until Phase 1 clears its exit checklist.** In particular P2-1 needs a working `WordDao` (P1-4) and a seeded database (P1-6), and P2-11 needs the widget kit (P1-11).

**Milestone:** P2-12 closes the loop. A loss writes failed words back into the review queue, which is what turns a defeat into the thing that makes the next run easier.

---

## Task table

| ID | Task | Difficulty | Depends on | Unblocks | Track | Status |
|---|---|---|---|---|---|---|
| P2-1 | Question contracts | high | P1-4, P1-6 | everything below | · | ⬜ |
| P2-2 | Damage + timer bonus | medium | P2-1, P1-8 | P2-11 | A | ⬜ |
| P2-3 | Generators A — meaning | medium | P2-1 | P2-8 | A | ⬜ |
| P2-4 | Generators B — form & usage | medium | P2-1 | P2-8 | A | ⬜ |
| P2-5 | Generators C — puzzle | medium | P2-1 | P2-8 | A | ⬜ |
| P2-6 | TTS + listening generator | medium | P2-1 | P2-8 | B | ⬜ |
| P2-7 | Question input views | high | P2-1, P1-11 | P2-11 | B | ⬜ |
| P2-8 | Monsters + encounter builder | medium | P1-5, P2-1 | P2-9, P2-11 | A | ⬜ |
| P2-9 | `RunEngine` | high | P1-4, P2-8 | P2-10, P2-12 | A | ⬜ |
| P2-10 | Realm select + dungeon map | medium | P2-9, P1-11 | — | B | ⬜ |
| P2-11 | Battle screen | high | P2-2, P2-7, P2-8 | P2-12 | B | ⬜ |
| P2-12 | Run end + Spoils | high | P2-9, P2-11, P1-7 | — | · | ⬜ |

**Critical path:** P2-1 → P2-8 → P2-9 → P2-11 → P2-12

**Dependency graph:**

```
                    ┌── P2-2 ──────────────────┐
                    │                          │
                    ├── P2-3 ──┐               │
                    │          │               │
P2-1 ───────────────┼── P2-4 ──┼── P2-8 ──┬── P2-11 ──┬── P2-12
                    │          │          │           │
                    ├── P2-5 ──┤          └── P2-9 ───┤
                    │          │                  │   │
                    ├── P2-6 ──┘                  └── P2-10
                    │                                 │
                    └── P2-7 ─────────────────────────┘
```

**Parallel split.** P2-1 is the only bottleneck — after it merges the tracks separate cleanly:

- **Track A** (logic): P2-2 → P2-3 → P2-4 → P2-5 → P2-8 → P2-9. Owns `game/`.
- **Track B** (UI): P2-7 → P2-6 → P2-11 → P2-10. Owns `ui/`.
- P2-3, P2-4, and P2-5 are independent of each other. If Track B finishes early, split them.
- P2-12 is the rejoin point.

**Deferred past Phase 2:** Register/formality (type 12) is C1+ only, gated above the target audience. Affix harvest uses an offline answer key in the seed data; the Datamuse `sp=` wildcard query arrives in Phase 3.

---

# Task detail

## P2-1 · Question contracts

**Difficulty:** high · **Track:** · · **Depends on:** P1-4, P1-6 · **Unblocks:** every other Phase 2 task

**Do this first and freeze the API.** Both tracks build against it. Changing a signature after fan-out means telling the other person.

**Files:** `game/question/{Question,Answer,QuestionResult,QuestionType,QuestionGenerator}.java`

### The completion ratio is the whole design

Question types do not share a pass/fail shape. Wordle has six guesses. Affix harvest scores 0–N. Definition-matching is binary. Rather than special-casing each one in the damage code:

> **Every question type returns a completion ratio 0.0–1.0.** Damage is `base × (1 − ratio) × depthMultiplier`.

Harvest 3 of 5 → 0.6 → take 40% damage. Wordle solved on guess 5 of 6 → 0.8 → chip damage. Binary types return exactly 0.0 or 1.0. One formula, every type plugs in, each type's `base` tunes independently.

```java
public enum QuestionType {
    DEFINITION_TO_WORD, WORD_TO_DEFINITION, SYNONYM_ANTONYM,
    WORD_FORM, CLOZE, COLLOCATION,
    ANAGRAM, SENTENCE_SCRAMBLE, WORDLE, AFFIX_HARVEST,
    LISTENING_SPELLING
}

public interface QuestionGenerator {
    QuestionType type();
    boolean canGenerate(Word word);        // false when a field is missing
    Question generate(Word word, List<Word> pool, Random rng);
    QuestionResult score(Question q, Answer a);
}
```

`QuestionResult` carries `ratio`, `wordId`, `elapsedMillis`, and the correct answer for the recap screen.

`canGenerate` matters: not every word has collocations or forms. A generator that cannot build a question must say so rather than emitting a broken one.

`Random` is passed in, never constructed inside. That makes generation reproducible from a run seed and makes the tests deterministic.

**Done when**
- [ ] Zero Android imports in `game/question/`
- [ ] `QuestionType` values match the `questionTypes` strings in `assets/monsters.json`
- [ ] Ratio is documented as clamped to `[0.0, 1.0]`, enforced in `QuestionResult`'s constructor

---

## P2-2 · Damage + timer bonus

**Difficulty:** medium · **Track:** A · **Depends on:** P2-1, P1-8 · **Unblocks:** P2-11

**Files:** `game/combat/Damage.java`, `game/combat/TimerBonus.java`, `app/src/test/java/.../DamageTest.java`

### Damage is inverse to difficulty

Failing an *easy* question hurts more than failing a hard one. This models "you should have known that," and it stops stretch vocabulary from being a death sentence — players stay willing to attempt words above their level instead of playing safe. Say this explicitly in the report; it is the kind of design decision a grader probes.

Base damage against a 100 HP pool:

| Question vs. player's CEFR | Base |
|---|---|
| Below level | 18–20 |
| At level | 10–12 |
| One band above | 5–6 |
| Two+ bands above | 2–3 |

Depth multiplier: floor 1 → 1.0, floor 2 → 1.5, floor 3 → 2.0. Tolerance works out to roughly 5–8 mistakes per run.

```java
public static int compute(int playerCefrOrdinal, int wordCefrOrdinal,
                          double ratio, int floor, Set<String> relics);
```

### Timer: bonuses only, never penalties

Punishing slow-but-correct answers would train fast guessing over actual thinking, which fights the entire point of the app. Questions never time out and never fail you.

| Elapsed | Result |
|---|---|
| < 10s | full bonus |
| < 20s | partial bonus |
| ≥ 20s | base score, no penalty |

Thresholds come from `Prefs` (P1-8), defaulting to 10s/20s.

**Relics** are a `switch` over the effect keys from `assets/relics.json`. `FIRST_MISS_FREE`, `RATIO_FLOOR_20`, `DEPTH_MULT_MINUS_25`, and `STRETCH_DAMAGE_HALVED` all land here. Plain branches — no effect engine.

**Done when**
- [ ] A test per CEFR band confirming below-level hurts most
- [ ] Ratio 1.0 always yields exactly 0 damage regardless of floor or band
- [ ] A slow correct answer scores no worse than a fast wrong one
- [ ] Each relic effect has its own test

---

## P2-3 · Generators A — meaning

**Difficulty:** medium · **Track:** A · **Depends on:** P2-1

Definition→Word (Sphinx), Word→Definition (the easier A1–A2 variant), Synonym/Antonym (Twins).

All three are multiple choice. **Distractor quality is the whole task.** Pull distractors from the same CEFR band and the same topic — a distractor from a different band makes the answer obvious by elimination and teaches nothing. Never let the correct answer appear twice, and shuffle position with the passed-in `Random`.

Scoring is binary: 1.0 or 0.0.

**Done when** distractors are same-band and same-topic, and a test confirms the correct answer's position is uniformly distributed across many generations.

---

## P2-4 · Generators B — form & usage

**Difficulty:** medium · **Track:** A · **Depends on:** P2-1

Word form / morphology (Mimic), Cloze (Void-eater), Collocation (Chimera).

- **Word form** — `decide → decision → decisive`, presented in a sentence so the grammatical slot disambiguates. Reads the `forms` field.
- **Cloze** — take `example`, blank out the headword, keep enough context that exactly one answer fits. Gate to **B1+** per §5 of the design doc; it needs grammatical maturity.
- **Collocation** — "make/do a decision", "open/turn on the light". §7 of the design doc calls this the sleeper hit: the hardest area for Vietnamese learners because of L1 interference, rarely gamified anywhere, and highly defensible pedagogically. Worth extra care.

`canGenerate` returns false when `forms` or `collocations` is empty. Not every seed word carries them.

**Done when** CEFR gating is enforced in `canGenerate`, and a cloze whose blank has more than one valid filler is rejected at generation time rather than marked wrong at scoring time.

---

## P2-5 · Generators C — puzzle

**Difficulty:** medium · **Track:** A · **Depends on:** P2-1

Anagram (filler/pacing), Sentence scramble (A1–A2), Wordle (Cipher), Affix harvest (Hydra).

**Wordle** is deliberately **rare** — it is more game than learning. Six guesses, green/yellow/gray letter feedback. Ratio is `(7 − guessNumber) / 6`, so solving on guess 5 gives 0.33 and only chip damage. Solve on guess 1 → 1.0. Fail all six → 0.0.

**Affix harvest** is the partial-credit showcase. Given `re—` or `—tion`, the player types as many valid words as possible before the timer. Ratio is `found / keySize`, capped at 1.0. Phase 2 scores against an offline answer key stored in the seed data; Phase 3 swaps in the Datamuse `sp=` wildcard query. Build it behind an interface so that swap touches one class.

The post-fight "words you missed" screen is fed from here.

**Done when** Wordle's letter-feedback algorithm handles repeated letters correctly (the classic bug: `SPEED` guessed against `ERASE`), and harvest ratio is capped at 1.0 when a player finds more words than the key holds.

---

## P2-6 · TTS + listening generator

**Difficulty:** medium · **Track:** B · **Depends on:** P2-1

Listening→Spelling (Echo). TTS plays the word, the player types it. This makes text-to-speech a **gameplay mechanic** rather than a reference button — worth calling out as a device integration in the report.

Reuse `ui/widget/Speaker.java` from P1-12. Extend it with replay (limited, say 3 plays per question) and a speech-rate control.

Scoring is Levenshtein-based rather than binary, so a one-letter slip is not a total loss: `ratio = 1 − (distance / targetLength)`, clamped at 0.

**Done when** TTS init failure, a missing voice, and a muted device each degrade to a skippable question rather than a crash or a soft-lock. This is a demo-day failure mode — handle it properly.

---

## P2-7 · Question input views

**Difficulty:** high · **Track:** B · **Depends on:** P2-1, P1-11 · **Unblocks:** P2-11

**Files:** `ui/battle/view/QuestionView.java` + four subclasses

Eleven question types do **not** need eleven views. Four families cover all of them:

| View | Serves |
|---|---|
| `McqView` | Definition→Word, Word→Definition, Synonym/Antonym, Collocation, Word form |
| `TextInputView` | Cloze, Listening→Spelling, Anagram |
| `WordleGridView` | Wordle |
| `OrderingView` | Sentence scramble, Affix harvest |

This is the one place an abstract base is justified: four implementations sharing real behaviour, and eleven types collapsing onto it. The `minimal-app-design` skill permits abstraction that eliminates genuine duplication — this qualifies, a repository layer would not.

```java
public abstract class QuestionView extends FrameLayout {
    public abstract void bind(Question q);
    public interface Listener { void onAnswered(Answer a, long elapsedMillis); }
}
```

`BattleActivity` swaps these into a container. No Fragments.

### ⚠️ Colorblind-safe feedback

Wordle's green/yellow/gray is a known accessibility gap and it is cheap to fix. **Pair every colour with a glyph:** ✓ correct position, ~ wrong position, ✗ absent. Apply the same rule everywhere colour carries meaning, not just Wordle.

**Done when**
- [ ] All eleven types render through exactly four views
- [ ] Grayscale display mode: every feedback state is still readable
- [ ] Answer timing starts on `bind()`, not on the first keystroke
- [ ] The soft keyboard does not cover the input field on a small screen

---

## P2-8 · Monsters + encounter builder

**Difficulty:** medium · **Track:** A · **Depends on:** P1-5, P2-1 · **Unblocks:** P2-9, P2-11

**Files:** `game/run/Encounter.java`, `content/MonsterCatalog.java`, `ui/widget/MonsterRenderer.java`

**Monsters have no HP.** A monster is a fixed checklist of question slots. Clear all slots and it dies. Harder monsters mean more slots and more types. The player is the only health pool — damage flows one direction. This halves the balancing work versus two health pools and costs nothing in feel.

**A monster's question types are permanent.** Same monster, same shape, every encounter; only the words change. Players learn to read enemies and self-select which skills to practise. That is a pedagogical feature, not flavour.

`Encounter` builds the slot list: pick the monster, pull `slots` words matching the run's topic and the player's CEFR (weighted toward SRS-due words), and assign each slot a generator from the monster's permanent type list.

**Bosses** get 5 slots across 3 types, run as phases: two of type A, two of type B, then a single high-stakes finisher of type C. Optional shield mechanic: the first slot must be cleared with a specific type before others count, which forces skill breadth instead of tanking through with one strength.

**`MonsterRenderer` is an interface from day one.** §8 of the design doc asks for this explicitly as the escape valve: if someone who can draw sprites turns up later, ASCII swaps for bitmaps in one class without touching anything else. `AsciiMonsterRenderer` is the only implementation for now.

**Done when** every monster in `monsters.json` builds a valid encounter, boss phases advance in order, and a monster whose types cannot be generated from the available words falls back rather than crashing.

---

## P2-9 · RunEngine

**Difficulty:** high · **Track:** A · **Depends on:** P1-4, P2-8 · **Unblocks:** P2-10, P2-12

**Files:** `game/run/{RunEngine,NodeGen,RunState,Relic}.java`, `db/dao/RunDao.java` (extend)

Structure, from [`project-context.md`](../project-context.md) §6: **3 floors × 4 steps**, two node choices per step. A two-column ladder, not a general graph — it renders in ASCII trivially and stores as a flat list.

- Steps 1–3 roll weighted from `{BATTLE ×3, REST, TREASURE}`
- Step 4 is `ELITE` on floors 1–2, `BOSS` on floor 3
- 100 HP carried across floors, no free heal between them. `REST` gives 30 HP or a due-word review
- Depth multiplier 1.0 / 1.5 / 2.0

`NodeGen` is seeded from `Run.seed` so a run regenerates identically — essential for reproducing a bug rather than guessing at it.

**Resume after kill** is a real requirement, not polish. Every node resolution commits to Room immediately. Killing the app mid-battle and reopening must restore the run, because that is exactly what happens when a phone rings during a demo.

**Done when**
- [ ] The same seed produces the same map every time (unit-tested, no Android needed)
- [ ] Force-quit mid-run and reopen restores floor, step, HP, and relics
- [ ] HP reaching 0 transitions to the run-end state exactly once, never twice

---

## P2-10 · Realm select + dungeon map

**Difficulty:** medium · **Track:** B · **Depends on:** P2-9, P1-11

**Files:** `ui/realm/RealmSelectActivity.java`, `ui/map/DungeonMapActivity.java`

**Realm select** lists topic maps — Food, Travel, Business, Emotions — filtered by CEFR. Each is a vocabulary domain, not a menu skin: words come only from that topic and level, which is what makes the focused-practice argument defensible.

Also surface **Echo Trial** here: the same roguelike structure, but words are pulled from *all* unlocked topics prioritising whatever the SRS says is due. It is the daily clear-my-review-queue mode.

**Dungeon map** draws the two-column ladder in ASCII with node-type glyphs, the current position marked, and cleared nodes struck through. Tap a node to enter it. Only the two nodes at the current step are tappable.

**Done when** the map survives rotation-free process death, and re-entering an in-progress run lands on the map at the right position rather than restarting.

---

## P2-11 · Battle screen

**Difficulty:** high · **Track:** B · **Depends on:** P2-2, P2-7, P2-8 · **Unblocks:** P2-12

**Files:** `ui/battle/BattleActivity.java`, `res/layout/activity_battle.xml`

The screen that carries the game. Layout, top to bottom: monster ASCII art, slot checklist, HP bar, timer, and the `QuestionView` container.

Flow: bind a question → start the timer → collect the answer → score it → apply damage → animate → advance the slot → repeat until slots are clear or HP hits 0.

Animations from P1-11: screen shake on damage, scramble-then-resolve when a word is revealed, typewriter for monster dialogue, character-by-character colour flash on correct letters.

Every answered question writes a `WordEvent` row immediately. That row is what P2-12's Spoils and the Phase 4 stats screen both read.

**Done when**
- [ ] Slots clear one per correct answer and the checklist matches the monster's declared shape
- [ ] An easy miss visibly hurts more than a hard miss
- [ ] Killing the app mid-battle and reopening restores the battle, not just the run
- [ ] The timer never fails a question — it only awards bonuses

---

## P2-12 · Run end + Spoils

**Difficulty:** high · **Track:** · · **Depends on:** P2-9, P2-11, P1-7

**This closes the loop and is the Phase 2 milestone.**

**Files:** `ui/reward/RewardActivity.java`, `ui/reward/SpoilsActivity.java`, `game/run/RunResult.java`

### Spoils

When a run ends, every word failed during it gets its SRS interval reset to **due now**, so it resurfaces immediately. Framed in-fiction as Spoils or Remnants: you died, but you carried something back.

This is simultaneously good roguelike design and literally correct spaced-repetition practice. It converts a loss into the thing that makes the next run easier.

Read the failed words from `WordEvent` where `ratio < 1.0`, grouped by word.

### ⚠️ The permadeath boundary

**Resetting `dueAt` to now is not the same as resetting mastery.** Ease, repetitions, and lapse history are preserved exactly. The word comes back sooner; it does not come back as if never learned.

Ending a run clears **only** run-scoped state: HP, floor, step, held relics, run currency, node path. It must never erase or roll back a word's mastery level. Losing a battle is a game setback, never a loss of what the player actually learned.

P1-4's `PermadeathBoundaryTest` guards this. Extend it to cover the Spoils path specifically.

### Rewards and meta-progression

`RewardActivity` offers a choice of 1 of 3 relics after battles and treasure nodes. On run end, write Marks earned, best floor, run count, and streak to `Profile`. A "words you missed" recap lists every failed word with its definition, which is the screen that makes the loss feel like learning.

**Done when**
- [ ] Failed words are due now in Practice immediately after a loss
- [ ] Those words' ease and interval are **unchanged** from before the run
- [ ] `Profile` updates survive a force-quit
- [ ] Winning and losing both route through the same cleanup — no leaked run rows

---

## Phase 2 exit checklist

1. Start a run — the map shows floor 1 with two node choices
2. Fight a full battle: slots clear one per correct answer, HP drops on failures, an easy miss hurts more than a hard one
3. Answer one question under 10s and one over 20s — the bonus applies, and slow-but-correct is never punished
4. Kill the app mid-battle and reopen — the run resumes
5. Lose on purpose — Spoils lists the failed words, they are due now in Practice, **and their ease/interval were not reset**
6. Reach floor 3 — the depth multiplier doubles damage
7. Grayscale the display — Wordle feedback is still readable from ✓ / ~ / ✗
8. Airplane mode — everything works except TTS
9. `gradlew.bat test` passes, including the seeded-map reproducibility test
