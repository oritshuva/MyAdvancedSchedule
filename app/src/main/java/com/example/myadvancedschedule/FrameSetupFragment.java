package com.example.myadvancedschedule;

// First setup-wizard step where users define weekly schedule constraints that
// drive all subsequent day pages and generated lesson time slots.

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Step 1 of the setup wizard: weekly frame setup.
 * This step captures schedule constraints once so all generated day pages use
 * consistent timing rules and period counts.
 */
public class FrameSetupFragment extends Fragment {

    private static final Pattern TIME_PATTERN = Pattern.compile("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$");

    private CheckBox checkSunday, checkMonday, checkTuesday, checkWednesday,
            checkThursday, checkFriday, checkSaturday;
    private TextInputEditText editStartTime, editLessonDuration, editBreakDuration, editMaxLessons;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate form-only fragment; actual persistence is handled by SetupScheduleActivity.
        View view = inflater.inflate(R.layout.fragment_frame_setup, container, false);
        bindViews(view);
        return view;
    }

    private void bindViews(View view) {
        // Keep view binding isolated for readability and easier maintenance.
        checkSunday = view.findViewById(R.id.checkSunday);
        checkMonday = view.findViewById(R.id.checkMonday);
        checkTuesday = view.findViewById(R.id.checkTuesday);
        checkWednesday = view.findViewById(R.id.checkWednesday);
        checkThursday = view.findViewById(R.id.checkThursday);
        checkFriday = view.findViewById(R.id.checkFriday);
        checkSaturday = view.findViewById(R.id.checkSaturday);
        editStartTime = view.findViewById(R.id.editStartTime);
        editLessonDuration = view.findViewById(R.id.editLessonDuration);
        editBreakDuration = view.findViewById(R.id.editBreakDuration);
        editMaxLessons = view.findViewById(R.id.editMaxLessons);
    }

    /**
     * Returns the current frame setup data from the form, or null if validation fails.
     * Caller should show an error message when null is returned.
     */
    @Nullable
    public FrameSetupData getFrameSetupData() {
        // Return null on invalid input so parent activity can block progression safely.
        List<String> selectedDays = getSelectedDays();
        if (selectedDays.isEmpty()) {
            return null;
        }

        String startTime = editStartTime.getText() != null ? editStartTime.getText().toString().trim() : "";
        if (!isValidTime(startTime)) {
            editStartTime.setError(getString(R.string.setup_error_invalid_time));
            return null;
        }
        editStartTime.setError(null);

        int lessonDur = parseInt(editLessonDuration, 1, 120, "lesson duration");
        int breakDur = parseInt(editBreakDuration, 0, 60, "break duration");
        int maxLessons = parseInt(editMaxLessons, 1, 15, "max lessons");
        if (lessonDur < 0 || breakDur < 0 || maxLessons < 0) {
            return null;
        }

        return new FrameSetupData(selectedDays, startTime, lessonDur, breakDur, maxLessons);
    }

    private List<String> getSelectedDays() {
        // Preserve weekday order so generated tabs follow a predictable left-to-right flow.
        List<String> days = new ArrayList<>();
        if (checkSunday.isChecked()) days.add("Sunday");
        if (checkMonday.isChecked()) days.add("Monday");
        if (checkTuesday.isChecked()) days.add("Tuesday");
        if (checkWednesday.isChecked()) days.add("Wednesday");
        if (checkThursday.isChecked()) days.add("Thursday");
        if (checkFriday.isChecked()) days.add("Friday");
        if (checkSaturday.isChecked()) days.add("Saturday");
        return days;
    }

    private static boolean isValidTime(String time) {
        // Strict HH:mm validation prevents slot-generation math errors later in setup.
        if (TextUtils.isEmpty(time)) return false;
        return TIME_PATTERN.matcher(time).matches();
    }

    private int parseInt(TextInputEditText edit, int min, int max, String fieldName) {
        // Clamp numeric setup fields to practical bounds to avoid unusable schedules.
        String s = edit.getText() != null ? edit.getText().toString().trim() : "";
        if (TextUtils.isEmpty(s)) {
            edit.setError(getString(R.string.setup_error_invalid_number));
            return -1;
        }
        try {
            int value = Integer.parseInt(s);
            if (value < min || value > max) {
                edit.setError(getString(R.string.setup_error_invalid_number));
                return -1;
            }
            edit.setError(null);
            return value;
        } catch (NumberFormatException e) {
            edit.setError(getString(R.string.setup_error_invalid_number));
            return -1;
        }
    }
}
