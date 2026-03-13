package com.example.myadvancedschedule;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class AddEventActivity extends AppCompatActivity {

    private TextInputEditText etEventTitle, etStartTime, etEndTime, etNote, etReminder;
    private Button btnSave, btnDelete, btnCancel;

    private String eventType, eventDay;
    private Event currentEvent;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        initViews();

        eventType = getIntent().getStringExtra("type");
        eventDay = getIntent().getStringExtra("day");
        isEditMode = getIntent().getBooleanExtra("edit_mode", false);

        if (isEditMode) {
            currentEvent = (Event) getIntent().getSerializableExtra("event");
            loadEventData();
            btnDelete.setVisibility(Button.VISIBLE);
        }

        setupButtons();
    }

    private void initViews() {
        etEventTitle = findViewById(R.id.etEventTitle);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        etNote = findViewById(R.id.etNote);
        etReminder = findViewById(R.id.etReminder);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        btnCancel = findViewById(R.id.btnCancel);

        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));
        etReminder.setOnClickListener(v -> showTimePicker(etReminder));
    }

    private void setupButtons() {
        btnSave.setOnClickListener(v -> saveEvent());
        btnDelete.setOnClickListener(v -> deleteEvent());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void showTimePicker(TextInputEditText editText) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    String time = String.format("%02d:%02d", hourOfDay, minute);
                    editText.setText(time);
                }, 8, 0, true);
        timePickerDialog.show();
    }

    private void loadEventData() {
        if (currentEvent != null) {
            etEventTitle.setText(currentEvent.getTitle());
            etStartTime.setText(currentEvent.getStartTime());
            etEndTime.setText(currentEvent.getEndTime());
            if (currentEvent.getNote() != null) {
                etNote.setText(currentEvent.getNote());
            }
            if (currentEvent.getReminderTime() != null) {
                etReminder.setText(currentEvent.getReminderTime());
            }
        }
    }

    private void saveEvent() {
        String title = etEventTitle.getText().toString().trim();
        String startTime = etStartTime.getText().toString().trim();
        String endTime = etEndTime.getText().toString().trim();
        String note = etNote.getText().toString().trim();
        String reminder = etReminder.getText().toString().trim();

        if (title.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            Toast.makeText(this, "נא למלא את כל השדות הנדרשים", Toast.LENGTH_SHORT).show();
            return;
        }

        Event event;
        if (isEditMode) {
            event = currentEvent;
            event.setTitle(title);
            event.setStartTime(startTime);
            event.setEndTime(endTime);
        } else {
            event = new Event(title, startTime, endTime, eventDay, eventType);
            event.setUserId(FirebaseHelper.getInstance().getCurrentUserId());
        }

        event.setNote(note);
        event.setReminderTime(reminder);

        if (isEditMode) {
            FirebaseHelper.getInstance().getEventsCollection()
                    .document(event.getId())
                    .set(event)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "האירוע עודכן בהצלחה", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "שגיאה בעדכון האירוע", Toast.LENGTH_SHORT).show();
                    });
        } else {
            FirebaseHelper.getInstance().getEventsCollection()
                    .add(event)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "האירוע נוסף בהצלחה", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "שגיאה בהוספת האירוע", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void deleteEvent() {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת אירוע")
                .setMessage("האם אתה בטוח שברצונך למחוק אירוע זה?")
                .setPositiveButton("מחק", (dialog, which) -> {
                    FirebaseHelper.getInstance().getEventsCollection()
                            .document(currentEvent.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "האירוע נמחק בהצלחה", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "שגיאה במחיקת האירוע", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}
