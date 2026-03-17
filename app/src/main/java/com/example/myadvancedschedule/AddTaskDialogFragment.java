package com.example.myadvancedschedule;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class AddTaskDialogFragment extends DialogFragment {

    private TextInputEditText editTaskTitle, editDueTime;
    private OnTaskAddedListener listener;
    private Task taskToEdit;

    public interface OnTaskAddedListener {
        void onTaskAdded(Task task, boolean isEdit);
    }

    public static AddTaskDialogFragment newInstance() {
        return new AddTaskDialogFragment();
    }

    public static AddTaskDialogFragment newInstance(Task task) {
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
        AlertDialog.Builder b = new AlertDialog.Builder(requireActivity());
        View view = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_add_task, null);
        editTaskTitle = view.findViewById(R.id.editTaskTitle);
        editDueTime = view.findViewById(R.id.editDueTime);

        if (taskToEdit != null) {
            if (taskToEdit.getTitle() != null) {
                editTaskTitle.setText(taskToEdit.getTitle());
            }
            if (taskToEdit.getDueTime() != null) {
                editDueTime.setText(taskToEdit.getDueTime());
            }
        }
        b.setView(view)
                .setTitle(taskToEdit == null ? R.string.task_add : R.string.task_edit)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> saveTask())
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dismiss());
        return b.create();
    }

    private void saveTask() {
        String title = editTaskTitle.getText() != null ? editTaskTitle.getText().toString().trim() : "";
        if (title.isEmpty()) {
            editTaskTitle.setError(getString(R.string.task_title_hint));
            return;
        }
        String due = editDueTime.getText() != null ? editDueTime.getText().toString().trim() : "";
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean isEdit = taskToEdit != null;
        Task task;
        if (isEdit) {
            task = taskToEdit;
            task.setTitle(title);
            task.setDueTime(due.isEmpty() ? "—" : due);
        } else {
            task = new Task(title, due.isEmpty() ? "—" : due, false);
        }
        FirestoreHelper helper = new FirestoreHelper();
        if (isEdit) {
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
}
