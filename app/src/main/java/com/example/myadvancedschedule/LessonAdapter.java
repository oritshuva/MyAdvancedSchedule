package com.example.myadvancedschedule;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

// Basic lesson list adapter used where a compact textual representation is enough.
// Click and long-click callbacks let host screens choose between view/edit actions.

public class LessonAdapter extends RecyclerView.Adapter<LessonAdapter.LessonViewHolder> {
    private List<Lesson> lessons;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Lesson lesson);
        void onItemLongClick(Lesson lesson);
    }

    public LessonAdapter(List<Lesson> lessons) {
        // Data is managed by host; adapter only renders and forwards interactions.
        this.lessons = lessons;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public LessonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lesson, parent, false);
        return new LessonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LessonViewHolder holder, int position) {
        // Binding is delegated to holder to keep adapter logic minimal.
        Lesson lesson = lessons.get(position);
        holder.bind(lesson, listener);
    }

    @Override
    public int getItemCount() {
        return lessons.size();
    }

    public static class LessonViewHolder extends RecyclerView.ViewHolder {
        TextView subjectTextView;
        TextView teacherTextView;
        TextView classroomTextView;
        TextView dayTextView;
        TextView periodTextView;

        public LessonViewHolder(@NonNull View itemView) {
            super(itemView);
            subjectTextView = itemView.findViewById(R.id.textSubject);
            teacherTextView = itemView.findViewById(R.id.textTeacher);
            classroomTextView = itemView.findViewById(R.id.textClassroom);
            dayTextView = itemView.findViewById(R.id.textDay);
            periodTextView = itemView.findViewById(R.id.textPeriod);
        }

        public void bind(Lesson lesson, OnItemClickListener listener) {
            // Render key lesson fields directly for quick list scanning.
            subjectTextView.setText(lesson.getSubject());
            teacherTextView.setText(lesson.getTeacher());
            classroomTextView.setText(lesson.getClassroom());
            dayTextView.setText(lesson.getDay());
            periodTextView.setText(String.valueOf(lesson.getPeriod()));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(lesson);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onItemLongClick(lesson);
                }
                return true;
            });
        }
    }
}
