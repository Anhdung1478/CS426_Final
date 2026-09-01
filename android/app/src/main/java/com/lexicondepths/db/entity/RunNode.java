package com.lexicondepths.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.lexicondepths.db.NodeType;

/**
 * One node on the map. slot is 0 or 1 (the branch choice).
 * Wiped by RunDao.clearRunState() when the run ends — see the permadeath boundary.
 */
@Entity(indices = {@Index("runId")})
public class RunNode {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long runId;
    public int floor;
    public int step;
    public int slot;

    @NonNull
    public NodeType type;

    public boolean cleared = false;

    @Nullable
    public String monsterId;

    /** How many of the monster's slots are already answered — lets a killed app resume mid-battle. */
    public int slotsCleared = 0;

    /** Whether FIRST_MISS_FREE has already been spent in this node's encounter (per-battle, not per-run). */
    public boolean firstMissUsed = false;
}
