package com.example.myadvancedschedule;

import java.io.Serializable;

// Core schedule model shared by setup flow, schedule tabs, adapters, and Firestore CRUD.
// One unified model keeps school and after-school rendering/editing logic interoperable.

public class Lesson implements Serializable {
    private String id;
    private String subject;
    private String teacher;
    private String classroom;
    private String day;
    private int period;
    private String startTime;
    private String endTime;
    /** "school" (default) or "after_school" */
    private String scheduleType;

    // Constructor for new lessons (without ID)
    public Lesson(String subject, String teacher, String classroom, String day, int period, String startTime, String endTime) {
        // Used before persistence, when Firestore document ID is not yet known.
        this.subject = subject;
        this.teacher = teacher;
        this.classroom = classroom;
        this.day = day;
        this.period = period;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Constructor for existing lessons (with ID)
    public Lesson(String id, String subject, String teacher, String classroom, String day, int period, String startTime, String endTime) {
        // Used when loading/editing existing entries where identity must be preserved.
        this.id = id;
        this.subject = subject;
        this.teacher = teacher;
        this.classroom = classroom;
        this.day = day;
        this.period = period;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Empty constructor for Firestore
    public Lesson() {
        // Required by Firestore object mapper.
    }

    public String getScheduleType() {
        // Defaulting to "school" keeps legacy documents compatible with newer dual-schedule UI.
        return scheduleType != null ? scheduleType : "school";
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getTeacher() {
        return teacher;
    }

    public String getClassroom() {
        return classroom;
    }

    public String getDay() {
        return day;
    }

    public int getPeriod() {
        return period;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public void setClassroom(String classroom) {
        this.classroom = classroom;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
