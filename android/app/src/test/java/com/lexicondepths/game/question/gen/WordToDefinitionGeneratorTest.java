package com.lexicondepths.game.question.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;

import org.junit.Test;

import java.util.List;
import java.util.Random;

public class WordToDefinitionGeneratorTest {

    private final WordToDefinitionGenerator generator = new WordToDefinitionGenerator();

    @Test
    public void promptShowsTheHeadwordAndOptionsAreDefinitions() {
        Word target = TestWords.word(1, "decide", CefrLevel.A2, "business",
                "to choose something", "You must decide now.");
        List<Word> pool = TestWords.poolAround(target, 10);

        Question q = generator.generate(target, pool, new Random(3));

        assertTrue(q.prompt.contains("decide"));
        assertEquals(target.definition, q.correctAnswer);
        assertTrue(q.options.contains(target.definition));
        for (String option : q.options) {
            if (!option.equals(target.definition)) {
                assertTrue(option.startsWith("filler definition"));
            }
        }
    }

    @Test
    public void scoresBinary() {
        Word target = TestWords.word(1, "decide", CefrLevel.A2, "business",
                "to choose something", "You must decide now.");
        List<Word> pool = TestWords.poolAround(target, 10);
        Question q = generator.generate(target, pool, new Random(9));

        assertEquals(1.0, generator.score(q, Answer.ofOption(q.correctOptionIndex, 100)).ratio, 1e-9);
        int wrongIndex = (q.correctOptionIndex + 1) % q.options.size();
        assertEquals(0.0, generator.score(q, Answer.ofOption(wrongIndex, 100)).ratio, 1e-9);
    }

    @Test
    public void cannotGenerateWithoutADefinition() {
        Word target = TestWords.word(1, "x", CefrLevel.A2, "business", "", "example.");
        assertFalse(generator.canGenerate(target));
    }
}
