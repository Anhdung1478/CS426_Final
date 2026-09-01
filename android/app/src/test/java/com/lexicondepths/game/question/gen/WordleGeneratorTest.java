package com.lexicondepths.game.question.gen;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.gen.WordleGenerator.LetterState;

import org.junit.Test;

import java.util.Collections;
import java.util.Random;

import static com.lexicondepths.game.question.gen.WordleGenerator.LetterState.ABSENT;
import static com.lexicondepths.game.question.gen.WordleGenerator.LetterState.CORRECT;
import static com.lexicondepths.game.question.gen.WordleGenerator.LetterState.PRESENT;

public class WordleGeneratorTest {

    private final WordleGenerator generator = new WordleGenerator();

    @Test
    public void repeatedGuessLettersDoNotOverclaimBeyondTargetsSupply() {
        // Classic bug case: two E's in the guess against two E's in the target, at different
        // positions — every E must be accounted for by the target's actual count, not just
        // "does the letter appear anywhere".
        LetterState[] states = WordleGenerator.feedback("SPEED", "ERASE");
        assertArrayEquals(new LetterState[]{PRESENT, ABSENT, PRESENT, PRESENT, ABSENT}, states);
    }

    @Test
    public void repeatedGuessLetterBeyondTargetsBudgetIsAbsentNotPresent() {
        // Target has exactly two B's, both consumed by exact-position greens; the other three
        // B's in the guess must not also be marked PRESENT.
        LetterState[] states = WordleGenerator.feedback("BBBBB", "ABBEY");
        assertArrayEquals(new LetterState[]{ABSENT, CORRECT, CORRECT, ABSENT, ABSENT}, states);
    }

    @Test
    public void allCorrectWhenGuessMatchesTarget() {
        LetterState[] states = WordleGenerator.feedback("CRANE", "CRANE");
        assertArrayEquals(new LetterState[]{CORRECT, CORRECT, CORRECT, CORRECT, CORRECT}, states);
    }

    @Test
    public void ratioIsOneWhenSolvedOnFirstGuess() {
        Word word = TestWords.word(1, "crane", CefrLevel.B1, "food", "a bird", "A crane flew by.");
        Question q = generator.generate(word, Collections.emptyList(), new Random(1));

        double ratio = generator.score(q, Answer.ofText("CRANE", 100)).ratio;
        assertEquals(1.0, ratio, 1e-9);
    }

    @Test
    public void solvingOnGuessFiveGivesChipDamageRatio() {
        Word word = TestWords.word(1, "crane", CefrLevel.B1, "food", "a bird", "A crane flew by.");
        Question q = generator.generate(word, Collections.emptyList(), new Random(1));

        double ratio = generator.score(q, Answer.ofText("aaaaa,bbbbb,ccccc,ddddd,CRANE", 100)).ratio;
        assertEquals(2.0 / 6.0, ratio, 1e-9);
    }

    @Test
    public void failingAllSixGuessesScoresZero() {
        Word word = TestWords.word(1, "crane", CefrLevel.B1, "food", "a bird", "A crane flew by.");
        Question q = generator.generate(word, Collections.emptyList(), new Random(1));

        double ratio = generator.score(q, Answer.ofText("wrong1,wrong2,wrong3,wrong4,wrong5,wrong6", 100)).ratio;
        assertEquals(0.0, ratio, 1e-9);
    }

    @Test
    public void aGuessBeyondTheSixthIsIgnored() {
        Word word = TestWords.word(1, "crane", CefrLevel.B1, "food", "a bird", "A crane flew by.");
        Question q = generator.generate(word, Collections.emptyList(), new Random(1));

        double ratio = generator.score(q,
                Answer.ofText("w1,w2,w3,w4,w5,w6,CRANE", 100)).ratio;
        assertEquals(0.0, ratio, 1e-9);
    }
}
