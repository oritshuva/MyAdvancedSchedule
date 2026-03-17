package com.example.myadvancedschedule;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editEmail, editPassword;
    private Button btnLogin;
    private TextView textRegister;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // אתחול Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // קישור כל המרכיבים
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);
        textRegister = findViewById(R.id.textRegister);
        progressBar = findViewById(R.id.progressBar);

        // לחצן התחברות
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // מעבר לדף הרשמה
        textRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loginUser() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        // בדיקות תקינות
        if (TextUtils.isEmpty(email)) {
            editEmail.setError("נא להזין אימייל");
            editEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editPassword.setError("נא להזין סיסמה");
            editPassword.requestFocus();
            return;
        }

        // הצג ProgressBar
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        // התחברות ל-Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "ההתחברות הצליחה", Toast.LENGTH_SHORT).show();

                        // Decide whether to go straight to the main screen or run setup
                        // based on whether the user already has lessons in Firestore.
                        FirestoreHelper helper = new FirestoreHelper();
                        helper.getAllLessons(new FirestoreHelper.OnLessonsLoadedListener() {
                            @Override
                            public void onLessonsLoaded(java.util.List<Lesson> lessons) {
                                Class<?> target = (lessons != null && !lessons.isEmpty())
                                        ? MainActivity.class
                                        : SetupScheduleActivity.class;
                                Intent intent = new Intent(LoginActivity.this, target);
                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void onError(String error) {
                                // On error, fall back to setup so the user can rebuild.
                                Intent intent = new Intent(LoginActivity.this, SetupScheduleActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    } else {
                        Toast.makeText(LoginActivity.this, "ההתחברות נכשלה", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
