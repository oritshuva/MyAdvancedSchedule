package com.example.myadvancedschedule;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword;
    private Button btnSignup, btnBack;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseHelper.getInstance().getAuth();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignup = findViewById(R.id.btnSignup);
        btnBack = findViewById(R.id.btnBack);

        btnSignup.setEnabled(false);

        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateInputs();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etName.addTextChangedListener(validationWatcher);
        etEmail.addTextChangedListener(validationWatcher);
        etPassword.addTextChangedListener(validationWatcher);

        btnSignup.setOnClickListener(v -> signupUser());
        btnBack.setOnClickListener(v -> finish());
    }

    private void validateInputs() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        boolean isNameValid = name.length() >= 2 && name.length() <= 20 &&
                name.matches("[a-zA-Zא-ת ]+");
        boolean isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches();
        boolean isPasswordValid = password.length() >= 6;

        btnSignup.setEnabled(isNameValid && isEmailValid && isPasswordValid);
    }

    private void signupUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        btnSignup.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Save user data to Firestore
                        String userId = auth.getCurrentUser().getUid();
                        User user = new User(name, email);
                        user.setId(userId);

                        FirebaseHelper.getInstance().getUsersCollection()
                                .document(userId)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "שגיאה בשמירת נתונים",
                                            Toast.LENGTH_SHORT).show();
                                    btnSignup.setEnabled(true);
                                });
                    } else {
                        String errorMsg = "שגיאה בהרשמה";
                        if (task.getException() != null &&
                                task.getException().getMessage().contains("already in use")) {
                            errorMsg = "האימייל כבר קיים במערכת";
                        }
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                        btnSignup.setEnabled(true);
                    }
                });
    }
}
