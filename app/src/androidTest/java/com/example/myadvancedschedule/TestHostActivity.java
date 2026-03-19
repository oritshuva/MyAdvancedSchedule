package com.example.myadvancedschedule;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Test-only activity used by instrumentation tests to render fragments.
 */
public class TestHostActivity extends AppCompatActivity {
    static final int CONTAINER_ID = 0x7f0b00aa; // Arbitrary stable ID for test-only fragment container.

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout container = new FrameLayout(this);
        container.setId(CONTAINER_ID);
        setContentView(container);
    }
}

