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
    // Always non-null today: only BattleActivity writes WordEvent rows. Practice grades a
    // card by self-rating, which is not a completion ratio and must not be averaged into
    // the per-question-type accuracy the stats screen reports. Nullable so a future
    // non-run question source does not need a schema change.
    public Long runId;

    @NonNull
    public String questionType;

    public double ratio;
    public int damageDealt;
    public long elapsedMillis;
    public long timestamp;
}
