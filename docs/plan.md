# Lexicon Depths — Roadmap

Index of phase plans, their gates, and what is open to work on right now.

**This file holds no task detail.** It says which phase is open and where its tasks live. Detail lives in the phase files.

| Document | Role |
|---|---|
| [`project-idea.md`](../project-idea.md) | Game design and rationale. The *why*. |
| [`project-context.md`](../project-context.md) | Code navigation: feature → file. Read first in a fresh session. |
| **this file** | Which phase is open, and the progression between them. |
| `docs/phase-N.md` | Task tables and per-task detail for one phase. |

---

## Progression

```
   ✅ Phase 1 ──▶ ✅ Phase 2 ──▶ 🔒 Phase 3 ──▶ 🔒 Phase 4
     12/12          12/12          not planned    not planned
   Foundation    Playable run    AI maps        Polish
```

Legend: ✅ closed · 🟢 open · ⬜ blocked by an earlier gate · 🔒 not planned yet

The project plans **two phases ahead**. Phase 3 gets written in detail only once Phase 2 closes.

---

## Phases

### ✅ Phase 1 — Foundation · **CLOSED**

📄 [`docs/phase-1.md`](phase-1.md) — 12 tasks, all done

The app launches, seeds a word bank into Room, runs a working SM-2 scheduler, renders a terminal UI in English and Vietnamese, and reviews flashcards. No combat.

**Progress: 12/12** — full exit checklist passed on-device (build, `PermadeathBoundaryTest`, fresh-install seeding, Vietnamese locale with zero tofu boxes, a 5-card Practice session surviving a force-quit, and the `game/` Android-import check). See [`report-phase1.md`](../report-phase1.md).

**Milestone:** P1-12 (flashcards) exercised the whole learning engine before any monster exists — verified end-to-end with distinct SM-2 outcomes per grade.

---

### ✅ Phase 2 — The playable run · **CLOSED**

📄 [`docs/phase-2.md`](phase-2.md) — 12 tasks, all done

Pick a topic, walk a branching 3-floor dungeon, fight monsters with real question types, take damage, and finish the run dead or victorious. Nine of the twelve question types ship here.

**Progress: 12/12** — full exit checklist passed: a run starts from Realm Select, a full battle plays out with slots clearing and HP dropping, the timer bonus applies without ever penalizing, a force-quit mid-battle resumes exactly where it left off, a loss runs the Spoils flow (failed words due now, ease/interval untouched), floor 3 doubles damage via the depth multiplier, and `gradlew.bat test` is green at 98 tests. See [`report-phase2.md`](../report-phase2.md).

**Milestone reached:** P2-12 closes the loop — a loss writes failed words back into the review queue, and both winning and losing route through the same run-cleanup transaction.

---

### 🔒 Phase 3 — AI map generation · not planned

The Spring Boot proxy in `backend/`, live DeepSeek map generation, and the My Library screen. Also swaps affix harvest onto the Datamuse `sp=` wildcard query.

The one constraint worth recording now, because it shapes Phase 2's interfaces: **never call DeepSeek from the client.** The API key would be extractable from the APK. Everything routes through the proxy, whose `.env` holds the key.

Not planned in detail yet — Phase 2 just closed. Per the "two phases ahead, never more" rule below, writing `docs/phase-3.md` is the next planning step, not part of this session's scope.

---

### 🔒 Phase 4 — Polish · not planned

Vocabulary stats screen, SRS review notifications with the Android 13+ `POST_NOTIFICATIONS` runtime permission flow, onboarding tooltips, achievements, and the test/edge-case list for the report.

---

## Working agreement

**Two people, two tracks.** Every task carries a track tag so you can work at once without touching the same files:

- **Track A** — data and game logic. Owns `db/`, `game/`, `content/`, `assets/`.
- **Track B** — UI and resources. Owns `ui/`, `res/`.
- **·** — a shared handoff point. One person does it while the other reviews.

The tracks meet at exactly two frozen contracts: **P1-4** (DAO signatures) and **P2-1** (the question API). Do each first in its phase, then fan out. Neither track changes those signatures without telling the other person.

**Difficulty tiers** are set by how much reasoning a task needs, so work routes to the right person or model:

| Tier | Meaning |
|---|---|
| **low** | Implementation only. The decisions are already made. |
| **medium** | Some design judgment inside a defined boundary. |
| **high** | The shape itself needs working out — schema design, algorithms, and any API both tracks depend on. |

**Before any task:** read [`.claude/skills/minimal-app-design/SKILL.md`](../.claude/skills/minimal-app-design/SKILL.md). It is binding and it overrides conventional Android advice.

**After any task:** `cd android && gradlew.bat assembleDebug && gradlew.bat test`, then flip the status cell in the phase file's task table.

---

## Keeping this current

When a task completes, update two places:

1. The task's row in `docs/phase-N.md` (status → ✅)
2. The progress count in this file's Progression block and the phase entry

When a phase closes, mark the next one 🟢 and write the detail file for the phase after it — always two ahead, never more.
