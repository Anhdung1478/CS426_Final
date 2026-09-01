# Lexicon Depths

A roguelike English vocabulary game for Android, aimed at intermediate Vietnamese ESL learners. Word puzzles (spelling, cloze, definition-matching, collocation, ...) are reskinned as dungeon combat, and an SM-2 spaced-repetition scheduler decides which words appear, disguised as difficulty tuning.

Dying in a run never rolls back what the player learned — permadeath only clears run-scoped state (HP, floor, relics), never SRS mastery data.

## Status

Phase 1 (Foundation) and Phase 2 (The playable run) are closed — 24/24 tasks. The app seeds a word bank, runs SM-2 flashcard review, and plays a full 3-floor dungeon run with 9 of 12 question types, in English and Vietnamese. Phase 3 (AI map generation via a backend proxy) is next, not yet started.

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
| [`docs/phase-1.md`](docs/phase-1.md) / [`docs/phase-2.md`](docs/phase-2.md) | Per-task detail for each closed phase. |
| [`report-phase1.md`](report-phase1.md) / [`report-phase2.md`](report-phase2.md) | Exit-checklist writeups for each phase. |
