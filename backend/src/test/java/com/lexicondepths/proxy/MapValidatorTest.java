package com.lexicondepths.proxy;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every rule in P3-1, asserted on the rejection path — the happy path is the easy half. */
class MapValidatorTest {

    /** A map with `count` valid words, then whatever `mutation` does to it. */
    private static String map(int count, String extraWordJson) {
        StringBuilder words = new StringBuilder();
        for (int i = 0; i < count; i++) {
            String head = "word" + i;
            if (i > 0) {
                words.append(',');
            }
            words.append("""
                    {"headword":"%s","cefr":"B1","pos":"noun","definition":"a made-up thing",
                     "example":"I saw a %s today.","viGloss":"x","synonyms":[],"antonyms":[],
                     "collocations":[],"forms":[],"affixKey":null}""".formatted(head, head));
        }
        if (extraWordJson != null) {
            words.append(',').append(extraWordJson);
        }
        return """
                {"name":"Kitchen Alchemy","topic":"Cooking","level":"b1","words":[%s]}"""
                .formatted(words);
    }

    private static String word(String headword, String field, String value) {
        ObjectNode node = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        node.put("headword", headword).put("cefr", "B1").put("pos", "noun")
                .put("definition", "a made-up thing").put("example", "I saw a " + headword + " today.");
        node.put(field, value);
        return node.toString();
    }

    private static MapValidator.InvalidMapException rejects(String raw) {
        return assertThrows(MapValidator.InvalidMapException.class, () -> MapValidator.validate(raw));
    }

    @Test
    void acceptsAValidMapAndNormalisesCase() throws Exception {
        ObjectNode result = MapValidator.validate(map(12, null));
        assertEquals("cooking", result.get("topic").asText());
        assertEquals("B1", result.get("level").asText());
        assertEquals(12, result.get("words").size());
    }

    @Test
    void stripsJsonFences() throws Exception {
        assertEquals(12, MapValidator.validate("```json\n" + map(12, null) + "\n```")
                .get("words").size());
    }

    @Test
    void stripsBareFencesAndLeadingProse() throws Exception {
        assertEquals(12, MapValidator.validate("Sure! Here you go:\n```\n" + map(12, null) + "\n```")
                .get("words").size());
    }

    @Test
    void copiesMapTopicOntoEveryWord() throws Exception {
        // The client pulls a realm's word pool by topic, so a word without one is invisible.
        ObjectNode result = MapValidator.validate(map(12, null));
        for (var word : result.get("words")) {
            assertEquals("cooking", word.get("topic").asText());
        }
    }

    @Test
    void rejectsNonJson() {
        rejects("I'm sorry, I can't help with that.");
    }

    @Test
    void rejectsTooFewWords() {
        assertTrue(rejects(map(7, null)).getMessage().contains("at least"));
    }

    @Test
    void dropsOneUnusableWordRatherThanLosingTheMap() throws Exception {
        // Found by running it: DeepSeek gave "unwind" an example that never said "unwind", and
        // rejecting the whole map over one word out of twelve is the wrong trade.
        ObjectNode result = MapValidator.validate(
                map(12, word("unwind", "example", "Take a break after work.")));
        assertEquals(12, result.get("words").size());
        for (var word : result.get("words")) {
            assertNotEquals("unwind", word.get("headword").asText());
        }
    }

    @Test
    void rejectsWhenTooFewWordsSurviveTheFilter() {
        assertTrue(rejects(map(4, word("saffron", "definition", "")))
                .getMessage().contains("were usable"));
    }

    @Test
    void rejectsTooManyWords() {
        assertTrue(rejects(map(25, null)).getMessage().contains("limit"));
    }

    @Test
    void dropsAWordWithABlankDefinition() throws Exception {
        assertEquals(11, MapValidator.validate(map(11, word("saffron", "definition", "  ")))
                .get("words").size());
    }

    @Test
    void dropsAWordWithABlankHeadword() throws Exception {
        assertEquals(11, MapValidator.validate(map(11, word("", "pos", "noun")))
                .get("words").size());
    }

    @Test
    void dropsAWordWithAnUnknownCefrLevel() throws Exception {
        assertEquals(11, MapValidator.validate(map(11, word("saffron", "cefr", "B7")))
                .get("words").size());
    }

    @Test
    void rejectsUnknownMapLevel() {
        rejects(map(12, null).replace("\"level\":\"b1\"", "\"level\":\"fluent\""));
    }

    @Test
    void dropsTheSecondOfTwoIdenticalHeadwords() throws Exception {
        assertEquals(12, MapValidator.validate(map(12, word("word0", "pos", "noun")))
                .get("words").size());
    }

    @Test
    void dropsAWordWhoseExampleOmitsIt() throws Exception {
        // A cloze cut from that sentence would have no answer in it.
        assertEquals(11, MapValidator.validate(
                map(11, word("saffron", "example", "The soup was delicious.")))
                .get("words").size());
    }

    @Test
    void rejectsAnEmptyWordList() {
        rejects("{\"name\":\"A\",\"topic\":\"b\",\"level\":\"B1\",\"words\":[]}");
    }

    @Test
    void fillsMissingListFieldsRatherThanDroppingTheWord() throws Exception {
        var result = MapValidator.validate(map(11, word("saffron", "pos", "noun")));
        assertEquals(12, result.get("words").size());
    }

    @Test
    void rejectsAMissingWordList() {
        rejects("{\"name\":\"A\",\"topic\":\"b\",\"level\":\"B1\"}");
    }
}
