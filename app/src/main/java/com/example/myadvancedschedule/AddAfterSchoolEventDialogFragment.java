package com.example.myadvancedschedule;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;

/**
 * Dialog for adding a simple after-school event backed by a Lesson with scheduleType="after_school".
 * Reuses the existing lessons/Firestore pipeline but exposes user-friendly event fields.
 */
public class AddAfterSchoolEventDialogFragment extends DialogFragment {

    private TextInputEditText inputTitle;
    private TextInputEditText inputStartTime;
    private TextInputEditText inputEndTime;
    private TextInputEditText inputDescription;
    private TextInputEditText inputLocation;

    public interface OnEventSavedListener {
        void onEventSaved();
    }

    private OnEventSavedListener listener;

    public static AddAfterSchoolEventDialogFragment newInstance() {
        return new AddAfterSchoolEventDialogFragment();
    }

    public void setOnEventSavedListener(OnEventSavedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_after_school_event, null, false);

        inputTitle = view.findViewById(R.id.inputEventTitle);
        inputStartTime = view.findViewById(R.id.inputEventStartTime);
        inputEndTime = view.findViewById(R.id.inputEventEndTime);
        inputDescription = view.findViewById(R.id.inputEventDescription);
        inputLocation = view.findViewById(R.id.inputEventLocation);

        View.OnClickListener timeClick = v -> showTimePicker((TextInputEditText) v);
        inputStartTime.setOnClickListener(timeClick);
        inputEndTime.setOnClickListener(timeClick);

        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.after_school_add_event_title)
                .setView(view)
                .setPositiveButton(R.string.save, (dialog, which) -> saveEvent())
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
    }

    private void showTimePicker(final TextInputEditText target) {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (TimePicker view, int hourOfDay, int minuteOfHour) ->
                        target.setText(String.format("%02d:%02d", hourOfDay, minuteOfHour)),
                hour,
                minute,
                true
        );
        dialog.show();
    }

    private void saveEvent() {
        String title = inputTitle.getText() != null ? inputTitle.getText().toString().trim() : "";
        String start = inputStartTime.getText() != null ? inputStartTime.getText().toString().trim() : "";
        String end = inputEndTime.getText() != null ? inputEndTime.getText().toString().trim() : "";
        String description = inputDescription.getText() != null ? inputDescription.getText().toString().trim() : "";
        String location = inputLocation.getText() != null ? inputLocation.getText().toString().trim() : "";

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(start) || TextUtils.isEmpty(end)) {
            Toast.makeText(requireContext(), R.string.after_school_add_event_validation, Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid == null) {
            Toast.makeText(requireContext(), R.string.error_load_lessons, Toast.LENGTH_SHORT).show();
            return;
        }

        String today = ScheduleFragmentHelper.getTodayDayName();

        Lesson lesson = new Lesson(
                title,
                description,
                location,
                today,
                0,
                start,
                end
        );
        lesson.setScheduleType("after_school");

        FirestoreHelper helper = new FirestoreHelper();
        helper.addLesson(uid, lesson, new FirestoreHelper.OnOperationCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), R.string.after_school_add_event_success, Toast.LENGTH_SHORT).show();
                if (listener != null) listener.onEventSaved();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

