package com.example.myadvancedschedule;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the RecyclerView in DayScheduleFragment.
 * Each item is a lesson card with time slot and subject/teacher/classroom fields.
 * Lesson times are pre-set; user fills subject, teacher, classroom.
 */
public class LessonSetupCardAdapter extends RecyclerView.Adapter<LessonSetupCardAdapter.CardViewHolder> {

    private final List<Lesson> lessons;

    public LessonSetupCardAdapter(List<Lesson> lessons) {
        this.lessons = lessons != null ? new ArrayList<>(lessons) : new ArrayList<>();
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lesson_setup_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Lesson lesson = lessons.get(position);
        holder.setLesson(lesson);
        holder.textTimeSlot.setText(lesson.getStartTime() + " - " + lesson.getEndTime());
        holder.editSubject.setText(lesson.getSubject() != null ? lesson.getSubject() : "");
        holder.editTeacher.setText(lesson.getTeacher() != null ? lesson.getTeacher() : "");
        holder.editClassroom.setText(lesson.getClassroom() != null ? lesson.getClassroom() : "");
    }

    @Override
    public int getItemCount() {
        return lessons.size();
    }

    public List<Lesson> getLessons() {
        return new ArrayList<>(lessons);
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView textTimeSlot;
        TextInputEditText editSubject, editTeacher, editClassroom;
        Lesson lesson;

        private final TextWatcher subjectWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (lesson != null) lesson.setSubject(s != null ? s.toString().trim() : "");
            }
        };
        private final TextWatcher teacherWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (lesson != null) lesson.setTeacher(s != null ? s.toString().trim() : "");
            }
        };
        private final TextWatcher classroomWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (lesson != null) lesson.setClassroom(s != null ? s.toString().trim() : "");
            }
        };

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            textTimeSlot = itemView.findViewById(R.id.textTimeSlot);
            editSubject = itemView.findViewById(R.id.editSubject);
            editTeacher = itemView.findViewById(R.id.editTeacher);
            editClassroom = itemView.findViewById(R.id.editClassroom);
            editSubject.addTextChangedListener(subjectWatcher);
            editTeacher.addTextChangedListener(teacherWatcher);
            editClassroom.addTextChangedListener(classroomWatcher);
        }

        void setLesson(Lesson lesson) {
            this.lesson = lesson;
        }
    }
}
