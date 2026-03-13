package com.example.myadvancedschedule;

import java.io.Serializable;

public class Event implements Serializable {
    private String id;
    private String title;
    private String startTime;
    private String endTime;
    private String day;
    private String type; // "school" or "after_school"
    private String note;
    private String reminderTime;
    private boolean isPassed;
    private String userId;
    private long timestamp;

    public Event() {
        // Required empty constructor for Firestore
    }

    public Event(String title, String startTime, String endTime, String day, String type) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.day = day;
        this.type = type;
        this.isPassed = false;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(String reminderTime) {
        this.reminderTime = reminderTime;
    }

    public boolean isPassed() {
        return isPassed;
    }

    public void setPassed(boolean passed) {
        isPassed = passed;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
