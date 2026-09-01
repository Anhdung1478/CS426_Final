package com.lexicondepths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.lexicondepths.db.AppDatabase;
import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.NodeType;
import com.lexicondepths.db.RunStatus;
import com.lexicondepths.db.entity.Run;
import com.lexicondepths.db.entity.RunNode;
import com.lexicondepths.db.entity.RunRelic;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.db.entity.WordEvent;
import com.lexicondepths.db.entity.WordProgress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Guards the project's central invariant: ending a run never touches WordProgress.
 * See project-context.md §5.
 */
@RunWith(AndroidJUnit4.class)
public class PermadeathBoundaryTest {

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

    @Test
    public void clearRunState_deletesRunScopedTables_leavesWordProgressUntouched() {
        // 1. Insert a Word and a WordProgress with a known ease, interval, and due date.
        Word word = new Word();
        word.headword = "decide";
        word.cefr = CefrLevel.A2;
        word.topic = "business";
        word.pos = "verb";
        word.definition = "to choose something after thinking about it";
        word.example = "They decided to open a second branch.";
        word.viGloss = "quyết định";
        word.synonyms = Collections.singletonList("choose");
        word.antonyms = Collections.singletonList("hesitate");
        word.collocations = Arrays.asList("make a decision", "decide on");
        word.forms = Arrays.asList("decides", "decided", "decision");
        db.wordDao().insertAll(Collections.singletonList(word));
        Word insertedWord = db.wordDao().getByHeadword("decide");

        WordProgress progress = new WordProgress();
        progress.wordId = insertedWord.id;
        progress.ease = 2.1;
        progress.interval = 6;
        progress.reps = 2;
        progress.lapses = 1;
        progress.dueAt = 1_700_000_000_000L;
        db.wordProgressDao().upsert(progress);

        // 2. Start a run, add nodes and relics, record a WordEvent.
        Run run = new Run();
        run.status = RunStatus.ACTIVE;
        run.hp = 100;
        run.floor = 1;
        run.step = 1;
        run.marks = 0;
        run.seed = 42L;
        run.startedAt = System.currentTimeMillis();
        long runId = db.runDao().insertRun(run);

        RunNode node = new RunNode();
        node.runId = runId;
        node.floor = 1;
        node.step = 1;
        node.slot = 0;
        node.type = NodeType.BATTLE;
        node.monsterId = "hydra";
        db.runDao().insertNodes(Collections.singletonList(node));

        RunRelic relic = new RunRelic();
        relic.runId = runId;
        relic.relicId = "lexicon_shard";
        relic.acquiredAt = System.currentTimeMillis();
        db.runDao().insertRelic(relic);

        WordEvent event = new WordEvent();
        event.wordId = insertedWord.id;
        event.runId = runId;
        event.questionType = "AFFIX_HARVEST";
        event.ratio = 0.6;
        event.damageDealt = 4;
        event.elapsedMillis = 8000;
        event.timestamp = System.currentTimeMillis();
        db.wordEventDao().insert(event);

        // 3. Call clearRunState().
        db.runDao().clearRunState(runId);

        // 4. Assert Run/RunNode/RunRelic rows are gone.
        assertNull("Run row must be deleted", db.runDao().getRun(runId));

        List<RunNode> remainingNodes = db.runDao().getNodesForRun(runId);
        assertTrue("RunNode rows must be deleted", remainingNodes.isEmpty());

        List<RunRelic> remainingRelics = db.runDao().getRelicsForRun(runId);
        assertTrue("RunRelic rows must be deleted", remainingRelics.isEmpty());

        // 5. Assert the WordProgress row is byte-for-byte identical.
        WordProgress afterClear = db.wordProgressDao().getByWordId(insertedWord.id);
        assertEquals(progress.wordId, afterClear.wordId);
        assertEquals(progress.ease, afterClear.ease, 0.0);
        assertEquals(progress.interval, afterClear.interval);
        assertEquals(progress.reps, afterClear.reps);
        assertEquals(progress.lapses, afterClear.lapses);
        assertEquals(progress.dueAt, afterClear.dueAt);
    }

    /**
     * P2-12's Spoils path: a loss resets dueAt to now for every failed word so it resurfaces
     * immediately, but per the design doc's permadeath boundary callout, that reset must never
     * touch ease, interval, reps, or lapses — the word comes back sooner, not as if never learned.
     */
    @Test
    public void spoils_resetDueNow_changesOnlyDueAt_leavesMasteryUntouched() {
        Word word = new Word();
        word.headword = "resilient";
        word.cefr = CefrLevel.B2;
        word.topic = "emotions";
        word.pos = "adjective";
        word.definition = "able to recover quickly from difficulties";
        word.example = "She stayed resilient after the setback.";
        word.synonyms = Collections.singletonList("tough");
        word.antonyms = Collections.singletonList("fragile");
        word.collocations = Collections.singletonList("remain resilient");
        word.forms = Arrays.asList("resiliently", "resilience");
        db.wordDao().insertAll(Collections.singletonList(word));
        Word insertedWord = db.wordDao().getByHeadword("resilient");

        WordProgress progress = new WordProgress();
        progress.wordId = insertedWord.id;
        progress.ease = 1.9;
        progress.interval = 12;
        progress.reps = 4;
        progress.lapses = 2;
        progress.dueAt = 1_800_000_000_000L; // far in the future — a normal, healthy schedule
        db.wordProgressDao().upsert(progress);

        // A run failed this word: WordEvent with ratio < 1.0 is what Spoils reads to find it.
        Run run = new Run();
        run.status = RunStatus.ACTIVE;
        run.hp = 0;
        run.floor = 2;
        run.step = 3;
        run.marks = 20;
        run.seed = 7L;
        run.startedAt = System.currentTimeMillis();
        long runId = db.runDao().insertRun(run);

        WordEvent event = new WordEvent();
        event.wordId = insertedWord.id;
        event.runId = runId;
        event.questionType = "CLOZE";
        event.ratio = 0.0;
        event.damageDealt = 19;
        event.elapsedMillis = 5000;
        event.timestamp = System.currentTimeMillis();
        db.wordEventDao().insert(event);

        List<WordEvent> failed = db.wordEventDao().getFailedEventsForRun(runId);
        assertEquals(1, failed.size());

        long resetNow = 1_700_000_000_000L; // strictly before the original dueAt — proves the reset actually moved it
        assertTrue("test setup: resetNow should be earlier than the original schedule", resetNow < progress.dueAt);
        db.wordProgressDao().resetDueNow(failed.get(0).wordId, resetNow);

        // Ending the run must still route through the same permadeath-safe cleanup.
        db.runDao().clearRunState(runId);

        WordProgress afterSpoils = db.wordProgressDao().getByWordId(insertedWord.id);
        assertEquals("Spoils must move dueAt to now", resetNow, afterSpoils.dueAt);
        assertEquals("ease must survive a loss unchanged", progress.ease, afterSpoils.ease, 0.0);
        assertEquals("interval must survive a loss unchanged", progress.interval, afterSpoils.interval);
        assertEquals("reps must survive a loss unchanged", progress.reps, afterSpoils.reps);
        assertEquals("lapses must survive a loss unchanged", progress.lapses, afterSpoils.lapses);
    }
}
