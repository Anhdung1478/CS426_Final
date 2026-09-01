package com.lexicondepths.game.combat;

/**
 * Bonuses only, never penalties — questions never time out and never fail the player.
 * Punishing slow-but-correct answers would train fast guessing over actual thinking, which
 * fights the whole point of the app. A wrong answer never earns a tier, no matter how fast,
 * so a slow correct answer never scores worse than a fast wrong one (both floor at NONE).
 */
public final class TimerBonus {

    private TimerBonus() {
    }

    public enum Tier {
        FULL, PARTIAL, NONE
    }

    /** fullBonusMs/partialBonusMs come from Prefs (P1-8), defaulting to 10000/20000. */
    public static Tier evaluate(long elapsedMillis, double ratio, long fullBonusMs, long partialBonusMs) {
        if (ratio <= 0.0) {
            return Tier.NONE;
        }
        if (elapsedMillis < fullBonusMs) {
            return Tier.FULL;
        }
        if (elapsedMillis < partialBonusMs) {
            return Tier.PARTIAL;
        }
        return Tier.NONE;
    }
}
