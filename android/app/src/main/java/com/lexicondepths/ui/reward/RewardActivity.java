package com.lexicondepths.ui.reward;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.lexicondepths.App;
import com.lexicondepths.R;
import com.lexicondepths.content.Relic;
import com.lexicondepths.content.RelicCatalog;
import com.lexicondepths.databinding.ActivityRewardBinding;
import com.lexicondepths.db.AppDatabase;
import com.lexicondepths.db.entity.Run;
import com.lexicondepths.db.entity.RunRelic;
import com.lexicondepths.game.run.RunEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Offers a choice of 1 of 3 relics after a non-boss battle win or a treasure node — the node
 * itself is already marked cleared by the caller before this screen launches. Picking one
 * inserts the RunRelic row and applies any immediate pickup effect (MAX_HP_PLUS_10); the rest
 * of the eight relics are plain Set<String> checks read later by Damage/TimerBonus/RunEngine.
 */
public class RewardActivity extends AppCompatActivity {

    public static final String EXTRA_RUN_ID = "runId";
    private static final int MAX_OFFERED = 3;

    private ActivityRewardBinding binding;
    private long runId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRewardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        runId = getIntent().getLongExtra(EXTRA_RUN_ID, -1);
        if (runId < 0) {
            finish();
            return;
        }

        binding.skipButton.setOnClickListener(v -> finish());
        loadOffer();
    }

    private void loadOffer() {
        App.get().io().execute(() -> {
            List<Relic> all = RelicCatalog.load(getApplicationContext());
            Set<String> held = new HashSet<>();
            for (RunRelic relic : App.get().db().runDao().getRelicsForRun(runId)) {
                held.add(relic.relicId);
            }
            List<Relic> candidates = new ArrayList<>();
            for (Relic relic : all) {
                if (!held.contains(relic.id)) {
                    candidates.add(relic);
                }
            }
            Collections.shuffle(candidates);
            List<Relic> offer = candidates.subList(0, Math.min(MAX_OFFERED, candidates.size()));
            runOnUiThread(() -> renderOffer(offer));
        });
    }

    private void renderOffer(List<Relic> offer) {
        binding.relicContainer.removeAllViews();
        if (offer.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.reward_none_available);
            empty.setTextAppearance(R.style.TextAppearance_Lexicon_Body);
            binding.relicContainer.addView(empty);
            return;
        }
        for (Relic relic : offer) {
            Button button = new Button(this);
            button.setText(relic.name + "\n" + relic.desc);
            button.setSingleLine(false); // Material's default button style forces one line
            button.setOnClickListener(v -> pick(relic));

            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = getResources().getDimensionPixelSize(R.dimen.space_sm);
            button.setLayoutParams(params);

            binding.relicContainer.addView(button);
        }
    }

    private void pick(Relic relic) {
        App.get().io().execute(() -> {
            AppDatabase db = App.get().db();
            RunRelic runRelic = new RunRelic();
            runRelic.runId = runId;
            runRelic.relicId = relic.id;
            runRelic.acquiredAt = System.currentTimeMillis();
            db.runDao().insertRelic(runRelic);

            Set<String> idsAfterPickup = new HashSet<>();
            for (RunRelic held : db.runDao().getRelicsForRun(runId)) {
                idsAfterPickup.add(held.relicId);
            }
            Run run = db.runDao().getRun(runId);
            RunEngine.applyRelicPickup(db, run, relic.effect, idsAfterPickup);

            runOnUiThread(this::finish);
        });
    }
}
