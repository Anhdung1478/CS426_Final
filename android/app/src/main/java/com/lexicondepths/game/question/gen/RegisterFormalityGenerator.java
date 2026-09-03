package com.lexicondepths.game.question.gen;

import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.QuestionGenerator;
import com.lexicondepths.game.question.QuestionResult;
import com.lexicondepths.game.question.QuestionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Type 12 of 12, deferred at Phase 2 close. "Rewrite for a formal register" — the word's own
 * example sentence as context, four same-register options, one of them right.
 *
 * Driven by Word.formalAlt, a nullable field set on a minority of seed words. That is the
 * affixKey pattern exactly, and reusing it means the gating needs no new code: canGenerate
 * returns false without the field, the type only ever appears via a monster that declares it,
 * and Encounter already handles a declared type with no eligible word. There is no CEFR check
 * in here at all.
 *
 * project-idea.md §5 gates this at C1+. The seed word bank is 76 A1 / 76 A2 / 76 B1 / 72 B2
 * with zero C1 and zero C2, so a C1 gate would make the type unreachable outside a forged C1
 * realm — a question type that cannot appear in a demo is not shipped. The seed tags formalAlt
 * on B2 words instead. The deviation is deliberate and is recorded in docs/phase-4.md.
 */
public final class RegisterFormalityGenerator implements QuestionGenerator {

    /**
     * Padding for a pool too thin to supply three real distractors — canGenerate sees one Word
     * and cannot know how many others carry formalAlt. Every entry is itself a formal register
     * word, so a padded question is still four options at one register, never a giveaway.
     */
    private static final List<String> FORMAL_FALLBACKS = Arrays.asList(
            "commence", "purchase", "assist", "obtain", "require", "conclude", "reside", "inquire");

    @Override
    public QuestionType type() {
        return QuestionType.REGISTER_FORMALITY;
    }

    @Override
    public boolean canGenerate(Word word) {
        if (word.formalAlt == null || word.formalAlt.trim().isEmpty()) {
            return false;
        }
        // The prompt asks which word in this sentence to replace, so the sentence has to
        // contain it. P3-1 already validates this rule, so forged words satisfy it by
        // construction; a hand-edited seed row might not.
        return word.example != null && containsHeadword(word);
    }

    @Override
    public Question generate(Word word, List<Word> pool, Random rng) {
        String correct = word.formalAlt.trim();
        String prompt = "Rewrite for a formal register: \"" + word.example
                + "\" — which word replaces \"" + word.headword + "\"?";
        QuestionSupport.McqOptions mcq =
                QuestionSupport.buildOptions(correct, distractors(word, pool, correct), rng);
        return new Question(type(), word.id, prompt, mcq.options, mcq.correctIndex, correct);
    }

    @Override
    public QuestionResult score(Question question, Answer answer) {
        return QuestionSupport.scoreMcq(question, answer);
    }

    /**
     * Other words' formalAlt values, never their headwords. A distractor at a different
     * register is one a learner can eliminate without knowing the answer, which teaches
     * nothing — the whole difficulty of this question type is that all four options are formal.
     */
    private static List<String> distractors(Word word, List<Word> pool, String correct) {
        List<String> candidates = new ArrayList<>();
        for (Word other : pool) {
            if (other.id == word.id || other.formalAlt == null) {
                continue;
            }
            String alt = other.formalAlt.trim();
            if (!alt.isEmpty() && !alt.equalsIgnoreCase(correct)) {
                candidates.add(alt);
            }
        }
        if (candidates.size() < QuestionSupport.OPTION_COUNT - 1) {
            for (String fallback : FORMAL_FALLBACKS) {
                if (!fallback.equalsIgnoreCase(correct)) {
                    candidates.add(fallback);
                }
            }
        }
        return candidates;
    }

    private static boolean containsHeadword(Word word) {
        String firstToken = word.headword.toLowerCase(Locale.ROOT).split("\\s+")[0];
        return word.example.toLowerCase(Locale.ROOT).contains(firstToken);
    }
}
