package com.lexicondepths.game.run;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.lexicondepths.content.Monster;
import com.lexicondepths.db.NodeType;
import com.lexicondepths.db.entity.RunNode;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NodeGenTest {

    private static Monster monster(String id, boolean boss) {
        Monster m = new Monster();
        m.id = id;
        m.name = id;
        m.questionTypes = Collections.singletonList("DEFINITION_TO_WORD");
        m.slots = boss ? 5 : 2;
        m.resists = Collections.emptyList();
        m.ascii = Collections.emptyList();
        m.boss = boss;
        return m;
    }

    private static List<Monster> battlePool() {
        List<Monster> pool = new ArrayList<>();
        pool.add(monster("hydra", false));
        pool.add(monster("sphinx", false));
        pool.add(monster("twins", false));
        return pool;
    }

    @Test
    public void sameSeedProducesTheSameMapEveryTime() {
        List<Monster> pool = battlePool();
        Monster boss = monster("archivist", true);

        List<RunNode> a = NodeGen.generate(42L, pool, boss);
        List<RunNode> b = NodeGen.generate(42L, pool, boss);

        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).floor, b.get(i).floor);
            assertEquals(a.get(i).step, b.get(i).step);
            assertEquals(a.get(i).slot, b.get(i).slot);
            assertEquals(a.get(i).type, b.get(i).type);
            assertEquals(a.get(i).monsterId, b.get(i).monsterId);
        }
    }

    @Test
    public void differentSeedsCanProduceDifferentMaps() {
        List<Monster> pool = battlePool();
        Monster boss = monster("archivist", true);

        List<RunNode> a = NodeGen.generate(1L, pool, boss);
        List<RunNode> b = NodeGen.generate(2L, pool, boss);

        boolean anyDifference = false;
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).type != b.get(i).type || !java.util.Objects.equals(a.get(i).monsterId, b.get(i).monsterId)) {
                anyDifference = true;
                break;
            }
        }
        org.junit.Assert.assertTrue("two different seeds produced an identical map", anyDifference);
    }

    @Test
    public void producesExactlyThreeFloorsOfFourStepsOfTwoSlots() {
        List<RunNode> nodes = NodeGen.generate(7L, battlePool(), monster("archivist", true));
        assertEquals(NodeGen.FLOORS * NodeGen.STEPS_PER_FLOOR * NodeGen.SLOTS_PER_STEP, nodes.size());
    }

    @Test
    public void step4IsEliteOnFloorsOneAndTwoAndBossOnFloorThree() {
        List<RunNode> nodes = NodeGen.generate(7L, battlePool(), monster("archivist", true));
        for (RunNode node : nodes) {
            if (node.step == NodeGen.STEPS_PER_FLOOR) {
                NodeType expected = node.floor == NodeGen.FLOORS ? NodeType.BOSS : NodeType.ELITE;
                assertEquals("floor " + node.floor + " step 4", expected, node.type);
            }
        }
    }

    @Test
    public void bossNodesUseTheDesignatedBossMonster() {
        Monster boss = monster("archivist", true);
        List<RunNode> nodes = NodeGen.generate(7L, battlePool(), boss);
        for (RunNode node : nodes) {
            if (node.type == NodeType.BOSS) {
                assertEquals("archivist", node.monsterId);
            }
        }
    }

    @Test
    public void restAndTreasureNodesCarryNoMonster() {
        List<RunNode> nodes = NodeGen.generate(3L, battlePool(), monster("archivist", true));
        for (RunNode node : nodes) {
            if (node.type == NodeType.REST || node.type == NodeType.TREASURE) {
                assertNull(node.monsterId);
            }
        }
    }

    @Test
    public void battleAndEliteNodesAlwaysHaveAMonster() {
        List<RunNode> nodes = NodeGen.generate(3L, battlePool(), monster("archivist", true));
        for (RunNode node : nodes) {
            if (node.type == NodeType.BATTLE || node.type == NodeType.ELITE) {
                assertNotNull(node.monsterId);
            }
        }
    }
}
