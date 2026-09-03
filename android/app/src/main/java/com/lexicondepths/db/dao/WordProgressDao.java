package com.lexicondepths.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.lexicondepths.db.entity.WordProgress;

@Dao
public interface WordProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(WordProgress progress);

    @Update
    void update(WordProgress progress);

    @Query("SELECT * FROM WordProgress WHERE wordId = :wordId")
    WordProgress getByWordId(long wordId);

    /** Spoils: reset dueAt to now for every failed word, leaving ease/reps/lapses untouched. */
    @Query("UPDATE WordProgress SET dueAt = :now WHERE wordId = :wordId")
    void resetDueNow(long wordId, long now);

    /**
     * INSERT OR IGNORE, so this is a no-op for a word that already has a row — which is what
     * keeps resetDueNowOrCreate from touching an existing word's ease/interval/reps/lapses.
     */
    @Query("INSERT OR IGNORE INTO WordProgress (wordId, ease, interval, reps, lapses, dueAt) "
            + "VALUES (:wordId, 2.5, 0, 0, 0, :now)")
    void createDueNowIfAbsent(long wordId, long now);

    /**
     * What Spoils actually needs. project-context.md §5 promises that on death "every word
     * failed during the run gets dueAt reset to now, so it resurfaces immediately" — but a
     * word met for the first time *in a battle* has no WordProgress row yet, and a bare UPDATE
     * silently matches nothing. Those are exactly the words a player just proved they don't
     * know, and they were the ones the promise skipped.
     *
     * The insert is defaults-only and the update touches dueAt alone, so the permadeath
     * boundary holds either way.
     */
    @Transaction
    default void resetDueNowOrCreate(long wordId, long now) {
        createDueNowIfAbsent(wordId, now);
        resetDueNow(wordId, now);
    }

    // ---- P4-1 stats aggregates. A query belongs to the table it reads, so the mastery
    // buckets live here rather than in a new stats DAO. matureDays is passed in from
    // StatsSnapshot.MASTERED_INTERVAL_DAYS — never a literal in the SQL.

    @Query("SELECT COUNT(*) FROM WordProgress WHERE `interval` < :matureDays")
    int countLearning(int matureDays);

    @Query("SELECT COUNT(*) FROM WordProgress WHERE `interval` >= :matureDays")
    int countMastered(int matureDays);

    @Query("SELECT COUNT(*) FROM WordProgress WHERE dueAt <= :now")
    int countDue(long now);
}
