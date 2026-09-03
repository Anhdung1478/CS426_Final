package com.lexicondepths.ui.map;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.lexicondepths.App;
import com.lexicondepths.R;
import com.lexicondepths.content.RelicCatalog;
import com.lexicondepths.databinding.ActivityDungeonMapBinding;
import com.lexicondepths.db.NodeType;
import com.lexicondepths.db.RunStatus;
import com.lexicondepths.db.entity.Run;
import com.lexicondepths.db.entity.RunNode;
import com.lexicondepths.game.run.NodeGen;
import com.lexicondepths.game.run.RunEngine;
import com.lexicondepths.game.run.RunState;
import com.lexicondepths.ui.battle.BattleActivity;
import com.lexicondepths.ui.practice.PracticeActivity;
import com.lexicondepths.ui.reward.RewardActivity;
import com.lexicondepths.ui.reward.SpoilsActivity;

import java.util.Set;

/**
 * The two-column ladder from RunState, drawn as ASCII with the current position marked and
 * cleared nodes struck through. Only the two nodes at the run's current floor/step are ever
 * added as buttons, so only those are tappable — everything else is display only.
 */
public class DungeonMapActivity extends AppCompatActivity {

    public static final String EXTRA_RUN_ID = "runId";
    private static final int REQ_REST_REVIEW = 1;
    private static final int REQ_TREASURE_REWARD = 2;

    private ActivityDungeonMapBinding binding;
    private long runId;
    private RunNode pendingRestNode;
    private RunNode pendingTreasureNode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDungeonMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        runId = getIntent().getLongExtra(EXTRA_RUN_ID, -1);
        if (runId < 0) {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    /** Re-reads the run from Room every time this screen becomes visible — after a battle, a
     * reward pick, a rest review, or a fresh process restore, it's always the source of truth. */
    private void reload() {
        App.get().io().execute(() -> {
            RunState state = RunEngine.loadState(App.get().db(), runId);
            runOnUiThread(() -> render(state));
        });
    }

    private void render(RunState state) {
        if (state == null) {
            finish();
            return;
        }
        if (state.run.status != RunStatus.ACTIVE) {
            goToSpoils();
            return;
        }

        int maxHp = RunEngine.maxHp(relicEffects(state));
        binding.hpBar.setValues(state.run.hp, maxHp);
        binding.hpText.setText(getString(R.string.dungeon_map_hp, state.run.hp, maxHp));
        binding.marksText.setText(getString(R.string.dungeon_map_marks, state.run.marks));
        binding.mapText.setText(renderMap(state));
        renderChoices(state);
    }

    /** Relic *effect* keys, never IDs — RunEngine branches on effects. See RelicCatalog.effectsFor. */
    private Set<String> relicEffects(RunState state) {
        return RelicCatalog.effectsFor(getApplicationContext(), state.relicIds());
    }

    private CharSequence renderMap(RunState state) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (int floor = 1; floor <= NodeGen.FLOORS; floor++) {
            sb.append("Floor ").append(String.valueOf(floor)).append('\n');
            for (int step = 1; step <= NodeGen.STEPS_PER_FLOOR; step++) {
                boolean isCurrent = floor == state.run.floor && step == state.run.step;
                sb.append(isCurrent ? " > " : "   ");
                for (int slot = 0; slot < NodeGen.SLOTS_PER_STEP; slot++) {
                    RunNode node = state.nodeAt(floor, step, slot);
                    if (node == null) {
                        continue;
                    }
                    int start = sb.length();
                    sb.append(glyphFor(node.type));
                    if (node.cleared) {
                        sb.setSpan(new StrikethroughSpan(), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    sb.append("  ");
                }
                sb.append('\n');
            }
        }
        return sb;
    }

    private static String glyphFor(NodeType type) {
        switch (type) {
            case BATTLE:
                return "B";
            case ELITE:
                return "E";
            case REST:
                return "R";
            case TREASURE:
                return "T";
            case BOSS:
                return "!";
            default:
                return "?";
        }
    }

    private void renderChoices(RunState state) {
        binding.choiceContainer.removeAllViews();
        for (RunNode node : state.currentChoices()) {
            Button button = new Button(this);
            button.setText(labelFor(node.type));
            button.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            button.setOnClickListener(v -> onNodeTapped(state, node));
            binding.choiceContainer.addView(button);
        }
    }

    private String labelFor(NodeType type) {
        switch (type) {
            case BATTLE:
                return getString(R.string.node_type_battle);
            case ELITE:
                return getString(R.string.node_type_elite);
            case REST:
                return getString(R.string.node_type_rest);
            case TREASURE:
                return getString(R.string.node_type_treasure);
            case BOSS:
                return getString(R.string.node_type_boss);
            default:
                return "?";
        }
    }

    private void onNodeTapped(RunState state, RunNode node) {
        switch (node.type) {
            case BATTLE:
            case ELITE:
            case BOSS:
                Intent intent = new Intent(this, BattleActivity.class);
                intent.putExtra(BattleActivity.EXTRA_RUN_ID, runId);
                intent.putExtra(BattleActivity.EXTRA_NODE_ID, node.id);
                startActivity(intent);
                break;
            case REST:
                showRestDialog(state, node);
                break;
            case TREASURE:
                // completeNode() waits until the reward pick returns (below) — a crash while
                // RewardActivity is showing must leave this node still "open" so re-tapping it
                // re-offers the same relic instead of silently losing it.
                pendingTreasureNode = node;
                Intent rewardIntent = new Intent(this, RewardActivity.class);
                rewardIntent.putExtra(RewardActivity.EXTRA_RUN_ID, runId);
                startActivityForResult(rewardIntent, REQ_TREASURE_REWARD);
                break;
            default:
                break;
        }
    }

    private void showRestDialog(RunState state, RunNode node) {
        Set<String> effects = relicEffects(state);
        int healAmount = RunEngine.restHealAmount(effects);
        new AlertDialog.Builder(this)
                .setTitle(R.string.rest_dialog_title)
                .setPositiveButton(getString(R.string.rest_dialog_heal, healAmount), (d, w) ->
                        App.get().io().execute(() -> {
                            Run run = App.get().db().runDao().getRun(runId);
                            RunEngine.completeRestNode(App.get().db(), run, node, healAmount, effects);
                            runOnUiThread(this::reload);
                        }))
                .setNegativeButton(R.string.rest_dialog_review, (d, w) -> {
                    pendingRestNode = node;
                    startActivityForResult(new Intent(this, PracticeActivity.class), REQ_REST_REVIEW);
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_REST_REVIEW && pendingRestNode != null) {
            completePendingNode(pendingRestNode);
            pendingRestNode = null;
        } else if (requestCode == REQ_TREASURE_REWARD && pendingTreasureNode != null) {
            completePendingNode(pendingTreasureNode);
            pendingTreasureNode = null;
        }
    }

    private void completePendingNode(RunNode node) {
        App.get().io().execute(() -> {
            Run run = App.get().db().runDao().getRun(runId);
            RunEngine.completeNode(App.get().db(), run, node);
            runOnUiThread(this::reload);
        });
    }

    private void goToSpoils() {
        Intent intent = new Intent(this, SpoilsActivity.class);
        intent.putExtra(SpoilsActivity.EXTRA_RUN_ID, runId);
        startActivity(intent);
        finish();
    }
}
