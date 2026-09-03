package com.lexicondepths.game.question.gen;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * The Phase 3 carry-over, closed. report-phase3.md flagged that the Datamuse path had only ever
 * been tested against a stub and "the real api.datamuse.com call has never been made from a
 * device". This makes it, from a device, against the live endpoint.
 *
 * It lives in androidTest rather than the JVM suite and skips itself when the network is
 * unreachable, so `gradlew test` stays hermetic and a green build never depends on someone
 * else's uptime. The stubbed DatamuseAffixKeySourceTest remains the behavioural test; this one
 * asserts only the two things a stub cannot: that the request goes out and that a real reply
 * parses.
 */
@RunWith(AndroidJUnit4.class)
public class DatamuseLiveTest {

    private static Word affixWord(String headword, String affixKey) {
        Word word = new Word();
        word.id = 1L;
        word.headword = headword;
        word.cefr = CefrLevel.B1;
        word.topic = "business";
        word.pos = "verb";
        word.definition = "a definition";
        word.example = "We " + headword + " every quarter.";
        word.synonyms = new ArrayList<>();
        word.antonyms = new ArrayList<>();
        word.collocations = new ArrayList<>();
        word.forms = new ArrayList<>();
        word.affixKey = affixKey;
        return word;
    }

    private static boolean datamuseReachable() {
        try {
            HttpURLConnection conn =
                    (HttpURLConnection) new URL("https://api.datamuse.com/words?max=1&sp=re*").openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            try {
                return conn.getResponseCode() == 200;
            } finally {
                conn.disconnect();
            }
        } catch (Exception offline) {
            return false;
        }
    }

    @Test
    public void liveDatamuseWidensTheAcceptedSetWithoutMovingTheTarget() {
        assumeTrue("api.datamuse.com unreachable — skipping the live check", datamuseReachable());

        Word word = affixWord("rebuild", "re-");
        List<Word> pool = Arrays.asList(
                word, affixWord("rethink", "re-"), affixWord("replace", "re-"));

        OfflineAffixKeySource offline = new OfflineAffixKeySource();
        DatamuseAffixKeySource live = new DatamuseAffixKeySource(offline);

        Set<String> target = live.wordsFor(word, pool);
        Set<String> accepted = live.acceptedFor(word, pool);

        // The target is what damage balance rides on, so the network must never move it.
        assertTrue("the target set must stay the offline one",
                target.equals(offline.wordsFor(word, pool)));
        assertTrue("accepted must contain the whole target", accepted.containsAll(target));
        assertTrue("a live reply should widen what counts beyond the seed pool: "
                        + accepted.size() + " accepted vs " + target.size() + " target",
                accepted.size() > target.size());
        for (String accepted_word : accepted) {
            assertTrue("every accepted word must actually carry the affix: " + accepted_word,
                    accepted_word.toLowerCase(java.util.Locale.ROOT).startsWith("re"));
        }
    }

}
