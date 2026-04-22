package com.example.myadvancedschedule;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

// Event editor screen for creating or updating after-school events.
// It keeps form entry, persistence, and reminder scheduling in one flow so users
// can finish event setup without navigating across multiple screens.

public class AddEventActivity extends AppCompatActivity {

    private TextInputEditText etEventTitle, etStartTime, etEndTime, etNote, etReminder;
    private Button btnSave, btnDelete, btnCancel;

    private String eventType, eventDay;
    private Event currentEvent;
    private boolean isEditMode = false;
    private long reminderTriggerAtMillis = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Read mode flags from intent so one activity supports both add and edit UX.
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
        // Bind form controls and attach time/reminder pickers for structured input.
        etEventTitle = findViewById(R.id.etEventTitle);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        etNote = findViewById(R.id.etNote);
        etReminder = findViewById(R.id.etReminder);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        btnCancel = findViewById(R.id.btnCancel);

        etStartTime.setOnClickListener(v -> {
            java.util.Calendar now = java.util.Calendar.getInstance();
            TimePickerDialog dialog = new TimePickerDialog(this,
                    (view, hourOfDay, minute) -> {
                        String time = String.format("%02d:%02d", hourOfDay, minute);
                        etStartTime.setText(time);
                    },
                    now.get(java.util.Calendar.HOUR_OF_DAY),
                    now.get(java.util.Calendar.MINUTE),
                    true);
            dialog.show();
        });
        etEndTime.setOnClickListener(v -> {
            java.util.Calendar now = java.util.Calendar.getInstance();
            TimePickerDialog dialog = new TimePickerDialog(this,
                    (view, hourOfDay, minute) -> {
                        String time = String.format("%02d:%02d", hourOfDay, minute);
                        etEndTime.setText(time);
                    },
                    now.get(java.util.Calendar.HOUR_OF_DAY),
                    now.get(java.util.Calendar.MINUTE),
                    true);
            dialog.show();
        });
        etReminder.setOnClickListener(v -> {
            ReminderDialogFragment dialog = ReminderDialogFragment.newInstance();
            dialog.setOnReminderConfirmedListener((triggerAtMillis, noteText) -> {
                // Keep selected timestamp in memory for alarm scheduling after persistence.
                reminderTriggerAtMillis = triggerAtMillis;
                // Show chosen date/time in the reminder field for user feedback.
                java.text.DateFormat df =
                        new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                etReminder.setText(df.format(new java.util.Date(triggerAtMillis)));
                // Also keep the note text in the local UI field so it will be saved with the event.
                etNote.setText(noteText);
            });
            dialog.show(getSupportFragmentManager(), "EventReminderDialog");
        });
    }

    private void setupButtons() {
        // Centralized button wiring keeps entry points for save/delete/cancel predictable.
        btnSave.setOnClickListener(v -> saveEvent());
        btnDelete.setOnClickListener(v -> deleteEvent());
        btnCancel.setOnClickListener(v -> finish());
    }

    // Legacy date/time picker methods for reminders have been replaced
    // by the unified ReminderDialogFragment used across the app.

    private void loadEventData() {
        // Prefill form when editing to minimize user re-entry and reduce mistakes.
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
        // Validate required fields early to avoid incomplete documents in Firestore.
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
            // Preserve original event identity and update only mutable fields.
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
            // Use set() for edit path to keep document ID stable across updates.
            FirebaseHelper.getInstance().getEventsCollection()
                    .document(event.getId())
                    .set(event)
                    .addOnSuccessListener(aVoid -> {
                        if (reminderTriggerAtMillis > 0) {
                            // Schedule reminder only after Firestore write succeeds,
                            // so notifications never reference unsaved event state.
                            ReminderUtils.scheduleEventReminder(this, event, reminderTriggerAtMillis, event.getNote());
                        }
                        Toast.makeText(this, "האירוע עודכן בהצלחה", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "שגיאה בעדכון האירוע", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Add path creates a new Firestore document, then stores returned ID in model.
            FirebaseHelper.getInstance().getEventsCollection()
                    .add(event)
                    .addOnSuccessListener(documentReference -> {
                        event.setId(documentReference.getId());
                        if (reminderTriggerAtMillis > 0) {
                            ReminderUtils.scheduleEventReminder(this, event, reminderTriggerAtMillis, event.getNote());
                        }
                        Toast.makeText(this, "האירוע נוסף בהצלחה", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "שגיאה בהוספת האירוע", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void deleteEvent() {
        // Destructive action uses confirmation dialog to prevent accidental data loss.
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
