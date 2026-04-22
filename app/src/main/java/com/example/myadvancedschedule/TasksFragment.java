package com.example.myadvancedschedule;

// Tasks tab controller: loads task data from Firestore, updates list UI state,
// and coordinates add/edit/delete/completion/reminder actions.

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class TasksFragment extends Fragment {

    private RecyclerView recyclerTasks;
    private LinearLayout emptyView;
    private FloatingActionButton fabAddTask;
    private TaskAdapter adapter;
    private FirestoreHelper firestoreHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the tasks screen and wire all list interactions.
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);
        recyclerTasks = view.findViewById(R.id.recyclerTasks);
        emptyView = view.findViewById(R.id.emptyView);
        fabAddTask = view.findViewById(R.id.fabAddTask);
        firestoreHelper = new FirestoreHelper();
        adapter = new TaskAdapter();
        adapter.setOnTaskActionListener(new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onTaskCheckedChanged(Task task, boolean completed) {
                // Persist completion state first, then update visible list if needed.
                String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                if (uid != null) {
                    firestoreHelper.updateTask(uid, task, new FirestoreHelper.OnOperationCompleteListener() {
                        @Override
                        public void onSuccess() {
                            if (completed) {
                                // Immediately remove completed tasks from the list once persisted.
                                adapter.removeTask(task);
                                toggleEmptyState();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            if (!isAdded()) return;
                            android.content.Context ctx = getContext();
                            if (ctx == null) return;
                            Toast.makeText(ctx, error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onTaskDeleted(Task task) {
                // Delete from backend, then reflect that deletion immediately in the adapter.
                firestoreHelper.deleteTask(task.getId(), new FirestoreHelper.OnOperationCompleteListener() {
                    @Override
                    public void onSuccess() {
                        adapter.removeTask(task);
                        toggleEmptyState();
                    }

                    @Override
                    public void onFailure(String error) {
                        if (!isAdded()) return;
                        android.content.Context ctx = getContext();
                        if (ctx == null) return;
                        Toast.makeText(ctx, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onTaskEdit(Task task) {
                // Open the same dialog in edit mode and refresh item content on save.
                AddTaskDialogFragment dialog = AddTaskDialogFragment.newInstance(task);
                dialog.setOnTaskAddedListener((updatedTask, isEdit) -> {
                    if (isEdit) {
                        adapter.updateTask(updatedTask);
                    }
                    toggleEmptyState();
                });
                dialog.show(getParentFragmentManager(), "EditTask");
            }

            @Override
            public void onTaskReminderRequested(Task task) {
                // Collect reminder details from user, store them, then schedule system alarm.
                if (!isAdded()) return;
                ReminderDialogFragment dialog = ReminderDialogFragment.newInstance();
                dialog.setOnReminderConfirmedListener((triggerAt, noteText) -> {
                    if (!isAdded()) return;
                    android.content.Context ctx = getContext();
                    if (ctx == null) return;
                    String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                    if (uid == null) {
                        Toast.makeText(ctx, R.string.login_failed, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Persist reminder metadata on the task, then schedule the notification.
                    task.setReminderTimeMillis(triggerAt);
                    task.setReminderDetail(noteText);
                    firestoreHelper.updateTask(uid, task, new FirestoreHelper.OnOperationCompleteListener() {
                        @Override
                        public void onSuccess() {
                            if (!isAdded()) return;
                            android.content.Context successCtx = getContext();
                            if (successCtx == null) return;
                            ReminderUtils.scheduleTaskReminder(successCtx, task, triggerAt, noteText);
                            Toast.makeText(successCtx, R.string.reminder_scheduled, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String error) {
                            if (!isAdded()) return;
                            android.content.Context failCtx = getContext();
                            if (failCtx == null) return;
                            Toast.makeText(failCtx, error, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
                dialog.show(getParentFragmentManager(), "TaskReminderDialog");
            }
        });
        recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTasks.setAdapter(adapter);
        fabAddTask.setOnClickListener(v -> {
            AddTaskDialogFragment d = AddTaskDialogFragment.newInstance();
            d.setOnTaskAddedListener((task, isEdit) -> {
                if (!isEdit) {
                    adapter.addTask(task);
                } else {
                    adapter.updateTask(task);
                }
                toggleEmptyState();
            });
            d.show(getParentFragmentManager(), "AddTask");
        });
        loadTasks();
        return view;
    }

    @Override
    public void onResume() {
        // Refresh tasks whenever the user returns to this tab.
        super.onResume();
        loadTasks();
    }

    private void loadTasks() {
        // Read current task collection and update list + empty state.
        firestoreHelper.getTasks(new FirestoreHelper.OnTasksLoadedListener() {
            @Override
            public void onTasksLoaded(List<Task> tasks) {
                adapter.setTasks(tasks);
                toggleEmptyState();
            }
            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                android.content.Context ctx = getContext();
                if (ctx == null) return;
                Toast.makeText(ctx, error, Toast.LENGTH_SHORT).show();
                emptyView.setVisibility(View.VISIBLE);
                recyclerTasks.setVisibility(View.GONE);
            }
        });
    }

    private void toggleEmptyState() {
        // Show placeholder when no tasks exist; otherwise show the RecyclerView.
        boolean empty = adapter.getItemCount() == 0;
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerTasks.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
