package com.example.myadvancedschedule;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class AddLessonDialogFragment extends DialogFragment {

    private Spinner spinnerSubject;
    private TextInputLayout layoutSubjectOther;
    private TextInputEditText editSubjectOther;
    private TextInputEditText editTeacher;
    private TextInputEditText editClassroom;
    private Spinner spinnerDay;
    private Spinner spinnerPeriod;
    private Spinner spinnerStartTime;
    private Spinner spinnerEndTime;

    private Lesson existingLesson;
    private OnLessonSavedListener listener;
    private List<String> subjectList = new ArrayList<>();
    private String otherLabel;

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
        loadSubjectsAndSetupSpinners();

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
            if (button != null) button.setOnClickListener(v -> saveLesson());
        });

        return dialog;
    }

    private void initializeViews(View view) {
        spinnerSubject = view.findViewById(R.id.spinnerSubject);
        layoutSubjectOther = view.findViewById(R.id.layoutSubjectOther);
        editSubjectOther = view.findViewById(R.id.editSubjectOther);
        editTeacher = view.findViewById(R.id.editTeacher);
        editClassroom = view.findViewById(R.id.editClassroom);
        spinnerDay = view.findViewById(R.id.spinnerDay);
        spinnerPeriod = view.findViewById(R.id.spinnerPeriod);
        spinnerStartTime = view.findViewById(R.id.spinnerStartTime);
        spinnerEndTime = view.findViewById(R.id.spinnerEndTime);
    }

    private void loadSubjectsAndSetupSpinners() {
        otherLabel = getString(R.string.subject_other);
        String userId = getCurrentUserId();
        if (userId == null) {
            setupSubjectSpinner(new ArrayList<>());
            setupOtherSpinners();
            return;
        }
        FirestoreHelper firestoreHelper = new FirestoreHelper();
        firestoreHelper.getSubjects(new FirestoreHelper.OnSubjectsLoadedListener() {
            @Override
            public void onSubjectsLoaded(List<String> subjects) {
                subjectList = subjects != null ? new ArrayList<>(subjects) : new ArrayList<>();
                setupSubjectSpinner(subjectList);
                setupOtherSpinners();
                if (existingLesson != null) populateFields();
            }
            @Override
            public void onError(String error) {
                setupSubjectSpinner(new ArrayList<>());
                setupOtherSpinners();
                if (existingLesson != null) populateFields();
            }
        });
    }

    private void setupSubjectSpinner(List<String> subjects) {
        List<String> options = new ArrayList<>(subjects);
        options.add(otherLabel);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(adapter);
        spinnerSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean isOther = otherLabel.equals(parent.getItemAtPosition(position));
                layoutSubjectOther.setVisibility(isOther ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupOtherSpinners() {
        // Day: English names
        ArrayAdapter<CharSequence> dayAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.days_array_english, android.R.layout.simple_spinner_item);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDay.setAdapter(dayAdapter);

        // Period: 1–15
        List<String> periods = new ArrayList<>();
        for (int i = 1; i <= 15; i++) periods.add(String.valueOf(i));
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, periods);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriod.setAdapter(periodAdapter);

        // Time options: 07:00 to 22:00, 15-min steps
        List<String> times = new ArrayList<>();
        for (int h = 7; h <= 22; h++) {
            for (int m = 0; m < 60; m += 15) {
                if (h == 22 && m > 0) break;
                times.add(String.format("%02d:%02d", h, m));
            }
        }
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, times);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStartTime.setAdapter(timeAdapter);
        spinnerEndTime.setAdapter(timeAdapter);
    }

    private void populateFields() {
        if (existingLesson == null) return;
        editTeacher.setText(existingLesson.getTeacher());
        editClassroom.setText(existingLesson.getClassroom());
        spinnerPeriod.setSelection(Math.max(0, Math.min(existingLesson.getPeriod() - 1, 14)));
        setSpinnerToValue(spinnerDay, existingLesson.getDay());
        setSpinnerToValue(spinnerStartTime, existingLesson.getStartTime());
        setSpinnerToValue(spinnerEndTime, existingLesson.getEndTime());
        String subj = existingLesson.getSubject();
        if (subj != null && !subj.isEmpty()) {
            int idx = subjectList.indexOf(subj);
            if (idx >= 0) spinnerSubject.setSelection(idx);
            else {
                spinnerSubject.setSelection(subjectList.size());
                layoutSubjectOther.setVisibility(View.VISIBLE);
                editSubjectOther.setText(subj);
            }
        }
    }

    private boolean isOtherSelected() {
        return spinnerSubject.getSelectedItem() != null && otherLabel.equals(spinnerSubject.getSelectedItem().toString());
    }

    private void setSpinnerToValue(Spinner spinner, String value) {
        if (value == null) return;
        ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinner.getAdapter();
        if (adapter == null) return;
        for (int i = 0; i < adapter.getCount(); i++) {
            if (value.equals(adapter.getItem(i).toString())) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private String getSelectedSubject() {
        Object sel = spinnerSubject.getSelectedItem();
        if (sel != null && otherLabel.equals(sel.toString())) {
            return editSubjectOther.getText() != null ? editSubjectOther.getText().toString().trim() : "";
        }
        return sel != null ? sel.toString() : "";
    }

    private void saveLesson() {
        if (!validateInputs()) return;

        String subject = getSelectedSubject();
        String teacher = editTeacher.getText() != null ? editTeacher.getText().toString().trim() : "";
        String classroom = editClassroom.getText() != null ? editClassroom.getText().toString().trim() : "";
        String day = spinnerDay.getSelectedItem() != null ? spinnerDay.getSelectedItem().toString() : "";
        int period = Integer.parseInt(spinnerPeriod.getSelectedItem().toString());
        String startTime = spinnerStartTime.getSelectedItem() != null ? spinnerStartTime.getSelectedItem().toString() : "";
        String endTime = spinnerEndTime.getSelectedItem() != null ? spinnerEndTime.getSelectedItem().toString() : "";

        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(getContext(), getString(R.string.error_saving_lesson), Toast.LENGTH_SHORT).show();
            return;
        }

        // If "Other" and new subject, save to Firestore
        if (isOtherSelected() && !subject.isEmpty()) {
            FirestoreHelper fh = new FirestoreHelper();
            fh.addSubject(userId, subject, new FirestoreHelper.OnOperationCompleteListener() {
                @Override
                public void onSuccess() {
                    saveLessonToFirestore(userId, subject, teacher, classroom, day, period, startTime, endTime);
                }
                @Override
                public void onFailure(String error) {
                    saveLessonToFirestore(userId, subject, teacher, classroom, day, period, startTime, endTime);
                }
            });
        } else {
            saveLessonToFirestore(userId, subject, teacher, classroom, day, period, startTime, endTime);
        }
    }

    private void saveLessonToFirestore(String userId, String subject, String teacher, String classroom,
                                       String day, int period, String startTime, String endTime) {
        Lesson lesson;
        if (existingLesson != null) {
            lesson = new Lesson(existingLesson.getId(), subject, teacher, classroom, day, period, startTime, endTime);
        } else {
            lesson = new Lesson(subject, teacher, classroom, day, period, startTime, endTime);
        }
        FirestoreHelper firestoreHelper = new FirestoreHelper();
        FirestoreHelper.OnOperationCompleteListener done = new FirestoreHelper.OnOperationCompleteListener() {
            @Override
            public void onSuccess() {
                if (getContext() != null) {
                    Toast.makeText(getContext(), existingLesson != null ? R.string.lesson_updated : R.string.lesson_added, Toast.LENGTH_SHORT).show();
                }
                if (listener != null) listener.onLessonSaved();
                dismiss();
            }
            @Override
            public void onFailure(String error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), getString(R.string.error_saving_lesson) + ": " + error, Toast.LENGTH_SHORT).show();
                }
            }
        };
        if (existingLesson != null) {
            firestoreHelper.updateLesson(userId, lesson, done);
        } else {
            firestoreHelper.addLesson(userId, lesson, done);
        }
    }

    private boolean validateInputs() {
        String subject = getSelectedSubject();
        if (subject.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.error_empty_subject), Toast.LENGTH_SHORT).show();
            return false;
        }
        String teacher = editTeacher.getText() != null ? editTeacher.getText().toString().trim() : "";
        if (teacher.isEmpty()) {
            editTeacher.setError(getString(R.string.error_empty_teacher));
            return false;
        }
        editTeacher.setError(null);
        String classroom = editClassroom.getText() != null ? editClassroom.getText().toString().trim() : "";
        if (classroom.isEmpty()) {
            editClassroom.setError(getString(R.string.error_empty_classroom));
            return false;
        }
        editClassroom.setError(null);
        return true;
    }

    public void setOnLessonSavedListener(OnLessonSavedListener listener) {
        this.listener = listener;
    }
}
