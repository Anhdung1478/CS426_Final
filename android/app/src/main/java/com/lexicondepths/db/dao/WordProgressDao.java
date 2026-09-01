package com.lexicondepths.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
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
}
