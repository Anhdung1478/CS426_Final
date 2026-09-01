package com.lexicondepths.game.run;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.lexicondepths.db.NodeType;
import com.lexicondepths.db.entity.Run;
import com.lexicondepths.db.entity.RunNode;
import com.lexicondepths.db.entity.RunRelic;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class RunStateTest {

    private static RunNode node(int floor, int step, int slot, NodeType type) {
        RunNode n = new RunNode();
        n.floor = floor;
        n.step = step;
        n.slot = slot;
        n.type = type;
        return n;
    }

    private static List<RunNode> ladder() {
        List<RunNode> nodes = new ArrayList<>();
        for (int floor = 1; floor <= 3; floor++) {
            for (int step = 1; step <= 4; step++) {
                nodes.add(node(floor, step, 0, NodeType.BATTLE));
                nodes.add(node(floor, step, 1, NodeType.TREASURE));
            }
        }
        return nodes;
    }

    @Test
    public void currentChoicesReturnsTheTwoNodesAtTheRunsPosition() {
        Run run = new Run();
        run.floor = 2;
        run.step = 3;
        RunState state = new RunState(run, ladder(), Collections.emptyList());

        List<RunNode> choices = state.currentChoices();

        assertEquals(2, choices.size());
        for (RunNode n : choices) {
            assertEquals(2, n.floor);
            assertEquals(3, n.step);
        }
    }

    @Test
    public void nodeAtReturnsNullWhenNothingMatches() {
        Run run = new Run();
        RunState state = new RunState(run, ladder(), Collections.emptyList());
        assertNull(state.nodeAt(9, 9, 0));
    }

    @Test
    public void isPastOnlyTrueBeforeTheCurrentFloorAndStep() {
        Run run = new Run();
        run.floor = 2;
        run.step = 2;
        RunState state = new RunState(run, ladder(), Collections.emptyList());

        assertTrue(state.isPast(1, 4));
        assertTrue(state.isPast(2, 1));
        assertFalse(state.isPast(2, 2));
        assertFalse(state.isPast(2, 3));
        assertFalse(state.isPast(3, 1));
    }

    @Test
    public void relicIdsCollectsHeldRelicIds() {
        Run run = new Run();
        RunRelic a = new RunRelic();
        a.relicId = "lexicon_shard";
        RunRelic b = new RunRelic();
        b.relicId = "steady_hand";
        RunState state = new RunState(run, Collections.emptyList(), Arrays.asList(a, b));

        Set<String> ids = state.relicIds();

        assertEquals(2, ids.size());
        assertTrue(ids.contains("lexicon_shard"));
        assertTrue(ids.contains("steady_hand"));
    }
}
