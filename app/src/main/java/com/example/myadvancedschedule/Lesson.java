package com.example.myadvancedschedule;

public class Lesson {
    private String day;
    private int periodNumber;
    private String subject;
    private String startTime;
    private String endTime;

    public Lesson(String day, int periodNumber, String subject, String startTime, String endTime) {
        this.day = day;
        this.periodNumber = periodNumber;
        this.subject = subject;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters and Setters
    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public int getPeriodNumber() {
        return periodNumber;
    }

    public void setPeriodNumber(int periodNumber) {
        this.periodNumber = periodNumber;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
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
}
