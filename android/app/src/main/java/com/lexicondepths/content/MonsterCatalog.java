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
 * Parses monsters.json into memory and stays there — monsters never change at
 * runtime, so a Room table would buy nothing.
 */
public final class MonsterCatalog {

    private static final String TAG = "MonsterCatalog";
    private static final String ASSET_FILE = "monsters.json";

    private static List<Monster> cache;

    private MonsterCatalog() {
    }

    public static synchronized List<Monster> load(Context context) {
        if (cache != null) {
            return cache;
        }
        List<Monster> monsters = new ArrayList<>();
        try {
            String json = readAsset(context, ASSET_FILE);
            JSONObject root = new JSONObject(json);
            JSONArray array = root.getJSONArray("monsters");
            for (int i = 0; i < array.length(); i++) {
                monsters.add(parseMonster(array.getJSONObject(i)));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load " + ASSET_FILE, e);
        }
        cache = Collections.unmodifiableList(monsters);
        return cache;
    }

    public static Monster getById(Context context, String id) {
        for (Monster m : load(context)) {
            if (m.id.equals(id)) {
                return m;
            }
        }
        return null;
    }

    private static Monster parseMonster(JSONObject obj) throws Exception {
        Monster m = new Monster();
        m.id = obj.getString("id");
        m.name = obj.getString("name");
        m.questionTypes = toStringList(obj.getJSONArray("questionTypes"));
        m.slots = obj.getInt("slots");
        m.resists = toStringList(obj.optJSONArray("resists"));
        m.ascii = toStringList(obj.optJSONArray("ascii"));
        return m;
    }

    private static List<String> toStringList(JSONArray array) throws Exception {
        List<String> list = new ArrayList<>();
        if (array == null) {
            return list;
        }
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getString(i));
        }
        return list;
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
