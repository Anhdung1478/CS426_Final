package com.lexicondepths.game.question.gen;

import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.QuestionGenerator;
import com.lexicondepths.game.question.QuestionResult;
import com.lexicondepths.game.question.QuestionType;

import java.util.List;
import java.util.Random;

/**
 * Chimera: picks one of the word's own collocations as correct, distractors are other
 * same-band/topic words' collocations. Called out in the design doc as the hardest area for
 * Vietnamese learners (L1 interference) and the least gamified elsewhere — worth the care.
 */
public final class CollocationGenerator implements QuestionGenerator {

    @Override
    public QuestionType type() {
        return QuestionType.COLLOCATION;
    }

    @Override
    public boolean canGenerate(Word word) {
        return word.collocations != null && !word.collocations.isEmpty();
    }

    @Override
    public Question generate(Word word, List<Word> pool, Random rng) {
        String correct = word.collocations.get(rng.nextInt(word.collocations.size()));
        List<String> distractorPool = QuestionSupport.collocations(QuestionSupport.sameBandTopic(word, pool));
        QuestionSupport.McqOptions mcq = QuestionSupport.buildOptions(correct, distractorPool, rng);
        String prompt = "Which phrase correctly uses \"" + word.headword + "\"?";
        return new Question(type(), word.id, prompt, mcq.options, mcq.correctIndex, correct);
    }

    @Override
    public QuestionResult score(Question question, Answer answer) {
        return QuestionSupport.scoreMcq(question, answer);
    }
}
