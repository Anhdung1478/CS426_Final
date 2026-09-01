package com.lexicondepths.game.run;

import com.lexicondepths.db.entity.Run;
import com.lexicondepths.db.entity.RunNode;
import com.lexicondepths.db.entity.RunRelic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An in-memory snapshot of one run: the Run row, its full node ladder, and held relics.
 * Pure and Android-free so the dungeon map (P2-10) can render from one object instead of
 * re-querying Room for every glyph.
 */
public final class RunState {

    public final Run run;
    public final List<RunNode> nodes;
    public final List<RunRelic> relics;

    public RunState(Run run, List<RunNode> nodes, List<RunRelic> relics) {
        this.run = run;
        this.nodes = nodes;
        this.relics = relics;
    }

    public RunNode nodeAt(int floor, int step, int slot) {
        for (RunNode node : nodes) {
            if (node.floor == floor && node.step == step && node.slot == slot) {
                return node;
            }
        }
        return null;
    }

    /** The nodes tappable right now — the two at the run's current floor/step. */
    public List<RunNode> currentChoices() {
        List<RunNode> choices = new ArrayList<>();
        for (int slot = 0; slot < NodeGen.SLOTS_PER_STEP; slot++) {
            RunNode node = nodeAt(run.floor, run.step, slot);
            if (node != null) {
                choices.add(node);
            }
        }
        return choices;
    }

    /** True once the run has moved past this step — used to render earlier rows as resolved. */
    public boolean isPast(int floor, int step) {
        return floor < run.floor || (floor == run.floor && step < run.step);
    }

    public Set<String> relicIds() {
        Set<String> ids = new HashSet<>();
        for (RunRelic relic : relics) {
            ids.add(relic.relicId);
        }
        return ids;
    }
}
