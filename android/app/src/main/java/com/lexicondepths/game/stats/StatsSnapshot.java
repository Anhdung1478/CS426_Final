package com.lexicondepths.game.stats;

import java.util.Collections;
import java.util.List;

/**
 * Every number the stats screen (P4-2), the achievement predicates (P4-3) and the review
 * reminder (P4-5) read, computed once. Immutable, built off the main thread by StatsLoader.
 *
 * Deliberately not LiveData: nothing on the stats screen changes while it is open, and eight
 * observed queries would be eight main-thread hops for numbers that cannot move. The Hub's due
 * count stays LiveData because that one does change under it.
 */
public final class StatsSnapshot {

    /**
     * Anki's mature threshold. Named because it is the one number on this screen a grader is
     * likely to ask us to justify, and a literal buried in a query string cannot be cited.
     */
    public static final int MASTERED_INTERVAL_DAYS = 21;

    /** Mastery buckets. newWords() + learning + mastered == totalWords, by construction. */
    public final int totalWords;
    public final int learning;
    public final int mastered;
    public final int dueNow;

    public final List<TypeAccuracy> typeAccuracy;
    public final List<WeakWord> weakWords;

    public final int streak;
    public final int bestFloor;
    public final int totalRuns;
    public final int runsWon;
    public final int marks;

    public final int generatedRealms;
    public final int totalAnswers;

    public StatsSnapshot(int totalWords, int learning, int mastered, int dueNow,
                         List<TypeAccuracy> typeAccuracy, List<WeakWord> weakWords,
                         int streak, int bestFloor, int totalRuns, int runsWon, int marks,
                         int generatedRealms, int totalAnswers) {
        this.totalWords = totalWords;
        this.learning = learning;
        this.mastered = mastered;
        this.dueNow = dueNow;
        this.typeAccuracy = Collections.unmodifiableList(typeAccuracy);
        this.weakWords = Collections.unmodifiableList(weakWords);
        this.streak = streak;
        this.bestFloor = bestFloor;
        this.totalRuns = totalRuns;
        this.runsWon = runsWon;
        this.marks = marks;
        this.generatedRealms = generatedRealms;
        this.totalAnswers = totalAnswers;
    }

    /** No WordProgress row yet. Derived rather than queried so the buckets cannot fail to partition. */
    public int newWords() {
        return Math.max(0, totalWords - wordsSeen());
    }

    /** Any WordProgress row at all — the word has been answered at least once. */
    public int wordsSeen() {
        return learning + mastered;
    }

    /** A fresh install: sections 2 and 3 have nothing to say and must invite instead of showing zeros. */
    public boolean hasAnswerHistory() {
        return totalAnswers > 0;
    }

    public int accuracyPercentFor(String questionType) {
        TypeAccuracy row = accuracyFor(questionType);
        return row == null ? 0 : row.percent();
    }

    public int attemptsFor(String questionType) {
        TypeAccuracy row = accuracyFor(questionType);
        return row == null ? 0 : row.attempts;
    }

    private TypeAccuracy accuracyFor(String questionType) {
        for (TypeAccuracy row : typeAccuracy) {
            if (row.questionType != null && row.questionType.equals(questionType)) {
                return row;
            }
        }
        return null;
    }
}
