package com.lexicondepths.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.lexicondepths.db.entity.Profile;

@Dao
public interface ProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(Profile profile);

    @Query("SELECT * FROM Profile WHERE id = 1")
    LiveData<Profile> getProfile();

    @Query("SELECT * FROM Profile WHERE id = 1")
    Profile getProfileSync();

    /**
     * Deduction and assignment are one transaction on purpose: as two Activity-side calls, a
     * crash between them either takes the Marks without granting the relic or the reverse.
     *
     * Returns false and writes nothing when the player cannot afford it. Buying a second relic
     * replaces the first with no refund — stated on the shop screen so it is a choice, not a
     * surprise.
     */
    @Transaction
    default boolean buyRelic(String relicId, int price) {
        Profile profile = getProfileSync();
        if (profile == null || price < 0 || profile.marks < price) {
            return false;
        }
        profile.marks -= price;
        profile.pendingRelicId = relicId;
        upsert(profile);
        return true;
    }
}
