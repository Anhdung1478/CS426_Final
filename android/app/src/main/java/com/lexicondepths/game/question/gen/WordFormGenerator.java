package com.lexicondepths.game.question.gen;

import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.QuestionGenerator;
import com.lexicondepths.game.question.QuestionResult;
import com.lexicondepths.game.question.QuestionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mimic: blanks whichever form of the word (headword or one of its `forms`) actually appears
 * in its example sentence, then asks the player to pick the form that fits that grammatical
 * slot from the word's own other forms. The sentence disambiguates which form is correct.
 */
public final class WordFormGenerator implements QuestionGenerator {

    @Override
    public QuestionType type() {
        return QuestionType.WORD_FORM;
    }

    @Override
    public boolean canGenerate(Word word) {
        return word.forms != null && !word.forms.isEmpty() && QuestionSupport.findExampleBlank(word) != null;
    }

    @Override
    public Question generate(Word word, List<Word> pool, Random rng) {
        QuestionSupport.ExampleBlank exampleBlank = QuestionSupport.findExampleBlank(word);
        String prompt = QuestionSupport.blank(word.example, exampleBlank);

        List<String> ownForms = new ArrayList<>();
        ownForms.add(word.headword);
        ownForms.addAll(word.forms);
        ownForms.remove(exampleBlank.candidate);

        QuestionSupport.McqOptions mcq = QuestionSupport.buildOptions(exampleBlank.candidate, ownForms, rng);
        return new Question(type(), word.id, prompt, mcq.options, mcq.correctIndex, exampleBlank.candidate);
    }

    @Override
    public QuestionResult score(Question question, Answer answer) {
        return QuestionSupport.scoreMcq(question, answer);
    }
}
