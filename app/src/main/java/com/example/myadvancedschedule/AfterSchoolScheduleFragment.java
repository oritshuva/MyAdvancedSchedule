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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AfterSchoolScheduleFragment extends Fragment {

    private RecyclerView recyclerLessons;
    private LinearLayout emptyView;
    private ProgressBar progressBar;
    private TextView textDayTitle;
    private TextView textDateSubtitle;
    private TextView textInfoMessage;
    private FloatingActionButton fabAddEvent;
    private TabLayout tabDays;
    private LessonCardAdapter adapter;
    private FirestoreHelper firestoreHelper;
    private String currentDayName;

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
        textInfoMessage = view.findViewById(R.id.textInfoMessage);
        fabAddEvent = view.findViewById(R.id.fabAddEvent);
        tabDays = view.findViewById(R.id.tabDays);

        firestoreHelper = new FirestoreHelper();
        adapter = new LessonCardAdapter();
        adapter.setAfterSchoolEventActionListener(new LessonCardAdapter.OnAfterSchoolEventActionListener() {
            @Override
            public void onDelete(Lesson lesson) {
                firestoreHelper.deleteLesson(lesson.getId(), new FirestoreHelper.OnOperationCompleteListener() {
                    @Override
                    public void onSuccess() {
                        // Reload day smoothly without full-screen white flash.
                        loadLessonsForCurrentDay();
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onShare(Lesson lesson) {
                String title = lesson.getSubject() != null ? lesson.getSubject() : getString(R.string.after_school_title);
                String time = "";
                if (lesson.getStartTime() != null || lesson.getEndTime() != null) {
                    String start = lesson.getStartTime() != null ? lesson.getStartTime() : "";
                    String end = lesson.getEndTime() != null ? lesson.getEndTime() : "";
                    time = start.isEmpty() && end.isEmpty() ? "" : start + " – " + end;
                }
                String location = lesson.getClassroom() != null ? lesson.getClassroom() : "";
                StringBuilder shareText = new StringBuilder();
                shareText.append(title);
                if (!time.isEmpty()) {
                    shareText.append(" (").append(time).append(")");
                }
                if (!location.isEmpty()) {
                    shareText.append(" @ ").append(location);
                }

                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(android.content.Intent.EXTRA_TEXT, shareText.toString());
                startActivity(android.content.Intent.createChooser(intent, null));
            }

            @Override
            public void onReminder(Lesson lesson) {
                if (!isAdded()) return;
                ReminderDialogFragment dialog = ReminderDialogFragment.newInstance();
                dialog.setOnReminderConfirmedListener((triggerAt, noteText) -> {
                    if (!isAdded()) return;
                    Event event = new Event();
                    event.setId(lesson.getId());
                    event.setTitle(lesson.getSubject());
                    event.setDay(lesson.getDay());
                    event.setType("after_school");
                    event.setNote(noteText);
                    ReminderUtils.scheduleEventReminder(requireContext(), event, triggerAt, noteText);
                    Toast.makeText(requireContext(), R.string.reminder_scheduled, Toast.LENGTH_SHORT).show();
                });
                dialog.show(getParentFragmentManager(), "AfterSchoolReminderDialog");
            }

            @Override
            public void onDoneChanged(Lesson lesson, boolean done) {
                if (!done) {
                    return;
                }
                // Marking an after-school event as done removes it from the list and Firestore.
                firestoreHelper.deleteLesson(lesson.getId(), new FirestoreHelper.OnOperationCompleteListener() {
                    @Override
                    public void onSuccess() {
                        adapter.removeLesson(lesson);
                        boolean empty = adapter.getItemCount() == 0;
                        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
                        recyclerLessons.setVisibility(empty ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        adapter.setShareEnabled(true, lesson -> {
            String title = lesson.getSubject() != null ? lesson.getSubject() : getString(R.string.after_school_title);
            String time = "";
            if (lesson.getStartTime() != null || lesson.getEndTime() != null) {
                String start = lesson.getStartTime() != null ? lesson.getStartTime() : "";
                String end = lesson.getEndTime() != null ? lesson.getEndTime() : "";
                time = start.isEmpty() && end.isEmpty() ? "" : start + " – " + end;
            }
            String location = lesson.getClassroom() != null ? lesson.getClassroom() : "";
            StringBuilder shareText = new StringBuilder();
            shareText.append(title);
            if (!time.isEmpty()) {
                shareText.append(" (").append(time).append(")");
            }
            if (!location.isEmpty()) {
                shareText.append(" @ ").append(location);
            }

            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(android.content.Intent.EXTRA_TEXT, shareText.toString());
            startActivity(android.content.Intent.createChooser(intent, null));
        });
        recyclerLessons.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerLessons.setAdapter(adapter);
        setupDayTabsAndHeader();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLessonsForCurrentDay();
    }

    private void setupDayTabsAndHeader() {
        if (tabDays != null) {
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
            if (selected != null) selected.select();

            tabDays.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    Object tag = tab.getTag();
                    if (tag instanceof String) {
                        currentDayName = (String) tag;
                        configureHeaderForDay(currentDayName);
                        loadLessonsForCurrentDay();
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    onTabSelected(tab);
                }
            });
        } else {
            currentDayName = ScheduleFragmentHelper.getTodayDayName();
        }
        configureHeaderForDay(currentDayName);
        loadLessonsForCurrentDay();
        configureFab();
    }

    private void configureHeaderForDay(String dayName) {
        String day = dayName != null ? dayName : ScheduleFragmentHelper.getTodayDayName();
        if (textDayTitle != null) {
            String title = getString(R.string.today_after_school_title, day);
            textDayTitle.setText(title);
        }
        if (textDateSubtitle != null) {
            String dateStr = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
                    .format(new Date());
            textDateSubtitle.setText(dateStr);
        }
        if (textInfoMessage != null) {
            textInfoMessage.setVisibility(View.VISIBLE);
            textInfoMessage.setText(R.string.after_school_info_message);
        }
    }

    private void configureFab() {
        if (fabAddEvent != null) {
            fabAddEvent.setVisibility(View.VISIBLE);
            fabAddEvent.setOnClickListener(v -> {
                AddAfterSchoolEventDialogFragment dialog =
                        AddAfterSchoolEventDialogFragment.newInstance();
                dialog.setOnEventSavedListener(this::loadLessonsForCurrentDay);
                dialog.show(getParentFragmentManager(), "AddAfterSchoolEvent");
            });
        }
    }

    private void loadLessonsForCurrentDay() {
        String day = currentDayName != null ? currentDayName : ScheduleFragmentHelper.getTodayDayName();
        progressBar.setVisibility(View.VISIBLE);
        recyclerLessons.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        firestoreHelper.getLessonsForToday("after_school", day, new FirestoreHelper.OnLessonsLoadedListener() {
            @Override
            public void onLessonsLoaded(List<Lesson> lessons) {
                progressBar.setVisibility(View.GONE);
                adapter.setLessons(lessons);
                boolean empty = lessons == null || lessons.isEmpty();
                emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
                recyclerLessons.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                emptyView.setVisibility(View.VISIBLE);
                recyclerLessons.setVisibility(View.GONE);
            }
        });
    }
}
