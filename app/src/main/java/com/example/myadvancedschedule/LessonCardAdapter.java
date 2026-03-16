package com.example.myadvancedschedule;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Adapter for lesson cards (Subject, Teacher, Classroom, Start – End time). */
public class LessonCardAdapter extends RecyclerView.Adapter<LessonCardAdapter.LessonCardViewHolder> {

    public interface OnLessonShareListener {
        void onLessonShare(Lesson lesson);
    }

    private final List<Lesson> lessons = new ArrayList<>();
    private boolean shareEnabled = false;
    private OnLessonShareListener shareListener;

    public void setShareEnabled(boolean enabled, OnLessonShareListener listener) {
        this.shareEnabled = enabled;
        this.shareListener = listener;
    }

    public void setLessons(List<Lesson> newLessons) {
        lessons.clear();
        if (newLessons != null && !newLessons.isEmpty()) {
            // Sort by period to build a clear daily timeline
            List<Lesson> sorted = new ArrayList<>(newLessons);
            Collections.sort(sorted, Comparator.comparingInt(Lesson::getPeriod));

            List<Lesson> expanded = new ArrayList<>();
            int expectedPeriod = sorted.get(0).getPeriod();

            for (Lesson lesson : sorted) {
                // Insert "Free period" slots for missing periods
                while (expectedPeriod > 0 && expectedPeriod < lesson.getPeriod()) {
                    Lesson free = new Lesson();
                    free.setPeriod(expectedPeriod);
                    free.setSubject("Free period / חלון");
                    free.setTeacher("");
                    free.setClassroom("");
                    free.setStartTime("");
                    free.setEndTime("");
                    free.setScheduleType(lesson.getScheduleType());
                    expanded.add(free);
                    expectedPeriod++;
                }
                expanded.add(lesson);
                expectedPeriod = lesson.getPeriod() + 1;
            }

            lessons.addAll(expanded);
        }
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

        Context context = holder.itemView.getContext();
        boolean isFreePeriod = lesson.getSubject() == null
                || lesson.getSubject().trim().isEmpty()
                || "Free period / חלון".equals(lesson.getSubject());

        int period = lesson.getPeriod();
        if (period > 0) {
            holder.textLessonNumber.setText(
                    context.getString(R.string.lesson_number_format, period));
        } else {
            holder.textLessonNumber.setText("");
        }

        if (isFreePeriod) {
            holder.textSubject.setText(context.getString(R.string.free_period_label));
            holder.textTeacher.setText("");
            holder.textClassroom.setText("");
        } else {
            String subject = lesson.getSubject() != null ? lesson.getSubject() : "";
            String teacher = lesson.getTeacher() != null ? lesson.getTeacher() : "";
            String classroom = lesson.getClassroom() != null ? lesson.getClassroom() : "";

            holder.textSubject.setText(
                    context.getString(R.string.lesson_subject_format, subject));
            holder.textTeacher.setText(
                    context.getString(R.string.lesson_teacher_format, teacher));
            holder.textClassroom.setText(
                    context.getString(R.string.lesson_classroom_format, classroom));
        }

        String start = lesson.getStartTime() != null ? lesson.getStartTime() : "";
        String end = lesson.getEndTime() != null ? lesson.getEndTime() : "";
        holder.textTime.setText(start.isEmpty() && end.isEmpty() ? "" : start + " – " + end);

        // Simple subject-based color indicator for visual timetable style
        int color = pickColorForSubject(lesson.getSubject());
        holder.viewSubjectIndicator.setBackgroundColor(color);

        if (shareEnabled && shareListener != null && !isFreePeriod) {
            holder.itemView.setOnLongClickListener(v -> {
                shareListener.onLessonShare(lesson);
                return true;
            });
        } else {
            holder.itemView.setOnLongClickListener(null);
        }
    }

    private int pickColorForSubject(String subject) {
        if (subject == null) return Color.parseColor("#FFB74D"); // default amber
        int hash = Math.abs(subject.hashCode());
        switch (hash % 5) {
            case 0: return Color.parseColor("#42A5F5"); // blue
            case 1: return Color.parseColor("#66BB6A"); // green
            case 2: return Color.parseColor("#AB47BC"); // purple
            case 3: return Color.parseColor("#EF5350"); // red
            default: return Color.parseColor("#FFB74D"); // amber
        }
    }

    @Override
    public int getItemCount() {
        return lessons.size();
    }

    static class LessonCardViewHolder extends RecyclerView.ViewHolder {
        View viewSubjectIndicator;
        TextView textLessonNumber, textSubject, textTeacher, textClassroom, textTime;

        LessonCardViewHolder(@NonNull View itemView) {
            super(itemView);
            viewSubjectIndicator = itemView.findViewById(R.id.viewSubjectIndicator);
            textLessonNumber = itemView.findViewById(R.id.textLessonNumber);
            textSubject = itemView.findViewById(R.id.textSubject);
            textTeacher = itemView.findViewById(R.id.textTeacher);
            textClassroom = itemView.findViewById(R.id.textClassroom);
            textTime = itemView.findViewById(R.id.textTime);
        }
    }
}
