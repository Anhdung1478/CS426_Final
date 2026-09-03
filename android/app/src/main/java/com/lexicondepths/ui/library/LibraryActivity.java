package com.lexicondepths.ui.library;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.lexicondepths.App;
import com.lexicondepths.R;
import com.lexicondepths.content.MapApi;
import com.lexicondepths.content.MapJson;
import com.lexicondepths.content.Monster;
import com.lexicondepths.content.MonsterCatalog;
import com.lexicondepths.content.RealmImport;
import com.lexicondepths.content.RelicCatalog;
import com.lexicondepths.databinding.ActivityLibraryBinding;
import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.dao.RealmDao;
import com.lexicondepths.game.run.RunEngine;
import com.lexicondepths.ui.map.DungeonMapActivity;
import com.lexicondepths.ui.widget.Typewriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * My Library, and the forge that fills it. The two live on one screen deliberately: forging a
 * realm and watching its row appear below the button is the moment Phase 3 has to feel like
 * magic, and a screen transition in the middle of it would spend that moment on navigation.
 */
public class LibraryActivity extends AppCompatActivity {

    private static final String FALLBACK_ASSET = "fallback_map.json";

    private ActivityLibraryBinding binding;
    private List<RealmDao.LibraryRow> rows = new ArrayList<>();
    private boolean forging;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLibraryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        CefrLevel[] levels = CefrLevel.values();
        String[] labels = new String[levels.length];
        for (int i = 0; i < levels.length; i++) {
            labels[i] = levels[i].name();
        }
        binding.levelSpinner.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        binding.levelSpinner.setSelection(App.get().prefs().cefrLevel().ordinal());

        binding.forgeButton.setOnClickListener(v -> forge());
        binding.retryButton.setOnClickListener(v -> forge());
        binding.offlineButton.setOnClickListener(v -> forgeOffline());
        binding.filterInput.addTextChangedListener(new SimpleWatcher(this::render));

        // LiveData, so a completed forge pushes its own row in with no manual refresh.
        App.get().db().realmDao().getGeneratedRealms().observe(this, observed -> {
            rows = observed == null ? new ArrayList<>() : observed;
            render();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Typewriter.cancel(binding.statusText);
    }

    // ---- Forging -------------------------------------------------------------------------

    private void forge() {
        String topic = binding.topicInput.getText().toString().trim();
        if (topic.isEmpty() || forging) {
            return;
        }
        CefrLevel level = CefrLevel.values()[binding.levelSpinner.getSelectedItemPosition()];
        String baseUrl = App.get().prefs().mapApiBaseUrl();

        startForging(getString(R.string.library_forging, topic));
        App.get().io().execute(() -> {
            try {
                String body = MapApi.generateMap(baseUrl, topic, level);
                finishForge(MapJson.parseMap(body));
            } catch (IOException | MapJson.InvalidMapException e) {
                failForge(e.getMessage());
            }
        });
    }

    /**
     * Demo survival. Wi-Fi fails in front of graders, and the fallback routes through the exact
     * same parse-validate-import path as the live one — so it cannot quietly rot: if it broke,
     * the live path would be broken too and MapJsonTest would say so.
     */
    private void forgeOffline() {
        if (forging) {
            return;
        }
        startForging(getString(R.string.library_forging_offline));
        App.get().io().execute(() -> {
            try (InputStream in = getAssets().open(FALLBACK_ASSET);
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
                finishForge(MapJson.parseMap(json.toString()));
            } catch (IOException | MapJson.InvalidMapException e) {
                failForge(e.getMessage());
            }
        });
    }

    private void finishForge(MapJson.GeneratedMap map) {
        RealmImport.importMap(App.get().db(), map);
        // The observer re-renders; nothing else to do but hand the controls back.
        onUi(() -> {
            forging = false;
            binding.forgeButton.setEnabled(true);
            binding.topicInput.getText().clear();
            binding.recoveryRow.setVisibility(View.GONE);
            setStatus(getString(R.string.library_forged, map.name));
        });
    }

    private void failForge(String message) {
        onUi(() -> {
            forging = false;
            binding.forgeButton.setEnabled(true);
            setStatus(getString(R.string.library_forge_failed, message == null ? "" : message));
            // Retry keeps the typed topic; the offline realm is the escape hatch beside it.
            binding.recoveryRow.setVisibility(View.VISIBLE);
        });
    }

    /** The "Forging…" typewriter is still running on a fast failure and would overwrite this. */
    private void setStatus(String message) {
        Typewriter.cancel(binding.statusText);
        binding.statusText.setText(message);
    }

    private void startForging(String message) {
        forging = true;
        binding.forgeButton.setEnabled(false);
        binding.recoveryRow.setVisibility(View.GONE);
        binding.statusText.setVisibility(View.VISIBLE);
        Typewriter.start(binding.statusText, message, 25);
    }

    /** A forge can outlive its Activity — 60s of DeepSeek is long enough to background the app. */
    private void onUi(Runnable action) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                action.run();
            }
        });
    }

    // ---- The library itself --------------------------------------------------------------

    private void render() {
        binding.realmContainer.removeAllViews();
        String filter = binding.filterInput.getText().toString().trim().toLowerCase(Locale.ROOT);

        int shown = 0;
        for (RealmDao.LibraryRow row : rows) {
            if (!matches(row, filter)) {
                continue;
            }
            shown++;
            addRow(getString(R.string.library_row_format,
                    row.realm.name,
                    row.realm.topic,
                    row.realm.cefrMin.name(),
                    row.realm.cefrMax.name(),
                    row.wordCount,
                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(row.realm.createdAt))),
                    () -> play(row.realm.id));
        }
        if (shown == 0) {
            addRow(getString(rows.isEmpty()
                    ? R.string.library_empty : R.string.library_no_matches), null);
        }
    }

    /** One box covers "filterable by topic and level" — the list is short and typing beats a dropdown. */
    private static boolean matches(RealmDao.LibraryRow row, String filter) {
        if (filter.isEmpty()) {
            return true;
        }
        String haystack = (row.realm.name + " " + row.realm.topic + " "
                + row.realm.cefrMin.name() + " " + row.realm.cefrMax.name()).toLowerCase(Locale.ROOT);
        return haystack.contains(filter);
    }

    private void addRow(String label, Runnable onPick) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setEnabled(onPick != null);
        if (onPick != null) {
            button.setOnClickListener(v -> onPick.run());
        }
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = getResources().getDimensionPixelSize(R.dimen.space_sm);
        button.setLayoutParams(params);
        binding.realmContainer.addView(button);
    }

    /** Identical to RealmSelectActivity's path: a forged realm is a realm. */
    private void play(long realmId) {
        App.get().io().execute(() -> {
            List<Monster> monsters = MonsterCatalog.load(getApplicationContext());
            long runId = RunEngine.startRun(App.get().db(), realmId, monsters,
                    RelicCatalog.load(getApplicationContext()));
            onUi(() -> {
                Intent intent = new Intent(this, DungeonMapActivity.class);
                intent.putExtra(DungeonMapActivity.EXTRA_RUN_ID, runId);
                startActivity(intent);
                finish();
            });
        });
    }

    private static final class SimpleWatcher implements TextWatcher {
        private final Runnable onChanged;

        SimpleWatcher(Runnable onChanged) {
            this.onChanged = onChanged;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            onChanged.run();
        }
    }
}
