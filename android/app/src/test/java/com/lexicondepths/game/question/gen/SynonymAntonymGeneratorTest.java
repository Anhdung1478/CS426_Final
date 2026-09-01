package com.lexicondepths.game.question.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Question;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SynonymAntonymGeneratorTest {

    private final SynonymAntonymGenerator generator = new SynonymAntonymGenerator();

    @Test
    public void usesSynonymWhenOnlySynonymsExist() {
        Word target = TestWords.word(1, "brave", CefrLevel.A1, "emotions", "not afraid", "She was brave.");
        target.synonyms = Arrays.asList("courageous");
        List<Word> pool = TestWords.poolAround(target, 10);

        Question q = generator.generate(target, pool, new Random(1));

        assertEquals("courageous", q.correctAnswer);
        assertTrue(q.prompt.contains("synonym"));
    }

    @Test
    public void usesAntonymWhenOnlyAntonymsExist() {
        Word target = TestWords.word(1, "brave", CefrLevel.A1, "emotions", "not afraid", "She was brave.");
        target.antonyms = Arrays.asList("afraid");
        List<Word> pool = TestWords.poolAround(target, 10);

        Question q = generator.generate(target, pool, new Random(1));

        assertEquals("afraid", q.correctAnswer);
        assertTrue(q.prompt.contains("antonym"));
    }

    @Test
    public void picksBothModesOverManyGenerationsWhenBothExist() {
        Word target = TestWords.word(1, "brave", CefrLevel.A1, "emotions", "not afraid", "She was brave.");
        target.synonyms = Arrays.asList("courageous");
        target.antonyms = Arrays.asList("afraid");
        List<Word> pool = TestWords.poolAround(target, 10);

        boolean sawSynonym = false;
        boolean sawAntonym = false;
        Random rng = new Random(5);
        for (int i = 0; i < 50; i++) {
            Question q = generator.generate(target, pool, rng);
            sawSynonym |= q.correctAnswer.equals("courageous");
            sawAntonym |= q.correctAnswer.equals("afraid");
        }
        assertTrue(sawSynonym);
        assertTrue(sawAntonym);
    }

    @Test
    public void cannotGenerateWithNeitherSynonymsNorAntonyms() {
        Word target = TestWords.word(1, "brave", CefrLevel.A1, "emotions", "not afraid", "She was brave.");
        assertFalse(generator.canGenerate(target));
    }
}
