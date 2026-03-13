package com.example.myadvancedschedule;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnAddLesson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnAddLesson = findViewById(R.id.btnAddLesson);
        btnAddLesson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddLessonDialog();
            }
        });
    }

    private void showAddLessonDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_lesson, null);

        // פרטי השדות מהדיאלוג
        EditText etSubject = dialogView.findViewById(R.id.etSubject);
        EditText etStartTime = dialogView.findViewById(R.id.etStartTime);
        EditText etEndTime = dialogView.findViewById(R.id.etEndTime);
        Spinner spDay = dialogView.findViewById(R.id.spDay);

        builder.setView(dialogView)
                .setTitle("Add Lesson")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String subject = etSubject.getText().toString();
                    String startTime = etStartTime.getText().toString();
                    String endTime = etEndTime.getText().toString();
                    String day = spDay.getSelectedItem().toString();

                    // הוסף כאן לוגיקת השמירה ל-Firebase Firestore
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
