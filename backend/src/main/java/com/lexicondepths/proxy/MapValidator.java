package com.lexicondepths.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Guards the P3-1 contract. An LLM reliably produces two defects: markdown fences around
 * otherwise-valid JSON, and plausible objects with an empty string where a definition belongs.
 * Both corrupt the client's library table, so neither leaves this class.
 *
 * Pure and Spring-free so it can be unit-tested without booting a context.
 */
public final class MapValidator {

    static final int MIN_WORDS = 8;
    static final int MAX_WORDS = 24;

    private static final List<String> LEVELS = List.of("A1", "A2", "B1", "B2", "C1", "C2");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MapValidator() {
    }

    /** Thrown for any contract violation. The message is safe to show a player. */
    public static class InvalidMapException extends Exception {
        public InvalidMapException(String message) {
            super(message);
        }
    }

    /**
     * Strips markdown fences and any prose around them. `response_format: json_object` makes
     * fences unlikely rather than impossible, and this is six lines.
     */
    static String stripFences(String raw) {
        String text = raw == null ? "" : raw.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < start) {
            return text;
        }
        return text.substring(start, end + 1);
    }

    /** Validates, normalizes, and returns the map. Throws rather than returning anything partial. */
    public static ObjectNode validate(String raw) throws InvalidMapException {
        JsonNode root;
        try {
            root = MAPPER.readTree(stripFences(raw));
        } catch (Exception e) {
            throw new InvalidMapException("Model returned something that is not JSON.");
        }
        if (!root.isObject()) {
            throw new InvalidMapException("Model returned JSON that is not an object.");
        }

        ObjectNode map = (ObjectNode) root;
        String name = text(map, "name");
        String topic = text(map, "topic").toLowerCase(Locale.ROOT);
        String level = text(map, "level").toUpperCase(Locale.ROOT);

        require(!name.isEmpty(), "Map has no name.");
        require(!topic.isEmpty(), "Map has no topic.");
        require(LEVELS.contains(level), "Map level \"" + level + "\" is not a CEFR level.");

        JsonNode words = map.get("words");
        require(words != null && words.isArray(), "Map has no word list.");
        require(words.size() <= MAX_WORDS,
                "Map has " + words.size() + " words; the limit is " + MAX_WORDS + ".");

        // One unusable word must not cost a good map. Models reliably slip on one entry out of
        // twelve — usually an example sentence that never says the headword — and dropping that
        // word keeps the guarantee that matters (everything imported is playable) without
        // throwing away the other eleven. Too few survivors still fails, below.
        Set<String> seen = new HashSet<>();
        ArrayNode usable = MAPPER.createArrayNode();
        for (JsonNode wordNode : words) {
            if (!wordNode.isObject()) {
                continue;
            }
            ObjectNode word = (ObjectNode) wordNode;
            if (isUsable(word, topic, seen)) {
                usable.add(word);
            }
        }
        require(usable.size() >= MIN_WORDS, "Only " + usable.size() + " of " + words.size()
                + " words were usable; a run needs at least " + MIN_WORDS + ".");

        map.set("words", usable);
        map.put("topic", topic);
        map.put("level", level);
        return map;
    }

    /** Normalizes the word in place and reports whether it can carry a question. */
    private static boolean isUsable(ObjectNode word, String mapTopic, Set<String> seen) {
        String headword = text(word, "headword");
        if (headword.isEmpty() || !seen.add(headword.toLowerCase(Locale.ROOT))) {
            return false;
        }
        for (String field : List.of("definition", "example", "pos")) {
            if (text(word, field).isEmpty()) {
                return false;
            }
        }

        String cefr = text(word, "cefr").toUpperCase(Locale.ROOT);
        if (!LEVELS.contains(cefr)) {
            return false;
        }

        // A cloze cut from an example that never mentions the word is unanswerable.
        String example = text(word, "example").toLowerCase(Locale.ROOT);
        if (!example.contains(headword.toLowerCase(Locale.ROOT).split("\\s+")[0])) {
            return false;
        }

        word.put("cefr", cefr);
        // The client queries a realm's word pool by topic, so every word must carry the map's.
        word.put("topic", mapTopic);
        for (String listField : List.of("synonyms", "antonyms", "collocations", "forms")) {
            if (!(word.get(listField) instanceof ArrayNode)) {
                word.putArray(listField);
            }
        }
        return true;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText().trim();
    }

    private static void require(boolean condition, String message) throws InvalidMapException {
        if (!condition) {
            throw new InvalidMapException(message);
        }
    }
}
