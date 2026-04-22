package com.example.myadvancedschedule;

// Per-day setup fragment that converts generated time slots into editable
// lesson rows, enabling structured schedule creation during onboarding.

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

/**
 * Step 2: One fragment per selected day. Shows a RecyclerView of lesson cards.
 * Pre-filling period times keeps user effort focused on academic details
 * (subject/teacher/classroom) rather than repetitive time entry.
 */
public class DayScheduleFragment extends Fragment {

    private static final String ARG_DAY = "day";
    private static final String ARG_TIME_SLOTS = "time_slots";

    private String dayName;
    private ArrayList<TimeSlot> timeSlots;
    private LessonSetupCardAdapter adapter;
    private List<Lesson> lessons;

    public static DayScheduleFragment newInstance(String dayName, ArrayList<TimeSlot> timeSlots) {
        // Use arguments bundle so Android can recreate this fragment with full state.
        DayScheduleFragment fragment = new DayScheduleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DAY, dayName);
        args.putSerializable(ARG_TIME_SLOTS, timeSlots);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        // Restore immutable day context once; UI layer consumes prepared lesson list.
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            dayName = getArguments().getString(ARG_DAY, "");
            @SuppressWarnings("unchecked")
            ArrayList<TimeSlot> slots = (ArrayList<TimeSlot>) getArguments().getSerializable(ARG_TIME_SLOTS);
            timeSlots = slots != null ? slots : new ArrayList<>();
        } else {
            dayName = "";
            timeSlots = new ArrayList<>();
        }
        buildLessonsFromSlots();
    }

    private void buildLessonsFromSlots() {
        // Convert each slot into a lesson scaffold so adapter edits mutate final output objects.
        lessons = new ArrayList<>();
        for (int i = 0; i < timeSlots.size(); i++) {
            TimeSlot slot = timeSlots.get(i);
            Lesson lesson = new Lesson(
                    "",
                    "",
                    "",
                    dayName,
                    i + 1,
                    slot.getStartTime(),
                    slot.getEndTime()
            );
            lessons.add(lesson);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Build day-specific editor list; each row corresponds to one generated period.
        View view = inflater.inflate(R.layout.fragment_day_schedule, container, false);
        TextView tvDayName = view.findViewById(R.id.tvDayName);
        tvDayName.setText(dayName);

        RecyclerView rvLessons = view.findViewById(R.id.rvLessons);
        rvLessons.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LessonSetupCardAdapter(lessons);
        rvLessons.setAdapter(adapter);
        return view;
    }

    /** Returns the list of lessons for this day (subject/teacher/classroom from user input). */
    public List<Lesson> getLessons() {
        // Prefer adapter state because it reflects the latest in-row user edits.
        return adapter != null ? adapter.getLessons() : new ArrayList<>(lessons);
    }

    public String getDayName() {
        return dayName;
    }
}
