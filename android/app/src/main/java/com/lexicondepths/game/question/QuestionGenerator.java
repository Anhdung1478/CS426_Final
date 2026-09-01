package com.lexicondepths.game.question;

import com.lexicondepths.db.entity.Word;

import java.util.List;
import java.util.Random;

/**
 * One implementation per QuestionType, in game/question/gen/. Every type scores through the
 * same 0.0-1.0 completion ratio (see QuestionResult) so the damage formula never special-cases
 * a question shape.
 */
public interface QuestionGenerator {

    QuestionType type();

    /** False when a field this type needs (definition, forms, collocations, ...) is missing. */
    boolean canGenerate(Word word);

    /**
     * pool is the candidate word list generation may pull distractors from — same CEFR band
     * and topic as word, per the design doc. rng is passed in rather than constructed inside,
     * which is what makes generation reproducible from a run seed and the tests deterministic.
     */
    Question generate(Word word, List<Word> pool, Random rng);

    QuestionResult score(Question question, Answer answer);
}
