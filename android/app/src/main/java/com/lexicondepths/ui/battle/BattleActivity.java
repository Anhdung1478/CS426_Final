package com.lexicondepths.ui.battle;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.lexicondepths.App;
import com.lexicondepths.R;
import com.lexicondepths.content.Monster;
import com.lexicondepths.content.MonsterCatalog;
import com.lexicondepths.content.RelicCatalog;
import com.lexicondepths.databinding.ActivityBattleBinding;
import com.lexicondepths.db.AppDatabase;
import com.lexicondepths.db.NodeType;
import com.lexicondepths.db.RunStatus;
import com.lexicondepths.db.entity.Realm;
import com.lexicondepths.db.entity.Run;
import com.lexicondepths.db.entity.RunNode;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.db.entity.WordEvent;
import com.lexicondepths.game.combat.Damage;
import com.lexicondepths.game.combat.TimerBonus;
import com.lexicondepths.game.question.Answer;
import com.lexicondepths.game.question.Question;
import com.lexicondepths.game.question.QuestionResult;
import com.lexicondepths.game.run.Encounter;
import com.lexicondepths.game.run.RunEngine;
import com.lexicondepths.ui.battle.view.QuestionView;
import com.lexicondepths.ui.battle.view.TextInputView;
import com.lexicondepths.ui.reward.RewardActivity;
import com.lexicondepths.ui.reward.SpoilsActivity;
import com.lexicondepths.ui.widget.AsciiMonsterRenderer;
import com.lexicondepths.ui.widget.Scramble;
import com.lexicondepths.ui.widget.Shake;
import com.lexicondepths.ui.widget.Speaker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The screen that carries the game: bind a question, start the timer (QuestionView.bind does
 * this), collect the answer, score it, apply damage, animate, advance the slot, repeat until
 * the encounter clears or HP hits 0. Every answer writes a WordEvent immediately and commits
 * HP/slot progress to Room in the same call — see RunEngine.applyDamageAndCommit — which is
 * what lets a force-quit mid-battle resume exactly where it left off.
 *
 * The whole Encounter's questions are generated eagerly, right after Encounter.build, using
 * the same Random the build itself consumed. That makes the sequence of generated Questions a
 * pure function of (seed, node, pool) regardless of when they're displayed, so resuming after a
 * kill just means re-running this same generation and skipping to RunNode.slotsCleared —
 * no separate "fast forward the RNG" step needed.
 */
public class BattleActivity extends AppCompatActivity {

    public static final String EXTRA_RUN_ID = "runId";
    public static final String EXTRA_NODE_ID = "nodeId";
    private static final int REQ_REWARD = 1;

    private ActivityBattleBinding binding;
    private long runId;
    private long nodeId;

    private Run run;
    private RunNode node;
    private Monster monster;
    private List<Word> pool;
    private Encounter encounter;
    private List<Question> questions;
    private Set<String> relicIds;
    private Set<String> relicEffects;
    private int currentIndex;
    private Speaker speaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBattleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        runId = getIntent().getLongExtra(EXTRA_RUN_ID, -1);
        nodeId = getIntent().getLongExtra(EXTRA_NODE_ID, -1);
        if (runId < 0 || nodeId < 0) {
            finish();
            return;
        }

