package com.example.myadvancedschedule;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Adapter for lesson cards (Subject, Teacher, Classroom, Start – End time). */
public class LessonCardAdapter extends RecyclerView.Adapter<LessonCardAdapter.LessonCardViewHolder> {

    private final List<Lesson> lessons = new ArrayList<>();

    public void setLessons(List<Lesson> newLessons) {
        lessons.clear();
        if (newLessons != null) lessons.addAll(newLessons);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LessonCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lesson_card, parent, false);
        return new LessonCardViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LessonCardViewHolder holder, int position) {
        Lesson lesson = lessons.get(position);
        holder.textSubject.setText(lesson.getSubject() != null ? lesson.getSubject() : "");
        holder.textTeacher.setText(lesson.getTeacher() != null ? lesson.getTeacher() : "");
        holder.textClassroom.setText(lesson.getClassroom() != null ? lesson.getClassroom() : "");
        String start = lesson.getStartTime() != null ? lesson.getStartTime() : "";
        String end = lesson.getEndTime() != null ? lesson.getEndTime() : "";
        holder.textTime.setText(start.isEmpty() && end.isEmpty() ? "" : start + " – " + end);
    }

    @Override
    public int getItemCount() {
        return lessons.size();
    }

    static class LessonCardViewHolder extends RecyclerView.ViewHolder {
        TextView textSubject, textTeacher, textClassroom, textTime;

        LessonCardViewHolder(@NonNull View itemView) {
            super(itemView);
            textSubject = itemView.findViewById(R.id.textSubject);
            textTeacher = itemView.findViewById(R.id.textTeacher);
            textClassroom = itemView.findViewById(R.id.textClassroom);
            textTime = itemView.findViewById(R.id.textTime);
        }
    }
}
