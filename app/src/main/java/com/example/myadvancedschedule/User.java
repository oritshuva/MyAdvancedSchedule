package com.example.myadvancedschedule;

// Firestore user profile model used for account metadata beyond auth credentials.
// This keeps app-specific identity fields separate from FirebaseAuth core user state.

public class User {
    private String id;
    private String name;
    private String email;
    private long createdAt;

    public User() {
        // Firestore object mapper requires a public no-arg constructor.
    }

    public User(String name, String email) {
        // Capture creation timestamp once so profile ordering/auditing is stable.
        this.name = name;
        this.email = email;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
