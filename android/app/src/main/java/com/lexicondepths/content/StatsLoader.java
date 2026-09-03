package com.lexicondepths.content;

import com.lexicondepths.db.AppDatabase;
import com.lexicondepths.db.entity.Profile;
import com.lexicondepths.game.stats.StatsSnapshot;

/**
 * Assembles the one StatsSnapshot the stats screen, the achievements and the review reminder
 * all read. content/ is already where the readers of Room that return plain objects live —
 * SeedLoader, MonsterCatalog, RealmImport — and this is exactly that shape.
 *
 * Blocking; call it from App.io().
 */
public final class StatsLoader {

    /** One miss is noise. Two attempts is the cheapest bar that keeps a fluke off the list. */
    static final int MIN_ATTEMPTS_FOR_WEAK = 2;
    static final int WEAK_WORD_LIMIT = 10;

    private StatsLoader() {
    }

    public static StatsSnapshot load(AppDatabase db, long now) {
        int mature = StatsSnapshot.MASTERED_INTERVAL_DAYS;
        Profile profile = db.profileDao().getProfileSync();
        if (profile == null) {
            profile = new Profile();
        }
        return new StatsSnapshot(
                db.wordDao().count(),
                db.wordProgressDao().countLearning(mature),
                db.wordProgressDao().countMastered(mature),
                db.wordProgressDao().countDue(now),
                db.wordEventDao().getTypeAccuracy(),
                db.wordEventDao().getWeakWords(MIN_ATTEMPTS_FOR_WEAK, WEAK_WORD_LIMIT),
                profile.streak,
                profile.bestFloor,
                profile.totalRuns,
                profile.runsWon,
                profile.marks,
                db.realmDao().countGenerated(),
                db.wordEventDao().countAnswers());
    }
}
