package com.example.myadvancedschedule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data collected in Step 1 (FrameSetupFragment).
 * Used to generate time slots and day fragments in Step 2.
 */
public class FrameSetupData implements Serializable {

    private List<String> selectedDays;
    private String startTime;           // e.g. "08:00"
    private int lessonDurationMinutes;
    private int breakDurationMinutes;
    private int maxLessons;

    public FrameSetupData() {
        this.selectedDays = new ArrayList<>();
        this.startTime = "08:00";
        this.lessonDurationMinutes = 45;
        this.breakDurationMinutes = 10;
        this.maxLessons = 8;
    }

    public FrameSetupData(List<String> selectedDays, String startTime,
                          int lessonDurationMinutes, int breakDurationMinutes, int maxLessons) {
        this.selectedDays = selectedDays != null ? new ArrayList<>(selectedDays) : new ArrayList<>();
        this.startTime = startTime != null ? startTime : "08:00";
        this.lessonDurationMinutes = lessonDurationMinutes;
        this.breakDurationMinutes = breakDurationMinutes;
        this.maxLessons = maxLessons;
    }

    public List<String> getSelectedDays() {
        return selectedDays;
    }

    public void setSelectedDays(List<String> selectedDays) {
        this.selectedDays = selectedDays != null ? new ArrayList<>(selectedDays) : new ArrayList<>();
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public int getLessonDurationMinutes() {
        return lessonDurationMinutes;
    }

    public void setLessonDurationMinutes(int lessonDurationMinutes) {
        this.lessonDurationMinutes = lessonDurationMinutes;
    }

    public int getBreakDurationMinutes() {
        return breakDurationMinutes;
    }

    public void setBreakDurationMinutes(int breakDurationMinutes) {
        this.breakDurationMinutes = breakDurationMinutes;
    }

    public int getMaxLessons() {
        return maxLessons;
    }

    public void setMaxLessons(int maxLessons) {
        this.maxLessons = maxLessons;
    }
}
