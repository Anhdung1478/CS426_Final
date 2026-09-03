package com.lexicondepths.game.stats;

/**
 * One row of "SELECT questionType, AVG(ratio), COUNT(*) ... GROUP BY questionType".
 *
 * Plain fields, no Room annotation: Room maps a projection onto any POJO whose field names
 * match the column aliases, so this lives in game/ and the DAO imports it — not the reverse.
 * That keeps the §7 "game/ imports no Android classes" rule satisfiable.
 *
 * The test-side builder is a static factory rather than a second constructor because Room
 * warns when a projection class offers more than one usable constructor.
 */
public class TypeAccuracy {

    public String questionType;
    public double avgRatio;
    public int attempts;

    public static TypeAccuracy of(String questionType, double avgRatio, int attempts) {
        TypeAccuracy row = new TypeAccuracy();
        row.questionType = questionType;
        row.avgRatio = avgRatio;
        row.attempts = attempts;
        return row;
    }

    public int percent() {
        return (int) Math.round(avgRatio * 100);
    }
}
