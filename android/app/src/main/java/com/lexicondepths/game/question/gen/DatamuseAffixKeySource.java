package com.lexicondepths.game.question.gen;

import com.lexicondepths.db.entity.Word;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * P2-5 built affix harvest behind AffixKeySource so this swap would touch one class. Datamuse
 * needs no key, so it is the one external call that can go direct from the client.
 *
 * <p><b>The target set is deliberately not widened.</b> Ratio is found/keySize, so replacing a
 * five-word offline key with a hundred Datamuse words would score a player who typed five correct
 * answers at 0.05 and hit them for near-full damage. Datamuse's value here is <i>accepting</i>
 * more correct answers, not demanding more — so the offline key stays the target and Datamuse
 * only extends what counts.
 *
 * <p>No Android imports: java.net keeps game/ testable under plain JUnit, and the response is a
 * fixed shape that a small scanner reads without org.json (which is Android-only).
 */
public class DatamuseAffixKeySource implements AffixKeySource {

    private static final String ENDPOINT = "https://api.datamuse.com/words?max=100&sp=";
    // A fight must not stall on a slow network; the offline key is a fine answer.
    private static final int TIMEOUT_MS = 3000;

    private final AffixKeySource offline;
    private final Map<String, Set<String>> cache = new HashMap<>();

    public DatamuseAffixKeySource(AffixKeySource offline) {
        this.offline = offline;
    }

    /** The target. Unchanged from Phase 2, which is what keeps damage balance untouched. */
    @Override
    public Set<String> wordsFor(Word word, List<Word> pool) {
        return offline.wordsFor(word, pool);
    }

    /** What counts: the target, plus every real word Datamuse knows with the same affix. */
    @Override
    public Set<String> acceptedFor(Word word, List<Word> pool) {
        Set<String> accepted = new TreeSet<>(wordsFor(word, pool));
        accepted.addAll(lookUp(word.affixKey));
        return accepted;
    }

    private Set<String> lookUp(String affixKey) {
        String pattern = toWildcard(affixKey);
        if (pattern == null) {
            return java.util.Collections.emptySet();
        }
        Set<String> cached = cache.get(pattern);
        if (cached != null) {
            return cached;
        }
        Set<String> found;
        try {
            found = parseWords(fetch(pattern));
        } catch (Exception e) {
            // No network, a timeout, a 500: fall back silently. Airplane mode plays as Phase 2 did.
            found = java.util.Collections.emptySet();
        }
        cache.put(pattern, found);
        return found;
    }

    /** "re-" becomes {@code re*}, "-tion" becomes {@code *tion}. */
    static String toWildcard(String affixKey) {
        if (affixKey == null) {
            return null;
        }
        String affix = affixKey.trim().toLowerCase(Locale.ROOT);
        if (affix.endsWith("-")) {
            String stem = affix.substring(0, affix.length() - 1);
            return stem.isEmpty() ? null : stem + "*";
        }
        if (affix.startsWith("-")) {
            String stem = affix.substring(1);
            return stem.isEmpty() ? null : "*" + stem;
        }
        return affix.isEmpty() ? null : affix + "*";
    }

    /** Pulls every {@code "word":"…"} out of Datamuse's fixed array shape. */
    static Set<String> parseWords(String json) {
        Set<String> words = new TreeSet<>();
        String marker = "\"word\":\"";
        int at = json.indexOf(marker);
        while (at >= 0) {
            int start = at + marker.length();
            int end = json.indexOf('"', start);
            if (end < 0) {
                break;
            }
            String word = json.substring(start, end).toLowerCase(Locale.ROOT);
            // Datamuse returns multi-word entries too; harvest only scores single words.
            if (!word.isEmpty() && word.chars().allMatch(Character::isLetter)) {
                words.add(word);
            }
            at = json.indexOf(marker, end);
        }
        return words;
    }

    /** Non-private and the reason this class is not final: tests override it, never hitting the network. */
    String fetch(String pattern) throws Exception {
        String url = ENDPOINT + URLEncoder.encode(pattern, StandardCharsets.UTF_8.name());
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new java.io.IOException("Datamuse returned HTTP " + conn.getResponseCode());
            }
            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[4096];
                int read;
                while ((read = in.read(chunk)) != -1) {
                    buffer.write(chunk, 0, read);
                }
                return buffer.toString(StandardCharsets.UTF_8.name());
            }
        } finally {
            conn.disconnect();
        }
    }
}
