# Lexicon Depths — Project Handoff

*Working title. A roguelike English vocabulary game for Android.*

**Status:** Concept locked, gameplay mostly designed. Data model and run structure still open.

---

## 1. The pitch

An English vocabulary learning game structured as a roguelike dungeon crawler. Word puzzles (Wordle-style spelling, cloze, definition-matching, affix harvesting) are reskinned as combat. A spaced-repetition algorithm decides which words appear, disguised as difficulty tuning.

**Target users:** Intermediate ESL learners (teens/adults), self-study or supplementing classroom work — with Vietnamese speakers as the primary first-locale audience.

**The realistic problem being solved:** Vocabulary retention apps have an *engagement* problem, not a *content* problem. Spaced repetition is proven pedagogy, but flashcard drilling is boring and users quit. The roguelike wrapper — permadeath, run variety, meta-progression, loot — supplies the intrinsic motivation that flashcard apps lack, without diluting the underlying learning science.

**Why this satisfies the assignment brief:** clearly identified users, a documented real problem, 4+ meaningfully connected screens, persistent local data (Room), and multiple external/device integrations (LLM API, dictionary API, TTS, notifications).

---

## 2. Core gameplay loop

A **run** is a procedurally-assembled dungeon: a branching node path (Slay the Spire style) of battles, treasure rooms, and rest/review rooms. Losing all HP ends the run. Meta-progression persists between runs.

### Combat model — asymmetric, decided

**Monsters have no HP.** A monster is a fixed checklist of question slots. Clear all slots → monster dies. Harder monsters = more slots, more question types.

**The player is the only health pool.** Damage flows one direction: you're not out-damaging the monster, you're surviving it. This halves the balancing work versus two health pools and costs nothing in gameplay feel.

### Damage: inverse to question difficulty — decided

Failing an *easy* question hurts more than failing a hard one. Rationale (worth stating explicitly in the report): it models *"you should have known that,"* and it stops stretch vocabulary from being a death sentence, so players stay willing to attempt words above their level instead of playing safe.

Baseline player HP = 100:

| Question relative to player's CEFR level | Damage on fail |
|---|---|
| Below level (should be automatic) | 18–20 |
| At level | 10–12 |
| One band above (stretch) | 5–6 |
| Two+ bands above (bonus) | 2–3 |

Tolerance is roughly 5–8 mistakes per run — survivable but tense.

**Depth multiplier:** multiply damage by floor (×1.0 early, ×1.5 mid, ×2.0 boss floor). One number, easy to tune.

### Normalizing partial credit — decided

Question types don't share a pass/fail shape (Wordle has 6 guesses; affix harvest is scored 0–N; definition-matching is basically binary). Solution:

> Every question type returns a **completion ratio 0.0–1.0**. Damage = `base × (1 − ratio) × depthMultiplier`.

Harvest 3 of 5 → take 40% damage. Wordle solved on guess 5 of 6 → 0.8, small chip damage. Binary types return 0.0 or 1.0. One formula, every type plugs in, each type's `base` tunable independently.

### Timer bonus — decided (one mode only)

Bonuses only, never penalties. Punishing slow-but-correct answers would train fast guessing over actual thinking, which fights the app's purpose.

- **< 10s** → full bonus (e.g. 10 base + 5)
- **< 20s** → partial bonus (10 base + 2)
- **≥ 20s** → base points only; questions never time out or fail you

Fits the RPG skin as "critical hits." Thresholds and bonus values stored in settings (Room/DataStore) with 10s/20s defaults, exposed as sliders.

*Note: an earlier 3-way toggle (off / hidden / visible) was considered and cut — one mode is enough for scope.*

### Loss → library — decided

When a run ends, every word failed during that run is flagged and pushed into the review queue at elevated priority (mechanically: SRS interval reset to "due now"). Framed in-fiction as **Spoils** or **Remnants** — you died, but you carried something back.

This is both good roguelike design and literally correct spaced-repetition practice. It converts a loss into the thing that makes the next run easier.

