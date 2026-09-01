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

public class WordFormGeneratorTest {

    private final WordFormGenerator generator = new WordFormGenerator();

    @Test
    public void blanksTheFormThatAppearsInTheExampleAndOffersItsOwnOtherForms() {
        Word target = TestWords.word(1, "decide", CefrLevel.B1, "business",
                "to choose something", "They decided to open a second branch.");
        target.forms = Arrays.asList("decided", "decision", "decisive");
        List<Word> pool = TestWords.poolAround(target, 10);

        Question q = generator.generate(target, pool, new Random(2));

        assertEquals("decided", q.correctAnswer);
        assertTrue(q.prompt.contains("____"));
        assertFalse(q.prompt.contains("decided"));
        assertTrue(q.options.contains("decided"));
        for (String option : q.options) {
            assertTrue(option.equals("decide") || target.forms.contains(option));
        }
    }

    @Test
    public void scoresBinary() {
        Word target = TestWords.word(1, "decide", CefrLevel.B1, "business",
                "to choose something", "They decided to open a second branch.");
        target.forms = Arrays.asList("decided", "decision", "decisive");
        List<Word> pool = TestWords.poolAround(target, 10);
        Question q = generator.generate(target, pool, new Random(4));

        assertEquals(1.0, generator.score(q, Answer.ofOption(q.correctOptionIndex, 100)).ratio, 1e-9);
        int wrongIndex = (q.correctOptionIndex + 1) % q.options.size();
        assertEquals(0.0, generator.score(q, Answer.ofOption(wrongIndex, 100)).ratio, 1e-9);
    }

    @Test
    public void cannotGenerateWithoutForms() {
        Word target = TestWords.word(1, "decide", CefrLevel.B1, "business",
                "to choose something", "They decided to open a second branch.");
        assertFalse(generator.canGenerate(target));
    }

    @Test
    public void cannotGenerateWhenNoFormAppearsInTheExample() {
        Word target = TestWords.word(1, "decide", CefrLevel.B1, "business",
                "to choose something", "This sentence mentions none of the candidates.");
        target.forms = Arrays.asList("decided", "decision");
        assertFalse(generator.canGenerate(target));
    }
}
