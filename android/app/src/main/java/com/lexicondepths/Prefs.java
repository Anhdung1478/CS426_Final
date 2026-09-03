package com.lexicondepths;

import android.content.Context;
import android.content.SharedPreferences;

import com.lexicondepths.BuildConfig;
import com.lexicondepths.db.CefrLevel;

/**
 * Typed SharedPreferences wrapper. Uses the same file/key SeedLoader writes
 * seed_version to, so the two never fight over the underlying store.
 */
public class Prefs {

    private static final String PREFS_NAME = "lexicon_prefs";
    private static final String KEY_CEFR = "cefr_level";
    private static final String KEY_LOCALE = "ui_locale";
    private static final String KEY_TIMER_FULL = "timer_full_bonus_ms";
    private static final String KEY_TIMER_PARTIAL = "timer_partial_bonus_ms";
    private static final String KEY_MAP_API_BASE_URL = "map_api_base_url";
    private static final String KEY_REMINDERS_ENABLED = "reminders_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_ONBOARDING_SEEN = "onboarding_seen";

    private static final CefrLevel DEFAULT_CEFR = CefrLevel.B1;
    private static final int DEFAULT_TIMER_FULL_MS = 10000;
    private static final int DEFAULT_TIMER_PARTIAL_MS = 20000;
    private static final int DEFAULT_REMINDER_HOUR = 20;

    private final SharedPreferences prefs;

    public Prefs(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public CefrLevel cefrLevel() {
        return CefrLevel.valueOf(prefs.getString(KEY_CEFR, DEFAULT_CEFR.name()));
    }

    public void setCefrLevel(CefrLevel level) {
        prefs.edit().putString(KEY_CEFR, level.name()).apply();
    }

    /** "" means system default; otherwise a BCP-47 tag such as "en" or "vi". */
    public String uiLocale() {
        return prefs.getString(KEY_LOCALE, "");
    }

    public void setUiLocale(String languageTag) {
        prefs.edit().putString(KEY_LOCALE, languageTag).apply();
    }

    public int timerFullBonusMs() {
        return prefs.getInt(KEY_TIMER_FULL, DEFAULT_TIMER_FULL_MS);
    }

    public int timerPartialBonusMs() {
        return prefs.getInt(KEY_TIMER_PARTIAL, DEFAULT_TIMER_PARTIAL_MS);
    }

    /**
     * Where the Phase 3 proxy is. Defaults to BuildConfig.MAP_API_BASE_URL (10.0.2.2 — the
     * emulator's host alias). A physical device needs the laptop's LAN IP, which changes with
     * the Wi-Fi network, so it is typed here rather than rebuilt into the APK.
     */
    public String mapApiBaseUrl() {
        String stored = prefs.getString(KEY_MAP_API_BASE_URL, "").trim();
        return stored.isEmpty() ? BuildConfig.MAP_API_BASE_URL : stored;
    }

    /** Blank restores the build-config default rather than breaking every call. */
    public void setMapApiBaseUrl(String url) {
        String trimmed = url == null ? "" : url.trim();
        // Storing the default verbatim would pin it, so a later build-config change wouldn't take.
        String stored = BuildConfig.MAP_API_BASE_URL.equals(trimmed) ? "" : trimmed;
        prefs.edit().putString(KEY_MAP_API_BASE_URL, stored).apply();
    }

    /**
     * Off by default. The permission prompt fires when this is switched on (P4-6), not at
     * launch — a cold POST_NOTIFICATIONS ask before the user knows what the app is, is what
     * trains people to hit Deny reflexively.
     */
    public boolean remindersEnabled() {
        return prefs.getBoolean(KEY_REMINDERS_ENABLED, false);
    }

    public void setRemindersEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply();
    }

    public int reminderHour() {
        int hour = prefs.getInt(KEY_REMINDER_HOUR, DEFAULT_REMINDER_HOUR);
        return hour < 0 || hour > 23 ? DEFAULT_REMINDER_HOUR : hour;
    }

    public void setReminderHour(int hourOfDay) {
        prefs.edit().putInt(KEY_REMINDER_HOUR, Math.max(0, Math.min(23, hourOfDay))).apply();
    }

    /** Gates the onboarding carousel (P4-7) to exactly one showing per install. */
    public boolean onboardingSeen() {
        return prefs.getBoolean(KEY_ONBOARDING_SEEN, false);
    }

    public void setOnboardingSeen() {
        prefs.edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply();
    }

    /** Bounded so partial can never be less than full. */
    public void setTimerBonuses(int fullMs, int partialMs) {
        int full = Math.max(0, fullMs);
        int partial = Math.max(full, partialMs);
        prefs.edit()
                .putInt(KEY_TIMER_FULL, full)
                .putInt(KEY_TIMER_PARTIAL, partial)
                .apply();
    }
}
