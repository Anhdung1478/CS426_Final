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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AnagramGeneratorTest {

    private final AnagramGenerator generator = new AnagramGenerator();

    @Test
    public void gatesOutWordsShorterThanThreeLetters() {
        Word ox = TestWords.word(1, "ox", CefrLevel.A1, "food", "an animal", "The ox is strong.");
        assertFalse(generator.canGenerate(ox));
    }

    @Test
    public void scramblesToADifferentOrderButSameLetters() {
        Word word = TestWords.word(1, "decide", CefrLevel.B1, "business", "to choose", "Decide now.");
        Question q = generator.generate(word, Collections.emptyList(), new Random(1));

        assertNotEquals(word.headword, q.prompt);
        char[] scrambled = q.prompt.toCharArray();
        char[] original = word.headword.toCharArray();
        Arrays.sort(scrambled);
        Arrays.sort(original);
        assertEquals(new String(original), new String(scrambled));
        assertEquals("decide", q.correctAnswer);
    }

    @Test
    public void scoresBinaryCaseInsensitively() {
        Word word = TestWords.word(1, "decide", CefrLevel.B1, "business", "to choose", "Decide now.");
        Question q = generator.generate(word, Collections.emptyList(), new Random(1));

        assertEquals(1.0, generator.score(q, Answer.ofText(" Decide ", 100)).ratio, 1e-9);
        assertEquals(0.0, generator.score(q, Answer.ofText("wrong", 100)).ratio, 1e-9);
    }

    @Test
    public void everyGeneratedLetterMultisetRoundTripsAcrossManySeeds() {
        Word word = TestWords.word(1, "palatable", CefrLevel.B2, "food", "tasty", "It was palatable.");
        for (int seed = 0; seed < 50; seed++) {
            Question q = generator.generate(word, Collections.emptyList(), new Random(seed));
            char[] scrambled = q.prompt.toCharArray();
            char[] original = word.headword.toCharArray();
            Arrays.sort(scrambled);
            Arrays.sort(original);
            assertTrue(Arrays.equals(original, scrambled));
        }
    }
}
