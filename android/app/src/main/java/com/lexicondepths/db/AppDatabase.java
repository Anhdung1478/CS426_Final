package com.lexicondepths.db;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

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
        version = 3,
        exportSchema = true
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    /**
     * Phase 4's three new columns. This exists instead of Room's destructive
     * fallback, which dropped every table on a version mismatch — including WordProgress, the one table
     * project-context.md §5 says a run ending must never touch. An APK upgrade would have done
     * exactly what losing a run is forbidden to do, and the comment justifying the fallback
     * ("no shipped installs to migrate yet") stopped being true at submission.
     *
     * There is deliberately no 1 -> 3 path: 1 -> 2 already ran destructively during
     * development and there are no v1 installs outside this repo. 2 -> 3 is the real one.
     */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE Profile ADD COLUMN runsWon INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE Profile ADD COLUMN pendingRelicId TEXT");
            db.execSQL("ALTER TABLE Word ADD COLUMN formalAlt TEXT");
        }
    };

    public abstract WordDao wordDao();

    public abstract WordProgressDao wordProgressDao();

    public abstract RealmDao realmDao();

    public abstract RunDao runDao();

    public abstract WordEventDao wordEventDao();

    public abstract ProfileDao profileDao();
}
