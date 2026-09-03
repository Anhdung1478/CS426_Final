package com.lexicondepths.game.stats;

import com.lexicondepths.game.question.QuestionType;

import java.util.function.Predicate;

/**
 * Nine predicates over a StatsSnapshot. Nothing is stored — no Achievement table, no
 * unlockedAt column, no unlock detection wired into SpoilsActivity or BattleActivity.
 *
 * What that costs, stated rather than discovered: there is no "achievement unlocked" moment.
 * A player who crosses 50 mastered words finds out the next time they open the stats screen.
 * That is a real loss of game feel and the right trade in the last phase — a table plus unlock
 * detection in two Activities that currently pass their tests is new risk bought for a toast.
 *
 * ⚠️ Every predicate reads only Profile-, WordProgress-, WordEvent- and Realm-derived fields,
 * never anything run-scoped. Run/RunNode/RunRelic are wiped by RunDao.clearRunState(), so a
 * predicate over them would silently re-lock an earned achievement the moment a run ended —
 * the §5 permadeath boundary violated in a new place.
 */
public enum Achievements {

    FIRST_DESCENT(s -> s.totalRuns >= 1),
    DELVER(s -> s.bestFloor >= 3),
    DEPTH_CONQUEROR(s -> s.runsWon >= 1),
    WORD_HOARD(s -> s.wordsSeen() >= Threshold.WORD_HOARD_WORDS),
    LEXICOGRAPHER(s -> s.mastered >= Threshold.LEXICOGRAPHER_MASTERED),
    UNBROKEN(s -> s.streak >= Threshold.UNBROKEN_STREAK),
    REALM_FORGER(s -> s.generatedRealms >= 1),
    DILIGENT(s -> s.totalAnswers >= Threshold.DILIGENT_ANSWERS),
    /** The only achievement rewarding a specific skill: collocation is the hardest area for
     * Vietnamese learners (project-idea.md §3), so this is the one worth earning. */
    SILVER_TONGUE(s -> s.attemptsFor(QuestionType.COLLOCATION.name()) >= Threshold.SILVER_TONGUE_ATTEMPTS
            && s.accuracyPercentFor(QuestionType.COLLOCATION.name()) >= Threshold.SILVER_TONGUE_PERCENT);

    /**
     * A nested holder, not plain static fields on the enum: an enum constant's initializer runs
     * before the enum's own statics, so referencing them directly is an illegal forward
     * reference. The tests assert against these rather than against repeated literals.
     */
    public static final class Threshold {
        public static final int WORD_HOARD_WORDS = 150;
        public static final int LEXICOGRAPHER_MASTERED = 50;
        public static final int UNBROKEN_STREAK = 7;
        public static final int DILIGENT_ANSWERS = 200;
        public static final int SILVER_TONGUE_ATTEMPTS = 20;
        public static final int SILVER_TONGUE_PERCENT = 80;

        private Threshold() {
        }
    }

    private final Predicate<StatsSnapshot> unlocked;

    Achievements(Predicate<StatsSnapshot> unlocked) {
        this.unlocked = unlocked;
    }

    public boolean isUnlockedBy(StatsSnapshot snapshot) {
        return unlocked.test(snapshot);
    }
}
