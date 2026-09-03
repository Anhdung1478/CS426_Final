package com.lexicondepths.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lexicondepths.App;

/** Fired by the daily alarm. Room work goes to App.io(); goAsync keeps the process alive for it. */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Context appContext = context.getApplicationContext();
        PendingResult pending = goAsync();
        App.get().io().execute(() -> {
            try {
                ReviewReminder.postIfDue(appContext);
            } finally {
                pending.finish();
            }
        });
    }
}
