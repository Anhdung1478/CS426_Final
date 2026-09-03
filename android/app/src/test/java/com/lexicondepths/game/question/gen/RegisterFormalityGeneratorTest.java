package com.lexicondepths.game.question.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.QuestionResult;
import com.lexicondepths.game.question.QuestionType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RegisterFormalityGeneratorTest {

    private final RegisterFormalityGenerator generator = new RegisterFormalityGenerator();

    private static Word word(long id, String headword, String formalAlt) {
        Word w = TestWords.word(id, headword, CefrLevel.B2, "business",
                "definition of " + headword, "The report will " + headword + " next week.");
        w.formalAlt = formalAlt;
        return w;
    }

    @Test
    public void typeIsRegisterFormality() {
        assertEquals(QuestionType.REGISTER_FORMALITY, generator.type());
    }

    @Test
    public void canGenerateOnlyWithAFormalAlternative() {
        assertFalse("no formalAlt means this type simply cannot fire",
                generator.canGenerate(word(1, "leverage", null)));
        assertFalse("a blank formalAlt is the same as none",
                generator.canGenerate(word(2, "leverage", "   ")));
        assertTrue(generator.canGenerate(word(3, "leverage", "utilize")));
    }

    @Test
    public void canGenerateRejectsAnExampleThatNeverSaysTheHeadword() {
        Word word = word(4, "leverage", "utilize");
        word.example = "The quarterly numbers were disappointing.";
        assertFalse(generator.canGenerate(word));
    }

    @Test
    public void everyDistractorIsAFormalAlternativeNeverAHeadword() {
        Word target = word(1, "overhead", "expenditure");
        List<Word> pool = new ArrayList<>();
        pool.add(target);
        pool.add(word(2, "asset", "holding"));
        pool.add(word(3, "leverage", "utilize"));
        pool.add(word(4, "incentive", "inducement"));

        Question question = generator.generate(target, pool, new Random(7));

        assertEquals(4, question.options.size());
        assertEquals("expenditure", question.options.get(question.correctOptionIndex));
        for (String option : question.options) {
            assertNotEquals("a headword among the options would be a different register",
                    "overhead", option);
            assertNotEquals("asset", option);
            assertNotEquals("leverage", option);
            assertNotEquals("incentive", option);
        }
    }

    /**
     * canGenerate sees one Word and cannot know how many others in the pool carry formalAlt, so
     * a thin pool must pad rather than emit a two-option MCQ.
     */
    @Test
    public void aPoolWithNoOtherFormalAltsStillYieldsFourOptions() {
        Word target = word(1, "overhead", "expenditure");
        List<Word> pool = new ArrayList<>();
        pool.add(target);
        pool.add(word(2, "revenue", null));
        pool.add(word(3, "invoice", null));

        Question question = generator.generate(target, pool, new Random(3));

        assertEquals(4, question.options.size());
        assertEquals("expenditure", question.options.get(question.correctOptionIndex));
    }

    @Test
    public void thePromptCarriesTheExampleSentenceAndTheHeadword() {
        Word target = word(1, "overhead", "expenditure");
        Question question = generator.generate(target, List.of(target), new Random(1));
        assertTrue(question.prompt.contains(target.example));
        assertTrue(question.prompt.contains("overhead"));
    }

    @Test
    public void scoringIsBinary() {
        Word target = word(1, "overhead", "expenditure");
        Question question = generator.generate(target, List.of(target), new Random(1));

        QuestionResult right = generator.score(question, Answer.ofOption(question.correctOptionIndex, 1000));
        assertEquals(1.0, right.ratio, 0.0);

        int wrongIndex = (question.correctOptionIndex + 1) % question.options.size();
        QuestionResult wrong = generator.score(question, Answer.ofOption(wrongIndex, 1000));
        assertEquals(0.0, wrong.ratio, 0.0);
    }
}
