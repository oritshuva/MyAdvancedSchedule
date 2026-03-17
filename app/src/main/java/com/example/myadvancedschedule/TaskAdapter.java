package com.example.myadvancedschedule;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> tasks = new ArrayList<>();
    private OnTaskActionListener listener;

    public interface OnTaskActionListener {
        void onTaskCheckedChanged(Task task, boolean completed);
        void onTaskDeleted(Task task);
        void onTaskEdit(Task task);
        void onTaskReminderRequested(Task task);
    }

    public void setOnTaskActionListener(OnTaskActionListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<Task> newTasks) {
        tasks.clear();
        if (newTasks != null) tasks.addAll(newTasks);
        notifyDataSetChanged();
    }

    public void addTask(Task task) {
        if (task == null) return;
        tasks.add(task);
        notifyItemInserted(tasks.size() - 1);
    }

    public void removeTask(Task task) {
        if (task == null) return;
        int index = tasks.indexOf(task);
        if (index >= 0) {
            tasks.remove(index);
            notifyItemRemoved(index);
        }
    }

    public void updateTask(Task task) {
        if (task == null) return;
        int index = tasks.indexOf(task);
        if (index >= 0) {
            tasks.set(index, task);
            notifyItemChanged(index);
        }
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.textTaskTitle.setText(task.getTitle());
        holder.textDueTime.setText(task.getDueTime() != null ? task.getDueTime() : "");
        holder.checkCompleted.setChecked(task.isCompleted());
        holder.textTaskTitle.setAlpha(task.isCompleted() ? 0.6f : 1f);
        holder.checkCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            holder.textTaskTitle.setAlpha(isChecked ? 0.6f : 1f);
            if (listener != null) listener.onTaskCheckedChanged(task, isChecked);
        });

        holder.buttonDelete.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                Task toDelete = tasks.get(adapterPosition);
                listener.onTaskDeleted(toDelete);
            }
        });

        holder.buttonShare.setOnClickListener(v -> {
            String title = task.getTitle() != null ? task.getTitle() : "";
            String due = task.getDueTime() != null ? task.getDueTime() : "";
            String text = v.getContext().getString(R.string.task_shared_text, title, due);
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
            v.getContext().startActivity(Intent.createChooser(sendIntent, null));
        });

        holder.buttonReminder.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskReminderRequested(task);
            }
        });

        holder.buttonEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskEdit(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkCompleted;
        TextView textTaskTitle, textDueTime;
        ImageButton buttonShare, buttonReminder, buttonDelete, buttonEdit;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkCompleted = itemView.findViewById(R.id.checkCompleted);
            textTaskTitle = itemView.findViewById(R.id.textTaskTitle);
            textDueTime = itemView.findViewById(R.id.textDueTime);
            buttonShare = itemView.findViewById(R.id.buttonShare);
            buttonReminder = itemView.findViewById(R.id.buttonReminder);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
        }
    }
}
