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

public class AffixHarvestGeneratorTest {

    private final AffixHarvestGenerator generator = new AffixHarvestGenerator();

    private static Word affixWord(long id, String headword, String affixKey) {
        Word w = TestWords.word(id, headword, CefrLevel.B1, "emotions", "def of " + headword, headword + " example.");
        w.affixKey = affixKey;
        return w;
    }

    @Test
    public void gatesOutWordsWithNoAffixKey() {
        Word word = TestWords.word(1, "cat", CefrLevel.A1, "food", "an animal", "The cat sat.");
        assertFalse(generator.canGenerate(word));
    }

    @Test
    public void keySetIsEveryPoolWordSharingTheAffixPlusItself() {
        Word target = affixWord(1, "unhappy", "un-");
        List<Word> pool = Arrays.asList(
                target,
                affixWord(2, "unfair", "un-"),
                affixWord(3, "dislike", "dis-"));

        Question q = generator.generate(target, pool, new Random(1));

        assertTrue(q.correctAnswer.contains("unhappy"));
        assertTrue(q.correctAnswer.contains("unfair"));
        assertFalse(q.correctAnswer.contains("dislike"));
    }

    @Test
    public void ratioIsFoundOverKeySizeCappedAtOne() {
        Word target = affixWord(1, "unhappy", "un-");
        List<Word> pool = Arrays.asList(target, affixWord(2, "unfair", "un-"));
        Question q = generator.generate(target, pool, new Random(1));

        // key = {unhappy, unfair}, keySize 2. Player finds both, plus a duplicate and a wrong word.
        double ratio = generator.score(q, Answer.ofText("unhappy, UnFair unhappy notintheset", 100)).ratio;
        assertEquals(1.0, ratio, 1e-9);
    }

    @Test
    public void partialCreditForFindingSomeButNotAll() {
        Word target = affixWord(1, "unhappy", "un-");
        List<Word> pool = Arrays.asList(target, affixWord(2, "unfair", "un-"));
        Question q = generator.generate(target, pool, new Random(1));

        double ratio = generator.score(q, Answer.ofText("unhappy", 100)).ratio;
        assertEquals(0.5, ratio, 1e-9);
    }
}
