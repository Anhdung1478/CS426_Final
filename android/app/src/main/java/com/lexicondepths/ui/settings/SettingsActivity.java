package com.lexicondepths.ui.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.lexicondepths.App;
import com.lexicondepths.Prefs;
import com.lexicondepths.R;
import com.lexicondepths.databinding.ActivitySettingsBinding;
import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Profile;

/**
 * CEFR level, UI locale, the two timer-bonus thresholds, and the Phase 3 proxy URL.
 * Typed reads/writes go through Prefs.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final int MS_PER_STEP = 500;
    private static final int SEEK_MAX_STEPS = 60;
    private static final String[] LOCALE_TAGS = {"", "en", "vi"};

    private ActivitySettingsBinding binding;
    private Prefs prefs;
    private boolean ready;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        prefs = App.get().prefs();

        setUpCefrSpinner();
        setUpLocaleSpinner();
        setUpTimerSeekBars();
        binding.mapApiUrlInput.setText(prefs.mapApiBaseUrl());
        ready = true;
    }

    /**
     * Saved on the way out rather than per keystroke: half a typed IP is not a URL, and every
     * intermediate value would be persisted.
     */
    @Override
    protected void onPause() {
        super.onPause();
        prefs.setMapApiBaseUrl(binding.mapApiUrlInput.getText().toString());
    }

    private void setUpCefrSpinner() {
        CefrLevel[] levels = CefrLevel.values();
        String[] labels = new String[levels.length];
        for (int i = 0; i < levels.length; i++) {
            labels[i] = levels[i].name();
        }
        binding.cefrSpinner.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        binding.cefrSpinner.setSelection(prefs.cefrLevel().ordinal());
        binding.cefrSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!ready) {
                    return;
                }
                CefrLevel level = levels[position];
                prefs.setCefrLevel(level);
                App.get().io().execute(() -> {
                    Profile profile = App.get().db().profileDao().getProfileSync();
                    if (profile == null) {
                        profile = new Profile();
                    }
                    profile.cefrLevel = level;
                    App.get().db().profileDao().upsert(profile);
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setUpLocaleSpinner() {
        String[] labels = {
                getString(R.string.settings_locale_system),
                getString(R.string.settings_locale_en),
                getString(R.string.settings_locale_vi)
        };
        binding.localeSpinner.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));

        String currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        int selected = 0;
        for (int i = 0; i < LOCALE_TAGS.length; i++) {
            if (LOCALE_TAGS[i].equals(currentTag)) {
                selected = i;
                break;
            }
        }
        binding.localeSpinner.setSelection(selected);

        binding.localeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!ready) {
                    return;
                }
                String tag = LOCALE_TAGS[position];
                prefs.setUiLocale(tag);
                AppCompatDelegate.setApplicationLocales(tag.isEmpty()
                        ? LocaleListCompat.getEmptyLocaleList()
                        : LocaleListCompat.forLanguageTags(tag));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setUpTimerSeekBars() {
        binding.timerFullSeek.setMax(SEEK_MAX_STEPS);
        binding.timerPartialSeek.setMax(SEEK_MAX_STEPS);
        binding.timerFullSeek.setProgress(prefs.timerFullBonusMs() / MS_PER_STEP);
        binding.timerPartialSeek.setProgress(prefs.timerPartialBonusMs() / MS_PER_STEP);
        updateTimerLabels();

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                // Bounded: dragging full above partial pulls partial up; dragging partial below full snaps it back.
                if (seekBar == binding.timerFullSeek && progress > binding.timerPartialSeek.getProgress()) {
                    binding.timerPartialSeek.setProgress(progress);
                } else if (seekBar == binding.timerPartialSeek && progress < binding.timerFullSeek.getProgress()) {
                    seekBar.setProgress(binding.timerFullSeek.getProgress());
                    return;
                }
                updateTimerLabels();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefs.setTimerBonuses(
                        binding.timerFullSeek.getProgress() * MS_PER_STEP,
                        binding.timerPartialSeek.getProgress() * MS_PER_STEP);
            }
        };
        binding.timerFullSeek.setOnSeekBarChangeListener(listener);
        binding.timerPartialSeek.setOnSeekBarChangeListener(listener);
    }

    private void updateTimerLabels() {
        binding.timerFullLabel.setText(getString(
                R.string.settings_timer_full, binding.timerFullSeek.getProgress() * MS_PER_STEP));
        binding.timerPartialLabel.setText(getString(
                R.string.settings_timer_partial, binding.timerPartialSeek.getProgress() * MS_PER_STEP));
    }
}
