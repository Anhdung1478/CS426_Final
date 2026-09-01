package com.lexicondepths.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.lexicondepths.db.entity.WordEvent;

import java.util.List;

@Dao
public interface WordEventDao {

    @Insert
    void insert(WordEvent event);

    @Query("SELECT * FROM WordEvent WHERE runId = :runId")
    List<WordEvent> getEventsForRun(long runId);

    @Query("SELECT * FROM WordEvent WHERE runId = :runId AND ratio < 1.0")
    List<WordEvent> getFailedEventsForRun(long runId);
}
