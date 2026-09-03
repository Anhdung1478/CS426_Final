package com.lexicondepths.content;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.lexicondepths.db.AppDatabase;
import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Realm;
import com.lexicondepths.db.entity.RealmWord;
import com.lexicondepths.db.entity.Word;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * First launch populates Room from assets/words_seed.json; later launches skip it.
 * Guarded on the seed file's version number, not a boolean, so a later content
 * update can re-seed. Must be called off the main thread — see App.io().
 */
public final class SeedLoader {

    private static final String TAG = "SeedLoader";
    private static final String PREFS_NAME = "lexicon_prefs";
    private static final String KEY_SEED_VERSION = "seed_version";
    private static final String ASSET_FILE = "words_seed.json";

    private SeedLoader() {
    }

    public static void run(Context context, AppDatabase db) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int storedVersion = prefs.getInt(KEY_SEED_VERSION, 0);

        String json;
        try {
            json = readAsset(context, ASSET_FILE);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read " + ASSET_FILE + " — leaving database untouched", e);
            return;
        }

        int fileVersion;
        List<Word> words;
        try {
            JSONObject root = new JSONObject(json);
            fileVersion = root.getInt("version");
            if (fileVersion <= storedVersion) {
                return; // already seeded at this version
            }
            words = parseWords(root.getJSONArray("words"));
        } catch (Exception e) {
            Log.e(TAG, "Corrupt or truncated " + ASSET_FILE + " — leaving database untouched", e);
            return;
        }

        final List<Word> toInsert = words;
        db.runInTransaction(() -> {
            db.wordDao().insertAll(toInsert);
            // A re-seed at a higher version reaches words that are already in the table, which
            // a conflict-ignoring insert never can. Without this an upgraded install would have
            // formalAlt null on every row and could never produce a register question.
            for (Word word : toInsert) {
                if (word.formalAlt != null || word.affixKey != null) {
                    db.wordDao().backfillOptionalFields(word.headword, word.formalAlt, word.affixKey);
                }
            }
        });
        seedRealms(db);

        prefs.edit().putInt(KEY_SEED_VERSION, fileVersion).apply();
    }

    /**
     * Realm Select (P2-10) needs one Realm row per topic to point Run.realmId at. Guarded on
     * an existing row count, not the word seed version, so a future content bump that only
     * changes definitions doesn't duplicate realms for topics that already exist.
     */
    private static void seedRealms(AppDatabase db) {
        if (db.realmDao().count() > 0) {
            return;
        }
        Map<String, List<Word>> byTopic = new LinkedHashMap<>();
        for (Word word : db.wordDao().getAll()) {
            byTopic.computeIfAbsent(word.topic, k -> new ArrayList<>()).add(word);
        }
        for (Map.Entry<String, List<Word>> entry : byTopic.entrySet()) {
            CefrLevel min = CefrLevel.C2;
            CefrLevel max = CefrLevel.A1;
            for (Word word : entry.getValue()) {
                if (word.cefr.ordinal() < min.ordinal()) {
                    min = word.cefr;
                }
                if (word.cefr.ordinal() > max.ordinal()) {
                    max = word.cefr;
                }
            }
            Realm realm = new Realm();
            realm.name = capitalize(entry.getKey());
            realm.topic = entry.getKey();
            realm.cefrMin = min;
            realm.cefrMax = max;
            realm.createdAt = System.currentTimeMillis();
            long realmId = db.realmDao().insert(realm);

            List<RealmWord> joins = new ArrayList<>();
            for (Word word : entry.getValue()) {
                RealmWord join = new RealmWord();
                join.realmId = realmId;
                join.wordId = word.id;
                joins.add(join);
            }
            db.realmDao().insertRealmWords(joins);
        }
    }

    private static String capitalize(String topic) {
        return topic.isEmpty() ? topic : Character.toUpperCase(topic.charAt(0)) + topic.substring(1);
    }

    /**
     * One unusable row must not cost the other 299. MapJson.parseMap already skips-and-continues
     * for exactly this reason on the network path; letting the exception escape here meant a
     * single bad example sentence left a fresh install with an empty word bank and no visible
     * error — which is what it did, undetected, from P3-1 until the P4-13 edge-case pass. The
     * skip is logged so a content mistake is loud in logcat rather than silent everywhere.
     */
    private static List<Word> parseWords(JSONArray array) throws Exception {
        List<Word> words = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            try {
                words.add(MapJson.parseWord(array.getJSONObject(i)));
            } catch (MapJson.InvalidMapException unusable) {
                Log.e(TAG, "Skipping unusable seed word: " + unusable.getMessage());
            }
        }
        return words;
    }

    private static String readAsset(Context context, String name) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = context.getAssets().open(name);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}
