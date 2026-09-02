package com.lexicondepths.content;

import com.lexicondepths.db.AppDatabase;
import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Realm;
import com.lexicondepths.db.entity.RealmWord;
import com.lexicondepths.db.entity.Word;

import java.util.ArrayList;
import java.util.List;

/**
 * Writes a validated map into Room as one transaction and returns the new realmId. Once this
 * returns, a forged realm is indistinguishable from a seeded one — RunEngine reads it through
 * exactly the same queries.
 *
 * Blocking; call it from App.io().
 */
public final class RealmImport {

    private RealmImport() {
    }

    public static long importMap(AppDatabase db, MapJson.GeneratedMap map) {
        long[] realmId = new long[1];
        db.runInTransaction(() -> realmId[0] = insert(db, map));
        return realmId[0];
    }

    private static long insert(AppDatabase db, MapJson.GeneratedMap map) {
        // Word.headword is uniquely indexed and a generated "travel" map will collide with the
        // seed. Skipping collisions would silently shrink the realm — a 12-word map becoming an
        // 8-word dungeon with no error anywhere — so existing rows get joined instead of dropped.
        List<Long> inserted = db.wordDao().insertAllReturningIds(map.words);

        List<RealmWord> joins = new ArrayList<>();
        CefrLevel min = CefrLevel.C2;
        CefrLevel max = CefrLevel.A1;
        for (int i = 0; i < map.words.size(); i++) {
            Word word = map.words.get(i);
            long wordId = inserted.get(i);
            if (wordId == -1L) {
                Word existing = db.wordDao().getByHeadword(word.headword);
                if (existing == null) {
                    continue; // Can only happen if another writer deleted it between the two calls.
                }
                // Its WordProgress is deliberately untouched: a word you have been learning for
                // a month does not reset because it turned up in a new realm.
                wordId = existing.id;
            }
            RealmWord join = new RealmWord();
            join.realmId = 0; // filled in below, once the realm row exists
            join.wordId = wordId;
            joins.add(join);

            if (word.cefr.ordinal() < min.ordinal()) {
                min = word.cefr;
            }
            if (word.cefr.ordinal() > max.ordinal()) {
                max = word.cefr;
            }
        }

        Realm realm = new Realm();
        realm.name = map.name;
        realm.topic = map.topic;
        realm.cefrMin = min;
        realm.cefrMax = max;
        realm.generated = true;
        realm.createdAt = System.currentTimeMillis();
        long id = db.realmDao().insert(realm);

        for (RealmWord join : joins) {
            join.realmId = id;
        }
        db.realmDao().insertRealmWords(joins);
        return id;
    }
}
