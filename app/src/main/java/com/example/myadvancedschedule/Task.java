package com.example.myadvancedschedule;

// Task entity model for UI + Firestore, including optional reminder metadata
// so scheduled notifications can be reconstructed from persisted state.

import java.io.Serializable;

/**
 * Task domain model used by the Tasks tab and Firestore persistence layer.
 * It intentionally includes optional reminder metadata so scheduling decisions
 * can survive app restarts and be reconstructed from backend state.
 */
public class Task implements Serializable {
    private String id;
    private String title;
    private String dueTime;  // e.g. "14:30" or "Tomorrow 10:00"
    private boolean completed;
    // Optional reminder metadata (millis since epoch and user-entered detail text).
    private Long reminderTimeMillis;
    private String reminderDetail;

    public Task() {
        // Required for Firebase/serialization frameworks.
    }

    public Task(String title, String dueTime, boolean completed) {
        // Constructor used when creating new local tasks before Firestore assigns ID.
        this.title = title;
        this.dueTime = dueTime;
        this.completed = completed;
    }

    public Task(String id, String title, String dueTime, boolean completed) {
        // Constructor used when hydrating existing tasks loaded from Firestore.
        this.id = id;
        this.title = title;
        this.dueTime = dueTime;
        this.completed = completed;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDueTime() { return dueTime; }
    public void setDueTime(String dueTime) { this.dueTime = dueTime; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Long getReminderTimeMillis() {
        return reminderTimeMillis;
    }

    public void setReminderTimeMillis(Long reminderTimeMillis) {
        this.reminderTimeMillis = reminderTimeMillis;
    }

    public String getReminderDetail() {
        return reminderDetail;
    }

    public void setReminderDetail(String reminderDetail) {
        this.reminderDetail = reminderDetail;
    }
}
