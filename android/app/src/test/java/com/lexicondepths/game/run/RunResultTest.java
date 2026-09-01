package com.lexicondepths.game.run;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.lexicondepths.db.RunStatus;
import com.lexicondepths.db.entity.Run;
import com.lexicondepths.db.entity.WordEvent;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RunResultTest {

    private static WordEvent failedEvent(long wordId) {
        WordEvent e = new WordEvent();
        e.wordId = wordId;
        e.ratio = 0.5;
        return e;
    }

    @Test
    public void dedupesTheSameFailedWordAcrossMultipleEncounters() {
        Run run = new Run();
        run.status = RunStatus.LOST;
        run.floor = 2;
        run.marks = 40;

        List<WordEvent> failed = new ArrayList<>();
        failed.add(failedEvent(1L));
        failed.add(failedEvent(2L));
        failed.add(failedEvent(1L)); // same word missed twice across the run

        RunResult result = RunResult.from(run, failed, Collections.emptySet());

        assertEquals(2, result.failedWordIds.size());
        assertTrue(result.failedWordIds.contains(1L));
        assertTrue(result.failedWordIds.contains(2L));
    }

    @Test
    public void marksPassThroughUnchangedWithoutTheRelic() {
        Run run = new Run();
        run.status = RunStatus.WON;
        run.floor = 3;
        run.marks = 100;

        RunResult result = RunResult.from(run, Collections.emptyList(), Collections.emptySet());

        assertEquals(100, result.marksEarned);
    }

    @Test
    public void marksPlus25RelicRoundsUpAQuarterBonus() {
        Run run = new Run();
        run.status = RunStatus.WON;
        run.floor = 3;
        run.marks = 100;

        Set<String> relics = new HashSet<>();
        relics.add(RunEngine.MARKS_PLUS_25);
        RunResult result = RunResult.from(run, Collections.emptyList(), relics);

        assertEquals(125, result.marksEarned);
    }

    @Test
    public void carriesRunStatusAndFloorReachedThrough() {
        Run run = new Run();
        run.status = RunStatus.WON;
        run.floor = 3;
        run.marks = 0;

        RunResult result = RunResult.from(run, Collections.emptyList(), Collections.emptySet());

        assertEquals(RunStatus.WON, result.status);
        assertEquals(3, result.floorReached);
    }
}
