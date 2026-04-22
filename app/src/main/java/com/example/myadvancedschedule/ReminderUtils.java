package com.example.myadvancedschedule;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;
import java.util.Locale;

/**
 * Helper for showing and scheduling reminder notifications for tasks and events.
 */
public final class ReminderUtils {

    private static final String CHANNEL_ID = "myadvancedschedule_reminders";

    private ReminderUtils() {
    }

    private static void ensureChannel(Context context) {
        // Create the notification channel once on Android O+ so reminders can be displayed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Task and event reminders");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /** Generic API to show a reminder notification immediately. */
    public static void showReminderNotification(Context context, String title, String message) {
        // Build a safe notification payload even when title/message are missing.
        if (context == null) return;
        ensureChannel(context);
        String finalTitle = title != null && !title.trim().isEmpty()
                ? title
                : context.getString(R.string.app_name);
        String finalMessage = message != null && !message.trim().isEmpty()
                ? message
                : context.getString(R.string.task_reminder);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_reminder)
                .setContentTitle(finalTitle)
                .setContentText(finalMessage)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), builder.build());
    }

    /**
     * Schedule a one-shot reminder at the provided time.
     *
     * On Android 12+ this checks SCHEDULE_EXACT_ALARM permission / app-op before
     * attempting an exact alarm. If not allowed, it falls back to a non-exact alarm
     * (still schedules, but timing may be inexact) and never throws SecurityException.
     */
    public static void scheduleExactReminder(Context context, String title, String message, long triggerAtMillis, int requestCode) {
        // Schedule a one-time alarm that triggers ReminderReceiver at the requested time.
        if (context == null) return;
        if (triggerAtMillis <= System.currentTimeMillis()) return;


        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_TITLE, title);
        intent.putExtra(ReminderReceiver.EXTRA_MESSAGE, message);

        PendingIntent pendingIntent;
        try {
            pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        } catch (IllegalArgumentException e) {
            // Cannot create PendingIntent (invalid requestCode/intent). Fail silently.
            return;
        }

        try {
            boolean canUseExact = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                canUseExact = alarmManager.canScheduleExactAlarms();
            }

            if (canUseExact) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
            } else {
                // Fallback: schedule a non-exact alarm instead of crashing.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                    );
                } else {
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                    );
                }
            }
        } catch (SecurityException e) {
            // As a final safety net, fall back to a non-exact alarm and avoid crashing.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
            }
        } catch (Exception e) {
            // Any other runtime issues (OEM alarm limitations, IllegalArgumentException, etc.)
            // should not crash the app.
            return;
        }
    }

    /**
     * Convenience helper for tasks to schedule an exact reminder.
     */
    public static void scheduleTaskReminder(Context context, Task task, long triggerAtMillis) {
        // Derive stable identifiers so the same task reminder updates predictably.
        if (context == null) return;
        if (task == null) return;
        String title = context.getString(R.string.app_name);
        String message = task.getTitle() != null ? task.getTitle() : context.getString(R.string.task_reminder);
        int requestCode = buildStableCode(task.getId(), task.getTitle(), null);
        scheduleExactReminder(context, title, message, triggerAtMillis, requestCode);
    }

    /**
     * Schedule a task reminder including a custom reminder note text.
     * Title will be the task name and content text will be the reminder note.
     */
    public static void scheduleTaskReminder(Context context, Task task, long triggerAtMillis, String reminderText) {
        // Prefer a user-provided reminder note when available.
        if (context == null) return;
        if (task == null) return;
        String title = task.getTitle() != null ? task.getTitle() : context.getString(R.string.app_name);
        String message = (reminderText != null && !reminderText.trim().isEmpty())
                ? reminderText
                : context.getString(R.string.task_reminder);
        int requestCode = buildStableCode(task.getId(), task.getTitle(), null);
        scheduleExactReminder(context, title, message, triggerAtMillis, requestCode);
    }

    /**
     * Convenience helper for events (including after-school) to schedule an exact reminder.
     */
    public static void scheduleEventReminder(Context context, Event event, long triggerAtMillis) {
        // Schedule default event reminder content.
        if (context == null) return;
        if (event == null) return;
        String title = context.getString(R.string.app_name);
        String message = event.getTitle();
        int requestCode = buildStableCode(event.getId(), event.getDay(), event.getTitle());
        scheduleExactReminder(context, title, message, triggerAtMillis, requestCode);
    }

    /**
     * Schedule an event reminder including a custom reminder note text.
     * Title will be the event name and content text will be the reminder note.
     */
    public static void scheduleEventReminder(Context context, Event event, long triggerAtMillis, String reminderText) {
        // Schedule event reminder using custom message text from the dialog.
        if (context == null) return;
        if (event == null) return;
        String title = event.getTitle() != null ? event.getTitle() : context.getString(R.string.app_name);
        String message = (reminderText != null && !reminderText.trim().isEmpty())
                ? reminderText
                : context.getString(R.string.task_reminder);
        int requestCode = buildStableCode(event.getId(), event.getDay(), event.getTitle());
        scheduleExactReminder(context, title, message, triggerAtMillis, requestCode);
    }

    private static int buildStableCode(String id, String part1, String part2) {
        // Hash core identity fields into a repeatable request code per reminder target.
        String safeId = id != null ? id : "";
        String p1 = part1 != null ? part1 : "";
        String p2 = part2 != null ? part2 : "";
        String key = safeId + "|" + p1 + "|" + p2;
        return key.hashCode();
    }
}

