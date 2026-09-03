package com.lexicondepths.game.stats;

/** One of the ten lowest-average-ratio words, joined to Word for its headword. */
public class WeakWord {

    public long wordId;
    public String headword;
    public double avgRatio;
    public int attempts;

    public static WeakWord of(long wordId, String headword, double avgRatio, int attempts) {
        WeakWord row = new WeakWord();
        row.wordId = wordId;
        row.headword = headword;
        row.avgRatio = avgRatio;
        row.attempts = attempts;
        return row;
    }

    public int percent() {
        return (int) Math.round(avgRatio * 100);
    }
}
