package com.lexicondepths.game.question.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class QuestionSupportTest {

    @Test
    public void buildOptionsPlacesCorrectAnswerUniformlyAcrossManyGenerations() {
        List<String> distractors = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            distractors.add("distractor" + i);
        }
        Map<Integer, Integer> positionCounts = new HashMap<>();
        Random rng = new Random(42);
        int trials = 4000;
        for (int i = 0; i < trials; i++) {
            QuestionSupport.McqOptions mcq = QuestionSupport.buildOptions("correct", distractors, rng);
            positionCounts.merge(mcq.correctIndex, 1, Integer::sum);
        }

        assertEquals(QuestionSupport.OPTION_COUNT, positionCounts.size());
        for (int position = 0; position < QuestionSupport.OPTION_COUNT; position++) {
            double share = (double) positionCounts.getOrDefault(position, 0) / trials;
            assertTrue("position " + position + " share was " + share, share > 0.15 && share < 0.35);
        }
    }

    @Test
    public void buildOptionsNeverDuplicatesTheCorrectAnswer() {
        List<String> distractors = Arrays.asList("correct", "other", "another");
        QuestionSupport.McqOptions mcq = QuestionSupport.buildOptions("correct", distractors, new Random(1));

        long occurrences = mcq.options.stream().filter(o -> o.equals("correct")).count();
        assertEquals(1, occurrences);
    }

    @Test
    public void findExampleBlankMatchesTheHeadwordWhenNoFormIsPresent() {
        Word word = TestWords.word(1, "decide", CefrLevel.B1, "business",
                "to choose something", "You must decide now.");

        QuestionSupport.ExampleBlank blank = QuestionSupport.findExampleBlank(word);

        assertEquals("decide", blank.candidate);
    }

    @Test
    public void findExampleBlankPrefersTheLongerCandidateWhenMultipleAppear() {
        Word word = TestWords.word(1, "decide", CefrLevel.B1, "business",
                "to choose something", "He will decide only after they decided.");
        word.forms = Arrays.asList("decided", "decision");

        QuestionSupport.ExampleBlank blank = QuestionSupport.findExampleBlank(word);

        assertEquals("decided", blank.candidate);
    }

    @Test
    public void findExampleBlankRejectsAmbiguousRepeatedOccurrence() {
        Word word = TestWords.word(1, "order", CefrLevel.B1, "food",
                "to request food", "He gave the order, then a second order.");

        assertNull(QuestionSupport.findExampleBlank(word));
    }

    @Test
    public void findExampleBlankReturnsNullWhenNoCandidateAppears() {
        Word word = TestWords.word(1, "decide", CefrLevel.B1, "business",
                "to choose something", "This sentence has none of the target words.");

        assertNull(QuestionSupport.findExampleBlank(word));
    }
}
