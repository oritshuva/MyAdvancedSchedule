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

    public interface OnDateTimeSelectedListener {
        void onDateTimeSelected(long triggerAtMillis);
    }

    private ReminderUtils() {
    }

    private static void ensureChannel(Context context) {
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
     * Show a date picker followed by a time picker and invoke the listener with the
     * exact trigger time in milliseconds.
     */
    public static void showDateTimePicker(Context context, OnDateTimeSelectedListener listener) {
        if (listener == null) return;

        Calendar now = Calendar.getInstance();
        DatePickerDialog dateDialog = new DatePickerDialog(
                context,
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timeDialog = new TimePickerDialog(
                            context,
                            (TimePicker timeView, int hourOfDay, int minute) -> {
                                selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selected.set(Calendar.MINUTE, minute);
                                selected.set(Calendar.SECOND, 0);
                                selected.set(Calendar.MILLISECOND, 0);

                                long triggerAt = selected.getTimeInMillis();
                                if (triggerAt <= System.currentTimeMillis()) {
                                    Toast.makeText(context, R.string.reminder_time_in_past, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                listener.onDateTimeSelected(triggerAt);
                            },
                            now.get(Calendar.HOUR_OF_DAY),
                            now.get(Calendar.MINUTE),
                            true
                    );
                    timeDialog.show();
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        dateDialog.show();
    }

    /**
     * Schedule a one-shot reminder at the exact provided time.
     */
    public static void scheduleExactReminder(Context context, String title, String message, long triggerAtMillis, int requestCode) {
        if (context == null) return;
        if (triggerAtMillis <= System.currentTimeMillis()) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_TITLE, title);
        intent.putExtra(ReminderReceiver.EXTRA_MESSAGE, message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
        );
    }

    /**
     * Convenience helper for tasks to schedule an exact reminder.
     */
    public static void scheduleTaskReminder(Context context, Task task, long triggerAtMillis) {
        if (task == null) return;
        String title = context.getString(R.string.app_name);
        String message = task.getTitle() != null ? task.getTitle() : context.getString(R.string.task_reminder);
        int requestCode = buildStableCode(task.getId(), task.getTitle(), null);
        scheduleExactReminder(context, title, message, triggerAtMillis, requestCode);
    }

    /**
     * Convenience helper for events (including after-school) to schedule an exact reminder.
     */
    public static void scheduleEventReminder(Context context, Event event, long triggerAtMillis) {
        if (event == null) return;
        String title = context.getString(R.string.app_name);
        String message = event.getTitle();
        int requestCode = buildStableCode(event.getId(), event.getDay(), event.getTitle());
        scheduleExactReminder(context, title, message, triggerAtMillis, requestCode);
    }

    private static int buildStableCode(String id, String part1, String part2) {
        String safeId = id != null ? id : "";
        String p1 = part1 != null ? part1 : "";
        String p2 = part2 != null ? part2 : "";
        String key = safeId + "|" + p1 + "|" + p2;
        return key.hashCode();
    }
}

