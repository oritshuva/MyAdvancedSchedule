package com.example.myadvancedschedule;

import java.io.Serializable;

/**
 * A single time slot (start–end) for a lesson.
 * Used to display and build Lesson objects in DayScheduleFragment.
 */
public class TimeSlot implements Serializable {
    private String startTime;  // e.g. "08:00"
    private String endTime;    // e.g. "08:45"

    public TimeSlot(String startTime, String endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    /** Display label e.g. "08:00 - 08:45" */
    public String getLabel() {
        return startTime + " - " + endTime;
    }
}
