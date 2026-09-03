# Lexicon Depths

A roguelike English vocabulary game for Android, aimed at intermediate Vietnamese ESL learners. Word puzzles (spelling, cloze, definition-matching, collocation, ...) are reskinned as dungeon combat, and an SM-2 spaced-repetition scheduler decides which words appear, disguised as difficulty tuning.

Dying in a run never rolls back what the player learned — permadeath only clears run-scoped state (HP, floor, relics), never SRS mastery data.

## Status

**All four phases are closed — 47/47 tasks.** The app seeds a 300-word bank, runs SM-2 flashcard review, plays a full 3-floor dungeon run with all 12 question types in English and Vietnamese, forges new realms from a topic via a DeepSeek proxy, shows a vocabulary stats screen with achievements, spends Marks on a starting relic, and posts a daily review reminder.

146 unit tests, 18 backend tests, 18 instrumentation tests. See [`report-phase4.md`](report-phase4.md) for the final exit checklist, including what was verified how and what was not verified.

## Stack

Java 17, XML layouts + ViewBinding, Room for persistence. No Kotlin, no Compose, no DI framework — see [`project-context.md`](project-context.md) for why and [`.claude/skills/minimal-app-design/SKILL.md`](.claude/skills/minimal-app-design/SKILL.md) for the binding rules on Android code.

## Getting started

Open the `android/` folder (not the repo root) in Android Studio, or from a shell:

```
cd android
./gradlew assembleDebug   # gradlew.bat on Windows
./gradlew test
```

## Where to read next

| Document | What's in it |
|---|---|
| [`project-idea.md`](project-idea.md) | Game design and the pedagogical rationale — the *why*. |
| [`project-context.md`](project-context.md) | Feature → file map, Room schema, conventions — read this first in a new session. |
| [`docs/plan.md`](docs/plan.md) | Roadmap: which phase is open, progression between phases. |
| [`docs/phase-1.md`](docs/phase-1.md) … [`docs/phase-4.md`](docs/phase-4.md) | Per-task detail for each phase. |
| [`report-phase1.md`](report-phase1.md) … [`report-phase4.md`](report-phase4.md) | Exit-checklist writeups. Phase 4's also lists the five bugs the edge-case pass found. |
