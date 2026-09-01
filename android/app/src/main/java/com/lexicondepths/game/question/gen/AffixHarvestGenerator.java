package com.lexicondepths.game.question.gen;

import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.QuestionGenerator;
import com.lexicondepths.game.question.QuestionResult;
import com.lexicondepths.game.question.QuestionType;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Hydra: given an affix (e.g. "un-"), the player types as many words as they can that use it.
 * Partial-credit showcase: ratio is found/keySize. The key set comes from AffixKeySource — see
 * that interface for why it's not just inlined here. Answer.text carries every distinct word the
 * player typed, in any delimiter (comma, space, newline); score() tokenizes on runs of non-letters.
 */
public final class AffixHarvestGenerator implements QuestionGenerator {

    private final AffixKeySource keySource;

    public AffixHarvestGenerator() {
        this(new OfflineAffixKeySource());
    }

    AffixHarvestGenerator(AffixKeySource keySource) {
        this.keySource = keySource;
    }

    @Override
    public QuestionType type() {
        return QuestionType.AFFIX_HARVEST;
    }

    @Override
    public boolean canGenerate(Word word) {
        return word.affixKey != null && !word.affixKey.trim().isEmpty();
    }

    @Override
    public Question generate(Word word, List<Word> pool, Random rng) {
        Set<String> key = keySource.wordsFor(word, pool);
        String prompt = "Type as many words as you can that use \"" + word.affixKey + "\".";
        String correctAnswer = String.join(", ", key);
        return new Question(type(), word.id, prompt, Collections.emptyList(), -1, correctAnswer);
    }

    @Override
    public QuestionResult score(Question question, Answer answer) {
        String[] keyWords = question.correctAnswer.isEmpty() ? new String[0] : question.correctAnswer.split(", ");
        Set<String> key = new HashSet<>();
        Collections.addAll(key, keyWords);

        Set<String> found = new HashSet<>();
        if (answer.text != null) {
            for (String token : answer.text.split("[^a-zA-Z]+")) {
                if (!token.isEmpty() && key.contains(token.toLowerCase(Locale.ROOT))) {
                    found.add(token.toLowerCase(Locale.ROOT));
                }
            }
        }

        double ratio = key.isEmpty() ? 0.0 : Math.min(1.0, found.size() / (double) key.size());
        return new QuestionResult(question.wordId, ratio, answer.elapsedMillis, question.correctAnswer);
    }
}
