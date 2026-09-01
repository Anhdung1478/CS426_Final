package com.lexicondepths.game.run;

import com.lexicondepths.content.Monster;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.QuestionGenerator;
import com.lexicondepths.game.question.QuestionType;
import com.lexicondepths.game.question.gen.QuestionGenerators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * A monster's question-type list is permanent — only the words change (see phase-2.md P2-8).
 * Slots are grouped by type in the order the monster declares its types, with any remainder
 * slots going to the earlier types: for a 5-slot / 3-type monster that's 2/2/1, which is exactly
 * the boss "two of A, two of B, then a finisher of C" shape — no separate boss-phase code needed,
 * it falls out of the same distribution used for every monster.
 *
 * pool is the caller's candidate word list (already filtered to the run's topic/CEFR by
 * WordDao); dueWordIds is the caller's SRS-due set. Encounter stays a pure, Android-free
 * function — owning "which words are due" is RunEngine's (P2-9) job, the same reasoning
 * game/combat/Damage.java already follows for isFirstMissThisRun.
 */
public final class Encounter {

    public final List<Slot> slots;

    private Encounter(List<Slot> slots) {
        this.slots = Collections.unmodifiableList(slots);
    }

    public static final class Slot {
        public final QuestionType type;
        public final Word word;
        public final QuestionGenerator generator;

        Slot(QuestionType type, Word word, QuestionGenerator generator) {
            this.type = type;
            this.word = word;
            this.generator = generator;
        }
    }

    public static Encounter build(Monster monster, List<Word> pool, Set<Long> dueWordIds, Random rng) {
        List<QuestionType> declaredTypes = parseTypes(monster.questionTypes);
        List<QuestionType> slotTypeSequence = distribute(declaredTypes, monster.slots);

        List<Slot> slots = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        for (QuestionType type : slotTypeSequence) {
            Slot slot = pickSlot(type, declaredTypes, pool, dueWordIds, used, rng);
            if (slot != null) {
                slots.add(slot);
                used.add(slot.word.id);
            }
            // A type with no eligible word left in the pool falls back to nothing rather than
            // crashing — the slot is simply dropped, so the encounter has fewer than declared.
        }
        return new Encounter(slots);
    }

    private static Slot pickSlot(QuestionType preferredType, List<QuestionType> declaredTypes, List<Word> pool,
                                  Set<Long> dueWordIds, Set<Long> used, Random rng) {
        Word word = pickWord(preferredType, pool, dueWordIds, used, rng);
        if (word != null) {
            return new Slot(preferredType, word, QuestionGenerators.forType(preferredType));
        }
        for (QuestionType fallbackType : declaredTypes) {
            if (fallbackType == preferredType) {
                continue;
            }
            word = pickWord(fallbackType, pool, dueWordIds, used, rng);
            if (word != null) {
                return new Slot(fallbackType, word, QuestionGenerators.forType(fallbackType));
            }
        }
        return null;
    }

    private static Word pickWord(QuestionType type, List<Word> pool, Set<Long> dueWordIds,
                                  Set<Long> used, Random rng) {
        QuestionGenerator generator = QuestionGenerators.forType(type);
        if (generator == null) {
            return null;
        }
        List<Word> due = new ArrayList<>();
        List<Word> rest = new ArrayList<>();
        for (Word word : pool) {
            if (used.contains(word.id) || !generator.canGenerate(word)) {
                continue;
            }
            (dueWordIds.contains(word.id) ? due : rest).add(word);
        }
        Collections.shuffle(due, rng);
        Collections.shuffle(rest, rng);
        if (!due.isEmpty()) {
            return due.get(0);
        }
        return rest.isEmpty() ? null : rest.get(0);
    }

    /** Slot count split across types, remainder going to the earlier types in declaration order. */
    private static List<QuestionType> distribute(List<QuestionType> types, int slotCount) {
        List<QuestionType> sequence = new ArrayList<>();
        if (types.isEmpty()) {
            return sequence;
        }
        int base = slotCount / types.size();
        int remainder = slotCount % types.size();
        for (int i = 0; i < types.size(); i++) {
            int count = base + (i < remainder ? 1 : 0);
            for (int j = 0; j < count; j++) {
                sequence.add(types.get(i));
            }
        }
        return sequence;
    }

    private static List<QuestionType> parseTypes(List<String> raw) {
        List<QuestionType> types = new ArrayList<>();
        for (String name : raw) {
            try {
                types.add(QuestionType.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Malformed content-file entry — skip rather than crash the run.
            }
        }
        return types;
    }
}
