package com.example.myadvancedschedule;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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
    }

    public void setOnTaskActionListener(OnTaskActionListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<Task> newTasks) {
        tasks.clear();
        if (newTasks != null) tasks.addAll(newTasks);
        notifyDataSetChanged();
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
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkCompleted;
        TextView textTaskTitle, textDueTime;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkCompleted = itemView.findViewById(R.id.checkCompleted);
            textTaskTitle = itemView.findViewById(R.id.textTaskTitle);
            textDueTime = itemView.findViewById(R.id.textDueTime);
        }
    }
}
