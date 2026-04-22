package com.example.myadvancedschedule;

// Shared reminder dialog used across task/event flows to return one validated
// reminder payload (timestamp + optional note) with consistent UX behavior.

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

/**
 * Material-style dialog that lets the user pick a specific date, time,
 * and optional reminder note in a single UI.
 * This shared dialog keeps reminder UX consistent for both tasks and events.
 */
public class ReminderDialogFragment extends DialogFragment {

    public interface OnReminderConfirmedListener {
        void onReminderConfirmed(long triggerAtMillis, String noteText);
    }

    private OnReminderConfirmedListener listener;

    public static ReminderDialogFragment newInstance() {
        // Factory method aligns usage with other dialog fragments in the project.
        return new ReminderDialogFragment();
    }

    public void setOnReminderConfirmedListener(OnReminderConfirmedListener listener) {
        // Host provides callback to receive validated reminder payload.
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Note: in rare cases the fragment can be detached while the dialog is being built.
        // We must not use `requireActivity()` / `requireContext()` in that case.
        Context context = getContext();
        if (context == null) context = getActivity();
        if (context == null) return null;

        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_reminder, null, false);

        DatePicker datePicker = view.findViewById(R.id.datePicker);
        TimePicker timePicker = view.findViewById(R.id.timePicker);
        TextInputEditText editReminderNote = view.findViewById(R.id.editReminderNote);

        Calendar now = Calendar.getInstance();
        datePicker.updateDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        // Defensive initialization in case of OEM-specific TimePicker quirks.
        if (timePicker != null) {
            timePicker.setIs24HourView(true);
            timePicker.setHour(now.get(Calendar.HOUR_OF_DAY));
            timePicker.setMinute(now.get(Calendar.MINUTE));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view)
                .setPositiveButton(R.string.reminder_save, (dialog, which) -> {
                    // Re-check context/listener at click time because fragment state may change.
                    Context safeContext = getContext();
                    if (safeContext == null) safeContext = getActivity();
                    if (listener == null || safeContext == null) return;
                    try {
                        int year = datePicker.getYear();
                        int month = datePicker.getMonth();
                        int day = datePicker.getDayOfMonth();

                        int hour = 0;
                        int minute = 0;
                        if (timePicker != null) {
                            hour = timePicker.getHour();
                            minute = timePicker.getMinute();
                        }

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
                            // Reject past reminders to avoid immediate/incorrect alarm firing.
                            android.widget.Toast.makeText(safeContext, R.string.reminder_time_in_past,
                                    android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String note = "";
                        if (editReminderNote != null && editReminderNote.getText() != null) {
                            note = editReminderNote.getText().toString().trim();
                        }
                        listener.onReminderConfirmed(triggerAt, note);
                    } catch (Exception e) {
                        // Any parsing/runtime issue should fail gracefully instead of crashing the host screen.
                        android.content.Context toastContext = getContext();
                        if (toastContext == null) toastContext = getActivity();
                        if (toastContext != null) {
                            android.widget.Toast.makeText(toastContext,
                                    R.string.reminder_error_generic,
                                    android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        return builder.create();
    }
}

