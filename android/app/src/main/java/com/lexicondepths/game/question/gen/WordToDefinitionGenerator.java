package com.lexicondepths.game.question.gen;

import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.QuestionGenerator;
import com.lexicondepths.game.question.QuestionResult;
import com.lexicondepths.game.question.QuestionType;

import java.util.List;
import java.util.Random;

/** The easier A1-A2 counterpart to Definition->Word: shows the headword, picks its definition. */
public final class WordToDefinitionGenerator implements QuestionGenerator {

    @Override
    public QuestionType type() {
        return QuestionType.WORD_TO_DEFINITION;
    }

    @Override
    public boolean canGenerate(Word word) {
        return word.definition != null && !word.definition.isEmpty();
    }

    @Override
    public Question generate(Word word, List<Word> pool, Random rng) {
        List<String> distractorPool = QuestionSupport.definitions(QuestionSupport.sameBandTopic(word, pool));
        QuestionSupport.McqOptions mcq = QuestionSupport.buildOptions(word.definition, distractorPool, rng);
        String prompt = "What does \"" + word.headword + "\" mean?";
        return new Question(type(), word.id, prompt, mcq.options, mcq.correctIndex, word.definition);
    }

    @Override
    public QuestionResult score(Question question, Answer answer) {
        return QuestionSupport.scoreMcq(question, answer);
    }
}
