package com.lexicondepths.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * AlarmManager schedules do not survive a reboot. Fifteen lines, and without them the reminder
 * quietly stops working after the first restart.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            ReviewReminder.rearmIfEnabled(context.getApplicationContext());
        }
    }
}
