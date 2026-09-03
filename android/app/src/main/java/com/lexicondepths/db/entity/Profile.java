package com.lexicondepths.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.lexicondepths.db.CefrLevel;

/**
 * Singleton row (id=1): CEFR level, marks, streak, best floor, and the one relic bought with
 * Marks that the next run will start with.
 */
@Entity
public class Profile {

    @PrimaryKey
    public long id = 1L;

    @NonNull
    public CefrLevel cefrLevel = CefrLevel.B1;

    public int marks = 0;
    public int streak = 0;
    public int bestFloor = 0;
    public int totalRuns = 0;

    /**
     * A streak proves a *recent* win and nothing more — a later loss zeroes it, which would
     * retroactively re-lock a "win a run" achievement. runsWon only ever goes up.
     */
    public int runsWon = 0;

    /** Bought at the shop (P4-8), consumed by exactly one RunEngine.startRun. */
    @Nullable
    public String pendingRelicId;

    public long lastActiveAt = 0L;
}