### Critical rule: permadeath must never touch mastery data

Ending a run resets only **run-scoped state** (in-run currency, dungeon progress, HP). It must *never* erase or roll back a word's SRS mastery level. Losing a battle is a game setback, never a loss of what you actually learned.

State this explicitly in the design doc — a professor grading a "serious game" will likely probe exactly this.

---

## 3. Question types

Each type maps to an attack flavor. Some monsters resist certain types, forcing skill breadth.

| # | Type | Description | Notes |
|---|---|---|---|
| 1 | **Word form / morphology** | decide → decision → decisive, in context | Core |
| 2 | **Wordle** | Green/yellow/gray letter feedback | Deliberately **rare** — more game than learning |
| 3 | **Prefix/suffix harvest** | Given `re—` or `—tion`, type as many valid words as possible before timer | AoE attack; partial credit; scale by word count |
| 4 | **Cloze / fill in the blank** | Sentence with a gap | B1+ (needs grammatical maturity) |
| 5 | **Definition → word** | Given a definition, produce the word | Core |
| 6 | **Synonym / antonym** | Timed multiple choice | Core |
| 7 | **Collocation** | "make/do a decision", "open/turn on the light" | **Sleeper hit — promote to core.** Hardest area for Vietnamese learners (L1 interference), rarely gamified anywhere, highly defensible pedagogically |
| 8 | **Listening → spelling** | TTS plays word, player types it | Makes TTS a gameplay mechanic, not just a reference button |
| 9 | **Word → definition** | Reverse, multiple choice | Easier variant for A1–A2 |
| 10 | **Sentence scramble** | Reorder words into valid syntax | Good for A1–A2 |
| 11 | **Anagram / unscramble** | Quick strike | Filler/pacing |
| 12 | **Register / formality pick** | Which phrasing fits this context | C1+ only |

For affix harvest, use Datamuse's `sp=` wildcard query to generate the answer key server-side — enables partial credit scoring and a "words you missed" screen after the fight.

---

## 4. Monster design

**Rule: a monster's question types are permanent.** Same monster, same shape, every encounter. Only the *words* change. This lets players learn to read enemies and self-select which skills to practice — a real pedagogical feature, not just flavor.

| Monster | Question type | Why it fits |
|---|---|---|
| **Hydra** | Prefix/suffix harvest | Many heads, many words — cut one, more appear |
| **The Gap / Void-eater** | Fill in the blank | Literally eats words out of sentences |
| **Mimic / Shapeshifter** | Word form | Changes shape, same root |
| **Sphinx** | Definition → word | Riddles |
| **Cipher / Wraith** | Wordle | Rare, cryptic, drops good loot |
| **Twins / Mirror** | Synonym & antonym | Two faces |
| **Echo** | Listening → spelling | Hear it, write it |
| **Chimera** | Collocation | Mismatched parts stitched together |

**Bosses:** 5 slots, 3 types, run as phases — Phase 1 two of type A, Phase 2 two of type B, Phase 3 a single high-stakes finisher of type C.

Optional boss mechanic: a **shield** requiring the first slot to be cleared with a specific type before others count — forces skill breadth instead of tanking through with one strength.

---

## 5. Modes & content structure

### Realms (topic maps)
Each map is a vocabulary domain — Food, Travel, Business, Emotions — with words drawn only from that topic + CEFR level. Pedagogical justification: focused practice, not just menu skins.

### Echo Trial (formerly "Free Mode") — decided
Same roguelike structure, but the SRS algorithm pulls from **all unlocked topics**, prioritizing whatever's due for review. The daily "clear my review queue" mode.

Name chosen because it encodes what the mode actually *does* — words you've learned echoing back — rather than being generic fantasy flavor. Alternatives considered: The Nexus, The Deep / Endless Depths, The Wandering Path.

### Practice mode (flashcards)
Deliberately separate from the roguelike loop — a low-stakes option with no HP, no permadeath.

