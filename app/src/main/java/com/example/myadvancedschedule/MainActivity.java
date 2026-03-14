package com.example.myadvancedschedule;

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
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            String name = currentUserDisplayName();
            getSupportActionBar().setTitle(name != null && !name.isEmpty() ? getString(R.string.app_name) + " – " + name : getString(R.string.app_name));
        }
    }

    private String currentUserDisplayName() {
        FirebaseUser u = mAuth.getCurrentUser();
        return u != null ? u.getDisplayName() : null;
    }

    private void setupViewPagerAndTabs() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager.setAdapter(new MainPagerAdapter(this));
        viewPager.setOffscreenPageLimit(2);
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new TasksFragment();
                case 1: return new SchoolScheduleFragment();
                case 2: return new AfterSchoolScheduleFragment();
                default: return new TasksFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
