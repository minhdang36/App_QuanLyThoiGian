package com.example.myapplication.ui.ai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.ui.main.MainActivity;

public class AiTaskBreakdownActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView btnRegenerate;
    private Button btnApplyBreakdown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_task_breakdown);

        btnBack = findViewById(R.id.btnBack);
        btnRegenerate = findViewById(R.id.btnRegenerate);
        btnApplyBreakdown = findViewById(R.id.btnApplyBreakdown);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnRegenerate != null) {
            btnRegenerate.setOnClickListener(v -> {
                Toast.makeText(this, "AI đang tối ưu lại các bước phân rã...", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnApplyBreakdown != null) {
            btnApplyBreakdown.setOnClickListener(v -> {
                Toast.makeText(this, "Đã áp dụng các bước phân rã công việc!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(AiTaskBreakdownActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }
    }
}