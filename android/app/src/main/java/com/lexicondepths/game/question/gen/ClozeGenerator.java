package com.lexicondepths.game.question.gen;

import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.QuestionGenerator;
import com.lexicondepths.game.question.QuestionResult;
import com.lexicondepths.game.question.QuestionType;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Void-eater: blanks the headword (or the form of it) out of the example sentence; the player
 * types the missing word. Gated to B1+ per the design doc — it needs grammatical maturity a
 * beginner doesn't have yet. Free text, scored by QuestionSupport.findExampleBlank finding
 * exactly one occurrence, so an ambiguous sentence is rejected here rather than at scoring time.
 */
public final class ClozeGenerator implements QuestionGenerator {

    @Override
    public QuestionType type() {
        return QuestionType.CLOZE;
    }

    @Override
    public boolean canGenerate(Word word) {
        return isEligibleBand(word.cefr) && QuestionSupport.findExampleBlank(word) != null;
    }

    @Override
    public Question generate(Word word, List<Word> pool, Random rng) {
        QuestionSupport.ExampleBlank exampleBlank = QuestionSupport.findExampleBlank(word);
        String prompt = QuestionSupport.blank(word.example, exampleBlank);
        return new Question(type(), word.id, prompt, Collections.emptyList(), -1, exampleBlank.candidate);
    }

    @Override
    public QuestionResult score(Question question, Answer answer) {
        String given = answer.text == null ? "" : answer.text.trim();
        double ratio = given.equalsIgnoreCase(question.correctAnswer) ? 1.0 : 0.0;
        return new QuestionResult(question.wordId, ratio, answer.elapsedMillis, question.correctAnswer);
    }

    private static boolean isEligibleBand(CefrLevel cefr) {
        return cefr == CefrLevel.B1 || cefr == CefrLevel.B2 || cefr == CefrLevel.C1 || cefr == CefrLevel.C2;
    }
}
