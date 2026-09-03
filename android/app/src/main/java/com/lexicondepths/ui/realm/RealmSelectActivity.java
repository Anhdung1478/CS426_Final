package com.lexicondepths.ui.realm;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.lexicondepths.App;
import com.lexicondepths.R;
import com.lexicondepths.content.Monster;
import com.lexicondepths.content.MonsterCatalog;
import com.lexicondepths.content.RelicCatalog;
import com.lexicondepths.databinding.ActivityRealmSelectBinding;
import com.lexicondepths.db.entity.Realm;
import com.lexicondepths.db.entity.Run;
import com.lexicondepths.game.run.RunEngine;
import com.lexicondepths.ui.map.DungeonMapActivity;

import android.view.ViewGroup;
import android.widget.Button;

import java.util.List;

/**
 * Lists topic realms (seeded from the word bank's distinct topics — see SeedLoader) plus the
 * always-available Echo Trial. Picking one starts a brand-new RunEngine run; a defensive check
 * for an already-active run exists in case this screen is reached with one still in progress
 * (Hub's Resume Run button is the normal path there).
 */
public class RealmSelectActivity extends AppCompatActivity {

    private ActivityRealmSelectBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRealmSelectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        App.get().db().realmDao().getAll().observe(this, this::renderRealms);

        App.get().io().execute(() -> {
            Run active = App.get().db().runDao().getActiveRun();
            if (active != null) {
                runOnUiThread(() -> goToMap(active.id));
            }
        });
    }

    private void renderRealms(List<Realm> realms) {
        binding.realmContainer.removeAllViews();
        if (realms != null) {
            for (Realm realm : realms) {
                addOption(getString(R.string.realm_item_format, realm.name,
                        realm.cefrMin.name(), realm.cefrMax.name()), () -> startRun(realm.id));
            }
        }
        addOption(getString(R.string.realm_echo_trial), () -> startRun(null));
    }

    private void addOption(String label, Runnable onPick) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(v -> onPick.run());

        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = getResources().getDimensionPixelSize(R.dimen.space_sm);
        button.setLayoutParams(params);

        binding.realmContainer.addView(button);
    }

    private void startRun(Long realmId) {
        App.get().io().execute(() -> {
            List<Monster> monsters = MonsterCatalog.load(getApplicationContext());
            long runId = RunEngine.startRun(App.get().db(), realmId, monsters,
                    RelicCatalog.load(getApplicationContext()));
            runOnUiThread(() -> goToMap(runId));
        });
    }

    private void goToMap(long runId) {
        Intent intent = new Intent(this, DungeonMapActivity.class);
        intent.putExtra(DungeonMapActivity.EXTRA_RUN_ID, runId);
        startActivity(intent);
        finish();
    }
}
