package com.lexicondepths.game.question.gen;

import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.QuestionGenerator;
import com.lexicondepths.game.question.QuestionResult;
import com.lexicondepths.game.question.QuestionType;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Echo: TTS speaks the headword — ui/widget/Speaker plays it, this generator only supplies the
 * target text via correctAnswer — and the player types what they heard. Scored by Levenshtein
 * distance rather than binary, so a one-letter slip isn't a total loss.
 */
public final class ListeningSpellingGenerator implements QuestionGenerator {

    @Override
    public QuestionType type() {
        return QuestionType.LISTENING_SPELLING;
    }

    @Override
    public boolean canGenerate(Word word) {
        return word.headword != null && !word.headword.isEmpty();
    }

    @Override
    public Question generate(Word word, List<Word> pool, Random rng) {
        return new Question(type(), word.id, "Listen and type what you hear.",
                Collections.emptyList(), -1, word.headword);
    }

    @Override
    public QuestionResult score(Question question, Answer answer) {
        String given = (answer.text == null ? "" : answer.text.trim()).toLowerCase(Locale.ROOT);
        String target = question.correctAnswer.toLowerCase(Locale.ROOT);
        int distance = levenshtein(given, target);
        double ratio = 1.0 - (distance / (double) target.length());
        return new QuestionResult(question.wordId, ratio, answer.elapsedMillis, question.correctAnswer);
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }
}
