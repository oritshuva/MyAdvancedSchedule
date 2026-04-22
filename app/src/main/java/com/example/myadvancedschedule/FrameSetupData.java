package com.example.myadvancedschedule;

// Serializable setup payload carrying weekly timing parameters between wizard
// components so schedule generation remains deterministic and consistent.

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Step-1 setup payload used to build the rest of the schedule wizard.
 * Persisting these decisions in one object lets SetupScheduleActivity generate
 * all day pages and time slots from a single consistent source of truth.
 */
public class FrameSetupData implements Serializable {

    private List<String> selectedDays;
    private String startTime;           // e.g. "08:00"
    private int lessonDurationMinutes;
    private int breakDurationMinutes;
    private int maxLessons;

    public FrameSetupData() {
        // Opinionated defaults reduce initial friction and speed up first-time setup.
        this.selectedDays = new ArrayList<>();
        this.startTime = "08:00";
        this.lessonDurationMinutes = 45;
        this.breakDurationMinutes = 10;
        this.maxLessons = 8;
    }

    public FrameSetupData(List<String> selectedDays, String startTime,
                          int lessonDurationMinutes, int breakDurationMinutes, int maxLessons) {
        // Defensive copying prevents external list mutations from altering wizard state.
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
