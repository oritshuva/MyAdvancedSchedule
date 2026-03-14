package com.example.myadvancedschedule;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LessonEditAdapter extends RecyclerView.Adapter<LessonEditAdapter.LessonViewHolder> {
    private final Context context;
    private final List<Lesson> lessons;

    public LessonEditAdapter(Context context, List<Lesson> lessons) {
        this.context = context;
        this.lessons = lessons != null ? new ArrayList<>(lessons) : new ArrayList<>();
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
        holder.lesson = lesson;
        holder.editSubject.removeTextChangedListener(holder.subjectWatcher);
        holder.editTeacher.removeTextChangedListener(holder.teacherWatcher);
        holder.editClassroom.removeTextChangedListener(holder.classroomWatcher);
        holder.editSubject.setText(lesson.getSubject() != null ? lesson.getSubject() : "");
        holder.editTeacher.setText(lesson.getTeacher() != null ? lesson.getTeacher() : "");
        holder.editClassroom.setText(lesson.getClassroom() != null ? lesson.getClassroom() : "");
        holder.editSubject.addTextChangedListener(holder.subjectWatcher);
        holder.editTeacher.addTextChangedListener(holder.teacherWatcher);
        holder.editClassroom.addTextChangedListener(holder.classroomWatcher);
    }

    @Override
    public void onViewRecycled(@NonNull LessonViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.lesson != null) {
            holder.lesson.setSubject(holder.editSubject.getText() != null ? holder.editSubject.getText().toString().trim() : "");
            holder.lesson.setTeacher(holder.editTeacher.getText() != null ? holder.editTeacher.getText().toString().trim() : "");
            holder.lesson.setClassroom(holder.editClassroom.getText() != null ? holder.editClassroom.getText().toString().trim() : "");
        }
    }

    @Override
    public int getItemCount() {
        return lessons.size();
    }

    public List<Lesson> getLessons() {
        return new ArrayList<>(lessons);
    }

    static class LessonViewHolder extends RecyclerView.ViewHolder {
        EditText editSubject, editTeacher, editClassroom;
        Lesson lesson;
        private final TextWatcher subjectWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (lesson != null) lesson.setSubject(s != null ? s.toString().trim() : "");
            }
        };
        private final TextWatcher teacherWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (lesson != null) lesson.setTeacher(s != null ? s.toString().trim() : "");
            }
        };
        private final TextWatcher classroomWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (lesson != null) lesson.setClassroom(s != null ? s.toString().trim() : "");
            }
        };

        LessonViewHolder(@NonNull View itemView) {
            super(itemView);
            editSubject = itemView.findViewById(R.id.editSubject);
            editTeacher = itemView.findViewById(R.id.editTeacher);
            editClassroom = itemView.findViewById(R.id.editClassroom);
        }
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