        speaker = new Speaker(this);
        loadEncounter();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Scramble.cancel(binding.monsterNameText);
        Shake.cancel(binding.battleRoot);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speaker.shutdown();
    }

    private void loadEncounter() {
        App.get().io().execute(() -> {
            AppDatabase db = App.get().db();
            run = db.runDao().getRun(runId);
            node = db.runDao().getNode(nodeId);
            monster = MonsterCatalog.getById(getApplicationContext(), node.monsterId);

            HashSet<String> ids = new HashSet<>();
            for (com.lexicondepths.db.entity.RunRelic relic : db.runDao().getRelicsForRun(runId)) {
                ids.add(relic.relicId);
            }
            relicIds = ids;
            relicEffects = RelicCatalog.effectsFor(getApplicationContext(), ids);

            pool = loadPool(db, run);
            Set<Long> dueIds = new HashSet<>(db.wordDao().getDueWordIdsSync(System.currentTimeMillis()));

            Random rng = new Random(run.seed ^ node.id);
            encounter = Encounter.build(monster, pool, dueIds, rng);
            questions = new ArrayList<>();
            for (Encounter.Slot slot : encounter.slots) {
                questions.add(slot.generator.generate(slot.word, pool, rng));
            }
            currentIndex = Math.min(node.slotsCleared, questions.size());

            runOnUiThread(this::onEncounterReady);
        });
    }

    private static List<Word> loadPool(AppDatabase db, Run run) {
        if (run.realmId == null) {
            return db.wordDao().getAll();
        }
        Realm realm = db.realmDao().getById(run.realmId);
        return db.wordDao().getByTopic(realm.topic);
    }

    private void onEncounterReady() {
        if (questions.isEmpty() || currentIndex >= questions.size()) {
            // Either no eligible word existed for this monster's types, or the app was killed
            // right after the last slot committed but before the node was marked complete —
            // either way, the fight itself is already over.
            resolveClearedNode();
            return;
        }
        binding.monsterNameText.setText(monster.name);
        new AsciiMonsterRenderer().render(monster, binding.monsterAsciiText);
        Scramble.start(binding.monsterNameText, monster.name, 40);
        updateHp();
        bindCurrentQuestion();
    }

    private void updateHp() {
        int maxHp = RunEngine.maxHp(relicEffects);
        binding.hpBar.setValues(run.hp, maxHp);
        // The bar goes red below 30%, which is colour alone. The number is the pairing §7 asks
        // for, and it is also what TalkBack reads.
        binding.hpText.setText(getString(R.string.dungeon_map_hp, run.hp, maxHp));
    }

    private void bindCurrentQuestion() {
        binding.slotsText.setText(getString(R.string.battle_slots_progress, currentIndex + 1, questions.size()));
        binding.damageText.setText("");

        Encounter.Slot slot = encounter.slots.get(currentIndex);
        Question question = questions.get(currentIndex);

        QuestionView view = QuestionView.create(this, slot.type);
        if (view instanceof TextInputView) {
            ((TextInputView) view).setSpeaker(speaker);
        }
        view.setListener(this::onAnswered);

        binding.questionContainer.removeAllViews();
        binding.questionContainer.addView(view, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        view.bind(question);
    }

    private void onAnswered(Answer answer) {
        Encounter.Slot slot = encounter.slots.get(currentIndex);
        QuestionResult result = slot.generator.score(questions.get(currentIndex), answer);

        boolean firstMissAvailable = !node.firstMissUsed;
        int damage = Damage.compute(App.get().prefs().cefrLevel().ordinal(), slot.word.cefr.ordinal(),
                result.ratio, run.floor, relicEffects, firstMissAvailable);

        TimerBonus.Tier tier = TimerBonus.evaluate(result.elapsedMillis, result.ratio,
                App.get().prefs().timerFullBonusMs(), App.get().prefs().timerPartialBonusMs(), relicEffects);
        // The timer's bonus-only promise (never a penalty) is expressed here as damage
        // mitigation on a hit you already took, never as extra damage on a miss.
        if (tier == TimerBonus.Tier.FULL) {
            damage = (int) Math.round(damage * 0.5);
        } else if (tier == TimerBonus.Tier.PARTIAL) {
            damage = (int) Math.round(damage * 0.75);
        }
        int finalDamage = damage;

        if (result.ratio < 1.0 && firstMissAvailable) {
            node.firstMissUsed = true;
        }
        node.slotsCleared = currentIndex + 1;

        App.get().io().execute(() -> {
            WordEvent event = new WordEvent();
            event.wordId = slot.word.id;
            event.runId = runId;
            event.questionType = slot.type.name();
            event.ratio = result.ratio;
            event.damageDealt = finalDamage;
            event.elapsedMillis = result.elapsedMillis;
            event.timestamp = System.currentTimeMillis();
            App.get().db().wordEventDao().insert(event);

            boolean died = RunEngine.applyDamageAndCommit(App.get().db(), run, node, finalDamage);
            runOnUiThread(() -> onSlotResolved(died, finalDamage));
        });
    }

    private void onSlotResolved(boolean died, int damage) {
        updateHp();
        if (damage > 0) {
            binding.damageText.setText(getString(R.string.battle_damage_taken, damage));
            Shake.run(binding.battleRoot);
        } else {
            binding.damageText.setText(R.string.battle_damage_none);
        }

        if (died) {
            goToSpoils();
            return;
        }
        if (currentIndex + 1 >= questions.size()) {
            resolveClearedNode();
        } else {
            currentIndex++;
            bindCurrentQuestion();
        }
    }

    /**
     * Every slot is answered. A boss win ends the run immediately (no relic to pick — the run
     * is over). Otherwise the relic pick happens BEFORE completeNode() commits the node as
     * cleared and advances floor/step: if the app is killed while RewardActivity is showing,
     * reopening finds slotsCleared already at the full count but the node still uncleared, so
     * onEncounterReady's resume check lands right back here and re-offers the same reward
     * instead of silently losing it.
     */
    private void resolveClearedNode() {
        if (node.type == NodeType.BOSS) {
            completeNodeAndFinish();
        } else {
            Intent intent = new Intent(this, RewardActivity.class);
            intent.putExtra(RewardActivity.EXTRA_RUN_ID, runId);
            startActivityForResult(intent, REQ_REWARD);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_REWARD) {
            completeNodeAndFinish();
        }
    }

    private void completeNodeAndFinish() {
        App.get().io().execute(() -> {
            RunEngine.completeNode(App.get().db(), run, node);
            runOnUiThread(() -> {
                if (run.status == RunStatus.WON) {
                    goToSpoils();
                } else {
                    finish(); // back to DungeonMapActivity, which reloads at the advanced step
                }
            });
        });
    }

    private void goToSpoils() {
        Intent intent = new Intent(this, SpoilsActivity.class);
        intent.putExtra(SpoilsActivity.EXTRA_RUN_ID, runId);
        startActivity(intent);
        finish();
    }
}
