package com.lexicondepths.content;

import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The P3-1 contract, client side. One parser for both the bundled seed and anything arriving
 * over the network, so a field added to the seed format cannot silently fail to parse from
 * DeepSeek — and vice versa.
 *
 * The backend validates too. This validates anyway: "the server checked it" is an assumption,
 * and a malformed map reaching the library table is the failure that ruins a demo.
 */
public final class MapJson {

    static final int MIN_WORDS = 8;
    static final int MAX_WORDS = 24;

    private MapJson() {
    }

    /** A parsed, validated map. Realm CEFR bounds are derived from the words, as SeedLoader does. */
    public static final class GeneratedMap {
        public final String name;
        public final String topic;
        public final List<Word> words;

        GeneratedMap(String name, String topic, List<Word> words) {
            this.name = name;
            this.topic = topic;
            this.words = words;
        }
    }

    /** Thrown for any contract violation. The message is safe to show a player. */
    public static class InvalidMapException extends Exception {
        public InvalidMapException(String message) {
            super(message);
        }
    }

    /**
     * Discards markdown fences and any prose around the object. The backend asks DeepSeek for
     * `json_object` so fences are unlikely — unlikely is not never, and this is four lines.
     */
    static String stripFences(String raw) {
        String text = raw == null ? "" : raw.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return start < 0 || end < start ? text : text.substring(start, end + 1);
    }

    public static GeneratedMap parseMap(String raw) throws InvalidMapException {
        JSONObject root;
        try {
            root = new JSONObject(stripFences(raw));
        } catch (Exception e) {
            throw new InvalidMapException("The forge returned something that is not a realm.");
        }

        String name = root.optString("name", "").trim();
        String topic = root.optString("topic", "").trim().toLowerCase(Locale.ROOT);
        require(!name.isEmpty(), "That realm has no name.");
        require(!topic.isEmpty(), "That realm has no topic.");

        JSONArray array = root.optJSONArray("words");
        require(array != null, "That realm has no words.");
        require(array.length() <= MAX_WORDS,
                "That realm has " + array.length() + " words; the limit is " + MAX_WORDS + ".");

        // One unusable word must not cost a good map — models reliably slip on one entry out of
        // twelve. Dropping it keeps the guarantee that matters (everything imported is playable)
        // without throwing away the other eleven; too few survivors still fails, below.
        Set<String> seen = new HashSet<>();
        List<Word> words = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) {
                continue;
            }
            Word word;
            try {
                word = parseWord(obj);
            } catch (InvalidMapException unusable) {
                continue;
            }
            if (!seen.add(word.headword.toLowerCase(Locale.ROOT))) {
                continue;
            }
            // The run engine pulls a realm's pool by topic, so a word carrying another one
            // would be generated into the realm and then never appear in it.
            word.topic = topic;
            words.add(word);
        }
        require(words.size() >= MIN_WORDS, "Only " + words.size() + " of " + array.length()
                + " words were usable; a run needs at least " + MIN_WORDS + ".");
        return new GeneratedMap(name, topic, words);
    }

    /**
     * One word object → one Word row. Shared with SeedLoader; keep it tolerant of missing
     * optional fields and strict about the ones a question generator cannot work without.
     */
    public static Word parseWord(JSONObject obj) throws InvalidMapException {
        Word word = new Word();
        word.headword = obj.optString("headword", "").trim();
        require(!word.headword.isEmpty(), "A word has no headword.");
        word.pos = required(obj, "pos", word.headword);
        word.definition = required(obj, "definition", word.headword);
        word.example = required(obj, "example", word.headword);
        word.topic = obj.optString("topic", "").trim().toLowerCase(Locale.ROOT);

        try {
            word.cefr = CefrLevel.valueOf(obj.optString("cefr", "").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidMapException("\"" + word.headword + "\" has no CEFR level.");
        }

        // A cloze cut from an example that never mentions the word has no answer inside it.
        String firstToken = word.headword.toLowerCase(Locale.ROOT).split("\\s+")[0];
        require(word.example.toLowerCase(Locale.ROOT).contains(firstToken),
                "The example for \"" + word.headword + "\" does not use the word.");

        word.viGloss = blankToNull(obj.optString("viGloss", ""));
        word.affixKey = blankToNull(obj.optString("affixKey", ""));
        word.synonyms = toStringList(obj.optJSONArray("synonyms"));
        word.antonyms = toStringList(obj.optJSONArray("antonyms"));
        word.collocations = toStringList(obj.optJSONArray("collocations"));
        word.forms = toStringList(obj.optJSONArray("forms"));
        return word;
    }

    private static String required(JSONObject obj, String field, String headword)
            throws InvalidMapException {
        String value = obj.optString(field, "").trim();
        require(!value.isEmpty(), "\"" + headword + "\" has no " + field + ".");
        return value;
    }

    private static String blankToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() || "null".equals(trimmed) ? null : trimmed;
    }

    private static List<String> toStringList(JSONArray array) {
        List<String> list = new ArrayList<>();
        for (int i = 0; array != null && i < array.length(); i++) {
            String value = array.optString(i, "").trim();
            if (!value.isEmpty()) {
                list.add(value);
            }
        }
        return list;
    }

    private static void require(boolean condition, String message) throws InvalidMapException {
        if (!condition) {
            throw new InvalidMapException(message);
        }
    }
}
