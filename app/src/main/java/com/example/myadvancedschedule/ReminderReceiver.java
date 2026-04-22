package com.example.myadvancedschedule;

// BroadcastReceiver endpoint for scheduled reminders, allowing notifications
// to fire even when app UI components are not currently active.

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Alarm entry point for scheduled reminders.
 * Android can invoke this receiver while the app UI is closed, so reminder
 * delivery is decoupled from activity/fragment lifecycle state.
 */
public class ReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_MESSAGE = "extra_message";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Guard against malformed broadcasts to keep alarm delivery crash-safe.
        if (intent == null) return;
        String title = intent.getStringExtra(EXTRA_TITLE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        if (message == null || message.trim().isEmpty()) {
            // Use a localized fallback so users still get meaningful reminder text.
            message = context.getString(R.string.task_reminder);
        }
        // Delegate rendering to ReminderUtils to keep notification behavior consistent.
        ReminderUtils.showReminderNotification(context, title, message);
    }
}

