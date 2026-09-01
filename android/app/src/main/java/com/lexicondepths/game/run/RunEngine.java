package com.lexicondepths.game.run;

import com.lexicondepths.content.Monster;
import com.lexicondepths.db.AppDatabase;
import com.lexicondepths.db.NodeType;
import com.lexicondepths.db.RunStatus;
import com.lexicondepths.db.entity.Run;
import com.lexicondepths.db.entity.RunNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Owns run state: HP, floor/step, and resume-after-kill. Every method that mutates a Run or
 * RunNode commits it to Room immediately (via RunDao's transactional helpers) — killing the
 * app mid-battle and reopening must restore exactly where it left off, because that's what
 * happens when a phone rings during a demo.
 *
 * The HP/floor math is split into pure static methods (below) precisely so "HP reaching 0 ends
 * the run exactly once" is unit-testable without a database; the Room-touching methods further
 * down are thin wrappers around them and are exercised by build + manual walkthrough instead,
 * matching how P2-7's Android-only views were verified.
 */
public final class RunEngine {

    public static final int STARTING_HP = 100;
    public static final int DEFAULT_REST_HEAL = 30;

    // Relic effect keys not owned by Damage.java or TimerBonus.java — plain branches, no effect engine.
    public static final String MAX_HP_PLUS_10 = "MAX_HP_PLUS_10";
    public static final String REST_HEALS_50 = "REST_HEALS_50";
    public static final String MARKS_PLUS_25 = "MARKS_PLUS_25";

    private RunEngine() {
    }

    // ---- Pure core -----------------------------------------------------------------------

    public static final class DamageOutcome {
        public final int newHp;
        public final boolean justDied; // true only the first time HP crosses to <=0

        DamageOutcome(int newHp, boolean justDied) {
            this.newHp = newHp;
            this.justDied = justDied;
        }
    }

    /** currentStatus guards the transition so a second call after death never re-fires it. */
    public static DamageOutcome applyDamage(int currentHp, RunStatus currentStatus, int damage) {
        int newHp = Math.max(0, currentHp - Math.max(0, damage));
        boolean justDied = newHp <= 0 && currentStatus == RunStatus.ACTIVE;
        return new DamageOutcome(newHp, justDied);
    }

    public static int heal(int currentHp, int amount, int maxHp) {
        return Math.min(maxHp, currentHp + Math.max(0, amount));
    }

    public static int maxHp(Set<String> relicIds) {
        return STARTING_HP + (relicIds.contains(MAX_HP_PLUS_10) ? 10 : 0);
    }

    public static int restHealAmount(Set<String> relicIds) {
        return relicIds.contains(REST_HEALS_50) ? 50 : DEFAULT_REST_HEAL;
    }

    /** {floor, step} after clearing a non-terminal node — wraps to the next floor past step 4. */
    public static int[] nextStep(int floor, int step) {
        step++;
        if (step > NodeGen.STEPS_PER_FLOOR) {
            step = 1;
            floor++;
        }
        return new int[]{floor, step};
    }

    // ---- Room orchestration ----------------------------------------------------------------

    public static long startRun(AppDatabase db, Long realmId, List<Monster> allMonsters) {
        List<Monster> battleMonsters = new ArrayList<>();
        Monster bossMonster = null;
        for (Monster m : allMonsters) {
            if (m.boss) {
                bossMonster = m;
            } else {
                battleMonsters.add(m);
            }
        }

        Run run = new Run();
        run.realmId = realmId;
        run.hp = STARTING_HP;
        run.floor = 1;
        run.step = 1;
        run.marks = 0;
        run.seed = new Random().nextLong();
        run.status = RunStatus.ACTIVE;
        run.startedAt = System.currentTimeMillis();
        long runId = db.runDao().insertRun(run);

        List<RunNode> nodes = NodeGen.generate(run.seed, battleMonsters, bossMonster);
        for (RunNode node : nodes) {
            node.runId = runId;
        }
        db.runDao().insertNodes(nodes);
        return runId;
    }

    public static RunState loadState(AppDatabase db, long runId) {
        Run run = db.runDao().getRun(runId);
        if (run == null) {
            return null;
        }
        return new RunState(run, db.runDao().getNodesForRun(runId), db.runDao().getRelicsForRun(runId));
    }

    /** Commits HP + slot progress together. Returns true exactly once, the moment HP hits 0. */
    public static boolean applyDamageAndCommit(AppDatabase db, Run run, RunNode node, int damage) {
        DamageOutcome outcome = applyDamage(run.hp, run.status, damage);
        run.hp = outcome.newHp;
        if (outcome.justDied) {
            run.status = RunStatus.LOST;
            run.endedAt = System.currentTimeMillis();
        }
        db.runDao().commitSlotResult(node, run);
        return outcome.justDied;
    }

    /** Marks the node cleared and advances floor/step, or ends the run as WON if it was the boss. */
    public static void completeNode(AppDatabase db, Run run, RunNode node) {
        node.cleared = true;
        run.marks += marksFor(node.type);
        if (node.type == NodeType.BOSS) {
            run.status = RunStatus.WON;
            run.endedAt = System.currentTimeMillis();
        } else {
            int[] next = nextStep(run.floor, run.step);
            run.floor = next[0];
            run.step = next[1];
        }
        db.runDao().commitNodeCompletion(node, run);
    }

    /**
     * A REST node's heal-then-clear as one commit — folding the heal into completeNode's single
     * transaction (rather than a separate updateRun beforehand) closes the window where a crash
     * between two writes would leave the node still tappable and let a retry double-heal.
     */
    public static void completeRestNode(AppDatabase db, Run run, RunNode node, int healAmount, Set<String> relicIds) {
        run.hp = heal(run.hp, healAmount, maxHp(relicIds));
        completeNode(db, run, node);
    }

    /** MAX_HP_PLUS_10 grants its bonus immediately on pickup, not just as a higher future cap. */
    public static void applyRelicPickup(AppDatabase db, Run run, String effect, Set<String> relicIdsAfterPickup) {
        if (MAX_HP_PLUS_10.equals(effect)) {
            run.hp = heal(run.hp, 10, maxHp(relicIdsAfterPickup));
            db.runDao().updateRun(run);
        }
    }

    private static int marksFor(NodeType type) {
        switch (type) {
            case BATTLE:
                return 10;
            case ELITE:
                return 20;
            case BOSS:
                return 30;
            case TREASURE:
                return 15;
            default:
                return 0;
        }
    }
}
