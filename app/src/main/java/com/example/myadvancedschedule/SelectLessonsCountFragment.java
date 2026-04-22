package com.example.myadvancedschedule;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.HashMap;
import java.util.Map;

// Fragment for selecting per-day lesson counts in legacy setup flow.
// NumberPicker bounds enforce practical limits and prevent invalid schedule sizes.

public class SelectLessonsCountFragment extends Fragment {

    private NumberPicker npSunday, npMonday, npTuesday, npWednesday, npThursday;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate count-selection UI and initialize all weekday pickers.
        View view = inflater.inflate(R.layout.fragment_select_lessons_count, container, false);

        initNumberPickers(view);
        return view;
    }

    private void initNumberPickers(View view) {
        // Shared picker configuration keeps UX consistent across weekdays.
        npSunday = view.findViewById(R.id.npSunday);
        npMonday = view.findViewById(R.id.npMonday);
        npTuesday = view.findViewById(R.id.npTuesday);
        npWednesday = view.findViewById(R.id.npWednesday);
        npThursday = view.findViewById(R.id.npThursday);

        NumberPicker[] pickers = {npSunday, npMonday, npTuesday, npWednesday, npThursday};

        for (NumberPicker picker : pickers) {
            picker.setMinValue(1);
            picker.setMaxValue(15);
            picker.setValue(8); // Default value
            picker.setWrapSelectorWheel(false);
        }
    }

    public Map<String, Integer> getLessonCounts() {
        // Return explicit day-to-count mapping for downstream schedule generation logic.
        Map<String, Integer> counts = new HashMap<>();
        counts.put("Sunday", npSunday.getValue());
        counts.put("Monday", npMonday.getValue());
        counts.put("Tuesday", npTuesday.getValue());
        counts.put("Wednesday", npWednesday.getValue());
        counts.put("Thursday", npThursday.getValue());
        return counts;
    }
}
