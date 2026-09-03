package com.lexicondepths.notify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.app.NotificationManager;
import android.content.Context;
import android.service.notification.StatusBarNotification;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.lexicondepths.App;
import com.lexicondepths.db.AppDatabase;
import com.lexicondepths.db.CefrLevel;
import com.lexicondepths.db.entity.Word;
import com.lexicondepths.db.entity.WordProgress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

/**
 * The reminder's two rules, exercised in the app's own process against the real database.
 *
 * This exists because `adb shell am broadcast` cannot deliver to a non-exported receiver, so
 * "no notification appeared" from a shell broadcast proves nothing — it looks identical to the
 * broadcast never arriving. Calling postIfDue directly is the only honest check of the
 * zero-due rule, which is the behaviour docs/phase-4.md P4-5 specifically calls for.
 */
@RunWith(AndroidJUnit4.class)
public class ReviewReminderTest {

    private static final long TEST_WORD_ID = 999_777L;

    private Context context;
    private AppDatabase db;
    private NotificationManager notifications;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        db = App.get().db();
        notifications = context.getSystemService(NotificationManager.class);
        InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(
                context.getPackageName(), android.Manifest.permission.POST_NOTIFICATIONS);
        assumeTrue("needs POST_NOTIFICATIONS to observe anything", ReviewReminder.canPost(context));

        ReviewReminder.createChannel(context);
        notifications.cancelAll();
        clearTestRows();
    }

    @After
    public void tearDown() {
        notifications.cancelAll();
        clearTestRows();
    }

    /** Only ever touches the fixture row, so a developer's real progress survives the test. */
    private void clearTestRows() {
        db.getOpenHelper().getWritableDatabase()
                .execSQL("DELETE FROM WordProgress WHERE wordId = " + TEST_WORD_ID);
        db.getOpenHelper().getWritableDatabase()
                .execSQL("DELETE FROM Word WHERE id = " + TEST_WORD_ID);
    }

    private void insertDueWord(long dueAt) {
        Word word = new Word();
        word.id = TEST_WORD_ID;
        word.headword = "reminder-fixture-word";
        word.cefr = CefrLevel.B1;
        word.topic = "business";
        word.pos = "noun";
        word.definition = "a fixture used by ReviewReminderTest";
        word.example = "This is a reminder-fixture-word.";
        word.synonyms = new ArrayList<>();
        word.antonyms = new ArrayList<>();
        word.collocations = new ArrayList<>();
        word.forms = new ArrayList<>();
        db.wordDao().insertAll(Collections.singletonList(word));

        WordProgress progress = new WordProgress();
        progress.wordId = TEST_WORD_ID;
        progress.dueAt = dueAt;
        db.wordProgressDao().upsert(progress);
    }

    private StatusBarNotification ourNotification() {
        for (StatusBarNotification posted : notifications.getActiveNotifications()) {
            if (ReviewReminder.CHANNEL_ID.equals(posted.getNotification().getChannelId())) {
                return posted;
            }
        }
        return null;
    }

    /**
     * NotificationManager.notify hands off to a system service, so the post is not visible the
     * instant it returns. Polling for the condition rather than sleeping a fixed amount keeps
     * the test fast when it passes and honest when it fails.
     */
    private StatusBarNotification awaitNotification() {
        long deadline = System.currentTimeMillis() + 5000;
        StatusBarNotification posted = ourNotification();
        while (posted == null && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
            posted = ourNotification();
        }
        return posted;
    }

    /** The mirror of awaitNotification: give a post that should never happen time to show up. */
    private StatusBarNotification noNotificationAfterGracePeriod() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        return ourNotification();
    }

    /**
     * "0 words due" is worse than silence — it trains the user to swipe this app's
     * notifications away unread, which costs every future reminder too.
     */
    @Test
    public void aDueCountOfZeroPostsNothing() {
        // Everything already in the database is scheduled in the future or has no row at all;
        // the fixture is deliberately not inserted.
        long farPast = 1L;
        int realDue = db.wordProgressDao().countDue(farPast);
        assertEquals("fixture check: nothing should be due at the epoch", 0, realDue);

        ReviewReminder.postIfDue(context, farPast);

        assertEquals("a reminder with nothing to review must stay silent",
                null, noNotificationAfterGracePeriod());
    }

    @Test
    public void aDueWordPostsAReminder() {
        long now = System.currentTimeMillis();
        insertDueWord(now - 1000);

        ReviewReminder.postIfDue(context, now);

        StatusBarNotification posted = awaitNotification();
        assertNotNull("a word is due, so the reminder must arrive", posted);
        CharSequence body = posted.getNotification().extras.getCharSequence("android.text");
        assertNotNull(body);
        assertTrue("the body must carry the count the Hub would show: " + body,
                body.toString().contains(String.valueOf(db.wordProgressDao().countDue(now))));
    }

    /** Inexact daily repeat, so the only thing to get right is which day the first one lands on. */
    @Test
    public void nextTriggerIsTodayIfTheHourIsStillAheadOtherwiseTomorrow() {
        Calendar noon = Calendar.getInstance();
        noon.set(Calendar.HOUR_OF_DAY, 12);
        noon.set(Calendar.MINUTE, 0);
        noon.set(Calendar.SECOND, 0);
        noon.set(Calendar.MILLISECOND, 0);
        long noonToday = noon.getTimeInMillis();

        long fromMorning = ReviewReminder.nextTriggerAt(12, noonToday - 3 * 60 * 60 * 1000);
        assertEquals("9am should schedule for noon the same day", noonToday, fromMorning);

        long fromEvening = ReviewReminder.nextTriggerAt(12, noonToday + 3 * 60 * 60 * 1000);
        assertEquals("3pm should schedule for noon tomorrow",
                noonToday + 24 * 60 * 60 * 1000, fromEvening);
    }
}
