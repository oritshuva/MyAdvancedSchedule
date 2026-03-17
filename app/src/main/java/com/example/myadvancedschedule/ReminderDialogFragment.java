package com.example.myadvancedschedule;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

/**
 * Material-style dialog that lets the user pick a specific date, time,
 * and optional reminder note in a single UI.
 */
public class ReminderDialogFragment extends DialogFragment {

    public interface OnReminderConfirmedListener {
        void onReminderConfirmed(long triggerAtMillis, String noteText);
    }

    private OnReminderConfirmedListener listener;

    public static ReminderDialogFragment newInstance() {
        return new ReminderDialogFragment();
    }

    public void setOnReminderConfirmedListener(OnReminderConfirmedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_reminder, null, false);

        DatePicker datePicker = view.findViewById(R.id.datePicker);
        TimePicker timePicker = view.findViewById(R.id.timePicker);
        TextInputEditText editReminderNote = view.findViewById(R.id.editReminderNote);

        Calendar now = Calendar.getInstance();
        datePicker.updateDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        timePicker.setIs24HourView(true);
        timePicker.setHour(now.get(Calendar.HOUR_OF_DAY));
        timePicker.setMinute(now.get(Calendar.MINUTE));

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(view)
                .setPositiveButton(R.string.reminder_save, (dialog, which) -> {
                    if (listener == null) return;

                    int year = datePicker.getYear();
                    int month = datePicker.getMonth();
                    int day = datePicker.getDayOfMonth();

                    int hour;
                    int minute;
                    hour = timePicker.getHour();
                    minute = timePicker.getMinute();

                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, day);
                    selected.set(Calendar.HOUR_OF_DAY, hour);
                    selected.set(Calendar.MINUTE, minute);
                    selected.set(Calendar.SECOND, 0);
                    selected.set(Calendar.MILLISECOND, 0);

                    long triggerAt = selected.getTimeInMillis();
                    if (triggerAt <= System.currentTimeMillis()) {
                        if (getContext() != null) {
                            android.widget.Toast.makeText(
                                    getContext(),
                                    R.string.reminder_time_in_past,
                                    android.widget.Toast.LENGTH_SHORT
                            ).show();
                        }
                        return;
                    }

                    String note = "";
                    if (editReminderNote.getText() != null) {
                        note = editReminderNote.getText().toString().trim();
                    }
                    listener.onReminderConfirmed(triggerAt, note);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        return builder.create();
    }
}

