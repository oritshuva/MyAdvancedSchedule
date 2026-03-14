package com.example.myadvancedschedule;

import java.io.Serializable;

public class Lesson implements Serializable {
    private String id;
    private String subject;
    private String teacher;
    private String classroom;
    private String day;
    private int period;
    private String startTime;
    private String endTime;

    // Constructor for new lessons (without ID)
    public Lesson(String subject, String teacher, String classroom, String day, int period, String startTime, String endTime) {
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
