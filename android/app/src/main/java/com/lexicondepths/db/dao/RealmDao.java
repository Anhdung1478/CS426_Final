package com.lexicondepths.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Embedded;
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

    /**
     * My Library (P3-7) shows only forged realms, newest first, with a word count. The count is
     * a correlated subquery rather than a per-row lookup so the whole screen is one observed query.
     */
    @Query("SELECT r.*, (SELECT COUNT(*) FROM RealmWord rw WHERE rw.realmId = r.id) AS wordCount "
            + "FROM Realm r WHERE r.generated = 1 ORDER BY r.createdAt DESC")
    LiveData<List<LibraryRow>> getGeneratedRealms();

    class LibraryRow {
        @Embedded
        public Realm realm;
        public int wordCount;
    }
}
