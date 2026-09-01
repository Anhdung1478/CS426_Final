package com.lexicondepths.game.combat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DamageTest {

    private static final Set<String> NO_RELICS = Collections.emptySet();
    private static final int PLAYER = 2; // B1

    @Test
    public void belowLevelHurtsMostAtEveryBand() {
        int below = Damage.compute(PLAYER, PLAYER - 1, 0.0, 1, NO_RELICS, false);
        int at = Damage.compute(PLAYER, PLAYER, 0.0, 1, NO_RELICS, false);
        int oneAbove = Damage.compute(PLAYER, PLAYER + 1, 0.0, 1, NO_RELICS, false);
        int twoAbove = Damage.compute(PLAYER, PLAYER + 2, 0.0, 1, NO_RELICS, false);

        assertTrue(below > at);
        assertTrue(at > oneAbove);
        assertTrue(oneAbove > twoAbove);
    }

    @Test
    public void fullRatioAlwaysYieldsZeroDamage() {
        for (int floor = 1; floor <= 3; floor++) {
            for (int delta = -2; delta <= 3; delta++) {
                assertEquals(0, Damage.compute(PLAYER, PLAYER + delta, 1.0, floor, NO_RELICS, false));
            }
        }
    }

    @Test
    public void depthMultiplierDoublesFromFloorOneToFloorThree() {
        int floor1 = Damage.compute(PLAYER, PLAYER, 0.0, 1, NO_RELICS, false);
        int floor3 = Damage.compute(PLAYER, PLAYER, 0.0, 3, NO_RELICS, false);
        assertEquals(floor1 * 2, floor3);
    }

    @Test
    public void firstMissFreeZeroesOnlyTheFirstMissThisRun() {
        Set<String> relics = relics(Damage.FIRST_MISS_FREE);
        assertEquals(0, Damage.compute(PLAYER, PLAYER, 0.0, 1, relics, true));
        assertTrue(Damage.compute(PLAYER, PLAYER, 0.0, 1, relics, false) > 0);
    }

    @Test
    public void ratioFloorTwentyCapsTheMaximumDamageReduction() {
        Set<String> relics = relics(Damage.RATIO_FLOOR_20);
        int without = Damage.compute(PLAYER, PLAYER, 0.0, 1, NO_RELICS, false);
        int withFloor = Damage.compute(PLAYER, PLAYER, 0.0, 1, relics, false);

        assertTrue(withFloor < without);
        assertEquals(Math.round(without * 0.8), withFloor);
    }

    @Test
    public void depthMultMinus25ShrinksTheFloorMultiplier() {
        Set<String> relics = relics(Damage.DEPTH_MULT_MINUS_25);
        int without = Damage.compute(PLAYER, PLAYER, 0.0, 3, NO_RELICS, false);
        int withRelic = Damage.compute(PLAYER, PLAYER, 0.0, 3, relics, false);
        assertTrue(withRelic < without);
    }

    @Test
    public void stretchDamageHalvedOnlyAppliesAboveThePlayersLevel() {
        Set<String> relics = relics(Damage.STRETCH_DAMAGE_HALVED);

        int atLevelWith = Damage.compute(PLAYER, PLAYER, 0.0, 1, relics, false);
        int atLevelWithout = Damage.compute(PLAYER, PLAYER, 0.0, 1, NO_RELICS, false);
        assertEquals(atLevelWithout, atLevelWith);

        int aboveWith = Damage.compute(PLAYER, PLAYER + 1, 0.0, 1, relics, false);
        int aboveWithout = Damage.compute(PLAYER, PLAYER + 1, 0.0, 1, NO_RELICS, false);
        assertTrue(aboveWith < aboveWithout);
    }

    private static Set<String> relics(String effect) {
        Set<String> set = new HashSet<>();
        set.add(effect);
        return set;
    }
}
