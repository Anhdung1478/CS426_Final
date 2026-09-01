package com.lexicondepths.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Relics held this run. Wiped by RunDao.clearRunState() when the run ends —
 * see the permadeath boundary.
 */
@Entity(indices = {@Index("runId")})
public class RunRelic {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long runId;

    @NonNull
    public String relicId;

    public long acquiredAt;
}
