package com.lexicondepths.game.run;

import com.lexicondepths.db.RunStatus;
import com.lexicondepths.db.entity.Run;
import com.lexicondepths.db.entity.WordEvent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The end-of-run summary Spoils (P2-12) is built from: which words to reset dueAt on, and how
 * many Marks to credit to Profile. Pure and Android-free so the dedup and MARKS_PLUS_25 math
 * are unit-testable without a database.
 */
public final class RunResult {

    public final RunStatus status;
    public final int floorReached;
    public final int marksEarned;
    public final Set<Long> failedWordIds;

    private RunResult(RunStatus status, int floorReached, int marksEarned, Set<Long> failedWordIds) {
        this.status = status;
        this.floorReached = floorReached;
        this.marksEarned = marksEarned;
        this.failedWordIds = failedWordIds;
    }

    /**
     * failedEvents is WordEventDao.getFailedEventsForRun() — every ratio < 1.0 row for this run,
     * which can repeat a wordId across separate encounters; grouped down to one entry per word
     * per the design doc ("Read the failed words from WordEvent where ratio < 1.0, grouped by word").
     */
    public static RunResult from(Run run, List<WordEvent> failedEvents, Set<String> relicIds) {
        Set<Long> failedWordIds = new LinkedHashSet<>();
        for (WordEvent event : failedEvents) {
            failedWordIds.add(event.wordId);
        }
        int marks = relicIds.contains(RunEngine.MARKS_PLUS_25)
                ? (int) Math.round(run.marks * 1.25)
                : run.marks;
        return new RunResult(run.status, run.floor, marks, failedWordIds);
    }
}
