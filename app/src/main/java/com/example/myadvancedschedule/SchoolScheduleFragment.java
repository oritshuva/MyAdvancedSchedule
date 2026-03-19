package com.example.myadvancedschedule;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class SchoolScheduleFragment extends Fragment {

    private RecyclerView recyclerLessons;
    private LinearLayout emptyView;
    private ProgressBar progressBar;
    private TextView textDayTitle;
    private TextView textDateSubtitle;
    private TabLayout tabDays;
    private LessonCardAdapter adapter;
    private FirestoreHelper firestoreHelper;
    private String currentDayName;

    private MaterialButton btnEditSchedule;
    private MaterialButton btnSaveChanges;
    private TextView textEditModeIndicator;
    private boolean editMode = false;
    private boolean isLoadingLessons = false;
    private boolean isSavingEdits = false;
    private final Map<String, Lesson> originalEditableLessonsById = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);
        recyclerLessons = view.findViewById(R.id.recyclerLessons);
        emptyView = view.findViewById(R.id.emptyView);
        progressBar = view.findViewById(R.id.progressBar);
        textDayTitle = view.findViewById(R.id.textDayTitle);
        textDateSubtitle = view.findViewById(R.id.textDateSubtitle);
        tabDays = view.findViewById(R.id.tabDays);
        btnEditSchedule = view.findViewById(R.id.btnEditSchedule);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);
        textEditModeIndicator = view.findViewById(R.id.textEditModeIndicator);

        firestoreHelper = new FirestoreHelper();
        adapter = new LessonCardAdapter();
        adapter.setOnLessonClickListener(lesson -> {
            if (!isAdded()) return;
            if (editMode) return;
            // Open edit dialog pre-filled with this lesson.
            AddLessonDialogFragment dialog = AddLessonDialogFragment.newInstance(lesson);
            dialog.setOnLessonSavedListener(this::loadLessonsForCurrentDay);
            dialog.show(getParentFragmentManager(), "EditLesson");
        });
        recyclerLessons.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerLessons.setAdapter(adapter);
        setupDayTabs();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Ensure UI is consistent when returning to foreground.
        exitEditModeIfNeeded();
        loadLessonsForCurrentDay();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Prevent stale RecyclerView listeners or watchers when leaving.
        exitEditModeIfNeeded();
    }

    private void setupDayTabs() {
        if (tabDays == null) return;
        tabDays.removeAllTabs();
        String[] days = getResources().getStringArray(R.array.days_array_english);
        String today = ScheduleFragmentHelper.getTodayDayName();
        int selectedIndex = 0;
        for (int i = 0; i < days.length; i++) {
            String dayName = days[i];
            TabLayout.Tab tab = tabDays.newTab().setText(dayName.substring(0, 3));
            tab.setTag(dayName);
            tabDays.addTab(tab, false);
            if (dayName.equals(today)) {
                selectedIndex = i;
            }
        }
        currentDayName = days[selectedIndex];
        TabLayout.Tab selected = tabDays.getTabAt(selectedIndex);
        if (selected != null) {
            selected.select();
        }
        configureHeaderForDay(currentDayName);
        loadLessonsForCurrentDay();
        updateEditControls();

        tabDays.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Object tag = tab.getTag();
                if (tag instanceof String) {
                    currentDayName = (String) tag;
                    configureHeaderForDay(currentDayName);
                    loadLessonsForCurrentDay();
                    updateEditControls();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                onTabSelected(tab);
            }
        });
    }

    private void updateEditControls() {
        // Don't gate on `isAdded()` here: the edit/save buttons must be wired as soon
        // as the view hierarchy exists, otherwise UI tests (and early user taps)
        // may click before listeners are attached.
        if (tabDays == null || btnEditSchedule == null || btnSaveChanges == null) return;
        boolean canEditDay = currentDayName != null && !currentDayName.trim().isEmpty();
        boolean canEditNow = canEditDay && !isSavingEdits;

        if (editMode) {
            if (btnEditSchedule != null) btnEditSchedule.setVisibility(View.GONE);
            if (btnSaveChanges != null) btnSaveChanges.setVisibility(View.VISIBLE);
            if (textEditModeIndicator != null) textEditModeIndicator.setVisibility(View.VISIBLE);
            if (tabDays != null) tabDays.setEnabled(false);
        } else {
            if (textEditModeIndicator != null) textEditModeIndicator.setVisibility(View.GONE);
            if (tabDays != null) tabDays.setEnabled(!isLoadingLessons && !isSavingEdits);

            if (btnEditSchedule != null) {
                btnEditSchedule.setVisibility(View.VISIBLE);
                btnEditSchedule.setEnabled(canEditNow);
            }
            if (btnSaveChanges != null) btnSaveChanges.setVisibility(View.GONE);
        }

        // Wire buttons once we know the views exist.
        if (btnEditSchedule != null && btnSaveChanges != null) {
            btnEditSchedule.setOnClickListener(v -> enterEditModeIfAllowed());
            btnSaveChanges.setOnClickListener(v -> saveEdits());
        }
    }

    private void enterEditModeIfAllowed() {
        if (!isAdded()) return;
        if (isSavingEdits) {
            Toast.makeText(requireContext(), "Saving changes… Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentDayName == null || currentDayName.trim().isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.edit_schedule_only_today), Toast.LENGTH_SHORT).show();
            return;
        }
        if (editMode) return;

        editMode = true;
        originalEditableLessonsById.clear();
        for (Lesson l : adapter.getEditableSchoolLessons()) {
            originalEditableLessonsById.put(l.getId(), cloneForEditSnapshot(l));
        }

        adapter.setEditMode(true);
        updateEditControls();
    }

    private void exitEditModeIfNeeded() {
        if (!editMode) return;
        editMode = false;
        originalEditableLessonsById.clear();
        if (adapter != null) adapter.setEditMode(false);
        updateEditControls();
    }

    private Lesson cloneForEditSnapshot(Lesson original) {
        if (original == null) return null;
        Lesson copy = new Lesson(
                original.getId(),
                original.getSubject(),
                original.getTeacher(),
                original.getClassroom(),
                original.getDay(),
                original.getPeriod(),
                original.getStartTime(),
                original.getEndTime()
        );
        copy.setScheduleType(original.getScheduleType());
        return copy;
    }

    private void saveEdits() {
        if (!isAdded()) return;
        if (!editMode) return;
        isSavingEdits = true;

        String userId = firestoreHelper != null ? firestoreHelper.getCurrentUserId() : null;
        if (userId == null) {
            isSavingEdits = false;
            Toast.makeText(requireContext(), getString(R.string.error_saving_lesson), Toast.LENGTH_SHORT).show();
            return;
        }

        List<Lesson> editableLessons = adapter.getEditableSchoolLessons();
        if (editableLessons.isEmpty()) {
            isSavingEdits = false;
            Toast.makeText(requireContext(), getString(R.string.save_changes), Toast.LENGTH_SHORT).show();
            return;
        }

        List<Lesson> lessonsToUpdate = new java.util.ArrayList<>();
        for (Lesson l : editableLessons) {
            if (l == null || l.getId() == null) continue;

            String subjectNow = l.getSubject() != null ? l.getSubject().trim() : "";
            String teacherNow = l.getTeacher() != null ? l.getTeacher().trim() : "";
            String classroomNow = l.getClassroom() != null ? l.getClassroom().trim() : "";

            if (subjectNow.isEmpty()) {
                // If validation fails, reload to restore consistent UI/state.
                Toast.makeText(requireContext(), getString(R.string.error_empty_subject), Toast.LENGTH_SHORT).show();
                isSavingEdits = false;
                loadLessonsForCurrentDay();
                return;
            }
            if (teacherNow.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.error_empty_teacher), Toast.LENGTH_SHORT).show();
                isSavingEdits = false;
                loadLessonsForCurrentDay();
                return;
            }
            if (classroomNow.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.error_empty_classroom), Toast.LENGTH_SHORT).show();
                isSavingEdits = false;
                loadLessonsForCurrentDay();
                return;
            }

            Lesson original = originalEditableLessonsById.get(l.getId());
            boolean changed;
            if (original == null) {
                changed = true;
            } else {
                String subjectWas = original.getSubject() != null ? original.getSubject().trim() : "";
                String teacherWas = original.getTeacher() != null ? original.getTeacher().trim() : "";
                String classroomWas = original.getClassroom() != null ? original.getClassroom().trim() : "";
                changed = !subjectNow.equals(subjectWas) || !teacherNow.equals(teacherWas) || !classroomNow.equals(classroomWas);
            }

            if (changed) {
                l.setSubject(subjectNow);
                l.setTeacher(teacherNow);
                l.setClassroom(classroomNow);
                lessonsToUpdate.add(l);
            }
        }

        if (lessonsToUpdate.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.save_changes), Toast.LENGTH_SHORT).show();
            exitEditModeIfNeeded();
            isSavingEdits = false;
            return;
        }

        // Switch to read-only immediately (UI update right away), while we persist changes in background.
        exitEditModeIfNeeded();

        progressBar.setVisibility(View.VISIBLE);
        if (recyclerLessons != null) recyclerLessons.setEnabled(false);
        if (emptyView != null) emptyView.setVisibility(View.GONE);

        java.util.concurrent.atomic.AtomicInteger done = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicBoolean anyFailure = new java.util.concurrent.atomic.AtomicBoolean(false);
        final String dayAtStart = currentDayName;

        for (Lesson l : lessonsToUpdate) {
            firestoreHelper.updateLesson(userId, l, new FirestoreHelper.OnOperationCompleteListener() {
                @Override
                public void onSuccess() {
                    if (!isAdded()) return;
                    int completed = done.incrementAndGet();
                    if (completed >= lessonsToUpdate.size() && !anyFailure.get()) {
                        progressBar.setVisibility(View.GONE);
                        if (recyclerLessons != null) recyclerLessons.setEnabled(true);
                        isSavingEdits = false;
                        updateEditControls();
                        if (dayAtStart == null || dayAtStart.equals(currentDayName)) {
                            if (emptyView != null) emptyView.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), getString(R.string.lesson_updated), Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(String error) {
                    if (!isAdded()) return;
                    if (anyFailure.compareAndSet(false, true)) {
                        progressBar.setVisibility(View.GONE);
                        if (recyclerLessons != null) recyclerLessons.setEnabled(true);
                        isSavingEdits = false;
                        updateEditControls();
                        Toast.makeText(requireContext(), getString(R.string.error_saving_lesson) + ": " + error, Toast.LENGTH_LONG).show();
                        loadLessonsForCurrentDay();
                    }
                }
            });
        }
    }

    private void configureHeaderForDay(String dayName) {
        String day = dayName != null ? dayName : ScheduleFragmentHelper.getTodayDayName();
        if (textDayTitle != null) {
            String title = getString(R.string.today_schedule_title, day);
            textDayTitle.setText(title);
        }
        if (textDateSubtitle != null) {
            String dateStr = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
                    .format(new Date());
            textDateSubtitle.setText(dateStr);
        }
    }

    private void loadLessonsForCurrentDay() {
        String day = currentDayName != null ? currentDayName : ScheduleFragmentHelper.getTodayDayName();
        isLoadingLessons = true;
        updateEditControls();
        progressBar.setVisibility(View.VISIBLE);
        recyclerLessons.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        firestoreHelper.getLessonsForToday("school", day, new FirestoreHelper.OnLessonsLoadedListener() {
            @Override
            public void onLessonsLoaded(List<Lesson> lessons) {
                if (!isAdded()) return;
                isLoadingLessons = false;
                progressBar.setVisibility(View.GONE);
                adapter.setLessons(lessons);
                boolean empty = lessons == null || lessons.isEmpty();
                emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
                recyclerLessons.setVisibility(empty ? View.GONE : View.VISIBLE);
                updateEditControls();
            }
            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                isLoadingLessons = false;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                emptyView.setVisibility(View.VISIBLE);
                recyclerLessons.setVisibility(View.GONE);
                updateEditControls();
            }
        });
    }
}
