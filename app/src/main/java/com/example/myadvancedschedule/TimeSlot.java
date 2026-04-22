package com.example.myadvancedschedule;

// Lightweight value object representing one lesson window, used during setup
// to generate clear, reusable timetable periods across selected days.

import java.io.Serializable;

/**
 * A single time slot (start–end) for a lesson.
 * This value object keeps slot math separate from UI fields so setup flow can
 * generate reusable, deterministic periods across multiple day fragments.
 */
public class TimeSlot implements Serializable {
    private String startTime;  // e.g. "08:00"
    private String endTime;    // e.g. "08:45"

    public TimeSlot(String startTime, String endTime) {
        // Keep constructor lightweight because many slots are created during setup.
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
        // One canonical label format keeps spinner/list rendering consistent.
        return startTime + " - " + endTime;
    }
}
