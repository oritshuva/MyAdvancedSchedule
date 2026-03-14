package com.example.myadvancedschedule;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Launcher activity. If user is already authenticated, go to MainActivity.
 * Otherwise show Login and Create Account buttons.
 */
public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_welcome);

        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        MaterialButton btnCreateAccount = findViewById(R.id.btnCreateAccount);

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        });

        btnCreateAccount.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class));
        });
    }
}
