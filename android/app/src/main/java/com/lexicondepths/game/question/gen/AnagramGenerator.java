package com.lexicondepths.game.question.gen;

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
 * Filler/pacing puzzle: the headword's own letters, shuffled. Free text, scored binary.
 * Requires at least 3 letters so the scramble isn't trivially reversible by inspection.
 */
public final class AnagramGenerator implements QuestionGenerator {

    private static final int MIN_LENGTH = 3;

    @Override
    public QuestionType type() {
        return QuestionType.ANAGRAM;
    }

    @Override
    public boolean canGenerate(Word word) {
        return word.headword != null && word.headword.length() >= MIN_LENGTH;
    }

    @Override
    public Question generate(Word word, List<Word> pool, Random rng) {
        char[] letters = word.headword.toCharArray();
        String scrambled;
        int attempts = 0;
        do {
            shuffle(letters, rng);
            scrambled = new String(letters);
            attempts++;
        } while (scrambled.equalsIgnoreCase(word.headword) && attempts < 10);

        return new Question(type(), word.id, scrambled, Collections.emptyList(), -1, word.headword);
    }

    @Override
    public QuestionResult score(Question question, Answer answer) {
        String given = answer.text == null ? "" : answer.text.trim();
        double ratio = given.equalsIgnoreCase(question.correctAnswer) ? 1.0 : 0.0;
        return new QuestionResult(question.wordId, ratio, answer.elapsedMillis, question.correctAnswer);
    }

    private static void shuffle(char[] letters, Random rng) {
        for (int i = letters.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = letters[i];
            letters[i] = letters[j];
            letters[j] = tmp;
        }
    }
}
