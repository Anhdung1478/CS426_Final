package com.lexicondepths.game.run;

import com.lexicondepths.content.Monster;
import com.lexicondepths.db.NodeType;
import com.lexicondepths.db.entity.RunNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The two-column ladder from project-context.md §6: 3 floors x 4 steps x 2 slots, generated
 * from Run.seed so the same seed always reproduces the same map — essential for reproducing a
 * bug rather than guessing at it. runId is left unset; the caller (RunEngine) assigns it once
 * the Run row exists.
 */
public final class NodeGen {

    public static final int FLOORS = 3;
    public static final int STEPS_PER_FLOOR = 4;
    public static final int SLOTS_PER_STEP = 2;

    private NodeGen() {
    }

    public static List<RunNode> generate(long seed, List<Monster> battleMonsters, Monster bossMonster) {
        Random rng = new Random(seed);
        List<RunNode> nodes = new ArrayList<>();
        for (int floor = 1; floor <= FLOORS; floor++) {
            for (int step = 1; step <= STEPS_PER_FLOOR; step++) {
                NodeType type = typeFor(floor, step, rng);
                for (int slot = 0; slot < SLOTS_PER_STEP; slot++) {
                    RunNode node = new RunNode();
                    node.floor = floor;
                    node.step = step;
                    node.slot = slot;
                    node.type = type;
                    node.monsterId = monsterFor(type, battleMonsters, bossMonster, rng);
                    nodes.add(node);
                }
            }
        }
        return nodes;
    }

    /** Step 4 is ELITE on floors 1-2, BOSS on floor 3; steps 1-3 roll weighted {BATTLE x3, REST, TREASURE}. */
    private static NodeType typeFor(int floor, int step, Random rng) {
        if (step == STEPS_PER_FLOOR) {
            return floor == FLOORS ? NodeType.BOSS : NodeType.ELITE;
        }
        int roll = rng.nextInt(5);
        if (roll < 3) {
            return NodeType.BATTLE;
        }
        return roll == 3 ? NodeType.REST : NodeType.TREASURE;
    }

    private static String monsterFor(NodeType type, List<Monster> battleMonsters, Monster bossMonster, Random rng) {
        switch (type) {
            case BOSS:
                return bossMonster == null ? null : bossMonster.id;
            case BATTLE:
            case ELITE:
                return battleMonsters.isEmpty() ? null : battleMonsters.get(rng.nextInt(battleMonsters.size())).id;
            default:
                return null;
        }
    }
}
