package com.example.myadvancedschedule;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

// Account registration screen used in the primary onboarding flow.
// It validates user input client-side to reduce avoidable Firebase round-trips.

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText editName, editEmail, editPassword, editConfirmPassword;
    private Button btnRegister;
    private TextView textLogin;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize auth and wire form controls before user interaction begins.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // אתחול Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // קישור כל המרכיבים
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        textLogin = findViewById(R.id.textLogin);
        progressBar = findViewById(R.id.progressBar);

        // לחצן רישום
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        // מעבר לדף התחברות
        textLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void registerUser() {
        // Validate locally first so users get immediate feedback without network delay.
        String name = editName != null && editName.getText() != null ?
                editName.getText().toString().trim() : "";
        String email = editEmail != null && editEmail.getText() != null ?
                editEmail.getText().toString().trim() : "";
        String password = editPassword != null && editPassword.getText() != null ?
                editPassword.getText().toString().trim() : "";
        String confirmPassword = editConfirmPassword != null && editConfirmPassword.getText() != null ?
                editConfirmPassword.getText().toString().trim() : "";

        // בדיקות תקינות
        if (TextUtils.isEmpty(name)) {
            if (editName != null) {
                editName.setError(getString(R.string.error_empty_name));
                editName.requestFocus();
            }
            return;
        }

        if (TextUtils.isEmpty(email)) {
            if (editEmail != null) {
                editEmail.setError(getString(R.string.error_empty_email));
                editEmail.requestFocus();
            }
            return;
        }

        if (TextUtils.isEmpty(password)) {
            if (editPassword != null) {
                editPassword.setError(getString(R.string.error_empty_password));
                editPassword.requestFocus();
            }
            return;
        }

        if (password.length() < 6) {
            if (editPassword != null) {
                editPassword.setError(getString(R.string.error_short_password));
                editPassword.requestFocus();
            }
            return;
        }

        if (!password.equals(confirmPassword)) {
            if (editConfirmPassword != null) {
                editConfirmPassword.setError(getString(R.string.error_password_mismatch));
                editConfirmPassword.requestFocus();
            }
            return;
        }

        // הצג ProgressBar
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (btnRegister != null) {
            btnRegister.setEnabled(false);
        }

        // רישום משתמש ב-Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Re-enable form regardless of result to keep UI recoverable.
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        if (btnRegister != null) {
                            btnRegister.setEnabled(true);
                        }

                        if (task.isSuccessful()) {
                            // New users proceed directly to schedule setup so app has usable data.
                            Toast.makeText(RegisterActivity.this,
                                    getString(R.string.register_success),
                                    Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(RegisterActivity.this, SetupScheduleActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() :
                                    getString(R.string.register_failed);
                            Toast.makeText(RegisterActivity.this,
                                    getString(R.string.register_failed) + ": " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
