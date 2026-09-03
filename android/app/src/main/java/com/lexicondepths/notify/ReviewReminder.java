package com.lexicondepths.notify;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.lexicondepths.App;
import com.lexicondepths.R;
import com.lexicondepths.content.StatsLoader;
import com.lexicondepths.game.stats.StatsSnapshot;
import com.lexicondepths.ui.practice.PracticeActivity;

import java.util.Calendar;

/**
 * The daily "N words are due" nudge: channel, alarm, and the notification itself.
 *
 * notify/ rather than ui/ — nothing in here is a screen.
 *
 * AlarmManager, not WorkManager: WorkManager is not on §2's approved dependency list, and
 * minimal-app-design bans adding a library where an SDK API does the job. setInexactRepeating
 * plus a BroadcastReceiver is that API.
 *
 * Inexact is a decision, not a shortcut. Exact alarms need SCHEDULE_EXACT_ALARM on Android 12+,
 * a permission the Play Store scrutinises and which a daily vocabulary reminder has no claim
 * to. Letting the OS batch the wakeup is both the correct citizenship and one fewer permission
 * to justify.
 */
public final class ReviewReminder {

    static final String CHANNEL_ID = "review_reminder";
    private static final int NOTIFICATION_ID = 1001;
    private static final int ALARM_REQUEST_CODE = 2001;

    private ReviewReminder() {
    }

    /** Idempotent — creating an existing channel is a no-op, so this can be called freely. */
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notify_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.notify_channel_desc));
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    /** Arms (or re-arms, replacing any existing alarm) the daily repeat at the stored hour. */
    public static void schedule(Context context) {
        createChannel(context);
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        alarms.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                nextTriggerAt(App.get().prefs().reminderHour(), System.currentTimeMillis()),
                AlarmManager.INTERVAL_DAY,
                alarmIntent(context, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    public static void cancel(Context context) {
        PendingIntent existing = alarmIntent(context, PendingIntent.FLAG_NO_CREATE);
        if (existing == null) {
            return;
        }
        context.getSystemService(AlarmManager.class).cancel(existing);
        existing.cancel();
    }

    /**
     * Called from BootReceiver: AlarmManager schedules do not survive a reboot, and without
     * this the feature quietly stops working after the first restart — invisible in a demo and
     * obvious in use.
     */
    public static void rearmIfEnabled(Context context) {
        if (App.get().prefs().remindersEnabled()) {
            schedule(context);
        }
    }

    /**
     * The due count comes from P4-1's snapshot, the same object the Hub and the stats screen
     * read, so the notification's number cannot disagree with the one on screen.
     *
     * Blocking; call it from App.io().
     */
    static void postIfDue(Context context) {
        postIfDue(context, System.currentTimeMillis());
    }

    static void postIfDue(Context context, long now) {
        StatsSnapshot snapshot = StatsLoader.load(App.get().db(), now);
        if (snapshot.dueNow <= 0) {
            // "0 words due" is worse than silence — it trains the user to swipe this app's
            // notifications away unread, which costs every future reminder too.
            return;
        }
        if (!canPost(context)) {
            return;
        }
        // Straight to Practice: the reminder's whole purpose is the review session, and making
        // the user navigate to it from the Hub wastes the intent it just earned.
        Intent open = new Intent(context, PracticeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tap = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(context.getString(R.string.notify_review_title))
                .setContentText(context.getResources().getQuantityString(
                        R.plurals.notify_review_body, snapshot.dueNow, snapshot.dueNow))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(tap);
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification.build());
    }

    /** API 33+ can revoke POST_NOTIFICATIONS while the app is backgrounded; posting then throws. */
    public static boolean canPost(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** The next occurrence of hour:00 — today if it is still ahead, otherwise tomorrow. */
    static long nextTriggerAt(int hourOfDay, long now) {
        Calendar next = Calendar.getInstance();
        next.setTimeInMillis(now);
        next.set(Calendar.HOUR_OF_DAY, hourOfDay);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= now) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }
        return next.getTimeInMillis();
    }

    private static PendingIntent alarmIntent(Context context, int flags) {
        return PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                new Intent(context, ReminderReceiver.class),
                flags | PendingIntent.FLAG_IMMUTABLE);
    }
}
