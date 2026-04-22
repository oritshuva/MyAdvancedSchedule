package com.example.myadvancedschedule;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

// Multi-step setup wizard that collects weekly frame settings, builds per-day lesson forms,
// computes period time slots, and saves the initial schedule to Firestore.

/**
 * Setup wizard: Step 1 = FrameSetupFragment, Step 2 = ViewPager2 of DayScheduleFragment per day.
 * On save, writes each lesson to Firestore via FirestoreHelper (lessons collection).
 */
public class SetupScheduleActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private MaterialButton btnNext, btnPrevious;
    private TextView tvTitle, tvSubtitle;
    private ProgressBar progressBar;

    private List<Fragment> fragments = new ArrayList<>();
    private FrameSetupData frameData;
    private int currentStep = 0;
    private int totalSteps = 1;

    private FirebaseAuth auth;
    private FirestoreHelper firestoreHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize Firebase helpers and boot the setup flow from step 1.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_schedule);

        auth = FirebaseAuth.getInstance();
        firestoreHelper = new FirestoreHelper();

        initViews();
        setupStep1();
        setupNavigationButtons();
    }

    private void initViews() {
        // Bind layout controls and lock pager swiping until day pages are generated.
        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);
        btnPrevious = findViewById(R.id.btnPrevious);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        progressBar = findViewById(R.id.progressBar);

        viewPager.setUserInputEnabled(false);
        // Swiping enabled in Step 2 when day fragments are shown
        progressBar.setMax(1);
        progressBar.setProgress(1);
    }

    private void setupStep1() {
        // Start with the weekly-frame fragment as the first mandatory step.
        fragments.clear();
        fragments.add(new FrameSetupFragment());
        totalSteps = 1;
        setupViewPager();
        updateUI();
    }

    private void setupViewPager() {
        // Provide fragments to ViewPager and mirror page changes into UI state.
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return fragments.get(position);
            }

            @Override
            public int getItemCount() {
                return fragments.size();
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentStep = position;
                updateUI();
            }
        });
    }

    private void setupNavigationButtons() {
        // Wire forward/back navigation for the wizard flow.
        btnNext.setOnClickListener(v -> handleNext());
        btnPrevious.setOnClickListener(v -> handlePrevious());
    }

    private void handleNext() {
        // Validate current step, advance in wizard, or persist all lessons on final step.
        if (currentStep == 0) {
            FrameSetupFragment fragment = (FrameSetupFragment) fragments.get(0);
            frameData = fragment.getFrameSetupData();
            if (frameData == null) {
                Toast.makeText(this, getString(R.string.setup_error_select_days), Toast.LENGTH_SHORT).show();
                return;
            }
            if (frameData.getSelectedDays().isEmpty()) {
                Toast.makeText(this, getString(R.string.setup_error_select_days), Toast.LENGTH_SHORT).show();
                return;
            }
            buildDayFragments();
            viewPager.setAdapter(new FragmentStateAdapter(this) {
                @NonNull
                @Override
                public Fragment createFragment(int position) {
                    return fragments.get(position);
                }

                @Override
                public int getItemCount() {
                    return fragments.size();
                }
            });
            viewPager.setCurrentItem(1, true);
            currentStep = 1;
            totalSteps = fragments.size();
            progressBar.setMax(totalSteps);
            progressBar.setProgress(2);
            viewPager.setUserInputEnabled(true);
            updateUI();
        } else if (currentStep < totalSteps - 1) {
            viewPager.setCurrentItem(currentStep + 1, true);
            progressBar.setProgress(currentStep + 2);
            updateUI();
        } else {
            saveScheduleToFirestore();
        }
    }

    private void handlePrevious() {
        // Move one step back while preserving current fragment state.
        if (currentStep > 0) {
            viewPager.setCurrentItem(currentStep - 1, true);
            progressBar.setProgress(currentStep);
            updateUI();
        }
    }

    private void buildDayFragments() {
        // Rebuild fragment sequence: frame step + one day editor per selected weekday.
        fragments.clear();
        fragments.add(new FrameSetupFragment());

        ArrayList<TimeSlot> slots = computeTimeSlots(
                frameData.getStartTime(),
                frameData.getLessonDurationMinutes(),
                frameData.getBreakDurationMinutes(),
                frameData.getMaxLessons()
        );

        for (String day : frameData.getSelectedDays()) {
            fragments.add(DayScheduleFragment.newInstance(day, slots));
        }
    }

    /**
     * Computes time slots from start time, lesson duration, break duration, and max lessons.
     * e.g. 08:00, 45, 10, 8 -> 08:00-08:45, 08:55-09:40, ...
     */
    static ArrayList<TimeSlot> computeTimeSlots(String startTime, int lessonMin, int breakMin, int maxLessons) {
        // Convert frame settings into sequential start/end time ranges for each period.
        ArrayList<TimeSlot> result = new ArrayList<>();
        int[] start = parseTime(startTime);
        if (start == null) return result;
        int hour = start[0];
        int minute = start[1];

        for (int i = 0; i < maxLessons; i++) {
            String startStr = formatTime(hour, minute);
            minute += lessonMin;
            hour += minute / 60;
            minute = minute % 60;
            String endStr = formatTime(hour, minute);
            result.add(new TimeSlot(startStr, endStr));
            minute += breakMin;
            hour += minute / 60;
            minute = minute % 60;
        }
        return result;
    }

    private static int[] parseTime(String time) {
        // Parse HH:mm user input into numeric hour/minute pair.
        if (time == null || !time.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) return null;
        String[] parts = time.split(":");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }

    private static String formatTime(int hour, int minute) {
        // Normalize time output to two-digit HH:mm format.
        return String.format("%02d:%02d", hour % 24, minute % 60);
    }

    private void saveScheduleToFirestore() {
        // Collect all lessons from day fragments and start sequential persistence.
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnNext.setEnabled(false);

        List<Lesson> allLessons = new ArrayList<>();
        for (int i = 1; i < fragments.size(); i++) {
            Fragment f = fragments.get(i);
            if (f instanceof DayScheduleFragment) {
                allLessons.addAll(((DayScheduleFragment) f).getLessons());
            }
        }

        saveLessonsOneByOne(userId, allLessons, 0);
    }

    private void saveLessonsOneByOne(String userId, List<Lesson> lessons, int index) {
        // Save lessons in order so each failure is reported immediately and clearly.
        if (index >= lessons.size()) {
            progressBar.setVisibility(View.GONE);
            btnNext.setEnabled(true);
            Toast.makeText(this, "Schedule saved successfully!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        Lesson lesson = lessons.get(index);
        firestoreHelper.addLesson(userId, lesson, new FirestoreHelper.OnOperationCompleteListener() {
            @Override
            public void onSuccess() {
                saveLessonsOneByOne(userId, lessons, index + 1);
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                btnNext.setEnabled(true);
                Toast.makeText(SetupScheduleActivity.this, "Error saving: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateUI() {
        // Refresh wizard labels, progress, and button behavior for the active step.
        progressBar.setProgress(currentStep + 1);
        progressBar.setMax(Math.max(1, totalSteps));
        tvSubtitle.setText(getString(R.string.setup_step_format, currentStep + 1, totalSteps));

        if (currentStep == 0) {
            tvTitle.setText("Step 1 — Weekly frame");
        } else {
            String dayName = frameData != null && currentStep - 1 < frameData.getSelectedDays().size()
                    ? frameData.getSelectedDays().get(currentStep - 1)
                    : "Day";
            tvTitle.setText("Step 2 — " + dayName);
        }

        btnPrevious.setVisibility(currentStep > 0 ? View.VISIBLE : View.GONE);

        if (currentStep == totalSteps - 1 && totalSteps > 1) {
            btnNext.setText("Save schedule");
            btnNext.setIcon(getDrawable(android.R.drawable.ic_menu_save));
        } else {
            btnNext.setText("Next");
            btnNext.setIcon(null);
        }
    }

    @Override
    public void onBackPressed() {
        // Use wizard back navigation before exiting the setup activity itself.
        if (currentStep > 0) {
            handlePrevious();
        } else {
            super.onBackPressed();
        }
    }
}
