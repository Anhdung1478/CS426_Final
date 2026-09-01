package com.lexicondepths.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.lexicondepths.db.RunStatus;

/**
 * One dungeon attempt: hp, floor, step, marks, seed.
 * Wiped by RunDao.clearRunState() when the run ends — see the permadeath boundary.
 */
@Entity
public class Run {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @Nullable
    public Long realmId; // null == Echo Trial, pulling from all unlocked topics

    public int hp;
    public int floor;
    public int step;
    public int marks;
    public long seed;

    @NonNull
    public RunStatus status = RunStatus.ACTIVE;

    public long startedAt;
    public long endedAt;
}
