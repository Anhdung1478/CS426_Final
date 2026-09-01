package com.lexicondepths.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.lexicondepths.db.dao.ProfileDao;
import com.lexicondepths.db.dao.RealmDao;
import com.lexicondepths.db.dao.RunDao;
import com.lexicondepths.db.dao.WordDao;
import com.lexicondepths.db.dao.WordEventDao;
import com.lexicondepths.db.dao.WordProgressDao;
import com.lexicondepths.db.entity.Profile;
import com.lexicondepths.db.entity.Realm;
import com.lexicondepths.db.entity.RealmWord;
import com.lexicondepths.db.entity.Run;
import com.lexicondepths.db.entity.RunNode;
import com.lexicondepths.db.entity.RunRelic;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.db.entity.WordEvent;
import com.lexicondepths.db.entity.WordProgress;

@Database(
        entities = {
                Word.class, WordProgress.class,
                Realm.class, RealmWord.class,
                Run.class, RunNode.class, RunRelic.class,
                WordEvent.class, Profile.class
        },
        version = 1,
        exportSchema = true
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    public abstract WordDao wordDao();

    public abstract WordProgressDao wordProgressDao();

    public abstract RealmDao realmDao();

    public abstract RunDao runDao();

    public abstract WordEventDao wordEventDao();

    public abstract ProfileDao profileDao();
}
