package com.example.myadvancedschedule;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LessonEditAdapter extends RecyclerView.Adapter<LessonEditAdapter.LessonViewHolder> {
    private final Context context;
    private final List<Lesson> lessons;

    public LessonEditAdapter(Context context, List<Lesson> lessons) {
        this.context = context;
        this.lessons = lessons;
    }

    @NonNull
    @Override
    public LessonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_lesson_edit, parent, false);
        return new LessonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LessonViewHolder holder, int position) {
        Lesson lesson = lessons.get(position);

        holder.tvPeriod.setText("Period " + lesson.getPeriodNumber());
        holder.etSubject.setText(lesson.getSubject());
        holder.etStartTime.setText(lesson.getStartTime());
        holder.etEndTime.setText(lesson.getEndTime());

        // Save data when user inputs
        holder.etSubject.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                lesson.setSubject(holder.etSubject.getText().toString());
            }
        });
        holder.etStartTime.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                lesson.setStartTime(holder.etStartTime.getText().toString());
            }
        });
        holder.etEndTime.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                lesson.setEndTime(holder.etEndTime.getText().toString());
            }
        });
    }

    @Override
    public int getItemCount() {
        return lessons.size();
    }

    public List<Lesson> getLessons() {
        return lessons;
    }

    static class LessonViewHolder extends RecyclerView.ViewHolder {
        TextView tvPeriod;
        EditText etSubject, etStartTime, etEndTime;

        public LessonViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPeriod = itemView.findViewById(R.id.tvPeriod);
            etSubject = itemView.findViewById(R.id.etSubject);
            etStartTime = itemView.findViewById(R.id.etStartTime);
            etEndTime = itemView.findViewById(R.id.etEndTime);
        }
    }
}
