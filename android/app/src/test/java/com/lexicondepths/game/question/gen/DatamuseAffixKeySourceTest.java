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
import java.util.Set;

/** No network: fetch() is overridden. The point is the target/accepted split, not HTTP. */
public class DatamuseAffixKeySourceTest {

    private static Word affixWord(long id, String headword, String affixKey) {
        Word word = TestWords.word(id, headword, CefrLevel.B1, "emotions",
                "def of " + headword, headword + " example.");
        word.affixKey = affixKey;
        return word;
    }

    private final Word target = affixWord(1, "unhappy", "un-");
    private final List<Word> pool = Arrays.asList(target, affixWord(2, "unfair", "un-"));

    /** Datamuse knows plenty of un- words the seed has never heard of. */
    private DatamuseAffixKeySource sourceReturning(String json) {
        return new DatamuseAffixKeySource(new OfflineAffixKeySource()) {
            @Override
            String fetch(String pattern) {
                return json;
            }
        };
    }

    private static final String LIVE_JSON =
            "[{\"word\":\"unhappy\",\"score\":900},{\"word\":\"unusual\",\"score\":800},"
                    + "{\"word\":\"unlikely\",\"score\":700},{\"word\":\"under the weather\",\"score\":10}]";

    @Test
    public void wildcardPatternFollowsTheAffixPosition() {
        assertEquals("re*", DatamuseAffixKeySource.toWildcard("re-"));
        assertEquals("*tion", DatamuseAffixKeySource.toWildcard("-tion"));
    }

    @Test
    public void parsesWordsAndSkipsMultiWordEntries() {
        Set<String> words = DatamuseAffixKeySource.parseWords(LIVE_JSON);
        assertTrue(words.contains("unusual"));
        assertFalse("harvest scores single words only", words.contains("under the weather"));
    }

    @Test
    public void targetStaysTheOfflineKeySoDamageBalanceIsUnchanged() {
        // The whole P3-8 risk: widening the target would score five correct answers at 5/100.
        assertEquals(new OfflineAffixKeySource().wordsFor(target, pool),
                sourceReturning(LIVE_JSON).wordsFor(target, pool));
    }

    @Test
    public void acceptedSetIsWiderThanTheTarget() {
        Set<String> accepted = sourceReturning(LIVE_JSON).acceptedFor(target, pool);
        assertTrue(accepted.containsAll(new OfflineAffixKeySource().wordsFor(target, pool)));
        assertTrue(accepted.contains("unlikely"));
    }

    @Test
    public void aWordOnlyDatamuseKnowsStillScores() {
        AffixHarvestGenerator generator = new AffixHarvestGenerator(sourceReturning(LIVE_JSON));
        Question question = generator.generate(target, pool, new Random(1));

        // Target is {unhappy, unfair}, so two answers clear it — and "unusual", which the seed
        // has never heard of, is one of them.
        assertEquals(1.0, generator.score(question, Answer.ofText("unhappy unusual", 100)).ratio, 1e-9);
    }

    @Test
    public void networkFailureFallsBackToTheOfflineKey() {
        DatamuseAffixKeySource source = new DatamuseAffixKeySource(new OfflineAffixKeySource()) {
            @Override
            String fetch(String pattern) throws Exception {
                throw new java.io.IOException("airplane mode");
            }
        };
        assertEquals(new OfflineAffixKeySource().wordsFor(target, pool),
                source.acceptedFor(target, pool));
    }

    @Test
    public void lookupsAreCachedSoOneFightAsksOnce() {
        int[] calls = new int[1];
        DatamuseAffixKeySource source = new DatamuseAffixKeySource(new OfflineAffixKeySource()) {
            @Override
            String fetch(String pattern) {
                calls[0]++;
                return LIVE_JSON;
            }
        };
        source.acceptedFor(target, pool);
        source.acceptedFor(target, pool);
        assertEquals(1, calls[0]);
    }
}
