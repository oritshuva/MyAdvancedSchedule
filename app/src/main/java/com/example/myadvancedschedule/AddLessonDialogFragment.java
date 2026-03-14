package com.example.myadvancedschedule;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AddLessonDialogFragment extends DialogFragment {

    private EditText editSubject;
    private EditText editTeacher;
    private EditText editClassroom;
    private Spinner spinnerDay;
    private EditText editPeriod;
    private EditText editStartTime;
    private EditText editEndTime;

    private Lesson existingLesson;
    private OnLessonSavedListener listener;

    private static final String ARG_LESSON = "lesson";

    public interface OnLessonSavedListener {
        void onLessonSaved();
    }

    public static AddLessonDialogFragment newInstance() {
        return new AddLessonDialogFragment();
    }

    public static AddLessonDialogFragment newInstance(Lesson lesson) {
        AddLessonDialogFragment fragment = new AddLessonDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_LESSON, lesson);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            existingLesson = (Lesson) getArguments().getSerializable(ARG_LESSON);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_lesson, null);

        initializeViews(view);
        setupSpinner();

        if (existingLesson != null) {
            populateFields();
        }

        builder.setView(view)
                .setTitle(existingLesson == null ? R.string.add_lesson_title : R.string.edit_lesson_title)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> saveLesson());
        });

        return dialog;
    }

    private void initializeViews(View view) {
        editSubject = view.findViewById(R.id.editSubject);
        editTeacher = view.findViewById(R.id.editTeacher);
        editClassroom = view.findViewById(R.id.editClassroom);
        spinnerDay = view.findViewById(R.id.spinnerDay);
        editPeriod = view.findViewById(R.id.editPeriod);
        editStartTime = view.findViewById(R.id.editStartTime);
        editEndTime = view.findViewById(R.id.editEndTime);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.days_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDay.setAdapter(adapter);
    }

    private void populateFields() {
        editSubject.setText(existingLesson.getSubject());
        editTeacher.setText(existingLesson.getTeacher());
        editClassroom.setText(existingLesson.getClassroom());
        editPeriod.setText(String.valueOf(existingLesson.getPeriod()));
        editStartTime.setText(existingLesson.getStartTime());
        editEndTime.setText(existingLesson.getEndTime());

        String[] days = getResources().getStringArray(R.array.days_array);
        for (int i = 0; i < days.length; i++) {
            if (days[i].equals(existingLesson.getDay())) {
                spinnerDay.setSelection(i);
                break;
            }
        }
    }

    // 🔥 קבלת userId אמיתי
    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private void saveLesson() {
        if (!validateInputs()) {
            return;
        }

        String subject = editSubject.getText().toString().trim();
        String teacher = editTeacher.getText().toString().trim();
        String classroom = editClassroom.getText().toString().trim();
        String day = spinnerDay.getSelectedItem().toString();
        int period = Integer.parseInt(editPeriod.getText().toString().trim());
        String startTime = editStartTime.getText().toString().trim();
        String endTime = editEndTime.getText().toString().trim();

        // 🔥 קבלת userId אמיתי
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(getContext(), "Error: User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Lesson lesson;
        if (existingLesson != null) {
            lesson = new Lesson(
                    existingLesson.getId(),
                    subject,
                    teacher,
                    classroom,
                    day,
                    period,
                    startTime,
                    endTime
            );
        } else {
            lesson = new Lesson(subject, teacher, classroom, day, period, startTime, endTime);
        }

        FirestoreHelper firestoreHelper = new FirestoreHelper();

        if (existingLesson != null) {
            // 🔥 שימוש ב-userId אמיתי
            firestoreHelper.updateLesson(userId, lesson, new FirestoreHelper.OnOperationCompleteListener() {
                @Override
                public void onSuccess() {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.lesson_updated, Toast.LENGTH_SHORT).show();
                    }
                    if (listener != null) {
                        listener.onLessonSaved();
                    }
                    dismiss();
                }

                @Override
                public void onFailure(String error) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), getString(R.string.error_saving_lesson) + ": " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            // 🔥 שימוש ב-userId אמיתי
            firestoreHelper.addLesson(userId, lesson, new FirestoreHelper.OnOperationCompleteListener() {
                @Override
                public void onSuccess() {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.lesson_added, Toast.LENGTH_SHORT).show();
                    }
                    if (listener != null) {
                        listener.onLessonSaved();
                    }
                    dismiss();
                }

                @Override
                public void onFailure(String error) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), getString(R.string.error_saving_lesson) + ": " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private boolean validateInputs() {
        if (editSubject.getText().toString().trim().isEmpty()) {
            editSubject.setError(getString(R.string.error_empty_subject));
            return false;
        }

        if (editTeacher.getText().toString().trim().isEmpty()) {
            editTeacher.setError(getString(R.string.error_empty_teacher));
            return false;
        }

        if (editClassroom.getText().toString().trim().isEmpty()) {
            editClassroom.setError(getString(R.string.error_empty_classroom));
            return false;
        }

        String periodStr = editPeriod.getText().toString().trim();
        if (periodStr.isEmpty()) {
            editPeriod.setError(getString(R.string.error_invalid_period));
            return false;
        }

        try {
            int period = Integer.parseInt(periodStr);
            if (period < 1 || period > 15) {
                editPeriod.setError(getString(R.string.error_invalid_period));
                return false;
            }
        } catch (NumberFormatException e) {
            editPeriod.setError(getString(R.string.error_invalid_period));
            return false;
        }

        if (editStartTime.getText().toString().trim().isEmpty()) {
            editStartTime.setError(getString(R.string.error_invalid_time));
            return false;
        }

        if (editEndTime.getText().toString().trim().isEmpty()) {
            editEndTime.setError(getString(R.string.error_invalid_time));
            return false;
        }

        return true;
    }

    public void setOnLessonSavedListener(OnLessonSavedListener listener) {
        this.listener = listener;
    }
}