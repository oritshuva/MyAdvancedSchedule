package com.example.myadvancedschedule;

// MainActivity is the authenticated entry point of the app.
// It centralizes high-level navigation (Tasks / School / After-School) so users
// can switch contexts without leaving the same activity or rebuilding shared UI.

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Gate this screen behind Firebase auth to keep all tab fragments in a
        // valid user context. Redirecting early avoids fragment initialization
        // with null-user state and prevents unnecessary Firestore calls.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupToolbar();
        setupViewPagerAndTabs();
    }

    private void setupToolbar() {
        // The toolbar is shared by all tabs, so title updates happen centrally
        // instead of duplicating header logic inside each fragment.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            updateToolbarTitle(0);
        }
    }

    private String currentUserDisplayName() {
        // Defensive accessor: callers can request a display name without needing
        // to duplicate null checks around Firebase user state.
        FirebaseUser u = mAuth.getCurrentUser();
        return u != null ? u.getDisplayName() : null;
    }

    private void setupViewPagerAndTabs() {
        // ViewPager2 + TabLayout provides a single cohesive navigation surface.
        // Each position maps to a stable app section, which keeps mental model
        // simple for the user and predictable for fragment lifecycle handling.
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager.setAdapter(new MainPagerAdapter(this));
        viewPager.setOffscreenPageLimit(2);
        // Keep neighboring fragments alive so tab switches feel instant and do not
        // repeatedly trigger expensive reload logic in onCreate/onViewCreated.
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.tab_tasks);
                    tab.setIcon(R.drawable.ic_task_list);
                    break;
                case 1:
                    tab.setText(R.string.tab_school);
                    tab.setIcon(R.drawable.ic_school);
                    break;
                case 2:
                    tab.setText(R.string.tab_after_school);
                    tab.setIcon(R.drawable.ic_after_school);
                    break;
                default:
                    break;
            }
        }).attach();
        // Keep top-level title synchronized with visible content; this gives
        // immediate orientation feedback as users switch between schedule types.

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateToolbarTitle(position);
            }
        });
    }

    private void updateToolbarTitle(int position) {
        // Resolve title by section index so localization is handled via resources
        // and all header labels stay consistent with tab ordering.
        if (getSupportActionBar() == null) return;
        int titleRes;
        switch (position) {
            case 0:
                titleRes = R.string.tasks_title;
                break;
            case 1:
                titleRes = R.string.school_schedule_title;
                break;
            case 2:
                titleRes = R.string.after_school_title;
                break;
            default:
                titleRes = R.string.app_name;
                break;
        }
        getSupportActionBar().setTitle(getString(titleRes));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Actions here are intentionally global: add lesson and logout are
        // relevant regardless of which tab is currently selected.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Keep command handling in the activity so fragments stay focused on
        // section-specific behavior rather than cross-app actions.
        int id = item.getItemId();
        if (id == R.id.action_add_lesson) {
            AddLessonDialogFragment dialog = AddLessonDialogFragment.newInstance();
            dialog.setOnLessonSavedListener(() -> { /* Fragments refresh on onResume */ });
            dialog.show(getSupportFragmentManager(), "AddLesson");
            return true;
        }
        if (id == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        // Explicit confirmation protects against accidental taps because logout
        // clears the active session and resets user navigation context.
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static class MainPagerAdapter extends FragmentStateAdapter {
        MainPagerAdapter(FragmentActivity fa) {
            // Adapter lifecycle follows the activity, ensuring tabs are recreated
            // correctly after configuration changes (rotation, process restore).
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Fixed tab-to-fragment mapping keeps navigation deterministic and
            // avoids state ambiguity when restoring pager position.
            switch (position) {
                case 0: return new TasksFragment();
                case 1: return new SchoolScheduleFragment();
                case 2: return new AfterSchoolScheduleFragment();
                default: return new TasksFragment();
            }
        }

        @Override
        public int getItemCount() {
            // Contract for pager + mediator: exactly three top-level sections.
            return 3;
        }
    }
}
