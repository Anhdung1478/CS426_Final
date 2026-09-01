package com.lexicondepths.game.question.gen;

import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.QuestionGenerator;
import com.lexicondepths.game.question.QuestionResult;
import com.lexicondepths.game.question.QuestionType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Cipher: guess the headword within 6 tries, with per-letter feedback after each. Deliberately
 * rare per the design doc — more game than learning. Answer.text carries every guess made so
 * far, comma-separated, in order; ratio is (7 - solvingGuessNumber) / 6, or 0.0 if the word was
 * never guessed within the first 6 entries.
 */
public final class WordleGenerator implements QuestionGenerator {

    public static final int MAX_GUESSES = 6;

    public enum LetterState { CORRECT, PRESENT, ABSENT }

    @Override
    public QuestionType type() {
        return QuestionType.WORDLE;
    }

    @Override
    public boolean canGenerate(Word word) {
        return word.headword != null && !word.headword.isEmpty();
    }

    @Override
    public Question generate(Word word, List<Word> pool, Random rng) {
        String prompt = "Definition: " + word.definition;
        return new Question(type(), word.id, prompt, Collections.emptyList(), -1, word.headword);
    }

    @Override
    public QuestionResult score(Question question, Answer answer) {
        String[] guesses = (answer.text == null || answer.text.isEmpty())
                ? new String[0]
                : answer.text.split(",");

        double ratio = 0.0;
        for (int i = 0; i < guesses.length && i < MAX_GUESSES; i++) {
            if (guesses[i].trim().equalsIgnoreCase(question.correctAnswer)) {
                ratio = (MAX_GUESSES + 1 - (i + 1)) / (double) MAX_GUESSES;
                break;
            }
        }
        return new QuestionResult(question.wordId, ratio, answer.elapsedMillis, question.correctAnswer);
    }

    /**
     * Classic two-pass Wordle feedback: exact-position matches consume their target letter
     * first, then leftover guess letters are checked against the *remaining* letter counts —
     * this is what stops a guess with more copies of a letter than the target holds from being
     * marked PRESENT/CORRECT more times than the target actually has (e.g. SPEED vs ERASE).
     * guess and target must be the same length.
     */
    public static LetterState[] feedback(String guess, String target) {
        int length = target.length();
        LetterState[] states = new LetterState[length];
        Map<Character, Integer> remaining = new HashMap<>();

        for (int i = 0; i < length; i++) {
            char t = target.charAt(i);
            if (guess.charAt(i) == t) {
                states[i] = LetterState.CORRECT;
            } else {
                remaining.merge(t, 1, Integer::sum);
            }
        }
        for (int i = 0; i < length; i++) {
            if (states[i] == LetterState.CORRECT) {
                continue;
            }
            char g = guess.charAt(i);
            Integer count = remaining.get(g);
            if (count != null && count > 0) {
                states[i] = LetterState.PRESENT;
                remaining.put(g, count - 1);
            } else {
                states[i] = LetterState.ABSENT;
            }
        }
        return states;
    }
}
