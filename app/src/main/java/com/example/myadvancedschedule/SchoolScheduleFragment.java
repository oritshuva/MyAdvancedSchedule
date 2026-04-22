package com.example.myadvancedschedule;

// School schedule tab: renders day-based school lessons, loads data from Firestore,
// and lets users open lesson editing from the daily list.

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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SchoolScheduleFragment extends Fragment {

    private RecyclerView recyclerLessons;
    private LinearLayout emptyView;
    private ProgressBar progressBar;
    private TextView textDayTitle;
    private TextView textDateSubtitle;
    private TabLayout tabDays;
    private MaterialButton btnEditHelp;
    private LessonCardAdapter adapter;
    private FirestoreHelper firestoreHelper;
    private String currentDayName;
    private boolean isLoadingLessons = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the schedule UI and initialize adapter, tabs, and helper actions.
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);
        recyclerLessons = view.findViewById(R.id.recyclerLessons);
        emptyView = view.findViewById(R.id.emptyView);
        progressBar = view.findViewById(R.id.progressBar);
        textDayTitle = view.findViewById(R.id.textDayTitle);
        textDateSubtitle = view.findViewById(R.id.textDateSubtitle);
        tabDays = view.findViewById(R.id.tabDays);
        btnEditHelp = view.findViewById(R.id.btnEditHelp);

        firestoreHelper = new FirestoreHelper();
        adapter = new LessonCardAdapter();
        adapter.setOnLessonClickListener(lesson -> {
            if (!isAdded()) return;
            AddLessonDialogFragment dialog = AddLessonDialogFragment.newInstance(lesson);
            dialog.setOnLessonSavedListener(this::loadLessonsForCurrentDay);
            dialog.show(getParentFragmentManager(), "EditLesson");
        });
        recyclerLessons.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerLessons.setAdapter(adapter);
        setupHelpButton();
        setupDayTabs();
        return view;
    }

    @Override
    public void onResume() {
        // Reload data on return so edits from other screens are reflected immediately.
        super.onResume();
        loadLessonsForCurrentDay();
    }

    private void setupDayTabs() {
        // Build Monday-Friday tabs and preselect today's day for quick access.
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
    }

    private void setupHelpButton() {
        // Show quick usage guidance for editing lessons from this screen.
        if (btnEditHelp == null) return;
        btnEditHelp.setVisibility(View.VISIBLE);
        btnEditHelp.setOnClickListener(v -> {
            if (!isAdded()) return;
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.how_to_edit_title)
                    .setMessage(R.string.how_to_edit_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });
    }

    private void configureHeaderForDay(String dayName) {
        // Update header labels to match selected day and current date.
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
        // Disable day switching while loading, then restore UI using the fetched result.
        String day = currentDayName != null ? currentDayName : ScheduleFragmentHelper.getTodayDayName();
        isLoadingLessons = true;
        if (tabDays != null) {
            tabDays.setEnabled(false);
        }
        progressBar.setVisibility(View.VISIBLE);
        recyclerLessons.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        firestoreHelper.getLessonsForToday("school", day, new FirestoreHelper.OnLessonsLoadedListener() {
            @Override
            public void onLessonsLoaded(List<Lesson> lessons) {
                // Abort UI updates if this fragment is no longer attached to an activity.
                if (!isAdded()) return;
                isLoadingLessons = false;
                progressBar.setVisibility(View.GONE);
                adapter.setLessons(lessons);
                boolean empty = lessons == null || lessons.isEmpty();
                emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
                recyclerLessons.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (tabDays != null) {
                    tabDays.setEnabled(true);
                }
            }
            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                isLoadingLessons = false;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                emptyView.setVisibility(View.VISIBLE);
                recyclerLessons.setVisibility(View.GONE);
                if (tabDays != null) {
                    tabDays.setEnabled(true);
                }
            }
        });
    }
}
