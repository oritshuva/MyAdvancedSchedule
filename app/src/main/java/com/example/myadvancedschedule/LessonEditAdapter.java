package com.example.myadvancedschedule;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
        View view = LayoutInflater.from(context).inflate(R.layout.item_edit_lesson, parent, false);
        return new LessonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LessonViewHolder holder, int position) {
        Lesson lesson = lessons.get(position);

        // קביעת ערכים בהתאם לשיעור
        holder.editLessonName.setText(lesson.getName());
        holder.editTeacherName.setText(lesson.getTeacher());
        holder.editRoomNumber.setText(lesson.getRoom());
    }

    @Override
    public int getItemCount() {
        return lessons.size();
    }

    class LessonViewHolder extends RecyclerView.ViewHolder {
        EditText editLessonName;
        EditText editTeacherName;
        EditText editRoomNumber;

        public LessonViewHolder(@NonNull View itemView) {
            super(itemView);
            editLessonName = itemView.findViewById(R.id.editLessonName);
            editTeacherName = itemView.findViewById(R.id.editTeacherName);
            editRoomNumber = itemView.findViewById(R.id.editRoomNumber);
        }
    }
}
