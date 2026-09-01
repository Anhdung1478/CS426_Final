package com.lexicondepths;

import android.app.Application;

import androidx.room.Room;

import com.lexicondepths.content.SeedLoader;
import com.lexicondepths.db.AppDatabase;
import com.lexicondepths.db.entity.Profile;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App extends Application {

    private static App instance;

    private AppDatabase db;
    private Prefs prefs;
    private ExecutorService io;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        io = Executors.newFixedThreadPool(4);
        prefs = new Prefs(this);
        db = Room.databaseBuilder(this, AppDatabase.class, "lexicon.db").build();
        io.execute(() -> {
            SeedLoader.run(this, db);
            ensureProfile(db);
        });
    }

    private static void ensureProfile(AppDatabase db) {
        if (db.profileDao().getProfileSync() == null) {
            db.profileDao().upsert(new Profile());
        }
    }

    public static App get() {
        return instance;
    }

    public AppDatabase db() {
        return db;
    }

    public Prefs prefs() {
        return prefs;
    }

    public ExecutorService io() {
        return io;
    }
}
