package com.example.myadvancedschedule;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
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
                .add(lessonData)
                .addOnSuccessListener(documentReference -> {
                    lesson.setId(documentReference.getId());
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

                        Lesson lesson = new Lesson(id, subject, teacher, classroom, day, period, startTime, endTime);
                        if (scheduleType != null) lesson.setScheduleType(scheduleType);
                        lessons.add(lesson);
                    }
                    listener.onLessonsLoaded(lessons);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /** Load lessons for today filtered by scheduleType ("school" or "after_school"). */
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
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("title", task.getTitle());
        data.put("dueTime", task.getDueTime() != null ? task.getDueTime() : "");
        data.put("completed", task.isCompleted());
        db.collection(COLLECTION_TASKS)
                .add(data)
                .addOnSuccessListener(ref -> {
                    task.setId(ref.getId());
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
                        Task t = new Task(
                                doc.getId(),
                                doc.getString("title"),
                                doc.getString("dueTime"),
                                Boolean.TRUE.equals(doc.getBoolean("completed"))
                        );
                        tasks.add(t);
                    }
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
}