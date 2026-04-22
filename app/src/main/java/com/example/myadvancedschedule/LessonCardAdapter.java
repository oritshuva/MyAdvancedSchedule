package com.example.myadvancedschedule;

// Shared schedule-card adapter for school and after-school tabs, designed to
// present mixed row types consistently while keeping interactions predictable.

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

/**
 * Rich lesson-card adapter used by school and after-school schedule tabs.
 * It normalizes multiple card modes (regular lesson, free period, after-school event)
 * so both tabs can share one performant RecyclerView implementation.
 */
public class LessonCardAdapter extends RecyclerView.Adapter<LessonCardAdapter.LessonCardViewHolder> {

    public interface OnLessonShareListener {
        void onLessonShare(Lesson lesson);
    }

    public interface OnAfterSchoolEventActionListener {
        void onDelete(Lesson lesson);
        void onShare(Lesson lesson);
        void onReminder(Lesson lesson);
        void onDoneChanged(Lesson lesson, boolean done);
    }

    private final List<Lesson> lessons = new ArrayList<>();
    private boolean shareEnabled = false;
    private OnLessonShareListener shareListener;
    private OnAfterSchoolEventActionListener afterSchoolListener;
    private OnLessonClickListener lessonClickListener;

    /** Callback for tapping a regular (school) lesson card to edit it. */
    public interface OnLessonClickListener {
        void onLessonClick(Lesson lesson);
    }

    public void setShareEnabled(boolean enabled, OnLessonShareListener listener) {
        // Share is optional to keep interaction model context-specific per screen.
        this.shareEnabled = enabled;
        this.shareListener = listener;
    }

    public void setAfterSchoolEventActionListener(OnAfterSchoolEventActionListener listener) {
        this.afterSchoolListener = listener;
    }

    private static boolean isFreePeriod(Lesson lesson) {
        // Free-period placeholders created by the adapter have no Firestore ID.
        // Do not rely on the subject text, because users might edit a real lesson
        // to match the placeholder label.
        if (lesson == null) return true;
        return lesson.getId() == null;
    }

    public void setOnLessonClickListener(OnLessonClickListener listener) {
        this.lessonClickListener = listener;
    }

    public void addLesson(Lesson lesson) {
        // Incremental insert keeps UI responsive after single-item additions.
        if (lesson == null) return;
        lessons.add(lesson);
        notifyItemInserted(lessons.size() - 1);
    }

    public void removeLesson(Lesson lesson) {
        // Remove by current list identity to preserve adapter consistency.
        if (lesson == null) return;
        int index = lessons.indexOf(lesson);
        if (index >= 0) {
            lessons.remove(index);
            notifyItemRemoved(index);
        }
    }

