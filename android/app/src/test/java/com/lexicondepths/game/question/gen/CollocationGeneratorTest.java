package com.lexicondepths.game.question.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class CollocationGeneratorTest {

    private final CollocationGenerator generator = new CollocationGenerator();

    @Test
    public void picksOneOfTheWordsOwnCollocationsAsCorrect() {
        Word target = TestWords.word(1, "decide", CefrLevel.B1, "business",
                "to choose something", "You must decide now.");
        target.collocations = Arrays.asList("make a decision", "decide on", "decide against");
        List<Word> pool = TestWords.poolAround(target, 5);
        for (Word w : pool) {
            if (w != target) {
                w.collocations = Arrays.asList("other phrase for " + w.headword);
            }
        }

        Question q = generator.generate(target, pool, new Random(3));

        assertTrue(target.collocations.contains(q.correctAnswer));
        assertTrue(q.options.contains(q.correctAnswer));
    }

    @Test
    public void scoresBinary() {
        Word target = TestWords.word(1, "decide", CefrLevel.B1, "business",
                "to choose something", "You must decide now.");
        target.collocations = Arrays.asList("make a decision");
        List<Word> pool = TestWords.poolAround(target, 5);
        for (Word w : pool) {
            if (w != target) {
                w.collocations = Arrays.asList("other phrase for " + w.headword);
            }
        }
        Question q = generator.generate(target, pool, new Random(6));

        assertEquals(1.0, generator.score(q, Answer.ofOption(q.correctOptionIndex, 100)).ratio, 1e-9);
        int wrongIndex = (q.correctOptionIndex + 1) % q.options.size();
        assertEquals(0.0, generator.score(q, Answer.ofOption(wrongIndex, 100)).ratio, 1e-9);
    }

    @Test
    public void cannotGenerateWithoutCollocations() {
        Word target = TestWords.word(1, "x", CefrLevel.B1, "business", "def", "example.");
        assertFalse(generator.canGenerate(target));
    }
}
