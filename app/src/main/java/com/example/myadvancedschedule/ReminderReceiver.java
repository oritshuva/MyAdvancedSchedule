package com.example.myadvancedschedule;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receives scheduled reminder alarms and shows a notification.
 */
public class ReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_MESSAGE = "extra_message";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String title = intent.getStringExtra(EXTRA_TITLE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        if (message == null || message.trim().isEmpty()) {
            message = context.getString(R.string.task_reminder);
        }
        ReminderUtils.showReminderNotification(context, title, message);
    }
}

