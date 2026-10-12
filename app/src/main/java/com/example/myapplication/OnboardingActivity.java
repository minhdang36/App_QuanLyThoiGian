package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class OnboardingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // Ánh xạ các nút bấm trên màn hình
        TextView btnSkip = findViewById(R.id.btnSkip);
        TextView btnGetStarted = findViewById(R.id.btnGetStarted);
        TextView btnSignIn = findViewById(R.id.btnSignIn);

        // 1. Sự kiện cho nút "Bỏ qua"
        if (btnSkip != null) {
            btnSkip.setOnClickListener(v -> navigateToLogin());
        }

        // 2. Sự kiện cho nút "Bắt đầu ngay"
        if (btnGetStarted != null) {
            btnGetStarted.setOnClickListener(v -> navigateToLogin());
        }

        // 3. Sự kiện cho dòng chữ "Đăng nhập vào tài khoản hiện có"
        if (btnSignIn != null) {
            btnSignIn.setOnClickListener(v -> navigateToLogin());
        }
    }

    // Hàm dùng chung để chuyển sang trang Login
    private void navigateToLogin() {
        Intent intent = new Intent(OnboardingActivity.this, LoginActivity.class);
        startActivity(intent);
        // Lệnh finish() giúp đóng trang Chào mừng lại,
        // để khi người dùng vào trang Đăng nhập rồi bấm Back (<) thì app sẽ thoát ra ngoài chứ không quay ngược lại trang này.
        finish();
    }
}