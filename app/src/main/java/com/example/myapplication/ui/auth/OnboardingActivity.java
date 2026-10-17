package com.example.myapplication.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

public class OnboardingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        TextView btnSkip = findViewById(R.id.btnSkip);
        TextView btnGetStarted = findViewById(R.id.btnGetStarted);
        TextView btnSignIn = findViewById(R.id.btnSignIn);

        if (btnSkip != null) {
            btnSkip.setOnClickListener(v -> navigateToLogin());
        }

        if (btnGetStarted != null) {
            btnGetStarted.setOnClickListener(v -> navigateToLogin());
        }

        if (btnSignIn != null) {
            btnSignIn.setOnClickListener(v -> navigateToLogin());
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(OnboardingActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}