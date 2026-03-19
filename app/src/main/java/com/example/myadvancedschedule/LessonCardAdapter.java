package com.example.myadvancedschedule;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Adapter for lesson cards with a timetable-like layout. */
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
    private boolean editMode = false;
    private OnLessonShareListener shareListener;
    private OnAfterSchoolEventActionListener afterSchoolListener;
    private OnLessonClickListener lessonClickListener;

    /** Callback for tapping a regular (school) lesson card to edit it. */
    public interface OnLessonClickListener {
        void onLessonClick(Lesson lesson);
    }

    public void setShareEnabled(boolean enabled, OnLessonShareListener listener) {
        this.shareEnabled = enabled;
        this.shareListener = listener;
    }

    public void setAfterSchoolEventActionListener(OnAfterSchoolEventActionListener listener) {
        this.afterSchoolListener = listener;
    }

    public void setEditMode(boolean editMode) {
        if (this.editMode == editMode) return;
        this.editMode = editMode;
        notifyDataSetChanged();
    }

    public boolean isEditMode() {
        return editMode;
    }

    /** Returns a copy of school lessons that are editable inline (excludes free slots and after-school events). */
    public List<Lesson> getEditableSchoolLessons() {
        List<Lesson> editable = new ArrayList<>();
        for (Lesson l : lessons) {
            if (l == null) continue;
            if (l.getId() == null) continue; // free-period placeholders don't have IDs
            if ("after_school".equals(l.getScheduleType())) continue;
            if (isFreePeriod(l)) continue;
            editable.add(l);
        }
        return new ArrayList<>(editable);
    }

    private static boolean isFreePeriod(Lesson lesson) {
        if (lesson == null) return true;
        String subject = lesson.getSubject();
        return subject == null
                || subject.trim().isEmpty()
                || "Free period / חלון".equals(subject);
    }

    public void setOnLessonClickListener(OnLessonClickListener listener) {
        this.lessonClickListener = listener;
    }

    public void addLesson(Lesson lesson) {
        if (lesson == null) return;
        lessons.add(lesson);
        notifyItemInserted(lessons.size() - 1);
    }

    public void removeLesson(Lesson lesson) {
        if (lesson == null) return;
        int index = lessons.indexOf(lesson);
        if (index >= 0) {
            lessons.remove(index);
            notifyItemRemoved(index);
        }
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
        boolean isFreePeriod = isFreePeriod(lesson);
        boolean isAfterSchool = "after_school".equals(lesson.getScheduleType());
        boolean showEditFields = editMode && !isAfterSchool && !isFreePeriod;

        int period = lesson.getPeriod();
        if (period > 0 && !isAfterSchool) {
            holder.textLessonNumber.setText(
                    context.getString(R.string.lesson_number_format, period));
        } else {
            holder.textLessonNumber.setText("");
        }

        if (showEditFields) {
            // Inline editing: switch labels to TextInput fields.
            holder.textSubject.setVisibility(View.GONE);
            holder.textTeacher.setVisibility(View.GONE);
            holder.textClassroom.setVisibility(View.GONE);

            holder.layoutEditSubject.setVisibility(View.VISIBLE);
            holder.layoutEditTeacher.setVisibility(View.VISIBLE);
            holder.layoutEditClassroom.setVisibility(View.VISIBLE);

            // Remove old listeners before updating text to avoid duplicate bindings.
            if (holder.subjectWatcher != null) holder.editSubject.removeTextChangedListener(holder.subjectWatcher);
            if (holder.teacherWatcher != null) holder.editTeacher.removeTextChangedListener(holder.teacherWatcher);
            if (holder.classroomWatcher != null) holder.editClassroom.removeTextChangedListener(holder.classroomWatcher);

            String subject = lesson.getSubject() != null ? lesson.getSubject() : "";
            String teacher = lesson.getTeacher() != null ? lesson.getTeacher() : "";
            String classroom = lesson.getClassroom() != null ? lesson.getClassroom() : "";

            holder.editSubject.setText(subject);
            holder.editTeacher.setText(teacher);
            holder.editClassroom.setText(classroom);

            holder.subjectWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    lesson.setSubject(s != null ? s.toString() : "");
                }
            };
            holder.teacherWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    lesson.setTeacher(s != null ? s.toString() : "");
                }
            };
            holder.classroomWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    lesson.setClassroom(s != null ? s.toString() : "");
                }
            };

            holder.editSubject.addTextChangedListener(holder.subjectWatcher);
            holder.editTeacher.addTextChangedListener(holder.teacherWatcher);
            holder.editClassroom.addTextChangedListener(holder.classroomWatcher);

        } else if (isFreePeriod && !isAfterSchool) {
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

        // Keep subject indicator in a single consistent color defined in XML (no subject-based colors).

        if (shareEnabled && shareListener != null && !isFreePeriod) {
            holder.itemView.setOnLongClickListener(v -> {
                shareListener.onLessonShare(lesson);
                return true;
            });
        } else {
            holder.itemView.setOnLongClickListener(null);
        }

        // After-school events: show action row and wire up callbacks.
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

        // Tapping a regular (non-free, non–after-school) lesson opens edit dialog.
        if (!isAfterSchool && !isFreePeriod && lessonClickListener != null && !editMode) {
            holder.itemView.setOnClickListener(v -> lessonClickListener.onLessonClick(lesson));
        } else {
            holder.itemView.setOnClickListener(null);
        }

        // Ensure TextWatchers are detached when not editing to avoid updating recycled views.
        if (!showEditFields) {
            if (holder.subjectWatcher != null) holder.editSubject.removeTextChangedListener(holder.subjectWatcher);
            if (holder.teacherWatcher != null) holder.editTeacher.removeTextChangedListener(holder.teacherWatcher);
            if (holder.classroomWatcher != null) holder.editClassroom.removeTextChangedListener(holder.classroomWatcher);
            holder.subjectWatcher = null;
            holder.teacherWatcher = null;
            holder.classroomWatcher = null;
        }
    }

    @Override
    public int getItemCount() {
        return lessons.size();
    }

    static class LessonCardViewHolder extends RecyclerView.ViewHolder {
        View viewSubjectIndicator;
        TextView textLessonNumber, textSubject, textTeacher, textClassroom, textTime;
        TextInputLayout layoutEditSubject, layoutEditTeacher, layoutEditClassroom;
        TextInputEditText editSubject, editTeacher, editClassroom;
        TextWatcher subjectWatcher, teacherWatcher, classroomWatcher;
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
            editSubject = itemView.findViewById(R.id.editSubject);
            editTeacher = itemView.findViewById(R.id.editTeacher);
            editClassroom = itemView.findViewById(R.id.editClassroom);
            layoutAfterSchoolActions = itemView.findViewById(R.id.layoutAfterSchoolActions);
            checkAfterSchoolDone = itemView.findViewById(R.id.checkAfterSchoolDone);
            buttonAfterSchoolShare = itemView.findViewById(R.id.buttonAfterSchoolShare);
            buttonAfterSchoolReminder = itemView.findViewById(R.id.buttonAfterSchoolReminder);
            buttonAfterSchoolDelete = itemView.findViewById(R.id.buttonAfterSchoolDelete);
        }
    }
}
