package com.example.myadvancedschedule;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.CollectionReference;

// Lightweight Firebase access facade used by legacy screens.
// Centralizing instance retrieval reduces boilerplate and keeps collection paths consistent.

public class FirebaseHelper {
    private static FirebaseHelper instance;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private FirebaseHelper() {
        // Initialize once to reuse shared Firebase clients across screens.
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseHelper getInstance() {
        // Lazy singleton avoids repeated initialization and keeps access thread-safe.
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public FirebaseUser getCurrentUser() {
        // Direct pass-through used by callers that need user metadata.
        return auth.getCurrentUser();
    }

    public String getCurrentUserId() {
        // Null-safe UID resolver prevents duplicated null checks in UI classes.
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public CollectionReference getEventsCollection() {
        return db.collection("events");
    }

    public CollectionReference getUsersCollection() {
        return db.collection("users");
    }

    public Query getUserEvents(String day, String type) {
        // Server-side filtering keeps payload small and avoids loading unrelated events.
        return getEventsCollection()
                .whereEqualTo("userId", getCurrentUserId())
                .whereEqualTo("day", day)
                .whereEqualTo("type", type)
                .orderBy("startTime");
    }
}
