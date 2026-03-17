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
        tabDays = view.findViewById(R.id.tabDays);

        firestoreHelper = new FirestoreHelper();
        adapter = new LessonCardAdapter();
        recyclerLessons.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerLessons.setAdapter(adapter);
        setupDayTabs();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLessonsForCurrentDay();
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
        progressBar.setVisibility(View.VISIBLE);
        recyclerLessons.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        firestoreHelper.getLessonsForToday("school", day, new FirestoreHelper.OnLessonsLoadedListener() {
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
