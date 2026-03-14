package com.example.myadvancedschedule;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FillLessonsFragment extends Fragment {

    private static final String ARG_DAY = "day";
    private static final String ARG_LESSON_COUNT = "lesson_count";

    private String dayName;
    private int lessonCount;
    private LessonEditAdapter adapter;
    private List<Lesson> lessons;

    public static FillLessonsFragment newInstance(String day, int lessonCount) {
        FillLessonsFragment fragment = new FillLessonsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DAY, day);
        args.putInt(ARG_LESSON_COUNT, lessonCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            dayName = getArguments().getString(ARG_DAY);
            lessonCount = getArguments().getInt(ARG_LESSON_COUNT);
        }

        // Initialize lessons with placeholder times (valid Lesson constructor)
        lessons = new ArrayList<>();
        for (int i = 1; i <= lessonCount; i++) {
            lessons.add(new Lesson("", "", "", dayName, i, "08:00", "08:45"));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fill_lessons, container, false);

        TextView tvDayName = view.findViewById(R.id.tvDayName);
        tvDayName.setText(dayName);

        RecyclerView rvLessons = view.findViewById(R.id.rvLessons);
        rvLessons.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new LessonEditAdapter(getContext(), lessons);
        rvLessons.setAdapter(adapter);

        return view;
    }

    public List<Lesson> getLessons() {
        return adapter.getLessons();
    }
}
