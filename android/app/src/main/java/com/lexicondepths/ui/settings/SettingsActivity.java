package com.lexicondepths.ui.settings;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.os.LocaleListCompat;

import com.lexicondepths.App;
import com.lexicondepths.Prefs;
import com.lexicondepths.R;
import com.lexicondepths.databinding.ActivitySettingsBinding;
import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Profile;
import com.lexicondepths.notify.ReviewReminder;
import com.lexicondepths.ui.onboarding.OnboardingActivity;

import java.util.Locale;

/**
 * CEFR level, UI locale, the two timer-bonus thresholds, the Phase 3 proxy URL, the daily
 * review reminder, and Replay intro. Typed reads/writes go through Prefs.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final int MS_PER_STEP = 500;
    private static final int SEEK_MAX_STEPS = 60;
    private static final String[] LOCALE_TAGS = {"", "en", "vi"};

    private ActivitySettingsBinding binding;
    private Prefs prefs;
    private boolean ready;
    private ActivityResultLauncher<String> notificationPermission;
    private boolean blockedRouteVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        prefs = App.get().prefs();

        // Registered before the first onResume, as the contract requires.
        notificationPermission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), this::onPermissionResult);

        setUpCefrSpinner();
        setUpLocaleSpinner();
        setUpTimerSeekBars();
        setUpReminder();
        // Small, and it means a demo can show onboarding on command instead of clearing app
        // data in front of an audience. It replays the carousel; it never re-arms the gate.
        binding.replayIntroButton.setOnClickListener(v ->
                startActivity(new Intent(this, OnboardingActivity.class)));
        binding.mapApiUrlInput.setText(prefs.mapApiBaseUrl());
        ready = true;
        syncReminderUi();
    }

    /**
     * The user may have granted or revoked POST_NOTIFICATIONS in system settings while this
     * screen was backgrounded. A toggle that silently fails to arm anything is worse than no
     * toggle, so its state is re-derived every time the screen comes back.
     */
    @Override
    protected void onResume() {
        super.onResume();
        syncReminderUi();
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

    /**
     * project-idea.md §11 asks for this to be "built into the screen flow, not as an
     * afterthought". So it is a flow with three real branches rather than a requestPermissions
     * call:
     *
     *   API &lt; 33       - no such permission exists; the toggle simply works.
     *   API 33+ granted - schedule the alarm.
     *   API 33+ denied  - the toggle snaps back off, with a route to system settings.
     *
     * The request fires here, on the toggle, and never at launch: a cold POST_NOTIFICATIONS
     * prompt before the user knows what the app is, is what trains people to hit Deny.
     */
    private void setUpReminder() {
        String[] hours = new String[24];
        for (int hour = 0; hour < hours.length; hour++) {
            hours[hour] = hourLabel(hour);
        }
        binding.reminderHourSpinner.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, hours));
        binding.reminderHourSpinner.setSelection(prefs.reminderHour());
        binding.reminderHourSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!ready) {
                    return;
                }
                prefs.setReminderHour(position);
                if (prefs.remindersEnabled()) {
                    ReviewReminder.schedule(getApplicationContext()); // re-arm at the new hour
                }
                syncReminderUi();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        binding.reminderSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (!ready) {
                return;
            }
            if (!checked) {
                enableReminders(false);
                return;
            }
            if (ReviewReminder.canPost(this)) {
                enableReminders(true);
            } else {
                notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        });

        binding.reminderSettingsButton.setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())));
    }

    private void onPermissionResult(boolean granted) {
        // Denied leaves the toggle off, never on-but-silent.
        enableReminders(granted);
        // Re-prompting on a permanent denial does nothing at all - the dialog never appears and
        // the toggle looks broken. Distinguish a first refusal from a permanent one rather than
        // guessing, and offer system settings only when the dialog can no longer help.
        boolean canAskAgain = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ActivityCompat.shouldShowRequestPermissionRationale(
                        this, android.Manifest.permission.POST_NOTIFICATIONS);
        blockedRouteVisible = !granted && !canAskAgain;
        showBlockedRoute(blockedRouteVisible);
    }

    private void enableReminders(boolean enabled) {
        prefs.setRemindersEnabled(enabled);
        if (enabled) {
            ReviewReminder.schedule(getApplicationContext());
            blockedRouteVisible = false;
        } else {
            ReviewReminder.cancel(getApplicationContext());
        }
        syncReminderUi();
    }

    /** The toggle's state must always reflect whether a notification will actually arrive. */
    private void syncReminderUi() {
        boolean armed = prefs.remindersEnabled() && ReviewReminder.canPost(this);
        if (prefs.remindersEnabled() && !armed) {
            // Permission revoked from outside the app: stop claiming a reminder is coming.
            prefs.setRemindersEnabled(false);
            ReviewReminder.cancel(getApplicationContext());
        }
        boolean wasReady = ready;
        ready = false; // setChecked must not re-enter the toggle listener
        binding.reminderSwitch.setChecked(armed);
        ready = wasReady;

        binding.reminderHourSpinner.setEnabled(armed);
        binding.reminderHourLabel.setText(
                getString(R.string.settings_reminder_hour, hourLabel(prefs.reminderHour())));
        showBlockedRoute(blockedRouteVisible && !armed);
    }

    private static String hourLabel(int hourOfDay) {
        return String.format(Locale.US, "%02d:00", hourOfDay);
    }

    private void showBlockedRoute(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        binding.reminderBlockedText.setVisibility(visibility);
        binding.reminderSettingsButton.setVisibility(visibility);
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
