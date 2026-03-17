package com.example.myadvancedschedule;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreHelper {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private static final String COLLECTION_LESSONS = "lessons";
    private static final String COLLECTION_TASKS = "tasks";
    private static final String COLLECTION_SUBJECTS = "subjects";

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    // קבלת UID של המשתמש המחובר
    private String getUserId() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    // ממשק להאזנה לטעינת שיעורים
    public interface OnLessonsLoadedListener {
        void onLessonsLoaded(List<Lesson> lessons);
        void onError(String error);
    }

    // ממשק להאזנה להשלמת פעולה
    public interface OnOperationCompleteListener {
        void onSuccess();
        void onFailure(String error);
    }

    // 🔥 הוספת שיעור חדש (תוקן)
    public void addLesson(String userId, Lesson lesson, OnOperationCompleteListener listener) {
        if (userId == null) {
            listener.onFailure("User not logged in");
            return;
        }

        // Build a deterministic document ID to prevent exact-duplicate lessons for the same slot.
        // Combination: userId + scheduleType + day + period + subject hash.
        String scheduleType = lesson.getScheduleType() != null ? lesson.getScheduleType() : "school";
        String day = lesson.getDay() != null ? lesson.getDay() : "";
        int period = lesson.getPeriod();
        String subjectKey = lesson.getSubject() != null ? lesson.getSubject().trim().toLowerCase() : "";
        String rawKey = scheduleType + "|" + day + "|" + period + "|" + subjectKey;
        String docId = userId + "_" + Integer.toHexString(rawKey.hashCode());

        Map<String, Object> lessonData = new HashMap<>();
        lessonData.put("userId", userId);
        lessonData.put("subject", lesson.getSubject());  // 🔥 תוקן
        lessonData.put("teacher", lesson.getTeacher());
        lessonData.put("classroom", lesson.getClassroom());  // 🔥 תוקן
        lessonData.put("day", lesson.getDay());
        lessonData.put("period", lesson.getPeriod());  // 🔥 הוסף
        lessonData.put("startTime", lesson.getStartTime());
        lessonData.put("endTime", lesson.getEndTime());
        lessonData.put("scheduleType", lesson.getScheduleType() != null ? lesson.getScheduleType() : "school");

        db.collection(COLLECTION_LESSONS)
                .document(docId)
                .set(lessonData, SetOptions.merge())
                .addOnSuccessListener(documentReference -> {
                    lesson.setId(docId);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // 🔥 עדכון שיעור קיים (תוקן)
    public void updateLesson(String userId, Lesson lesson, OnOperationCompleteListener listener) {
        if (userId == null) {
            listener.onFailure("User not logged in");
            return;
        }

        Map<String, Object> lessonData = new HashMap<>();
        lessonData.put("userId", userId);
        lessonData.put("subject", lesson.getSubject());  // 🔥 תוקן
        lessonData.put("teacher", lesson.getTeacher());
        lessonData.put("classroom", lesson.getClassroom());  // 🔥 תוקן
        lessonData.put("day", lesson.getDay());
        lessonData.put("period", lesson.getPeriod());  // 🔥 הוסף
        lessonData.put("startTime", lesson.getStartTime());
        lessonData.put("endTime", lesson.getEndTime());
        lessonData.put("scheduleType", lesson.getScheduleType() != null ? lesson.getScheduleType() : "school");

        db.collection(COLLECTION_LESSONS)
                .document(lesson.getId())
                .set(lessonData)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // מחיקת שיעור
    public void deleteLesson(String lessonId, OnOperationCompleteListener listener) {
        db.collection(COLLECTION_LESSONS)
                .document(lessonId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // 🔥 קבלת כל השיעורים של המשתמש (תוקן)
    public void getAllLessons(OnLessonsLoadedListener listener) {
        String userId = getUserId();
        if (userId == null) {
            listener.onError("User not logged in");
            return;
        }

        db.collection(COLLECTION_LESSONS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Lesson> lessons = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            String id = document.getId();
                            String subject = document.getString("subject");
                            String teacher = document.getString("teacher");
                            String classroom = document.getString("classroom");
                            String day = document.getString("day");
                            Long periodLong = document.getLong("period");
                            int period = periodLong != null ? periodLong.intValue() : 0;
                            String startTime = document.getString("startTime");
                            String endTime = document.getString("endTime");
                            String scheduleType = document.getString("scheduleType");

                            // Skip obviously malformed documents to avoid crashes.
                            if (day == null || day.trim().isEmpty()) continue;

                            Lesson lesson = new Lesson(id, subject, teacher, classroom, day, period, startTime, endTime);
                            if (scheduleType != null) lesson.setScheduleType(scheduleType);
                            lessons.add(lesson);
                        } catch (Exception ignored) {
                            // Ignore a single bad document and continue loading the rest.
                        }
                    }
                    listener.onLessonsLoaded(lessons);
                })
                .addOnFailureListener(e -> listener.onError(e != null ? e.getMessage() : "Failed to load lessons"));
    }

    /** Load lessons for a specific day filtered by scheduleType ("school" or "after_school"). */
    public void getLessonsForToday(String scheduleType, String todayDayName, OnLessonsLoadedListener listener) {
        getAllLessons(new OnLessonsLoadedListener() {
            @Override
            public void onLessonsLoaded(List<Lesson> lessons) {
                List<Lesson> filtered = new ArrayList<>();
                for (Lesson l : lessons) {
                    String type = l.getScheduleType() != null ? l.getScheduleType() : "school";
                    if (type.equals(scheduleType) && todayDayName != null && todayDayName.equals(l.getDay())) {
                        filtered.add(l);
                    }
                }
                // Ensure deterministic ordering by time (HH:mm); fall back to period if needed.
                java.util.Collections.sort(filtered, (a, b) -> {
                    String aTime = a.getStartTime() != null ? a.getStartTime() : "";
                    String bTime = b.getStartTime() != null ? b.getStartTime() : "";
                    if (!aTime.isEmpty() && !bTime.isEmpty()) {
                        return aTime.compareTo(bTime);
                    }
                    return Integer.compare(a.getPeriod(), b.getPeriod());
                });
                listener.onLessonsLoaded(filtered);
            }
            @Override
            public void onError(String error) {
                listener.onError(error);
            }
        });
    }

    // ---------- Tasks ----------
    public interface OnTasksLoadedListener {
        void onTasksLoaded(List<Task> tasks);
        void onError(String error);
    }

    public void addTask(String userId, Task task, OnOperationCompleteListener listener) {
        if (userId == null) {
            listener.onFailure("User not logged in");
            return;
        }
        // Deterministic document ID to prevent duplicate tasks with the same title and due time.
        String titleKey = task.getTitle() != null ? task.getTitle().trim().toLowerCase() : "";
        String dueKey = task.getDueTime() != null ? task.getDueTime().trim() : "";
        String rawKey = titleKey + "|" + dueKey;
        String docId = userId + "_" + Integer.toHexString(rawKey.hashCode());

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("title", task.getTitle());
        data.put("dueTime", task.getDueTime() != null ? task.getDueTime() : "");
        data.put("completed", task.isCompleted());
        if (task.getReminderTimeMillis() != null) {
            data.put("reminderTimeMillis", task.getReminderTimeMillis());
        }
        if (task.getReminderDetail() != null && !task.getReminderDetail().trim().isEmpty()) {
            data.put("reminderDetail", task.getReminderDetail());
        }
        db.collection(COLLECTION_TASKS)
                .document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(ref -> {
                    task.setId(docId);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void getTasks(OnTasksLoadedListener listener) {
        String userId = getUserId();
        if (userId == null) {
            listener.onError("User not logged in");
            return;
        }
        db.collection(COLLECTION_TASKS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        boolean completed = Boolean.TRUE.equals(doc.getBoolean("completed"));
                        // Hide completed tasks from the main list; they are effectively "done".
                        if (completed) continue;

                        Task t = new Task(
                                doc.getId(),
                                doc.getString("title"),
                                doc.getString("dueTime"),
                                completed
                        );
                        Long rt = doc.getLong("reminderTimeMillis");
                        String detail = doc.getString("reminderDetail");
                        if (rt != null && rt > 0) {
                            t.setReminderTimeMillis(rt);
                        }
                        if (detail != null && !detail.trim().isEmpty()) {
                            t.setReminderDetail(detail.trim());
                        }
                        tasks.add(t);
                    }
                    // Sort tasks by dueTime text (HH:mm) so UI order is stable.
                    java.util.Collections.sort(tasks, (a, b) -> {
                        String at = a.getDueTime() != null ? a.getDueTime() : "";
                        String bt = b.getDueTime() != null ? b.getDueTime() : "";
                        return at.compareTo(bt);
                    });
                    listener.onTasksLoaded(tasks);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void updateTask(String userId, Task task, OnOperationCompleteListener listener) {
        if (userId == null) {
            listener.onFailure("User not logged in");
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("title", task.getTitle());
        data.put("dueTime", task.getDueTime() != null ? task.getDueTime() : "");
        data.put("completed", task.isCompleted());
        if (task.getReminderTimeMillis() != null) {
            data.put("reminderTimeMillis", task.getReminderTimeMillis());
        } else {
            data.remove("reminderTimeMillis");
        }
        if (task.getReminderDetail() != null && !task.getReminderDetail().trim().isEmpty()) {
            data.put("reminderDetail", task.getReminderDetail());
        } else {
            data.remove("reminderDetail");
        }
        db.collection(COLLECTION_TASKS)
                .document(task.getId())
                .set(data)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void deleteTask(String taskId, OnOperationCompleteListener listener) {
        db.collection(COLLECTION_TASKS)
                .document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // ---------- Subjects (for dropdown reuse) ----------
    public interface OnSubjectsLoadedListener {
        void onSubjectsLoaded(List<String> subjectNames);
        void onError(String error);
    }

    /** Load subject names for the current user (subjects collection + unique from lessons). */
    public void getSubjects(OnSubjectsLoadedListener listener) {
        String userId = getUserId();
        if (userId == null) {
            listener.onError("User not logged in");
            return;
        }
        List<String> names = new ArrayList<>();
        db.collection(COLLECTION_SUBJECTS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        String name = doc.getString("name");
                        if (name != null && !name.trim().isEmpty() && !names.contains(name.trim()))
                            names.add(name.trim());
                    }
                    // Also add unique subjects from existing lessons
                    db.collection(COLLECTION_LESSONS)
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(lessonSnap -> {
                                for (QueryDocumentSnapshot doc : lessonSnap) {
                                    String s = doc.getString("subject");
                                    if (s != null && !s.trim().isEmpty() && !names.contains(s.trim()))
                                        names.add(s.trim());
                                }
                                java.util.Collections.sort(names);
                                listener.onSubjectsLoaded(names);
                            })
                            .addOnFailureListener(e -> listener.onSubjectsLoaded(names));
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /** Add a new subject so it appears in the dropdown next time. */
    public void addSubject(String userId, String subjectName, OnOperationCompleteListener listener) {
        if (userId == null) {
            listener.onFailure("User not logged in");
            return;
        }
        String name = subjectName != null ? subjectName.trim() : "";
        if (name.isEmpty()) {
            listener.onFailure("Subject name is empty");
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("name", name);
        db.collection(COLLECTION_SUBJECTS)
                .add(data)
                .addOnSuccessListener(ref -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }
}