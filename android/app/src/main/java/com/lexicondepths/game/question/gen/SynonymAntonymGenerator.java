package com.lexicondepths.game.question.gen;

import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.QuestionGenerator;
import com.lexicondepths.game.question.QuestionResult;
import com.lexicondepths.game.question.QuestionType;

import java.util.List;
import java.util.Random;

/** Twins: picks a synonym or an antonym, whichever the word has data for (both if it has both). */
public final class SynonymAntonymGenerator implements QuestionGenerator {

    @Override
    public QuestionType type() {
        return QuestionType.SYNONYM_ANTONYM;
    }

    @Override
    public boolean canGenerate(Word word) {
        return !isEmpty(word.synonyms) || !isEmpty(word.antonyms);
    }

    @Override
    public Question generate(Word word, List<Word> pool, Random rng) {
        boolean useSynonym = pickSynonymMode(word, rng);
        List<String> candidates = useSynonym ? word.synonyms : word.antonyms;
        String correct = candidates.get(rng.nextInt(candidates.size()));

        List<String> distractorPool = QuestionSupport.headwords(QuestionSupport.sameBandTopic(word, pool));
        QuestionSupport.McqOptions mcq = QuestionSupport.buildOptions(correct, distractorPool, rng);

        String label = useSynonym ? "synonym" : "antonym";
        String prompt = "Choose a " + label + " for \"" + word.headword + "\".";
        return new Question(type(), word.id, prompt, mcq.options, mcq.correctIndex, correct);
    }

    @Override
    public QuestionResult score(Question question, Answer answer) {
        return QuestionSupport.scoreMcq(question, answer);
    }

    private boolean pickSynonymMode(Word word, Random rng) {
        boolean hasSynonym = !isEmpty(word.synonyms);
        boolean hasAntonym = !isEmpty(word.antonyms);
        if (hasSynonym && hasAntonym) {
            return rng.nextBoolean();
        }
        return hasSynonym;
    }

    private static boolean isEmpty(List<String> list) {
        return list == null || list.isEmpty();
    }
}
