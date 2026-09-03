package com.lexicondepths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.lexicondepths.content.Monster;
import com.lexicondepths.content.Relic;
import com.lexicondepths.db.AppDatabase;
import com.lexicondepths.db.entity.Profile;
import com.lexicondepths.db.entity.RunRelic;
import com.lexicondepths.game.run.RunEngine;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * P4-8's two rules, on a real database: the deduction and the assignment are one transaction,
 * and the purchase is consumed by exactly one run.
 *
 * The active-run case is the one worth having. RunEngine.startRun returns early when a run is
 * already going (the P3-5 guard); consuming the purchase on that path would take a relic the
 * player paid Marks for and give them nothing, on a code path they cannot see.
 */
@RunWith(AndroidJUnit4.class)
public class MarksPurchaseTest {

    private static final int PRICE = 60;

    private AppDatabase db;
    private List<Relic> catalog;
    private List<Monster> monsters;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();

        Relic shard = new Relic();
        shard.id = "lexicon_shard";
        shard.name = "Lexicon Shard";
        shard.desc = "+10 maximum health.";
        shard.effect = RunEngine.MAX_HP_PLUS_10;
        shard.price = PRICE;
        catalog = Collections.singletonList(shard);

        monsters = new ArrayList<>();
        monsters.add(monster("twins", false));
        monsters.add(monster("archivist", true));
    }

    private static Monster monster(String id, boolean boss) {
        Monster m = new Monster();
        m.id = id;
        m.name = id;
        m.questionTypes = Collections.singletonList("SYNONYM_ANTONYM");
        m.slots = 2;
        m.resists = Collections.emptyList();
        m.ascii = Collections.emptyList();
        m.boss = boss;
        return m;
    }

    private Profile profileWithMarks(int marks) {
        Profile profile = new Profile();
        profile.marks = marks;
        db.profileDao().upsert(profile);
        return profile;
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void buyingDeductsMarksAndAssignsTheRelicTogether() {
        profileWithMarks(100);

        assertTrue(db.profileDao().buyRelic("lexicon_shard", PRICE));

        Profile after = db.profileDao().getProfileSync();
        assertEquals(40, after.marks);
        assertEquals("lexicon_shard", after.pendingRelicId);
    }

    @Test
    public void buyingWithoutEnoughMarksWritesNothingAtAll() {
        profileWithMarks(59);

        assertFalse(db.profileDao().buyRelic("lexicon_shard", PRICE));

        Profile after = db.profileDao().getProfileSync();
        assertEquals("a refused purchase must not deduct anything", 59, after.marks);
        assertNull("nor assign the relic", after.pendingRelicId);
    }

    @Test
    public void thePurchaseIsConsumedByExactlyOneRun() {
        profileWithMarks(100);
        db.profileDao().buyRelic("lexicon_shard", PRICE);

        long firstRun = RunEngine.startRun(db, null, monsters, catalog);
        List<RunRelic> granted = db.runDao().getRelicsForRun(firstRun);
        assertEquals(1, granted.size());
        assertEquals("lexicon_shard", granted.get(0).relicId);
        assertNull("a purchase surviving into a second run is a free relic every run forever",
                db.profileDao().getProfileSync().pendingRelicId);

        // MAX_HP_PLUS_10 has to actually apply, not just exist as a row.
        assertEquals(RunEngine.STARTING_HP + 10, db.runDao().getRun(firstRun).hp);

        // End the run the way Spoils does, then start another: no relic this time.
        db.runDao().updateRun(endedRun(firstRun));
        db.runDao().clearRunState(firstRun);
        long secondRun = RunEngine.startRun(db, null, monsters, catalog);
        assertTrue(db.runDao().getRelicsForRun(secondRun).isEmpty());
        assertEquals(RunEngine.STARTING_HP, db.runDao().getRun(secondRun).hp);
    }

    @Test
    public void theActiveRunGuardLeavesThePurchaseIntact() {
        profileWithMarks(200);
        long firstRun = RunEngine.startRun(db, null, monsters, catalog); // no purchase yet
        assertTrue(db.runDao().getRelicsForRun(firstRun).isEmpty());

        db.profileDao().buyRelic("lexicon_shard", PRICE);

        // startRun returns the existing run instead of creating one. The player did not get a
        // new run, so they must not lose what they paid for.
        long sameRun = RunEngine.startRun(db, null, monsters, catalog);
        assertEquals(firstRun, sameRun);
        assertEquals("lexicon_shard", db.profileDao().getProfileSync().pendingRelicId);
        assertTrue("and it must not have leaked into the run already in progress",
                db.runDao().getRelicsForRun(firstRun).isEmpty());
    }

    private com.lexicondepths.db.entity.Run endedRun(long runId) {
        com.lexicondepths.db.entity.Run run = db.runDao().getRun(runId);
        assertNotNull(run);
        run.status = com.lexicondepths.db.RunStatus.LOST;
        return run;
    }
}
