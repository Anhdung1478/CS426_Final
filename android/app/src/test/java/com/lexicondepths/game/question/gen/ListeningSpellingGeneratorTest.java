package com.lexicondepths.game.question.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;

import org.junit.Test;

import java.util.Collections;
import java.util.Random;

public class ListeningSpellingGeneratorTest {

    private final ListeningSpellingGenerator generator = new ListeningSpellingGenerator();

    @Test
    public void exactSpellingScoresOne() {
        Word word = TestWords.word(1, "decide", CefrLevel.B1, "business", "to choose", "Decide now.");
        Question q = generator.generate(word, Collections.emptyList(), new Random(1));

        assertEquals(1.0, generator.score(q, Answer.ofText(" Decide ", 100)).ratio, 1e-9);
    }

    @Test
    public void oneLetterSlipIsNotATotalLoss() {
        Word word = TestWords.word(1, "decide", CefrLevel.B1, "business", "to choose", "Decide now.");
        Question q = generator.generate(word, Collections.emptyList(), new Random(1));

        double ratio = generator.score(q, Answer.ofText("decade", 100)).ratio;
        assertTrue("expected partial credit, got " + ratio, ratio > 0.0 && ratio < 1.0);
    }

    @Test
    public void completelyWrongAnswerFloorsAtZero() {
        Word word = TestWords.word(1, "cat", CefrLevel.A1, "food", "an animal", "The cat sat.");
        Question q = generator.generate(word, Collections.emptyList(), new Random(1));

        double ratio = generator.score(q, Answer.ofText("xyzxyzxyzxyz", 100)).ratio;
        assertEquals(0.0, ratio, 1e-9);
    }
}
