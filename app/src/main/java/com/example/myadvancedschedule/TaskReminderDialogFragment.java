package com.example.myadvancedschedule;

// Task-specific reminder picker dialog that captures date/time/detail together
// before scheduling, reducing reminder configuration friction for users.

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Reminder dialog specialized for tasks:
 * - Time picker (wheel-style dialog)
 * - Date picker (calendar dialog)
 * - Detail text field
 * - Explicit Save button at the bottom.
 * Keeping this as a dedicated task dialog allows task-specific UX without
 * affecting the generic reminder dialog used elsewhere.
 */
public class TaskReminderDialogFragment extends DialogFragment {

    public interface OnReminderConfirmedListener {
        void onReminderConfirmed(long triggerAtMillis, String detailText);
    }

    private OnReminderConfirmedListener listener;

    private int selectedYear;
    private int selectedMonth;
    private int selectedDay;
    private int selectedHour;
    private int selectedMinute;

    public static TaskReminderDialogFragment newInstance() {
        // Factory constructor for consistent fragment recreation patterns.
        return new TaskReminderDialogFragment();
    }

    public void setOnReminderConfirmedListener(OnReminderConfirmedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Manual save button inside layout allows richer validation flow before dismissal.
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_task_reminder, null, false);

        final Button buttonTime = view.findViewById(R.id.buttonTime);
        final Button buttonDate = view.findViewById(R.id.buttonDate);
        final Button buttonSave = view.findViewById(R.id.buttonSaveReminder);
        final TextInputEditText editDetail = view.findViewById(R.id.editReminderDetail);

        Calendar now = Calendar.getInstance();
        selectedYear = now.get(Calendar.YEAR);
        selectedMonth = now.get(Calendar.MONTH);
        selectedDay = now.get(Calendar.DAY_OF_MONTH);
        selectedHour = now.get(Calendar.HOUR_OF_DAY);
        selectedMinute = now.get(Calendar.MINUTE);

        updateTimeButtonLabel(buttonTime);
        updateDateButtonLabel(buttonDate);

        buttonTime.setOnClickListener(v -> showTimePicker(buttonTime));
        buttonDate.setOnClickListener(v -> showDatePicker(buttonDate));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();

        buttonSave.setOnClickListener(v -> {
            if (listener == null || getContext() == null) {
                // Nothing to return to; close safely.
                dialog.dismiss();
                return;
            }
            try {
                Calendar selected = Calendar.getInstance();
                selected.set(Calendar.YEAR, selectedYear);
                selected.set(Calendar.MONTH, selectedMonth);
                selected.set(Calendar.DAY_OF_MONTH, selectedDay);
                selected.set(Calendar.HOUR_OF_DAY, selectedHour);
                selected.set(Calendar.MINUTE, selectedMinute);
                selected.set(Calendar.SECOND, 0);
                selected.set(Calendar.MILLISECOND, 0);

                long triggerAt = selected.getTimeInMillis();
                if (triggerAt <= System.currentTimeMillis()) {
                    // Enforce future reminders so users do not receive immediate stale notifications.
                    Toast.makeText(
                            getContext(),
                            R.string.reminder_time_in_past,
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                String detail = "";
                if (editDetail != null && editDetail.getText() != null) {
                    detail = editDetail.getText().toString().trim();
                }

                listener.onReminderConfirmed(triggerAt, detail);
                dialog.dismiss();
            } catch (Exception e) {
                Toast.makeText(
                        getContext(),
                        R.string.reminder_error_generic,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        return dialog;
    }

    private void showTimePicker(Button buttonTime) {
        // Native picker prevents invalid time format input.
        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute;
                    updateTimeButtonLabel(buttonTime);
                },
                selectedHour,
                selectedMinute,
                true
        );
        dialog.show();
    }

    private void showDatePicker(Button buttonDate) {
        // Date picker keeps reminder date selection explicit and locale-safe.
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedYear = year;
                    selectedMonth = month;
                    selectedDay = dayOfMonth;
                    updateDateButtonLabel(buttonDate);
                },
                selectedYear,
                selectedMonth,
                selectedDay
        );
        dialog.show();
    }

    private void updateTimeButtonLabel(Button buttonTime) {
        // Reflect selected value immediately for confirmation before save.
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, selectedHour);
        c.set(Calendar.MINUTE, selectedMinute);
        DateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        buttonTime.setText(tf.format(c.getTime()));
    }

    private void updateDateButtonLabel(Button buttonDate) {
        // Use localized date formatting for readability.
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, selectedYear);
        c.set(Calendar.MONTH, selectedMonth);
        c.set(Calendar.DAY_OF_MONTH, selectedDay);
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        buttonDate.setText(df.format(c.getTime()));
    }
}

