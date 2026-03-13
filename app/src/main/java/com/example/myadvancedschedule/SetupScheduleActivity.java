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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetupScheduleActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private MaterialButton btnNext, btnPrevious;
    private TextView tvTitle, tvSubtitle;
    private ProgressBar progressBar;

    private List<Fragment> fragments = new ArrayList<>();
    private Map<String, Integer> lessonsPerDay = new HashMap<>();
    private Map<String, List<Lesson>> scheduleData = new HashMap<>();

    private String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday"};
    private int currentStep = 0;
    private int totalSteps = 6; // 1 selection page + 5 days

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_schedule);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupViewPager();
        setupNavigationButtons();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);
        btnPrevious = findViewById(R.id.btnPrevious);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        progressBar = findViewById(R.id.progressBar);

        // Disable user swiping - navigation only via buttons
        viewPager.setUserInputEnabled(false);
    }

    private void setupViewPager() {
        // Add selection fragment (first page)
        fragments.add(new SelectLessonsCountFragment());

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
        btnNext.setOnClickListener(v -> handleNext());
        btnPrevious.setOnClickListener(v -> handlePrevious());
    }

    private void handleNext() {
        if (currentStep == 0) {
            // Validate and get lesson counts
            SelectLessonsCountFragment fragment = (SelectLessonsCountFragment) fragments.get(0);
            lessonsPerDay = fragment.getLessonCounts();

            if (lessonsPerDay.isEmpty() || !validateLessonCounts()) {
                Toast.makeText(this, "Please select at least one lesson for each day", Toast.LENGTH_SHORT).show();
                return;
            }

            // Generate fragments for each day
            generateDayFragments();
            viewPager.getAdapter().notifyDataSetChanged();
            viewPager.setCurrentItem(1, true);

        } else if (currentStep < totalSteps - 1) {
            // Save current day's data
            FillLessonsFragment fragment = (FillLessonsFragment) fragments.get(currentStep);
            List<Lesson> lessons = fragment.getLessons();

            if (!validateLessons(lessons)) {
                Toast.makeText(this, "Please fill all lesson details", Toast.LENGTH_SHORT).show();
                return;
            }

            String dayName = days[currentStep - 1];
            scheduleData.put(dayName, lessons);

            viewPager.setCurrentItem(currentStep + 1, true);

        } else {
            // Last step - save to Firebase
            saveScheduleToFirebase();
        }
    }

    private void handlePrevious() {
        if (currentStep > 0) {
            viewPager.setCurrentItem(currentStep - 1, true);
        }
    }

    private void generateDayFragments() {
        fragments.clear();
        fragments.add(new SelectLessonsCountFragment()); // Keep selection page

        for (String day : days) {
            int lessonCount = lessonsPerDay.get(day);
            FillLessonsFragment fragment = FillLessonsFragment.newInstance(day, lessonCount);
            fragments.add(fragment);
        }

        totalSteps = fragments.size();
        progressBar.setMax(totalSteps);
    }

    private boolean validateLessonCounts() {
        for (Integer count : lessonsPerDay.values()) {
            if (count == null || count <= 0) {
                return false;
            }
        }
        return true;
    }

    private boolean validateLessons(List<Lesson> lessons) {
        if (lessons == null || lessons.isEmpty()) {
            return false;
        }

        for (Lesson lesson : lessons) {
            if (lesson.getSubject() == null || lesson.getSubject().trim().isEmpty() ||
                    lesson.getStartTime() == null || lesson.getStartTime().trim().isEmpty() ||
                    lesson.getEndTime() == null || lesson.getEndTime().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void saveScheduleToFirebase() {
        String userId = auth.getCurrentUser().getUid();

        Map<String, Object> scheduleMap = new HashMap<>();
        for (Map.Entry<String, List<Lesson>> entry : scheduleData.entrySet()) {
            String day = entry.getKey();
            List<Lesson> lessons = entry.getValue();

            List<Map<String, Object>> lessonsList = new ArrayList<>();
            for (Lesson lesson : lessons) {
                Map<String, Object> lessonMap = new HashMap<>();
                lessonMap.put("subject", lesson.getSubject());
                lessonMap.put("startTime", lesson.getStartTime());
                lessonMap.put("endTime", lesson.getEndTime());
                lessonMap.put("periodNumber", lesson.getPeriodNumber());
                lessonsList.add(lessonMap);
            }

            scheduleMap.put(day, lessonsList);
        }

        db.collection("users").document(userId).collection("schedule")
                .document("weekSchedule")
                .set(scheduleMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Schedule saved successfully! 🎉", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving schedule: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateUI() {
        // Update progress
        progressBar.setProgress(currentStep + 1);
        tvSubtitle.setText("Step " + (currentStep + 1) + " of " + totalSteps);

        // Update title
        if (currentStep == 0) {
            tvTitle.setText("Create Your Schedule");
        } else {
            tvTitle.setText("Fill " + days[currentStep - 1] + " Lessons");
        }

        // Update buttons
        btnPrevious.setVisibility(currentStep > 0 ? View.VISIBLE : View.GONE);

        if (currentStep == totalSteps - 1) {
            btnNext.setText("Save Schedule");
            btnNext.setIcon(getDrawable(android.R.drawable.ic_menu_save));
        } else {
            btnNext.setText("Next");
            btnNext.setIcon(null);
        }
    }

    @Override
    public void onBackPressed() {
        if (currentStep > 0) {
            handlePrevious();
        } else {
            super.onBackPressed();
        }
    }
}
