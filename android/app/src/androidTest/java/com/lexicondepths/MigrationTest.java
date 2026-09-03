package com.lexicondepths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.lexicondepths.db.AppDatabase;
import com.lexicondepths.db.entity.WordProgress;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

/**
 * The upgrade path, tested because until P4-11 there was not one: App.java used Room's
 * destructive fallback, which drops every table on a version mismatch — WordProgress included.
 * project-context.md §5 names that table as the one thing a run ending must never touch, and
 * an APK upgrade would have done exactly what losing a run is forbidden to do.
 *
 * So this test is not schema bookkeeping. It is the same invariant PermadeathBoundaryTest
 * guards, checked against the other threat.
 */
@RunWith(AndroidJUnit4.class)
public class MigrationTest {

    private static final String TEST_DB = "migration-test.db";

    @Rule
    public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase.class,
            java.util.Collections.emptyList(),
            new FrameworkSQLiteOpenHelperFactory());

    @Test
    public void migrate2To3_leavesEverySrsFieldUntouched() throws IOException {
        WordProgress before = new WordProgress();
        before.wordId = 1L;
        before.ease = 2.13;
        before.interval = 34;
        before.reps = 7;
        before.lapses = 3;
        before.dueAt = 1_777_000_000_000L;

        SupportSQLiteDatabase v2 = helper.createDatabase(TEST_DB, 2);
        v2.execSQL("INSERT INTO Word (id, headword, cefr, topic, pos, definition, example, "
                + "viGloss, synonyms, antonyms, collocations, forms, affixKey) VALUES "
                + "(1, 'resilient', 'B2', 'emotions', 'adjective', 'able to recover quickly', "
                + "'She stayed resilient after the setback.', 'kiên cường', '', '', '', '', NULL)");
        ContentValues progress = new ContentValues();
        progress.put("wordId", before.wordId);
        progress.put("ease", before.ease);
        progress.put("interval", before.interval);
        progress.put("reps", before.reps);
        progress.put("lapses", before.lapses);
        progress.put("dueAt", before.dueAt);
        v2.insert("WordProgress", android.database.sqlite.SQLiteDatabase.CONFLICT_ABORT, progress);
        v2.execSQL("INSERT INTO Profile (id, cefrLevel, marks, streak, bestFloor, totalRuns, "
                + "lastActiveAt) VALUES (1, 'B1', 140, 3, 2, 9, 0)");
        v2.close();

        // validateDroppedTables=true: a migration that quietly recreated a table would pass a
        // column check and still have destroyed the rows.
        helper.runMigrationsAndValidate(TEST_DB, 3, true, AppDatabase.MIGRATION_2_3);

        AppDatabase db = Room.databaseBuilder(
                        ApplicationProvider.getApplicationContext(), AppDatabase.class, TEST_DB)
                .addMigrations(AppDatabase.MIGRATION_2_3)
                .allowMainThreadQueries()
                .build();
        try {
            WordProgress after = db.wordProgressDao().getByWordId(1L);
            assertNotNull("the mastery row must survive an APK upgrade", after);
            assertEquals(before.ease, after.ease, 0.0);
            assertEquals(before.interval, after.interval);
            assertEquals(before.reps, after.reps);
            assertEquals(before.lapses, after.lapses);
            assertEquals(before.dueAt, after.dueAt);

            // The three new columns exist with their declared defaults.
            assertEquals(0, db.profileDao().getProfileSync().runsWon);
            assertNull(db.profileDao().getProfileSync().pendingRelicId);
            assertEquals(140, db.profileDao().getProfileSync().marks);
            assertNull(db.wordDao().getById(1L).formalAlt);
        } finally {
            db.close();
        }
    }

    /** The migration path and the create path both have to work — a fresh v3 install is not
     * exercised by runMigrationsAndValidate. */
    @Test
    public void freshV3DatabaseHasTheNewColumns() {
        AppDatabase db = Room.inMemoryDatabaseBuilder(
                        ApplicationProvider.getApplicationContext(), AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        try (Cursor profile = db.query("SELECT * FROM Profile", null);
             Cursor word = db.query("SELECT * FROM Word", null)) {
            assertTrue(hasColumn(profile, "runsWon"));
            assertTrue(hasColumn(profile, "pendingRelicId"));
            assertTrue(hasColumn(word, "formalAlt"));
            assertFalse("a v2-only column should not reappear", hasColumn(word, "obsolete"));
        } finally {
            db.close();
        }
    }

    private static boolean hasColumn(Cursor cursor, String name) {
        return cursor.getColumnIndex(name) >= 0;
    }
}
