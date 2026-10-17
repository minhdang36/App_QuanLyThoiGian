package com.example.myapplication.ui.ai;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

public class AiAutoScheduleActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnReOptimize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_auto_schedule);

        btnBack = findViewById(R.id.btnBack);
        btnReOptimize = findViewById(R.id.btnReOptimize);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnReOptimize != null) {
            btnReOptimize.setOnClickListener(v -> {
                Toast.makeText(this, "⚡ FlowAI đã dời lịch & xếp lại lịch trình tối ưu!", Toast.LENGTH_LONG).show();
            });
        }
    }
}