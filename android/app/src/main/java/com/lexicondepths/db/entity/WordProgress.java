package com.lexicondepths.db.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * The mastery table. Never touched by a run ending — see RunDao.clearRunState().
 */
@Entity(indices = {@Index("dueAt")})
public class WordProgress {

    @PrimaryKey
    public long wordId;

    public double ease = 2.5;
    public int interval = 0;
    public int reps = 0;
    public int lapses = 0;
    public long dueAt = 0L;
}
