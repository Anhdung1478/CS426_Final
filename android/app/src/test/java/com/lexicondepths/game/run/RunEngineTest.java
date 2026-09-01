package com.lexicondepths.game.run;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.lexicondepths.db.RunStatus;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RunEngineTest {

    @Test
    public void hpReachingZeroTransitionsExactlyOnce() {
        RunEngine.DamageOutcome first = RunEngine.applyDamage(5, RunStatus.ACTIVE, 10);
        assertEquals(0, first.newHp);
        assertTrue("first crossing to 0 must report justDied", first.justDied);

        // A caller that (correctly) already flipped status to LOST after `first` must never
        // see justDied again, even though the arithmetic still lands on 0.
        RunEngine.DamageOutcome second = RunEngine.applyDamage(0, RunStatus.LOST, 10);
        assertEquals(0, second.newHp);
        assertFalse("a second hit after death must not re-trigger the transition", second.justDied);
    }

    @Test
    public void damageNeverDropsHpBelowZero() {
        RunEngine.DamageOutcome outcome = RunEngine.applyDamage(3, RunStatus.ACTIVE, 999);
        assertEquals(0, outcome.newHp);
    }

    @Test
    public void negativeDamageIsClampedRatherThanHealing() {
        RunEngine.DamageOutcome outcome = RunEngine.applyDamage(50, RunStatus.ACTIVE, -20);
        assertEquals(50, outcome.newHp);
    }

    @Test
    public void healCapsAtTheGivenMaxHp() {
        assertEquals(100, RunEngine.heal(95, 20, 100));
        assertEquals(80, RunEngine.heal(50, 30, 100));
    }

    @Test
    public void maxHpRelicRaisesTheCapByTen() {
        assertEquals(100, RunEngine.maxHp(Collections.emptySet()));
        Set<String> relics = new HashSet<>();
        relics.add(RunEngine.MAX_HP_PLUS_10);
        assertEquals(110, RunEngine.maxHp(relics));
    }

    @Test
    public void restHealDefaultsTo30AndUpgradesWithItsRelic() {
        assertEquals(RunEngine.DEFAULT_REST_HEAL, RunEngine.restHealAmount(Collections.emptySet()));
        Set<String> relics = new HashSet<>();
        relics.add(RunEngine.REST_HEALS_50);
        assertEquals(50, RunEngine.restHealAmount(relics));
    }

    @Test
    public void nextStepAdvancesWithinAFloor() {
        assertArrayEquals(new int[]{1, 2}, RunEngine.nextStep(1, 1));
        assertArrayEquals(new int[]{1, 4}, RunEngine.nextStep(1, 3));
    }

    @Test
    public void nextStepWrapsToTheNextFloorPastStepFour() {
        assertArrayEquals(new int[]{2, 1}, RunEngine.nextStep(1, 4));
        assertArrayEquals(new int[]{3, 1}, RunEngine.nextStep(2, 4));
    }
}
