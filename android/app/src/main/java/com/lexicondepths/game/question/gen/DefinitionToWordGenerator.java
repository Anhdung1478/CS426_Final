package com.lexicondepths.game.question.gen;

import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.QuestionGenerator;
import com.lexicondepths.game.question.QuestionResult;
import com.lexicondepths.game.question.QuestionType;

import java.util.List;
import java.util.Random;

/** Sphinx: shows the definition, the player picks the headword it describes. */
public final class DefinitionToWordGenerator implements QuestionGenerator {

    @Override
    public QuestionType type() {
        return QuestionType.DEFINITION_TO_WORD;
    }

    @Override
    public boolean canGenerate(Word word) {
        return word.definition != null && !word.definition.isEmpty();
    }

    @Override
    public Question generate(Word word, List<Word> pool, Random rng) {
        List<String> distractorPool = QuestionSupport.headwords(QuestionSupport.sameBandTopic(word, pool));
        QuestionSupport.McqOptions mcq = QuestionSupport.buildOptions(word.headword, distractorPool, rng);
        return new Question(type(), word.id, word.definition, mcq.options, mcq.correctIndex, word.headword);
    }

    @Override
    public QuestionResult score(Question question, Answer answer) {
        return QuestionSupport.scoreMcq(question, answer);
    }
}