- Pulls from the same SRS due-queue, same Room tables, same mastery scores
- Front: word. Back: definition + example sentence + TTS pronunciation button
- Anki-style self-rating: again / hard / good / easy
- Does **not** affect run stats; **does** feed the same mastery data the battles use

Worth highlighting to the grader: two very different UX modes, one shared learning-science engine.

### CEFR level (A1–C2)
`level` field on the word bank. Users pick a starting level in onboarding (an optional 10-question placement quiz spanning A1→B2 is a nice-to-have, not required).

Level gates: which words appear, monster difficulty, and which question types unlock (cloze/register only from B1+/C1+).

Seed data: Oxford 3000/5000 word lists map roughly to CEFR bands — a defensible dataset rather than guessing difficulty by hand.

---

## 6. Localization

Two **independent** settings — do not conflate them:

- **UI locale** — menus, buttons, tutorial text. Standard Android resources: `values/strings.xml` + `values-vi/strings.xml`. Ship `en` + `vi` for the exam; note in the writeup that extending is just adding a `values-xx` folder (scalability evidence without building it).
- **Target language** — English, fixed. That's the point of the app.

Optional scaffolding: bilingual glosses (English word + Vietnamese meaning) at lower CEFR levels, mirroring how real ESL materials for Vietnamese learners work.

---

## 7. AI map generation

**Decision: live generation, results saved permanently to the player's own library.** Stronger pitch than pre-generated content — every player's dungeon collection is genuinely unique to them.

### Cost — non-issue
DeepSeek V4-Flash is $0.22 / M input tokens and $0.66 / M output tokens off-peak. A map generation call (structured JSON: word list, definitions, example sentences, distractors) runs a few hundred input tokens and ~1,000–1,500 output. That's **under $0.001 per map**. Hundreds of maps during development costs cents.

### Architecture — key constraint
**Never call DeepSeek from the client.** The API key would be extractable from the APK. Route through a small backend proxy.

For the local demo, run that backend on your own laptop:

1. **Minimal local server** (Node/Express or Python FastAPI), one endpoint `POST /generate-map` taking `{topic, level}`. Calls DeepSeek server-side, validates the returned JSON shape, returns clean JSON. Key lives in the server's `.env` only.
2. **Find your LAN IP** (`ipconfig` / `ifconfig`). Phone and laptop on the same Wi-Fi.
3. **Allow cleartext traffic** — Android blocks plain HTTP since API 28. Add `network_security_config.xml` permitting cleartext to your *specific* local IP, reference it in the manifest. Fine for local demo; don't ship as-is.
4. **Point the app's base URL at the laptop** — emulator uses `10.0.2.2` (host alias); physical device uses the LAN IP. Keep it as a build config field for easy swapping.
5. **Loading + error states** — LLM calls take a few seconds. Show an animation; handle failure with a retry ("couldn't reach the realm-forge, try again"). Wi-Fi hiccups happen during demos.
6. **One bundled offline fallback map** — if the live demo fails in front of the teacher, fall back gracefully instead of stalling.

### Prompt hygiene
Demand **strict JSON only** with an explicit schema in the system prompt. LLMs sometimes wrap output in markdown fences or add commentary. Strip fences (`.replace(/```json|```/g, '')`) and run a schema check (field count, no empty strings) before writing to the DB — a malformed map corrupting the library table mid-demo is an avoidable disaster.

### Library screen
Each generated map is a row in Room (topic, level, word list, generation timestamp, completion status). New screen: **My Library / Atlas / Grimoire** — all maps the player has generated, filterable by topic and level, replayable anytime.

---

## 8. Art direction

**Decision: terminal / ASCII aesthetic, fully committed — not a fallback.**

### Why it's the right call
The game is about text; a terminal is a text medium. Aesthetic and subject matter agree, which is more than most pixel-art games manage.

