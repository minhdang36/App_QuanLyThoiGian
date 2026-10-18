package com.example.myapplication.ui.ai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.Task;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.ui.main.MainActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AiTaskBreakdownActivity extends AppCompatActivity {

    private ImageButton btnBack, btnSparkleHeader;
    private EditText edtMainTaskTitle;
    private TextView tvTaskEstimate;

    private TextView tvStep1Title, tvStep1Time;
    private TextView tvStep2Title, tvStep2Time;
    private TextView tvStep3Title, tvStep3Time;
    private TextView tvStep4Title, tvStep4Time;
    private TextView tvTotalAiEstimate;

    private TextView btnRegenerate;
    private Button btnApplyBreakdown;

    private String aiRecommendedTimeStr = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_task_breakdown);

        btnBack = findViewById(R.id.btnBack);
        btnSparkleHeader = findViewById(R.id.btnSparkleHeader);
        edtMainTaskTitle = findViewById(R.id.edtMainTaskTitle);
        tvTaskEstimate = findViewById(R.id.tvTaskEstimate);

        tvStep1Title = findViewById(R.id.tvStep1Title);
        tvStep1Time = findViewById(R.id.tvStep1Time);
        tvStep2Title = findViewById(R.id.tvStep2Title);
        tvStep2Time = findViewById(R.id.tvStep2Time);
        tvStep3Title = findViewById(R.id.tvStep3Title);
        tvStep3Time = findViewById(R.id.tvStep3Time);
        tvStep4Title = findViewById(R.id.tvStep4Title);
        tvStep4Time = findViewById(R.id.tvStep4Time);
        tvTotalAiEstimate = findViewById(R.id.tvTotalAiEstimate);

        btnRegenerate = findViewById(R.id.btnRegenerate);
        btnApplyBreakdown = findViewById(R.id.btnApplyBreakdown);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnSparkleHeader != null) {
            btnSparkleHeader.setOnClickListener(v -> generateAiScheduleAndBreakdown());
        }

        if (btnRegenerate != null) {
            btnRegenerate.setOnClickListener(v -> generateAiScheduleAndBreakdown());
        }

        if (btnApplyBreakdown != null) {
            btnApplyBreakdown.setOnClickListener(v -> applyAndSaveTaskToServer());
        }

        generateAiScheduleAndBreakdown();
    }

    private void generateAiScheduleAndBreakdown() {
        String inputTitle = edtMainTaskTitle != null ? edtMainTaskTitle.getText().toString().trim() : "";
        if (inputTitle.isEmpty()) {
            inputTitle = "Công việc mới";
        }

        Calendar cal = Calendar.getInstance();
        int currentHour = cal.get(Calendar.HOUR_OF_DAY);

        int startHour = (currentHour < 17) ? Math.max(currentHour + 1, 9) : 9;
        int endHour = startHour + 2;

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        String dateStr = (currentHour < 17) ? "Hôm nay, " + dayFormat.format(cal.getTime()) : "Ngày mai, " + dayFormat.format(cal.getTime());
        aiRecommendedTimeStr = String.format(Locale.getDefault(), "📅 %s | 🕒 %02d:00 - %02d:00", dateStr, startHour, endHour);

        if (tvTaskEstimate != null) {
            tvTaskEstimate.setText(String.format(Locale.getDefault(), "⚡ FlowAI xếp lịch: %02d:00 - %02d:00 • ⏱ Ước tính: 2 giờ", startHour, endHour));
        }

        String titleLower = inputTitle.toLowerCase();
        if (titleLower.contains("slide") || titleLower.contains("thuyết trình") || titleLower.contains("báo cáo")) {
            if (tvStep1Title != null) tvStep1Title.setText("1. Thu thập dữ liệu & xây dựng dàn ý bài báo cáo");
            if (tvStep1Time != null) tvStep1Time.setText("⏱ 30 phút  •  Khởi động");

            if (tvStep2Title != null) tvStep2Title.setText("2. Soạn thảo slide chi tiết & thiết kế giao diện");
            if (tvStep2Time != null) tvStep2Time.setText("⏱ 45 phút  •  Trọng tâm");

            if (tvStep3Title != null) tvStep3Title.setText("3. Tổng hợp số liệu & tạo biểu đồ trực quan");
            if (tvStep3Time != null) tvStep3Time.setText("⏱ 30 phút  •  Xử lý dữ liệu");

            if (tvStep4Title != null) tvStep4Title.setText("4. Xem lại toàn bộ slide & tập luyện thuyết trình");
            if (tvStep4Time != null) tvStep4Time.setText("⏱ 15 phút  •  Hoàn tất");

        } else if (titleLower.contains("học") || titleLower.contains("ôn") || titleLower.contains("thi")) {
            if (tvStep1Title != null) tvStep1Title.setText("1. Xem lại lý thuyết chính & ghi chú quan trọng");
            if (tvStep1Time != null) tvStep1Time.setText("⏱ 30 phút  •  Ghi nhớ");

            if (tvStep2Title != null) tvStep2Title.setText("2. Giải bài tập mẫu & phân dạng câu hỏi");
            if (tvStep2Time != null) tvStep2Time.setText("⏱ 50 phút  •  Thực hành");

            if (tvStep3Title != null) tvStep3Title.setText("3. Làm đề thi thử & tự chấm điểm");
            if (tvStep3Time != null) tvStep3Time.setText("⏱ 30 phút  •  Thử thách");

            if (tvStep4Title != null) tvStep4Title.setText("4. Bổ sung các câu bị sai & tổng kết kiến thức");
            if (tvStep4Time != null) tvStep4Time.setText("⏱ 10 phút  •  Củng cố");

        } else {
            if (tvStep1Title != null) tvStep1Title.setText("1. Nghiên cứu tài liệu & chuẩn bị thông tin ban đầu");
            if (tvStep1Time != null) tvStep1Time.setText("⏱ 30 phút  •  Chuẩn bị");

            if (tvStep2Title != null) tvStep2Title.setText("2. Thực hiện nội dung cốt lõi của " + inputTitle);
            if (tvStep2Time != null) tvStep2Time.setText("⏱ 50 phút  •  Tập trung");

            if (tvStep3Title != null) tvStep3Title.setText("3. Họp trao đổi / Kiểm tra kết quả với nhóm");
            if (tvStep3Time != null) tvStep3Time.setText("⏱ 25 phút  •  Đánh giá");

            if (tvStep4Title != null) tvStep4Title.setText("4. Hoàn thiện chi tiết cuối cùng & gửi báo cáo");
            if (tvStep4Time != null) tvStep4Time.setText("⏱ 15 phút  •  Kết thúc");
        }

        if (tvTotalAiEstimate != null) {
            tvTotalAiEstimate.setText("⏱ Tổng thời gian AI gợi ý: 2 giờ 00 phút  •  Tối ưu cho bạn");
        }

        Toast.makeText(this, "⚡ FlowAI đã đề xuất thời gian & phân rã cho: " + inputTitle, Toast.LENGTH_SHORT).show();
    }

    private void applyAndSaveTaskToServer() {
        String finalTitle = edtMainTaskTitle != null ? edtMainTaskTitle.getText().toString().trim() : "";
        if (finalTitle.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên công việc!", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);

        if (currentUserId == -1) {
            Toast.makeText(this, "Lỗi: Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        String step1 = tvStep1Title != null ? tvStep1Title.getText().toString() : "";
        String step2 = tvStep2Title != null ? tvStep2Title.getText().toString() : "";
        String step3 = tvStep3Title != null ? tvStep3Title.getText().toString() : "";
        String step4 = tvStep4Title != null ? tvStep4Title.getText().toString() : "";

        String fullDescription = "Các bước AI phân rã:\n" + step1 + "\n" + step2 + "\n" + step3 + "\n" + step4;

        Task newTask = new Task();
        newTask.setTitle(finalTitle);
        newTask.setDescription(fullDescription);
        newTask.setTime(aiRecommendedTimeStr);
        newTask.setLocation("Lịch tự động FlowAI");
        newTask.setProjectId(1);
        newTask.setStatus("PENDING");
        newTask.setUserId(currentUserId);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        btnApplyBreakdown.setEnabled(false);
        btnApplyBreakdown.setText("Đang lưu...");

        apiService.addTask(newTask).enqueue(new Callback<Task>() {
            @Override
            public void onResponse(Call<Task> call, Response<Task> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AiTaskBreakdownActivity.this, "✔ Đã áp dụng & lưu công việc thành công!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(AiTaskBreakdownActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    btnApplyBreakdown.setEnabled(true);
                    btnApplyBreakdown.setText("✔ Áp dụng ngay");
                    Toast.makeText(AiTaskBreakdownActivity.this, "Lỗi lưu: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Task> call, Throwable t) {
                btnApplyBreakdown.setEnabled(true);
                btnApplyBreakdown.setText("✔ Áp dụng ngay");
                Toast.makeText(AiTaskBreakdownActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
