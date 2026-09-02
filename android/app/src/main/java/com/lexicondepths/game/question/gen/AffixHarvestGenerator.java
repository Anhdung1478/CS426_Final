package com.lexicondepths.game.question.gen;

import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.QuestionGenerator;
import com.lexicondepths.game.question.QuestionResult;
import com.lexicondepths.game.question.QuestionType;

import java.util.ArrayList;
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

    public AffixHarvestGenerator(AffixKeySource keySource) {
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
        Set<String> target = keySource.wordsFor(word, pool);
        // correctAnswer is the target (how many are needed); options is the accepted set (what
        // counts). Separating them is what lets Datamuse add answers without adding difficulty —
        // options was already empty for this type, so no contract changed. See P3-8.
        String prompt = "Type as many words as you can that use \"" + word.affixKey + "\".";
        List<String> accepted = new ArrayList<>(keySource.acceptedFor(word, pool));
        return new Question(type(), word.id, prompt, accepted, -1, String.join(", ", target));
    }

    @Override
    public QuestionResult score(Question question, Answer answer) {
        String[] targetWords = question.correctAnswer.isEmpty()
                ? new String[0] : question.correctAnswer.split(", ");
        Set<String> target = new HashSet<>();
        Collections.addAll(target, targetWords);

        // Empty options means the target is also the accepted set — an offline key source.
        Set<String> accepted = question.options.isEmpty() ? target : new HashSet<>(question.options);

        Set<String> found = new HashSet<>();
        if (answer.text != null) {
            for (String token : answer.text.split("[^a-zA-Z]+")) {
                String lower = token.toLowerCase(Locale.ROOT);
                if (!lower.isEmpty() && accepted.contains(lower)) {
                    found.add(lower);
                }
            }
        }

        double ratio = target.isEmpty() ? 0.0 : Math.min(1.0, found.size() / (double) target.size());
        return new QuestionResult(question.wordId, ratio, answer.elapsedMillis, question.correctAnswer);
    }
}