    public void setLessons(List<Lesson> newLessons) {
        // Full replace keeps adapter deterministic after async Firestore refreshes.
        lessons.clear();
        if (newLessons != null && !newLessons.isEmpty()) {
            // Sort by period so timeline reads top-to-bottom in chronological order.
            List<Lesson> sorted = new ArrayList<>(newLessons);
            Collections.sort(sorted, Comparator.comparingInt(Lesson::getPeriod));

            List<Lesson> expanded = new ArrayList<>();
            int expectedPeriod = sorted.get(0).getPeriod();

            for (Lesson lesson : sorted) {
                // Fill missing periods with placeholders so users see gaps in their day.
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
        boolean isFreePeriod = isFreePeriod(lesson);
        boolean isAfterSchool = "after_school".equals(lesson.getScheduleType());

        int period = lesson.getPeriod();
        if (period > 0 && !isAfterSchool) {
            holder.textLessonNumber.setText(
                    context.getString(R.string.lesson_number_format, period));
        } else {
            holder.textLessonNumber.setText("");
        }

        holder.layoutEditSubject.setVisibility(View.GONE);
        holder.layoutEditTeacher.setVisibility(View.GONE);
        holder.layoutEditClassroom.setVisibility(View.GONE);

        if (isFreePeriod && !isAfterSchool) {
            holder.layoutEditSubject.setVisibility(View.GONE);
            holder.layoutEditTeacher.setVisibility(View.GONE);
            holder.layoutEditClassroom.setVisibility(View.GONE);
            holder.textSubject.setVisibility(View.VISIBLE);
            holder.textSubject.setText(context.getString(R.string.free_period_label));
            holder.textTeacher.setText("");
            holder.textClassroom.setText("");
            holder.textTeacher.setVisibility(View.GONE);
            holder.textClassroom.setVisibility(View.GONE);
        } else if (isAfterSchool) {
            // After-school events: never show inline edit fields.
            holder.layoutEditSubject.setVisibility(View.GONE);
            holder.layoutEditTeacher.setVisibility(View.GONE);
            holder.layoutEditClassroom.setVisibility(View.GONE);
            holder.textSubject.setVisibility(View.VISIBLE);
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
            holder.layoutEditSubject.setVisibility(View.GONE);
            holder.layoutEditTeacher.setVisibility(View.GONE);
            holder.layoutEditClassroom.setVisibility(View.GONE);
            holder.textSubject.setVisibility(View.VISIBLE);
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

        // Static indicator color avoids visual overload and keeps card hierarchy clean.

        if (shareEnabled && shareListener != null && !isFreePeriod) {
            holder.itemView.setOnLongClickListener(v -> {
                shareListener.onLessonShare(lesson);
                return true;
            });
        } else {
            holder.itemView.setOnLongClickListener(null);
        }

        // Event action row is shown only for after-school cards to match user intent in that tab.
        if (isAfterSchool && afterSchoolListener != null) {
            holder.layoutAfterSchoolActions.setVisibility(View.VISIBLE);

            holder.checkAfterSchoolDone.setOnCheckedChangeListener(null);
            holder.checkAfterSchoolDone.setChecked(false);
            holder.itemView.setAlpha(holder.checkAfterSchoolDone.isChecked() ? 0.6f : 1f);

            holder.checkAfterSchoolDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
                holder.itemView.setAlpha(isChecked ? 0.6f : 1f);
                afterSchoolListener.onDoneChanged(lesson, isChecked);
            });

            holder.buttonAfterSchoolShare.setOnClickListener(v -> afterSchoolListener.onShare(lesson));
            holder.buttonAfterSchoolReminder.setOnClickListener(v -> afterSchoolListener.onReminder(lesson));
            holder.buttonAfterSchoolDelete.setOnClickListener(v -> afterSchoolListener.onDelete(lesson));
        } else if (holder.layoutAfterSchoolActions != null) {
            holder.layoutAfterSchoolActions.setVisibility(View.GONE);
        }

        // Regular lesson taps open editor; placeholders/events are intentionally non-editable here.
        if (!isAfterSchool && !isFreePeriod && lessonClickListener != null) {
            holder.itemView.setOnClickListener(v -> lessonClickListener.onLessonClick(lesson));
        } else {
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return lessons.size();
    }

    static class LessonCardViewHolder extends RecyclerView.ViewHolder {
        View viewSubjectIndicator;
        TextView textLessonNumber, textSubject, textTeacher, textClassroom, textTime;
        View layoutEditSubject, layoutEditTeacher, layoutEditClassroom;
        View layoutAfterSchoolActions;
        android.widget.CheckBox checkAfterSchoolDone;
        android.widget.ImageButton buttonAfterSchoolShare, buttonAfterSchoolReminder, buttonAfterSchoolDelete;

        LessonCardViewHolder(@NonNull View itemView) {
            super(itemView);
            viewSubjectIndicator = itemView.findViewById(R.id.viewSubjectIndicator);
            textLessonNumber = itemView.findViewById(R.id.textLessonNumber);
            textSubject = itemView.findViewById(R.id.textSubject);
            textTeacher = itemView.findViewById(R.id.textTeacher);
            textClassroom = itemView.findViewById(R.id.textClassroom);
            textTime = itemView.findViewById(R.id.textTime);
            layoutEditSubject = itemView.findViewById(R.id.layoutEditSubject);
            layoutEditTeacher = itemView.findViewById(R.id.layoutEditTeacher);
            layoutEditClassroom = itemView.findViewById(R.id.layoutEditClassroom);
            layoutAfterSchoolActions = itemView.findViewById(R.id.layoutAfterSchoolActions);
            checkAfterSchoolDone = itemView.findViewById(R.id.checkAfterSchoolDone);
            buttonAfterSchoolShare = itemView.findViewById(R.id.buttonAfterSchoolShare);
            buttonAfterSchoolReminder = itemView.findViewById(R.id.buttonAfterSchoolReminder);
            buttonAfterSchoolDelete = itemView.findViewById(R.id.buttonAfterSchoolDelete);
        }
    }
}
