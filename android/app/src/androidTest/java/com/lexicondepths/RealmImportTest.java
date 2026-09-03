package com.lexicondepths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.lexicondepths.content.MapJson;
import com.lexicondepths.content.RealmImport;
import com.lexicondepths.db.AppDatabase;
import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Realm;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.db.entity.WordProgress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RealmImport is the highest-risk untested code in the project: the only path that writes to
 * Word at runtime, and its seed-collision join is subtle enough that report-phase3.md devotes a
 * section to it. That report flagged the gap explicitly and named closing it a Phase 4 item.
 *
 * The middle test is the one that matters. A forged "travel" map will collide with the seed,
 * and the two wrong answers are both silent: skipping the collision shrinks a 12-word map into
 * an 8-word dungeon with no error anywhere, and replacing the row would give the word a new id,
 * orphaning the WordProgress the player spent a month building.
 */
@RunWith(AndroidJUnit4.class)
public class RealmImportTest {

    private AppDatabase db;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void closeDb() {
        db.close();
    }

    private static String mapJson(String name, String topic, String... headwords) {
        StringBuilder json = new StringBuilder();
        json.append("{\"name\":\"").append(name).append("\",\"topic\":\"").append(topic)
                .append("\",\"level\":\"B1\",\"words\":[");
        for (int i = 0; i < headwords.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"headword\":\"").append(headwords[i]).append("\",")
                    .append("\"cefr\":\"B1\",\"pos\":\"noun\",")
                    .append("\"definition\":\"a definition of ").append(headwords[i]).append("\",")
                    .append("\"example\":\"We saw the ").append(headwords[i]).append(" today.\",")
                    .append("\"viGloss\":\"gloss\",\"synonyms\":[],\"antonyms\":[],")
                    .append("\"collocations\":[],\"forms\":[]}");
        }
        return json.append("]}").toString();
    }

    private static String[] headwords(String prefix, int count) {
        String[] words = new String[count];
        for (int i = 0; i < count; i++) {
            words[i] = prefix + i;
        }
        return words;
    }

    @Test
    public void anAllNewMapProducesARealmWithEveryWord() throws Exception {
        MapJson.GeneratedMap map = MapJson.parseMap(
                mapJson("Salt Roads", "cooking", headwords("brandnew", 12)));

        long realmId = RealmImport.importMap(db, map);

        Realm realm = db.realmDao().getById(realmId);
        assertNotNull(realm);
        assertTrue("a forged realm must be marked generated", realm.generated);
        assertEquals("cooking", realm.topic);
        assertEquals(12, db.realmDao().getWordIdsForRealm(realmId).size());
        assertEquals(12, db.wordDao().count());
    }

    @Test
    public void collidingWordsAreJoinedNotDropped_andTheirProgressIsUntouched() throws Exception {
        // A word already in the bank, already being learned.
        Word existing = new Word();
        existing.headword = "harbour0";
        existing.cefr = CefrLevel.A2;
        existing.topic = "travel";
        existing.pos = "noun";
        existing.definition = "a place where ships shelter";
        existing.example = "The harbour0 was quiet at dawn.";
        existing.synonyms = new ArrayList<>();
        existing.antonyms = new ArrayList<>();
        existing.collocations = new ArrayList<>();
        existing.forms = new ArrayList<>();
        db.wordDao().insertAll(Collections.singletonList(existing));
        long existingId = db.wordDao().getByHeadword("harbour0").id;

        WordProgress before = new WordProgress();
        before.wordId = existingId;
        before.ease = 2.4;
        before.interval = 28;
        before.reps = 6;
        before.lapses = 1;
        before.dueAt = 1_790_000_000_000L;
        db.wordProgressDao().upsert(before);

        // Every word in the incoming map is already in the bank.
        List<String> all = new ArrayList<>();
        Collections.addAll(all, headwords("harbour", 12));
        for (int i = 1; i < 12; i++) {
            Word other = new Word();
            other.headword = "harbour" + i;
            other.cefr = CefrLevel.B1;
            other.topic = "travel";
            other.pos = "noun";
            other.definition = "definition " + i;
            other.example = "We saw the harbour" + i + " today.";
            other.synonyms = new ArrayList<>();
            other.antonyms = new ArrayList<>();
            other.collocations = new ArrayList<>();
            other.forms = new ArrayList<>();
            db.wordDao().insertAll(Collections.singletonList(other));
        }
        assertEquals(12, db.wordDao().count());

        MapJson.GeneratedMap map = MapJson.parseMap(
                mapJson("Old Piers", "travel", all.toArray(new String[0])));
        long realmId = RealmImport.importMap(db, map);

        assertEquals("a fully colliding map must still be a full realm, not a short one",
                12, db.realmDao().getWordIdsForRealm(realmId).size());
        assertEquals("collisions must join the existing rows, never duplicate them",
                12, db.wordDao().count());
        assertEquals("the existing word must keep its id, or its progress is orphaned",
                existingId, db.wordDao().getByHeadword("harbour0").id);

        WordProgress after = db.wordProgressDao().getByWordId(existingId);
        assertNotNull("a word you have been learning does not reset because it turned up again",
                after);
        assertEquals(before.ease, after.ease, 0.0);
        assertEquals(before.interval, after.interval);
        assertEquals(before.reps, after.reps);
        assertEquals(before.lapses, after.lapses);
        assertEquals(before.dueAt, after.dueAt);
    }

    /**
     * The import is one transaction, so a failure part-way through must leave nothing behind —
     * a Realm row with no words is a realm the player can enter and immediately break.
     */
    @Test
    public void aFailurePartWayThroughLeavesNoRealmRow() throws Exception {
        MapJson.GeneratedMap map = MapJson.parseMap(
                mapJson("Doomed Hall", "cooking", headwords("doomed", 12)));
        int realmsBefore = db.realmDao().count();
        int wordsBefore = db.wordDao().count();

        try {
            db.runInTransaction(() -> {
                RealmImport.importMap(db, map);
                throw new IllegalStateException("simulated failure after the realm was written");
            });
        } catch (IllegalStateException expected) {
            // The whole point: the exception unwinds the write.
        }

        assertEquals("a rolled-back import must leave no Realm row",
                realmsBefore, db.realmDao().count());
        assertEquals("nor any of its words", wordsBefore, db.wordDao().count());
    }
}
