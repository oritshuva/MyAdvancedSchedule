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
    private LessonCardAdapter adapter;
    private FirestoreHelper firestoreHelper;

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

        firestoreHelper = new FirestoreHelper();
        adapter = new LessonCardAdapter();
        recyclerLessons.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerLessons.setAdapter(adapter);
        configureHeader();
        loadLessons();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLessons();
    }

    private void configureHeader() {
        String today = ScheduleFragmentHelper.getTodayDayName();
        if (textDayTitle != null) {
            String title = getString(R.string.today_schedule_title, today);
            textDayTitle.setText(title);
        }
        if (textDateSubtitle != null) {
            String dateStr = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
                    .format(new Date());
            textDateSubtitle.setText(dateStr);
        }
    }

    private void loadLessons() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerLessons.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        String today = ScheduleFragmentHelper.getTodayDayName();
        firestoreHelper.getLessonsForToday("school", today, new FirestoreHelper.OnLessonsLoadedListener() {
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
