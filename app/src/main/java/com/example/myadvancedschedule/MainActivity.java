package com.example.myadvancedschedule;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LessonAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private LessonAdapter adapter;
    private List<Lesson> lessonList;
    private FloatingActionButton fabAdd;
    private LinearLayout emptyView;  // 🔥 שינוי ל-LinearLayout
    private ProgressBar progressBar;
    private FirestoreHelper firestoreHelper;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // אתחול Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // בדיקה אם המשתמש מחובר
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // אם אין משתמש מחובר, מעבר למסך התחברות
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // 🔥 הגדרת Toolbar
        setupToolbar();

        // אתחול UI
        initializeViews();

        // אתחול Firestore
        firestoreHelper = new FirestoreHelper();

        // טעינת שיעורים
        loadLessons();
    }

    // 🔥 הגדרת Toolbar
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && getSupportActionBar() != null) {
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                getSupportActionBar().setTitle("שלום, " + displayName);
            }
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        fabAdd = findViewById(R.id.fabAddLesson);  // 🔥 תוקן
        emptyView = findViewById(R.id.emptyView);
        progressBar = findViewById(R.id.progressBar);

        // הגדרת RecyclerView
        lessonList = new ArrayList<>();
        adapter = new LessonAdapter(lessonList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // לחצן הוספה
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddLessonDialog();
            }
        });
    }

    private void loadLessons() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        firestoreHelper.getAllLessons(new FirestoreHelper.OnLessonsLoadedListener() {
            @Override
            public void onLessonsLoaded(List<Lesson> lessons) {
                progressBar.setVisibility(View.GONE);

                lessonList.clear();
                lessonList.addAll(lessons);
                adapter.notifyDataSetChanged();

                if (lessonList.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this,
                        getString(R.string.error_load_lessons) + ": " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddLessonDialog() {
        AddLessonDialogFragment dialog = AddLessonDialogFragment.newInstance(null);
        dialog.setOnLessonSavedListener(new AddLessonDialogFragment.OnLessonSavedListener() {
            @Override
            public void onLessonSaved() {
                loadLessons();
            }
        });
        dialog.show(getSupportFragmentManager(), "AddLessonDialog");
    }

    @Override
    public void onEditClick(Lesson lesson) {
        AddLessonDialogFragment dialog = AddLessonDialogFragment.newInstance(lesson);
        dialog.setOnLessonSavedListener(new AddLessonDialogFragment.OnLessonSavedListener() {
            @Override
            public void onLessonSaved() {
                loadLessons();
            }
        });
        dialog.show(getSupportFragmentManager(), "EditLessonDialog");
    }

    @Override
    public void onDeleteClick(Lesson lesson) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_lesson))
                .setMessage("האם אתה בטוח שברצונך למחוק את השיעור?")
                .setPositiveButton("מחק", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteLesson(lesson);
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void deleteLesson(Lesson lesson) {
        progressBar.setVisibility(View.VISIBLE);

        firestoreHelper.deleteLesson(lesson.getId(), new FirestoreHelper.OnOperationCompleteListener() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this,
                        getString(R.string.lesson_deleted),
                        Toast.LENGTH_SHORT).show();
                loadLessons();
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this,
                        getString(R.string.error_delete_lesson) + ": " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("התנתקות")
                .setMessage("האם אתה בטוח שברצונך להתנתק?")
                .setPositiveButton("התנתק", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAuth.signOut();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}