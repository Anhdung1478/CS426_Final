package com.lexicondepths.game.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.lexicondepths.game.question.QuestionType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Every predicate at its boundary: one below the threshold, one at it. Plain JUnit, no
 * emulator — which is the whole reason Achievements and StatsSnapshot live in game/.
 */
public class AchievementsTest {

    private static final class Builder {
        int totalWords = 300;
        int learning;
        int mastered;
        int dueNow;
        List<TypeAccuracy> types = new ArrayList<>();
        List<WeakWord> weak = new ArrayList<>();
        int streak;
        int bestFloor;
        int totalRuns;
        int runsWon;
        int marks;
        int generatedRealms;
        int totalAnswers;

        StatsSnapshot build() {
            return new StatsSnapshot(totalWords, learning, mastered, dueNow, types, weak,
                    streak, bestFloor, totalRuns, runsWon, marks, generatedRealms, totalAnswers);
        }
    }

    private static Builder snapshot() {
        return new Builder();
    }

    private static void assertBoundary(Achievements achievement, StatsSnapshot below, StatsSnapshot at) {
        assertFalse(achievement + " must stay locked one below its threshold", achievement.isUnlockedBy(below));
        assertTrue(achievement + " must unlock at its threshold", achievement.isUnlockedBy(at));
    }

    @Test
    public void firstDescentUnlocksOnTheFirstRun() {
        Builder below = snapshot();
        Builder at = snapshot();
        at.totalRuns = 1;
        assertBoundary(Achievements.FIRST_DESCENT, below.build(), at.build());
    }

    @Test
    public void delverUnlocksAtFloorThree() {
        Builder below = snapshot();
        below.bestFloor = 2;
        Builder at = snapshot();
        at.bestFloor = 3;
        assertBoundary(Achievements.DELVER, below.build(), at.build());
    }

    @Test
    public void depthConquerorUnlocksOnTheFirstWin() {
        Builder below = snapshot();
        below.totalRuns = 20; // playing a lot is not winning
        Builder at = snapshot();
        at.runsWon = 1;
        assertBoundary(Achievements.DEPTH_CONQUEROR, below.build(), at.build());
    }

    @Test
    public void wordHoardCountsEverySeenWord() {
        Builder below = snapshot();
        below.learning = 100;
        below.mastered = Achievements.Threshold.WORD_HOARD_WORDS - 100 - 1;
        Builder at = snapshot();
        at.learning = 100;
        at.mastered = Achievements.Threshold.WORD_HOARD_WORDS - 100;
        assertBoundary(Achievements.WORD_HOARD, below.build(), at.build());
    }

    @Test
    public void lexicographerCountsOnlyMasteredWords() {
        Builder below = snapshot();
        below.learning = 200; // learning is not mastered, however many there are
        below.mastered = Achievements.Threshold.LEXICOGRAPHER_MASTERED - 1;
        Builder at = snapshot();
        at.mastered = Achievements.Threshold.LEXICOGRAPHER_MASTERED;
        assertBoundary(Achievements.LEXICOGRAPHER, below.build(), at.build());
    }

    @Test
    public void unbrokenUnlocksAtSevenDays() {
        Builder below = snapshot();
        below.streak = Achievements.Threshold.UNBROKEN_STREAK - 1;
        Builder at = snapshot();
        at.streak = Achievements.Threshold.UNBROKEN_STREAK;
        assertBoundary(Achievements.UNBROKEN, below.build(), at.build());
    }

    @Test
    public void realmForgerNeedsAGeneratedRealm() {
        Builder below = snapshot();
        Builder at = snapshot();
        at.generatedRealms = 1;
        assertBoundary(Achievements.REALM_FORGER, below.build(), at.build());
    }

    @Test
    public void diligentCountsAnswers() {
        Builder below = snapshot();
        below.totalAnswers = Achievements.Threshold.DILIGENT_ANSWERS - 1;
        Builder at = snapshot();
        at.totalAnswers = Achievements.Threshold.DILIGENT_ANSWERS;
        assertBoundary(Achievements.DILIGENT, below.build(), at.build());
    }

