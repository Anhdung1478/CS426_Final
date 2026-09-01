package com.lexicondepths.game.question.gen;

import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.QuestionGenerator;
import com.lexicondepths.game.question.QuestionResult;
import com.lexicondepths.game.question.QuestionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A1-A2 warm-up: reorder the word's own example sentence. The scrambled tokens ride in
 * Question.options — an ordering puzzle, not a selection — so the eventual OrderingView (P2-7)
 * can render them as tappable chips; the player's answer is the reconstructed sentence text.
 */
public final class SentenceScrambleGenerator implements QuestionGenerator {

    private static final int MIN_TOKENS = 4;

    @Override
    public QuestionType type() {
        return QuestionType.SENTENCE_SCRAMBLE;
    }

    @Override
    public boolean canGenerate(Word word) {
        return isEligibleBand(word.cefr) && tokenize(word.example).size() >= MIN_TOKENS;
    }

    @Override
    public Question generate(Word word, List<Word> pool, Random rng) {
        List<String> tokens = tokenize(word.example);
        String original = String.join(" ", tokens);

        List<String> scrambled = new ArrayList<>(tokens);
        int attempts = 0;
        do {
            Collections.shuffle(scrambled, rng);
            attempts++;
        } while (String.join(" ", scrambled).equals(original) && attempts < 10);

        return new Question(type(), word.id, "Put the sentence back in order.", scrambled, -1, original);
    }

    @Override
    public QuestionResult score(Question question, Answer answer) {
        String given = answer.text == null ? "" : answer.text.trim();
        double ratio = given.equalsIgnoreCase(question.correctAnswer) ? 1.0 : 0.0;
        return new QuestionResult(question.wordId, ratio, answer.elapsedMillis, question.correctAnswer);
    }

    private static List<String> tokenize(String sentence) {
        String trimmed = sentence.trim();
        return trimmed.isEmpty() ? Collections.emptyList() : Arrays.asList(trimmed.split("\\s+"));
    }

    private static boolean isEligibleBand(CefrLevel cefr) {
        return cefr == CefrLevel.A1 || cefr == CefrLevel.A2;
    }
}
