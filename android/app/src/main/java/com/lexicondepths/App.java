package com.lexicondepths;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
        // A real migration, not a destructive fallback: dropping every table on a version
        // mismatch would take WordProgress with it, which is the one thing §5 forbids.
        db = Room.databaseBuilder(this, AppDatabase.class, "lexicon.db")
                .addMigrations(AppDatabase.MIGRATION_2_3)
                .build();
        io.execute(() -> {
            SeedLoader.run(this, db);
            ensureProfile(db);
        });
        registerActivityLifecycleCallbacks(new InsetPadding());
    }

    /**
     * targetSdk 35 makes every Activity edge-to-edge, so without this each screen draws its
     * first line under the status bar clock and its last under the gesture bar.
     *
     * One lifecycle callback rather than a line in each of the twelve Activities, and rather
     * than android:fitsSystemWindows — that attribute *replaces* a view's padding, so every
     * layout would silently lose the android:padding it already declares. This adds to it.
     */
    private static final class InsetPadding implements ActivityLifecycleCallbacks {

        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle state) {
            ViewGroup content = activity.findViewById(android.R.id.content);
            if (content == null) {
                return;
            }
            // This callback runs from super.onCreate(), before setContentView, so the content
            // frame is still empty. Posting defers to just after onCreate returns.
            content.post(() -> applyTo(content.getChildAt(0)));
        }

        private static void applyTo(View root) {
            if (root == null) {
                return;
            }
            int left = root.getPaddingLeft();
            int top = root.getPaddingTop();
            int right = root.getPaddingRight();
            int bottom = root.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
                // The IME is in the mask because BattleActivity's free-text answers use
                // adjustResize; without it the keyboard covers the input it opened for.
                Insets bars = windowInsets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                view.setPadding(left + bars.left, top + bars.top,
                        right + bars.right, bottom + bars.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
            // The first inset dispatch has already happened by now; without this the padding
            // would not land until something else triggered a pass.
            ViewCompat.requestApplyInsets(root);
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle out) {
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
        }
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
