package com.lexicondepths.game.srs;

import static org.junit.Assert.assertEquals;

import com.lexicondepths.db.entity.WordProgress;

import org.junit.Test;

public class Sm2Test {

    private static final long NOW = 1_700_000_000_000L;

    private static WordProgress fresh() {
        WordProgress p = new WordProgress();
        p.wordId = 1L;
        p.ease = 2.5;
        p.interval = 0;
        p.reps = 0;
        p.lapses = 0;
        p.dueAt = 0L;
        return p;
    }

    @Test
    public void good_followsOneSixThenIntervalTimesEaseCurve() {
        WordProgress p = fresh();

        p = Sm2.apply(p, ReviewGrade.GOOD, NOW);
        assertEquals(1, p.interval);

        p = Sm2.apply(p, ReviewGrade.GOOD, NOW);
        assertEquals(6, p.interval);

        p = Sm2.apply(p, ReviewGrade.GOOD, NOW);
        assertEquals(15, p.interval); // 6 * 2.5, ease unchanged by GOOD
        assertEquals(2.5, p.ease, 1e-9);
    }

    @Test
    public void ease_neverDropsBelowFloor() {
        WordProgress p = fresh();
        for (int i = 0; i < 10; i++) {
            p = Sm2.apply(p, ReviewGrade.AGAIN, NOW);
        }
        assertEquals(Sm2.MIN_EASE, p.ease, 1e-9);
    }

    @Test
    public void ease_neverExceedsCeiling() {
        WordProgress p = fresh();
        for (int i = 0; i < 10; i++) {
            p = Sm2.apply(p, ReviewGrade.EASY, NOW);
        }
        assertEquals(Sm2.MAX_EASE, p.ease, 1e-9);
    }

    @Test
    public void again_resetsRepsToZero_whileIncrementingLapses() {
        WordProgress p = fresh();
        p.reps = 3;
        p.lapses = 0;

        WordProgress next = Sm2.apply(p, ReviewGrade.AGAIN, NOW);

        assertEquals(0, next.reps);
        assertEquals(1, next.lapses);
        assertEquals(0, next.interval);
        assertEquals(NOW, next.dueAt); // due now
    }

    @Test
    public void apply_returnsNewObject_doesNotMutateArgument() {
        WordProgress p = fresh();
        WordProgress next = Sm2.apply(p, ReviewGrade.GOOD, NOW);

        assertEquals(0, p.interval); // original untouched
        assertEquals(1, next.interval);
        org.junit.Assert.assertNotSame(p, next);
    }

    @Test
    public void tenReviewSequence_isDeterministic() {
        WordProgress p = fresh();

        ReviewGrade[] sequence = {
                ReviewGrade.GOOD, ReviewGrade.GOOD, ReviewGrade.GOOD, ReviewGrade.HARD,
                ReviewGrade.AGAIN, ReviewGrade.GOOD, ReviewGrade.GOOD, ReviewGrade.EASY,
                ReviewGrade.GOOD, ReviewGrade.HARD
        };

        for (ReviewGrade grade : sequence) {
            p = Sm2.apply(p, grade, NOW);
        }

        assertEquals(2.15, p.ease, 1e-9);
        assertEquals(47, p.interval);
        assertEquals(5, p.reps);
        assertEquals(1, p.lapses);
        assertEquals(NOW + 47L * Sm2.ONE_DAY_MILLIS, p.dueAt);
    }
}