    @Test
    public void silverTongueNeedsBothAccuracyAndVolume() {
        String collocation = QuestionType.COLLOCATION.name();

        Builder tooFewAttempts = snapshot();
        tooFewAttempts.types = Collections.singletonList(
                TypeAccuracy.of(collocation, 1.0, Achievements.Threshold.SILVER_TONGUE_ATTEMPTS - 1));

        Builder tooLowAccuracy = snapshot();
        tooLowAccuracy.types = Collections.singletonList(
                TypeAccuracy.of(collocation, 0.79, Achievements.Threshold.SILVER_TONGUE_ATTEMPTS));

        Builder at = snapshot();
        at.types = Collections.singletonList(
                TypeAccuracy.of(collocation, 0.80, Achievements.Threshold.SILVER_TONGUE_ATTEMPTS));

        assertFalse("19 perfect answers is not enough evidence",
                Achievements.SILVER_TONGUE.isUnlockedBy(tooFewAttempts.build()));
        assertBoundary(Achievements.SILVER_TONGUE, tooLowAccuracy.build(), at.build());
    }

    @Test
    public void silverTongueIgnoresOtherQuestionTypes() {
        Builder other = snapshot();
        other.types = Arrays.asList(
                TypeAccuracy.of(QuestionType.WORDLE.name(), 1.0, 500),
                TypeAccuracy.of(QuestionType.CLOZE.name(), 1.0, 500));
        assertFalse(Achievements.SILVER_TONGUE.isUnlockedBy(other.build()));
    }

    /**
     * The §5 permadeath boundary, restated as a test: nothing a run ending wipes may feed a
     * predicate. clearRunState() deletes Run/RunNode/RunRelic and nothing else, so proving the
     * inputs unchanged across a run ending is equivalent to proving no achievement re-locks.
     */
    @Test
    public void everyAchievementSurvivesARunEnding() {
        Builder unlockedEverything = snapshot();
        unlockedEverything.totalRuns = 5;
        unlockedEverything.runsWon = 2;
        unlockedEverything.bestFloor = 3;
        unlockedEverything.learning = 100;
        unlockedEverything.mastered = 100;
        unlockedEverything.streak = 9;
        unlockedEverything.generatedRealms = 2;
        unlockedEverything.totalAnswers = 400;
        unlockedEverything.types = Collections.singletonList(
                TypeAccuracy.of(QuestionType.COLLOCATION.name(), 0.9, 40));
        StatsSnapshot before = unlockedEverything.build();

        for (Achievements achievement : Achievements.values()) {
            assertTrue(achievement + " should be unlocked in the fixture", achievement.isUnlockedBy(before));
        }

        // A run ends and is lost: streak zeroes, totalRuns goes up, run-scoped tables vanish.
        // Every field the predicates read lives in Profile/WordProgress/WordEvent/Realm, so the
        // only one that moves is streak — and Unbroken is the only achievement that reads it.
        Builder afterLoss = snapshot();
        afterLoss.totalRuns = 6;
        afterLoss.runsWon = 2;
        afterLoss.bestFloor = 3;
        afterLoss.learning = 100;
        afterLoss.mastered = 100;
        afterLoss.streak = 0;
        afterLoss.generatedRealms = 2;
        afterLoss.totalAnswers = 420;
        afterLoss.types = Collections.singletonList(
                TypeAccuracy.of(QuestionType.COLLOCATION.name(), 0.9, 42));
        StatsSnapshot after = afterLoss.build();

        for (Achievements achievement : Achievements.values()) {
            if (achievement == Achievements.UNBROKEN) {
                continue; // a streak is a streak; breaking it is the point
            }
            assertTrue(achievement + " must not re-lock when a run ends", achievement.isUnlockedBy(after));
        }
    }

    @Test
    public void masteryBucketsPartitionTheWordBank() {
        Builder builder = snapshot();
        builder.totalWords = 300;
        builder.learning = 40;
        builder.mastered = 25;
        StatsSnapshot snapshot = builder.build();

        assertEquals(235, snapshot.newWords());
        assertEquals(65, snapshot.wordsSeen());
        assertEquals("buckets must sum to the word bank",
                snapshot.totalWords, snapshot.newWords() + snapshot.learning + snapshot.mastered);
    }

    @Test
    public void aFreshInstallReportsNoAnswerHistory() {
        assertFalse(snapshot().build().hasAnswerHistory());
        Builder played = snapshot();
        played.totalAnswers = 1;
        assertTrue(played.build().hasAnswerHistory());
    }
}
