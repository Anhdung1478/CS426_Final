package com.lexicondepths.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Every answer given: ratio, damage, timestamp. Feeds Spoils and stats.
 * Never touched by RunDao.clearRunState() — see the permadeath boundary.
 */
@Entity(indices = {@Index("wordId"), @Index("runId")})
public class WordEvent {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long wordId;

    @Nullable
    public Long runId; // null == answered in Practice mode, not a run

    @NonNull
    public String questionType;

    public double ratio;
    public int damageDealt;
    public long elapsedMillis;
    public long timestamp;
}
