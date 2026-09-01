package com.lexicondepths.game.combat;

import java.util.Set;

/**
 * base × (1 − ratio) × depthMultiplier. Base is inverse to difficulty on purpose: failing a
 * word below the player's level means "you should have known that" and hurts most, while
 * stretch vocabulary above their level barely stings — so players keep attempting words above
 * their level instead of playing safe. relics is the set of active relic *effect* keys (already
 * resolved from held relic IDs by the caller via RelicCatalog), matching assets/relics.json.
 */
public final class Damage {

    private static final int BELOW_LEVEL_BASE = 19;
    private static final int AT_LEVEL_BASE = 11;
    private static final int ONE_ABOVE_BASE = 6;
    private static final int TWO_PLUS_ABOVE_BASE = 3;

    private static final double RATIO_FLOOR = 0.20;
    private static final double DEPTH_MULT_REDUCTION = 0.75;
    private static final double STRETCH_HALVED = 0.5;

    public static final String FIRST_MISS_FREE = "FIRST_MISS_FREE";
    public static final String RATIO_FLOOR_20 = "RATIO_FLOOR_20";
    public static final String DEPTH_MULT_MINUS_25 = "DEPTH_MULT_MINUS_25";
    public static final String STRETCH_DAMAGE_HALVED = "STRETCH_DAMAGE_HALVED";

    private Damage() {
    }

    /**
     * isFirstMissThisEncounter is passed in rather than tracked here — Damage stays a pure
     * function, and "has a miss already happened" is state that belongs to the caller. Per
     * assets/relics.json's own description ("Your first miss each battle deals no damage"),
     * that's scoped to one encounter, not the whole run — RunNode.firstMissUsed (P2-9) is
     * what BattleActivity (P2-11) tracks it against.
     */
    public static int compute(int playerCefrOrdinal, int wordCefrOrdinal, double ratio, int floor,
                               Set<String> relics, boolean isFirstMissThisEncounter) {
        if (relics.contains(FIRST_MISS_FREE) && isFirstMissThisEncounter) {
            return 0;
        }

        int base = baseForBand(playerCefrOrdinal, wordCefrOrdinal);
        if (relics.contains(STRETCH_DAMAGE_HALVED) && wordCefrOrdinal > playerCefrOrdinal) {
            base = (int) Math.round(base * STRETCH_HALVED);
        }

        double effectiveRatio = relics.contains(RATIO_FLOOR_20) ? Math.max(ratio, RATIO_FLOOR) : ratio;

        double depthMultiplier = depthMultiplier(floor);
        if (relics.contains(DEPTH_MULT_MINUS_25)) {
            depthMultiplier *= DEPTH_MULT_REDUCTION;
        }

        double raw = base * (1.0 - effectiveRatio) * depthMultiplier;
        return Math.max(0, (int) Math.round(raw));
    }

    public static double depthMultiplier(int floor) {
        if (floor <= 1) {
            return 1.0;
        }
        if (floor == 2) {
            return 1.5;
        }
        return 2.0;
    }

    private static int baseForBand(int playerCefrOrdinal, int wordCefrOrdinal) {
        int delta = wordCefrOrdinal - playerCefrOrdinal;
        if (delta < 0) {
            return BELOW_LEVEL_BASE;
        }
        if (delta == 0) {
            return AT_LEVEL_BASE;
        }
        if (delta == 1) {
            return ONE_ABOVE_BASE;
        }
        return TWO_PLUS_ABOVE_BASE;
    }
}
