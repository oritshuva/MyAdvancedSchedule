package com.example.myadvancedschedule;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Small helper to show simple notification reminders for tasks and events.
 */
public final class ReminderUtils {

    private static final String CHANNEL_ID = "myadvancedschedule_reminders";

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

    public static void showImmediateTaskReminder(Context context, Task task) {
        ensureChannel(context);
        String title = context.getString(R.string.app_name);
        String message = task.getTitle() != null ? task.getTitle() : context.getString(R.string.task_reminder);
        showReminderNotification(context, title, message);
    }

    /** Generic API to show a reminder notification. */
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
     * Schedule a reminder for an event based on its day-of-week and reminderTime (HH:mm).
     * Returns true if a future reminder was scheduled.
     */
    public static boolean scheduleEventReminder(Context context, Event event) {
        if (event == null) return false;
        String reminderTime = event.getReminderTime();
        String dayName = event.getDay();
        if (reminderTime == null || reminderTime.trim().isEmpty() || dayName == null) {
            return false;
        }
        String[] parts = reminderTime.split(":");
        if (parts.length != 2) return false;
        int hour;
        int minute;
        try {
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }

        java.util.Calendar trigger = java.util.Calendar.getInstance();
        trigger.set(java.util.Calendar.SECOND, 0);
        trigger.set(java.util.Calendar.MILLISECOND, 0);

        int targetDow = mapDayNameToCalendar(dayName);
        if (targetDow == -1) {
            targetDow = trigger.get(java.util.Calendar.DAY_OF_WEEK);
        }

        // Find the next occurrence of the requested day/time.
        while (true) {
            int currentDow = trigger.get(java.util.Calendar.DAY_OF_WEEK);
            if (currentDow == targetDow) {
                trigger.set(java.util.Calendar.HOUR_OF_DAY, hour);
                trigger.set(java.util.Calendar.MINUTE, minute);
                if (trigger.getTimeInMillis() > System.currentTimeMillis()) {
                    break;
                }
            }
            trigger.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }

        long triggerAt = trigger.getTimeInMillis();
        if (triggerAt <= System.currentTimeMillis()) {
            return false;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return false;

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_TITLE, event.getTitle());
        intent.putExtra(ReminderReceiver.EXTRA_MESSAGE, event.getTitle());

        int requestCode = buildEventRequestCode(event);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
        );
        return true;
    }

    private static int mapDayNameToCalendar(String dayName) {
        if (dayName == null) return -1;
        String d = dayName.trim().toLowerCase(java.util.Locale.US);
        if (d.startsWith("sun")) return java.util.Calendar.SUNDAY;
        if (d.startsWith("mon")) return java.util.Calendar.MONDAY;
        if (d.startsWith("tue")) return java.util.Calendar.TUESDAY;
        if (d.startsWith("wed")) return java.util.Calendar.WEDNESDAY;
        if (d.startsWith("thu")) return java.util.Calendar.THURSDAY;
        if (d.startsWith("fri")) return java.util.Calendar.FRIDAY;
        if (d.startsWith("sat")) return java.util.Calendar.SATURDAY;
        return -1;
    }

    private static int buildEventRequestCode(Event event) {
        String idPart = event.getId() != null ? event.getId() : "";
        String key = idPart + "|" + event.getDay() + "|" + event.getReminderTime();
        return key.hashCode();
    }
}

