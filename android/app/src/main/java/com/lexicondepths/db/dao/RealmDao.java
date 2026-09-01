package com.lexicondepths.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.lexicondepths.db.entity.Realm;
import com.lexicondepths.db.entity.RealmWord;

import java.util.List;

@Dao
public interface RealmDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Realm realm);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertRealmWords(List<RealmWord> realmWords);

    @Query("SELECT * FROM Realm")
    LiveData<List<Realm>> getAll();

    @Query("SELECT COUNT(*) FROM Realm")
    int count();

    @Query("SELECT * FROM Realm WHERE id = :id")
    Realm getById(long id);

    @Query("SELECT wordId FROM RealmWord WHERE realmId = :realmId")
    List<Long> getWordIdsForRealm(long realmId);
}
