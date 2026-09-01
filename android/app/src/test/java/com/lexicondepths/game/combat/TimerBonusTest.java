package com.lexicondepths.game.combat;

import static org.junit.Assert.assertEquals;

import com.lexicondepths.game.combat.TimerBonus.Tier;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TimerBonusTest {

    private static final long FULL_MS = 10_000;
    private static final long PARTIAL_MS = 20_000;

    @Test
    public void underFullThresholdIsFullBonus() {
        assertEquals(Tier.FULL, TimerBonus.evaluate(9_999, 1.0, FULL_MS, PARTIAL_MS));
    }

    @Test
    public void betweenThresholdsIsPartialBonus() {
        assertEquals(Tier.PARTIAL, TimerBonus.evaluate(15_000, 1.0, FULL_MS, PARTIAL_MS));
    }

    @Test
    public void atOrOverPartialThresholdIsNoBonusButNoPenalty() {
        assertEquals(Tier.NONE, TimerBonus.evaluate(20_000, 1.0, FULL_MS, PARTIAL_MS));
        assertEquals(Tier.NONE, TimerBonus.evaluate(60_000, 1.0, FULL_MS, PARTIAL_MS));
    }

    @Test
    public void wrongAnswerNeverEarnsABonusRegardlessOfSpeed() {
        assertEquals(Tier.NONE, TimerBonus.evaluate(500, 0.0, FULL_MS, PARTIAL_MS));
    }

    @Test
    public void slowCorrectAnswerNeverScoresWorseThanFastWrongAnswer() {
        Tier slowCorrect = TimerBonus.evaluate(59_000, 1.0, FULL_MS, PARTIAL_MS);
        Tier fastWrong = TimerBonus.evaluate(200, 0.0, FULL_MS, PARTIAL_MS);
        assertEquals(Tier.NONE, slowCorrect);
        assertEquals(Tier.NONE, fastWrong); // both floor at NONE -- slow-correct is never worse
    }

    @Test
    public void quickenedQuillWidensBothWindowsByFiveSeconds() {
        Set<String> relics = new HashSet<>();
        relics.add(TimerBonus.TIMER_PLUS_5S);
        // 14s would be PARTIAL without the relic (>= 10s full window) but FULL with it (< 15s).
        assertEquals(Tier.FULL, TimerBonus.evaluate(14_000, 1.0, FULL_MS, PARTIAL_MS, relics));
        assertEquals(Tier.PARTIAL, TimerBonus.evaluate(14_000, 1.0, FULL_MS, PARTIAL_MS, Collections.emptySet()));
    }
}
