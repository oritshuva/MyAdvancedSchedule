package com.example.myadvancedschedule;

// Shared day-resolution utility used by schedule screens to keep weekday
// selection/filtering logic consistent across school and after-school flows.

import java.util.Calendar;

/**
 * Shared date helper used by schedule fragments.
 * Centralizing day-name resolution avoids duplicate Calendar logic and keeps
 * filtering behavior consistent between school and after-school tabs.
 */
public final class ScheduleFragmentHelper {

    private static final String[] DAYS = {
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    /** Calendar.SUNDAY=1, MONDAY=2, ... SATURDAY=7 */
    public static String getTodayDayName() {
        // Use device calendar so the default selected tab follows the user's locale/timezone day.
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int index = day - Calendar.SUNDAY;
        if (index >= 0 && index < DAYS.length) return DAYS[index];
        // Safe fallback prevents null labels and keeps UI rendering predictable.
        return DAYS[0];
    }
}
