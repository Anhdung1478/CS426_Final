package com.lexicondepths.game.srs;

import com.lexicondepths.db.entity.WordProgress;

/**
 * Simplified SM-2. Pure Java, no Android imports — testable with plain JUnit.
 * now is passed in rather than read from the clock, which is what makes this deterministic.
 */
public final class Sm2 {

    public static final double MIN_EASE = 1.3;
    public static final double MAX_EASE = 2.7;
    public static final long ONE_DAY_MILLIS = 24L * 60 * 60 * 1000;

    private Sm2() {
    }

    public static WordProgress apply(WordProgress current, ReviewGrade grade, long nowMillis) {
        double ease = current.ease;
        int interval = current.interval;
        int reps = current.reps;
        int lapses = current.lapses;

        switch (grade) {
            case AGAIN:
                interval = 0;
                reps = 0;
                lapses = lapses + 1;
                ease = ease - 0.20;
                break;
            case HARD:
                interval = Math.max(1, (int) Math.round(interval * 1.2));
                reps = reps + 1;
                ease = ease - 0.15;
                break;
            case GOOD:
                interval = nextGoodInterval(reps, interval, ease);
                reps = reps + 1;
                break;
            case EASY:
                interval = (int) Math.round(nextGoodInterval(reps, interval, ease) * 1.3);
                reps = reps + 1;
                ease = ease + 0.15;
                break;
        }

        ease = clamp(ease, MIN_EASE, MAX_EASE);

        WordProgress next = new WordProgress();
        next.wordId = current.wordId;
        next.ease = ease;
        next.interval = interval;
        next.reps = reps;
        next.lapses = lapses;
        next.dueAt = nowMillis + interval * ONE_DAY_MILLIS;
        return next;
    }

    private static int nextGoodInterval(int reps, int interval, double ease) {
        if (reps == 0) {
            return 1;
        }
        if (reps == 1) {
            return 6;
        }
        return (int) Math.round(interval * ease);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
