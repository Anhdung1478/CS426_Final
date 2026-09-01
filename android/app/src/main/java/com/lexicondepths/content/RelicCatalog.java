package com.lexicondepths.content;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses relics.json into memory and stays there — relics never change at
 * runtime, so a Room table would buy nothing.
 */
public final class RelicCatalog {

    private static final String TAG = "RelicCatalog";
    private static final String ASSET_FILE = "relics.json";

    private static List<Relic> cache;

    private RelicCatalog() {
    }

    public static synchronized List<Relic> load(Context context) {
        if (cache != null) {
            return cache;
        }
        List<Relic> relics = new ArrayList<>();
        try {
            String json = readAsset(context, ASSET_FILE);
            JSONObject root = new JSONObject(json);
            JSONArray array = root.getJSONArray("relics");
            for (int i = 0; i < array.length(); i++) {
                relics.add(parseRelic(array.getJSONObject(i)));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load " + ASSET_FILE, e);
        }
        cache = Collections.unmodifiableList(relics);
        return cache;
    }

    public static Relic getById(Context context, String id) {
        for (Relic r : load(context)) {
            if (r.id.equals(id)) {
                return r;
            }
        }
        return null;
    }

    private static Relic parseRelic(JSONObject obj) throws Exception {
        Relic r = new Relic();
        r.id = obj.getString("id");
        r.name = obj.getString("name");
        r.desc = obj.getString("desc");
        r.effect = obj.getString("effect");
        return r;
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
