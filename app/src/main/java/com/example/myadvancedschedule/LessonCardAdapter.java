package com.example.myadvancedschedule;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Adapter for lesson cards with a timetable-like layout. */
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
                // Insert "Free period" slots for missing periods (school schedule only)
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
        boolean isAfterSchool = "after_school".equals(lesson.getScheduleType());

        int period = lesson.getPeriod();
        if (period > 0 && !isAfterSchool) {
            holder.textLessonNumber.setText(
                    context.getString(R.string.lesson_number_format, period));
        } else {
            holder.textLessonNumber.setText("");
        }

        if (isFreePeriod && !isAfterSchool) {
            holder.textSubject.setText(context.getString(R.string.free_period_label));
            holder.textTeacher.setText("");
            holder.textClassroom.setText("");
            holder.textTeacher.setVisibility(View.GONE);
            holder.textClassroom.setVisibility(View.GONE);
        } else if (isAfterSchool) {
            String title = lesson.getSubject() != null ? lesson.getSubject() : "";
            String description = lesson.getTeacher() != null ? lesson.getTeacher() : "";
            String location = lesson.getClassroom() != null ? lesson.getClassroom() : "";

            holder.textSubject.setText(title);

            if (!description.isEmpty()) {
                holder.textTeacher.setVisibility(View.VISIBLE);
                holder.textTeacher.setText(
                        context.getString(R.string.event_description_format, description));
            } else {
                holder.textTeacher.setVisibility(View.GONE);
            }

            if (!location.isEmpty()) {
                holder.textClassroom.setVisibility(View.VISIBLE);
                holder.textClassroom.setText(
                        context.getString(R.string.event_location_format, location));
            } else {
                holder.textClassroom.setVisibility(View.GONE);
            }
        } else {
            String subject = lesson.getSubject() != null ? lesson.getSubject() : "";
            String teacher = lesson.getTeacher() != null ? lesson.getTeacher() : "";
            String classroom = lesson.getClassroom() != null ? lesson.getClassroom() : "";

            holder.textSubject.setText(
                    context.getString(R.string.lesson_subject_format, subject));
            holder.textTeacher.setVisibility(View.VISIBLE);
            holder.textTeacher.setText(
                    context.getString(R.string.lesson_teacher_format, teacher));
            holder.textClassroom.setVisibility(View.VISIBLE);
            holder.textClassroom.setText(
                    context.getString(R.string.lesson_classroom_format, classroom));
        }

        String start = lesson.getStartTime() != null ? lesson.getStartTime() : "";
        String end = lesson.getEndTime() != null ? lesson.getEndTime() : "";
        holder.textTime.setText(start.isEmpty() && end.isEmpty() ? "" : start + " – " + end);

        // Keep subject indicator in a single consistent color defined in XML (no subject-based colors).

        if (shareEnabled && shareListener != null && !isFreePeriod) {
            holder.itemView.setOnLongClickListener(v -> {
                shareListener.onLessonShare(lesson);
                return true;
            });
        } else {
            holder.itemView.setOnLongClickListener(null);
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
