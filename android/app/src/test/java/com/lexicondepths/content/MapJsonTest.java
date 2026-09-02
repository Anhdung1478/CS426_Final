package com.lexicondepths.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * MapJson is a trust boundary — the backend validates too, but "the server checked it" is an
 * assumption and a malformed map reaching the library table is the failure that ruins a demo.
 * So every rule is asserted on its rejection path.
 */
public class MapJsonTest {

    private static String map(int count, String extraWord) {
        StringBuilder words = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                words.append(',');
            }
            words.append(word("word" + i, null, null));
        }
        if (extraWord != null) {
            words.append(',').append(extraWord);
        }
        return "{\"name\":\"Kitchen Alchemy\",\"topic\":\"Cooking\",\"level\":\"B1\",\"words\":["
                + words + "]}";
    }

    /** A valid word object, with one field optionally overridden to something invalid. */
    private static String word(String headword, String field, String value) {
        StringBuilder json = new StringBuilder("{");
        String[][] fields = {
                {"headword", headword}, {"cefr", "B1"}, {"pos", "noun"},
                {"definition", "a made-up thing"}, {"example", "I saw a " + headword + " today."},
                {"viGloss", "x"}, {"affixKey", ""}
        };
        for (int i = 0; i < fields.length; i++) {
            String key = fields[i][0];
            String val = key.equals(field) ? value : fields[i][1];
            json.append(i > 0 ? "," : "").append('"').append(key).append("\":\"").append(val).append('"');
        }
        return json.append(",\"synonyms\":[\"a\"],\"collocations\":[]}").toString();
    }

    private static MapJson.InvalidMapException rejects(String raw) {
        return assertThrows(MapJson.InvalidMapException.class, () -> MapJson.parseMap(raw));
    }

    @Test
    public void parsesAValidMap() throws Exception {
        MapJson.GeneratedMap parsed = MapJson.parseMap(map(12, null));
        assertEquals("Kitchen Alchemy", parsed.name);
        assertEquals("cooking", parsed.topic);
        assertEquals(12, parsed.words.size());
        assertEquals(CefrLevel.B1, parsed.words.get(0).cefr);
    }

    @Test
    public void stampsTheMapTopicOntoEveryWord() throws Exception {
        // RunEngine pulls a realm's pool by topic; a word carrying another topic would be
        // generated into the realm and then never appear in it.
        for (Word word : MapJson.parseMap(map(12, null)).words) {
            assertEquals("cooking", word.topic);
        }
    }

    @Test
    public void treatsMissingListsAsEmptyRatherThanNull() throws Exception {
        Word word = MapJson.parseMap(map(12, null)).words.get(0);
        assertEquals(1, word.synonyms.size());
        assertTrue(word.antonyms.isEmpty());
        assertTrue(word.forms.isEmpty());
    }

    @Test
    public void blankOptionalFieldsBecomeNullNotEmptyString() throws Exception {
        assertNull(MapJson.parseMap(map(12, null)).words.get(0).affixKey);
    }

    @Test
    public void stripsJsonFences() throws Exception {
        assertEquals(12, MapJson.parseMap("```json\n" + map(12, null) + "\n```").words.size());
    }

    @Test
    public void stripsBareFencesAndLeadingProse() throws Exception {
        assertEquals(12, MapJson.parseMap("Sure! Here:\n```\n" + map(12, null) + "\n```").words.size());
    }

    @Test
    public void rejectsNonJson() {
        rejects("I'm sorry, I can't help with that.");
    }

    @Test
    public void rejectsTooFewWordsToFillARun() {
        assertTrue(rejects(map(7, null)).getMessage().contains("at least"));
    }

    @Test
    public void dropsOneUnusableWordRatherThanLosingTheMap() throws Exception {
        // Found by running it: DeepSeek gave "unwind" an example that never said "unwind", and
        // rejecting the whole map over one word out of twelve is the wrong trade.
        MapJson.GeneratedMap parsed = MapJson.parseMap(
                map(12, word("unwind", "example", "Take a break after work.")));
        assertEquals(12, parsed.words.size());
        for (Word word : parsed.words) {
            assertNotEquals("unwind", word.headword);
        }
    }

    @Test
    public void rejectsWhenTooFewWordsSurviveTheFilter() {
        assertTrue(rejects(map(4, word("saffron", "definition", "")))
                .getMessage().contains("were usable"));
    }

    @Test
    public void rejectsPaddedMaps() {
        assertTrue(rejects(map(25, null)).getMessage().contains("limit"));
    }

    @Test
    public void dropsAWordWithABlankDefinition() throws Exception {
        assertEquals(11, MapJson.parseMap(map(11, word("saffron", "definition", "   ")))
                .words.size());
    }

    @Test
    public void dropsAWordWithABlankPos() throws Exception {
        assertEquals(11, MapJson.parseMap(map(11, word("saffron", "pos", ""))).words.size());
    }

    @Test
    public void dropsAWordWithAnUnknownCefrLevel() throws Exception {
        assertEquals(11, MapJson.parseMap(map(11, word("saffron", "cefr", "B7"))).words.size());
    }

    @Test
    public void dropsTheSecondOfTwoIdenticalHeadwords() throws Exception {
        assertEquals(12, MapJson.parseMap(map(12, word("word0", null, null))).words.size());
    }

    @Test
    public void dropsAWordWhoseExampleOmitsIt() throws Exception {
        // A cloze cut from that sentence would contain no answer.
        assertEquals(11, MapJson.parseMap(
                map(11, word("saffron", "example", "The soup was delicious."))).words.size());
    }

    @Test
    public void rejectsAMissingWordList() {
        rejects("{\"name\":\"A\",\"topic\":\"b\",\"level\":\"B1\"}");
    }

    /**
     * The bundled fallback is a real DeepSeek response kept for demo survival, and it routes
     * through this exact parser. Asserting it here is what stops it rotting unnoticed.
     */
    @Test
    public void theBundledOfflineRealmIsValid() throws Exception {
        String json = new String(Files.readAllBytes(
                Paths.get("src/main/assets/fallback_map.json")), StandardCharsets.UTF_8);
        MapJson.GeneratedMap fallback = MapJson.parseMap(json);
        assertTrue(fallback.words.size() >= 8);
        assertEquals("cooking", fallback.topic);
    }

    @Test
    public void rejectsAnUnnamedRealm() {
        rejects(map(12, null).replace("\"name\":\"Kitchen Alchemy\"", "\"name\":\"\""));
    }
}