Practical wins: no sprite sheets, no DPI/resolution scaling problems, no asset pipeline, no art-consistency drift between teammates. Monsters are ASCII/box-drawing figures editable in a text file — a teammate can redesign one in thirty seconds without opening an art tool. Animations (typewriter reveal, blinking cursor, glitch-scramble, color flash) are trivial in Compose with basic text and coroutines.

### Risk 1 — Vietnamese diacritics (verify this first)
Most retro/pixel/terminal fonts have **no coverage** for `ế ộ ữ ẳ`. You'd get tofu boxes across the entire Vietnamese UI — a very visible failure given locale support is a stated requirement.

**Check before committing to a font.** IBM Plex Mono and JetBrains Mono both have proper Vietnamese support and are free/open-licensed. Test early by rendering something diacritic-heavy: *"Tiếng Việt — Cửa hàng đồ ăn"*.

Fallback option: monospace for English game content, a normal sans for Vietnamese UI chrome. That split reads as intentional rather than broken.

### Risk 2 — "terminal" can look lazy
What separates a deliberate aesthetic from a cop-out is **animation discipline**. Budget real time for roughly five signature animations rather than spreading effort thin:

- Typewriter reveal on monster dialogue
- Scramble-then-resolve when a word is revealed
- Character-by-character color flash on correct letters
- Subtle CRT scanline overlay
- Screen-shake on damage

Each is ~20 lines of Compose. These are what make it feel designed.

### Escape valve
Build the monster renderer as an **interface** from day one. If a teammate who can do sprites turns up later, ASCII monsters swap for pixel sprites in a single composable without touching anything else.

---

## 9. Screens

1. **Character Hub** — level, equipped class/deck, streak, daily challenge
2. **Realm select / Dungeon map** — node-based branching run path
3. **Battle** — puzzle UI, monster ASCII art, slot progress, HP bar, timer
4. **Reward / Relic** — post-battle power-up choice
5. **My Library** — generated maps, filterable, replayable
6. **Vocabulary stats** — mastered words, review queue, streak history, achievements
7. **Practice / Flashcards** — SRS review, self-rating
8. **Settings** — UI locale, CEFR level, timer thresholds

---

## 10. Technical stack

- **Client:** Kotlin + Jetpack Compose
- **Local persistence:** Room (source of truth for all gameplay)
- **SRS:** SM-2 or a simplified variant, per-word mastery + next-due-date
- **External:**
  - DeepSeek (via local proxy) — live map generation
  - Free Dictionary API / Datamuse — definitions, synonyms, affix wildcard queries
  - Android TTS — pronunciation, and a gameplay mechanic (listening questions)
  - Notifications — SRS review reminders

**Offline resilience:** Room is the source of truth. Network touches only *map generation* and *TTS*. Playing existing maps, flashcards, and stats must all work offline so a Wi-Fi hiccup degrades gracefully instead of breaking the app.

---

## 11. Outstanding gaps

Things still needing decisions or work:

- [ ] **Run structure** — node types, relics/power-ups, what meta-progression persists between runs. *This is the remaining gameplay gap before locking the data model.*
- [ ] **Room schema** — entities and relationships (blocked on the above)
- [ ] **Onboarding flow** — three unfamiliar mechanics stacked (Wordle feedback, roguelike navigation, RPG combat). 3–4 tooltip screens make a cold demo far easier to follow; easy polish points.
- [ ] **Colorblind-safe feedback** — Wordle's green/yellow/gray is a known accessibility gap. Pair colors with shapes/icons (✓ / ~ / ✗). Cheap to add, reads well in the writeup.
- [ ] **Runtime notification permission** — Android 13+ requires explicit `POST_NOTIFICATIONS` request. Build the request flow and a graceful denied-fallback into the screen flow, not as an afterthought.
- [ ] **Font verification** — Vietnamese diacritic coverage (see §8). Do this early.
- [ ] **Test plan / edge-case list for the report** — first launch with no internet, DeepSeek returning malformed JSON, empty word bank, run ending mid-battle, switching UI locale mid-session. Often explicitly on grading rubrics for "complete, working" apps.
