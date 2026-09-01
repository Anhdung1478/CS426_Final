package com.lexicondepths.game.question.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;

import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SentenceScrambleGeneratorTest {

    private final SentenceScrambleGenerator generator = new SentenceScrambleGenerator();

    @Test
    public void gatesOutBandsAboveA2() {
        Word b1 = TestWords.word(1, "cat", CefrLevel.B1, "food", "an animal", "The cat sat on the warm mat today.");
        assertFalse(generator.canGenerate(b1));
    }

    @Test
    public void gatesOutTooShortSentences() {
        Word word = TestWords.word(1, "cat", CefrLevel.A1, "food", "an animal", "Cats sleep.");
        assertFalse(generator.canGenerate(word));
    }

    @Test
    public void allowsA1AndA2WithEnoughTokens() {
        Word word = TestWords.word(1, "cat", CefrLevel.A2, "food", "an animal", "The cat sat on the mat.");
        assertTrue(generator.canGenerate(word));
    }

    @Test
    public void scrambledOptionsAreAPermutationOfTheOriginalTokens() {
        Word word = TestWords.word(1, "cat", CefrLevel.A1, "food", "an animal", "The cat sat on the warm mat.");
        Question q = generator.generate(word, Collections.emptyList(), new Random(7));

        List<String> options = q.options;
        assertEquals(7, options.size());
        assertNotEquals(String.join(" ", options), q.correctAnswer);
        assertEquals("The cat sat on the warm mat.", q.correctAnswer);

        List<String> sortedOptions = new java.util.ArrayList<>(options);
        List<String> sortedOriginal = java.util.Arrays.asList(q.correctAnswer.split("\\s+"));
        Collections.sort(sortedOptions);
        List<String> sortedOriginalCopy = new java.util.ArrayList<>(sortedOriginal);
        Collections.sort(sortedOriginalCopy);
        assertEquals(sortedOriginalCopy, sortedOptions);
    }

    @Test
    public void scoresExactReconstructionOnlyCaseInsensitively() {
        Word word = TestWords.word(1, "cat", CefrLevel.A1, "food", "an animal", "The cat sat on the warm mat.");
        Question q = generator.generate(word, Collections.emptyList(), new Random(7));

        assertEquals(1.0, generator.score(q, Answer.ofText(" the cat sat on the warm mat. ", 100)).ratio, 1e-9);
        assertEquals(0.0, generator.score(q, Answer.ofText("sat the cat on the warm mat.", 100)).ratio, 1e-9);
    }
}
