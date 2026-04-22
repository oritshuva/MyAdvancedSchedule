package com.example.myadvancedschedule;

// Task add/edit dialog that centralizes task input, due-date selection, and
// save routing so TasksFragment remains focused on list orchestration.

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
import com.google.firebase.auth.FirebaseAuth;

// Task creation/edit dialog used by TasksFragment.
// It captures due date/time in structured pickers to keep stored due values consistent.

public class AddTaskDialogFragment extends DialogFragment {

    private TextInputEditText editTaskTitle;
    private OnTaskAddedListener listener;
    private Task taskToEdit;

    private int selectedYear;
    private int selectedMonth;
    private int selectedDay;
    private int selectedHour;
    private int selectedMinute;

    public interface OnTaskAddedListener {
        void onTaskAdded(Task task, boolean isEdit);
    }

    public static AddTaskDialogFragment newInstance() {
        // Add mode entry point.
        return new AddTaskDialogFragment();
    }

    public static AddTaskDialogFragment newInstance(Task task) {
        // Edit mode entry point with existing task payload.
        AddTaskDialogFragment fragment = new AddTaskDialogFragment();
        fragment.taskToEdit = task;
        return fragment;
    }

    public void setOnTaskAddedListener(OnTaskAddedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Initialize defaults from current clock so users can save quickly with minimal taps.
        AlertDialog.Builder b = new AlertDialog.Builder(requireActivity());
        View view = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_add_task, null);
        editTaskTitle = view.findViewById(R.id.editTaskTitle);

        Button buttonDueTime = view.findViewById(R.id.buttonDueTime);
        Button buttonDueDate = view.findViewById(R.id.buttonDueDate);

        java.util.Calendar now = java.util.Calendar.getInstance();
        selectedYear = now.get(java.util.Calendar.YEAR);
        selectedMonth = now.get(java.util.Calendar.MONTH);
        selectedDay = now.get(java.util.Calendar.DAY_OF_MONTH);
        selectedHour = now.get(java.util.Calendar.HOUR_OF_DAY);
        selectedMinute = now.get(java.util.Calendar.MINUTE);

        updateTimeButtonLabel(buttonDueTime);
        updateDateButtonLabel(buttonDueDate);

        buttonDueTime.setOnClickListener(v -> showTimePicker(buttonDueTime));
        buttonDueDate.setOnClickListener(v -> showDatePicker(buttonDueDate));

        if (taskToEdit != null) {
            if (taskToEdit.getTitle() != null) {
                editTaskTitle.setText(taskToEdit.getTitle());
            }
            // If editing, try to parse an existing dueTime text back into date/time components is optional.
            // For simplicity, keep current date/time defaults while showing existing dueTime only in the list.
        }
        b.setView(view)
                .setTitle(taskToEdit == null ? R.string.task_add : R.string.task_edit)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> saveTask())
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dismiss());
        return b.create();
    }

    private void saveTask() {
        // Validate title and auth context before attempting Firestore writes.
        String title = editTaskTitle.getText() != null ? editTaskTitle.getText().toString().trim() : "";
        if (title.isEmpty()) {
            editTaskTitle.setError(getString(R.string.task_title_hint));
            return;
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean isEdit = taskToEdit != null;
        Task task;
        // Persist due datetime in one stable text format so sorting/display stay predictable.
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.YEAR, selectedYear);
        c.set(java.util.Calendar.MONTH, selectedMonth);
        c.set(java.util.Calendar.DAY_OF_MONTH, selectedDay);
        c.set(java.util.Calendar.HOUR_OF_DAY, selectedHour);
        c.set(java.util.Calendar.MINUTE, selectedMinute);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
        String due = fmt.format(c.getTime());
        if (isEdit) {
            task = taskToEdit;
            task.setTitle(title);
            task.setDueTime(due);
        } else {
            task = new Task(title, due, false);
        }
        FirestoreHelper helper = new FirestoreHelper();
        if (isEdit) {
            // Edit path keeps the same ID and updates fields in-place.
            helper.updateTask(userId, task, new FirestoreHelper.OnOperationCompleteListener() {
                @Override
                public void onSuccess() {
                    if (listener != null) listener.onTaskAdded(task, true);
                    dismiss();
                }
                @Override
                public void onFailure(String error) {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Add path generates deterministic ID in helper and returns task through callback.
            helper.addTask(userId, task, new FirestoreHelper.OnOperationCompleteListener() {
                @Override
                public void onSuccess() {
                    if (listener != null) listener.onTaskAdded(task, false);
                    dismiss();
                }
                @Override
                public void onFailure(String error) {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showTimePicker(Button target) {
        // Use native picker to prevent invalid 24h time strings.
        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute;
                    updateTimeButtonLabel(target);
                },
                selectedHour,
                selectedMinute,
                true
        );
        dialog.show();
    }

    private void showDatePicker(Button target) {
        // Date picker avoids locale-dependent parsing of free text dates.
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedYear = year;
                    selectedMonth = month;
                    selectedDay = dayOfMonth;
                    updateDateButtonLabel(target);
                },
                selectedYear,
                selectedMonth,
                selectedDay
        );
        dialog.show();
    }

    private void updateTimeButtonLabel(Button target) {
        // Immediate visual feedback helps users confirm chosen time before saving.
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.HOUR_OF_DAY, selectedHour);
        c.set(java.util.Calendar.MINUTE, selectedMinute);
        java.text.SimpleDateFormat tf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        target.setText(tf.format(c.getTime()));
    }

    private void updateDateButtonLabel(Button target) {
        // Locale-aware date label improves readability for end users.
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.YEAR, selectedYear);
        c.set(java.util.Calendar.MONTH, selectedMonth);
        c.set(java.util.Calendar.DAY_OF_MONTH, selectedDay);
        java.text.DateFormat df = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, java.util.Locale.getDefault());
        target.setText(df.format(c.getTime()));
    }
}
