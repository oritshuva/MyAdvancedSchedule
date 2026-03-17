package com.example.myadvancedschedule;

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
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);
        recyclerTasks = view.findViewById(R.id.recyclerTasks);
        emptyView = view.findViewById(R.id.emptyView);
        fabAddTask = view.findViewById(R.id.fabAddTask);
        firestoreHelper = new FirestoreHelper();
        adapter = new TaskAdapter();
        adapter.setOnTaskActionListener(new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onTaskCheckedChanged(Task task, boolean completed) {
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
                            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onTaskDeleted(Task task) {
                firestoreHelper.deleteTask(task.getId(), new FirestoreHelper.OnOperationCompleteListener() {
                    @Override
                    public void onSuccess() {
                        adapter.removeTask(task);
                        toggleEmptyState();
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onTaskEdit(Task task) {
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
                if (!isAdded()) return;
                ReminderDialogFragment dialog = ReminderDialogFragment.newInstance();
                dialog.setOnReminderConfirmedListener((triggerAt, noteText) -> {
                    if (!isAdded()) return;
                    String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                    if (uid == null) {
                        Toast.makeText(requireContext(), R.string.login_failed, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Persist reminder metadata on the task, then schedule the notification.
                    task.setReminderTimeMillis(triggerAt);
                    task.setReminderDetail(noteText);
                    firestoreHelper.updateTask(uid, task, new FirestoreHelper.OnOperationCompleteListener() {
                        @Override
                        public void onSuccess() {
                            ReminderUtils.scheduleTaskReminder(requireContext(), task, triggerAt, noteText);
                            Toast.makeText(requireContext(), R.string.reminder_scheduled, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
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
        super.onResume();
        loadTasks();
    }

    private void loadTasks() {
        firestoreHelper.getTasks(new FirestoreHelper.OnTasksLoadedListener() {
            @Override
            public void onTasksLoaded(List<Task> tasks) {
                adapter.setTasks(tasks);
                toggleEmptyState();
            }
            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                emptyView.setVisibility(View.VISIBLE);
                recyclerTasks.setVisibility(View.GONE);
            }
        });
    }

    private void toggleEmptyState() {
        boolean empty = adapter.getItemCount() == 0;
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerTasks.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
