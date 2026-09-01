package com.lexicondepths.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.lexicondepths.db.entity.Profile;

@Dao
public interface ProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(Profile profile);

    @Query("SELECT * FROM Profile WHERE id = 1")
    LiveData<Profile> getProfile();

    @Query("SELECT * FROM Profile WHERE id = 1")
    Profile getProfileSync();
}
