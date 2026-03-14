package com.example.myadvancedschedule;

import java.util.Calendar;

/** Returns current day name for filtering today's lessons. */
public final class ScheduleFragmentHelper {

    private static final String[] DAYS = {
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    /** Calendar.SUNDAY=1, MONDAY=2, ... SATURDAY=7 */
    public static String getTodayDayName() {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int index = day - Calendar.SUNDAY;
        if (index >= 0 && index < DAYS.length) return DAYS[index];
        return DAYS[0];
    }
}
