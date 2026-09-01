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
}
