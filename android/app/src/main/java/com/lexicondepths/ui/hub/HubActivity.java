package com.lexicondepths.ui.hub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.lexicondepths.App;
import com.lexicondepths.R;
import com.lexicondepths.databinding.ActivityHubBinding;
import com.lexicondepths.db.entity.Profile;
import com.lexicondepths.db.entity.Run;
import com.lexicondepths.ui.library.LibraryActivity;
import com.lexicondepths.ui.map.DungeonMapActivity;
import com.lexicondepths.ui.onboarding.OnboardingActivity;
import com.lexicondepths.ui.practice.PracticeActivity;
import com.lexicondepths.ui.realm.RealmSelectActivity;
import com.lexicondepths.ui.settings.SettingsActivity;
import com.lexicondepths.ui.shop.ShopActivity;
import com.lexicondepths.ui.stats.StatsActivity;
import com.lexicondepths.ui.widget.Typewriter;

/** Launcher and character hub: reads CEFR level, streak, due count, and Marks live from Room. */
public class HubActivity extends AppCompatActivity {

    private ActivityHubBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHubBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        App.get().db().profileDao().getProfile().observe(this, this::renderProfile);

        binding.resumeRunButton.setOnClickListener(v -> resumeActiveRun());
        binding.practiceButton.setOnClickListener(v -> startActivity(new Intent(this, PracticeActivity.class)));
        binding.realmButton.setOnClickListener(v -> startActivity(new Intent(this, RealmSelectActivity.class)));
        binding.libraryButton.setOnClickListener(v -> startActivity(new Intent(this, LibraryActivity.class)));
        binding.shopButton.setOnClickListener(v -> startActivity(new Intent(this, ShopActivity.class)));
        binding.statsButton.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
        binding.settingsButton.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        // Once per install, before anything else. OnboardingActivity owns setting the flag, so
        // skipping and finishing close the gate identically.
        if (!App.get().prefs().onboardingSeen()) {
            startActivity(new Intent(this, OnboardingActivity.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Typewriter.start(binding.titleText, getString(R.string.app_name), 40);
        checkForActiveRun();
        refreshDueCount();
    }

    /**
     * Read on resume rather than observed, because the query's "now" is an argument: an
     * observed getDueWords(System.currentTimeMillis()) binds the timestamp once at onCreate,
     * so a word that came due while the player was in Practice never showed up until the
     * process restarted — and the reminder, which builds a fresh snapshot every time it fires,
     * would then disagree with the Hub. Found by the P4-13 edge-case pass.
     */
    private void refreshDueCount() {
        App.get().io().execute(() -> {
            int due = App.get().db().wordDao().getDueWordIdsSync(System.currentTimeMillis()).size();
            runOnUiThread(() -> binding.dueText.setText(getString(R.string.hub_due, due)));
        });
    }

    /**
     * A phone ringing mid-battle kills the app; this is what makes reopening it land the player
     * back on their run instead of a dead end at the Hub — the actual resume-after-kill logic
     * (which node, how many slots answered) lives in RunEngine/DungeonMapActivity.
     */
    private void checkForActiveRun() {
        App.get().io().execute(() -> {
            Run active = App.get().db().runDao().getActiveRun();
            runOnUiThread(() -> binding.resumeRunButton.setVisibility(active != null ? View.VISIBLE : View.GONE));
        });
    }

    private void resumeActiveRun() {
        App.get().io().execute(() -> {
            Run active = App.get().db().runDao().getActiveRun();
            if (active == null) {
                return;
            }
            runOnUiThread(() -> {
                Intent intent = new Intent(this, DungeonMapActivity.class);
                intent.putExtra(DungeonMapActivity.EXTRA_RUN_ID, active.id);
                startActivity(intent);
            });
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Typewriter.cancel(binding.titleText);
    }

    private void renderProfile(Profile profile) {
        if (profile == null) {
            return;
        }
        binding.cefrText.setText(getString(R.string.hub_cefr, profile.cefrLevel.name()));
        binding.streakText.setText(getString(R.string.hub_streak, profile.streak));
        binding.marksText.setText(getString(R.string.hub_marks, profile.marks));
    }
}
