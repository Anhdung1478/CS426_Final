package com.lexicondepths.game.question.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;

import org.junit.Test;

import java.util.Random;

public class ClozeGeneratorTest {

    private final ClozeGenerator generator = new ClozeGenerator();

    @Test
    public void gatesOutA1AndA2() {
        Word a1 = TestWords.word(1, "decide", CefrLevel.A1, "business", "to choose", "You must decide now.");
        Word a2 = TestWords.word(2, "decide", CefrLevel.A2, "business", "to choose", "You must decide now.");
        assertFalse(generator.canGenerate(a1));
        assertFalse(generator.canGenerate(a2));
    }

    @Test
    public void allowsB1AndAbove() {
        Word b1 = TestWords.word(1, "decide", CefrLevel.B1, "business", "to choose", "You must decide now.");
        Word b2 = TestWords.word(2, "decide", CefrLevel.B2, "business", "to choose", "You must decide now.");
        Word c1 = TestWords.word(3, "decide", CefrLevel.C1, "business", "to choose", "You must decide now.");
        assertTrue(generator.canGenerate(b1));
        assertTrue(generator.canGenerate(b2));
        assertTrue(generator.canGenerate(c1));
    }

    @Test
    public void rejectsAnAmbiguousBlankAtGenerationTime() {
        Word word = TestWords.word(1, "order", CefrLevel.B1, "food",
                "to request food", "He gave the order, then a second order.");
        assertFalse(generator.canGenerate(word));
    }

    @Test
    public void blanksTheHeadwordAndScoresCaseInsensitively() {
        Word word = TestWords.word(1, "decide", CefrLevel.B1, "business",
                "to choose something", "You must decide now.");
        Question q = generator.generate(word, java.util.Collections.emptyList(), new Random(1));

        assertEquals("decide", q.correctAnswer);
        assertTrue(q.prompt.contains("____"));
        assertFalse(q.prompt.toLowerCase().contains("decide"));

        assertEquals(1.0, generator.score(q, Answer.ofText(" Decide ", 500)).ratio, 1e-9);
        assertEquals(0.0, generator.score(q, Answer.ofText("choose", 500)).ratio, 1e-9);
    }
}
