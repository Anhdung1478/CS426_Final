package com.lexicondepths.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.lexicondepths.db.CefrLevel;

/**
 * Singleton row (id=1): CEFR level, marks, streak, best floor.
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
    public long lastActiveAt = 0L;
}
