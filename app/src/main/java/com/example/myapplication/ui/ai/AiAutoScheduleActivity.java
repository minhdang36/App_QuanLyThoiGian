package com.example.myapplication.ui.ai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.ScheduleTimelineAdapter;
import com.example.myapplication.model.Task;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.ui.main.ProfileSettingsActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AiAutoScheduleActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageView imgAvatar;
    private Button btnReOptimize;

    private TextView tvEfficiencyPercent, tvOptimizedCount, tvOptimizedSummary, tvPeakFocus, tvEventCount;
    private RecyclerView rvTimeline;

    private LinearLayout day14, day15, day16, day17, day18, day19;
    private TextView tvDay14Label, tvDay14Num, tvDay15Label, tvDay15Num, tvDay16Label, tvDay16Num;
    private TextView tvDay17Label, tvDay17Num, tvDay18Label, tvDay18Num, tvDay19Label, tvDay19Num;

    private ScheduleTimelineAdapter timelineAdapter;
    private final List<Task> allUserTasks = new ArrayList<>();
    private final List<Task> displayedScheduleTasks = new ArrayList<>();

    private int selectedDayNumber = 17; // Mặc định hôm nay là 17

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_auto_schedule);

        btnBack = findViewById(R.id.btnBack);
        imgAvatar = findViewById(R.id.imgAvatar);
        btnReOptimize = findViewById(R.id.btnReOptimize);

        tvEfficiencyPercent = findViewById(R.id.tvEfficiencyPercent);
        tvOptimizedCount = findViewById(R.id.tvOptimizedCount);
        tvOptimizedSummary = findViewById(R.id.tvOptimizedSummary);
        tvPeakFocus = findViewById(R.id.tvPeakFocus);
        tvEventCount = findViewById(R.id.tvEventCount);

        rvTimeline = findViewById(R.id.rvTimeline);
        if (rvTimeline != null) {
            rvTimeline.setLayoutManager(new LinearLayoutManager(this));
            timelineAdapter = new ScheduleTimelineAdapter(displayedScheduleTasks);
            rvTimeline.setAdapter(timelineAdapter);
        }

        // 1. Nhấn Avatar -> Mở thông tin cá nhân (ProfileSettingsActivity)
        if (imgAvatar != null) {
            imgAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(AiAutoScheduleActivity.this, ProfileSettingsActivity.class);
                startActivity(intent);
            });
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 2. Thiết lập thanh chọn ngày tương tác (T2 14 -> T7 19)
        setupDaysStrip();

        // 3. Nút Tối ưu lại
        if (btnReOptimize != null) {
            btnReOptimize.setOnClickListener(v -> {
                filterAndOptimizeForDay(selectedDayNumber);
                Toast.makeText(this, "⚡ FlowAI đã xếp lại toàn bộ lịch trình tối ưu cho ngày " + selectedDayNumber + "!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupDaysStrip() {
        day14 = findViewById(R.id.day14);
        day15 = findViewById(R.id.day15);
        day16 = findViewById(R.id.day16);
        day17 = findViewById(R.id.day17);
        day18 = findViewById(R.id.day18);
        day19 = findViewById(R.id.day19);

        tvDay14Label = findViewById(R.id.tvDay14Label);
        tvDay14Num = findViewById(R.id.tvDay14Num);
        tvDay15Label = findViewById(R.id.tvDay15Label);
        tvDay15Num = findViewById(R.id.tvDay15Num);
        tvDay16Label = findViewById(R.id.tvDay16Label);
        tvDay16Num = findViewById(R.id.tvDay16Num);

        tvDay17Label = findViewById(R.id.tvDay17Label);
        tvDay17Num = findViewById(R.id.tvDay17Num);
        tvDay18Label = findViewById(R.id.tvDay18Label);
        tvDay18Num = findViewById(R.id.tvDay18Num);
        tvDay19Label = findViewById(R.id.tvDay19Label);
        tvDay19Num = findViewById(R.id.tvDay19Num);

        Calendar todayCal = Calendar.getInstance();
        selectedDayNumber = todayCal.get(Calendar.DAY_OF_MONTH);

        if (day14 != null) day14.setOnClickListener(v -> selectDayCapsule(14, day14, tvDay14Label, tvDay14Num));
        if (day15 != null) day15.setOnClickListener(v -> selectDayCapsule(15, day15, tvDay15Label, tvDay15Num));
        if (day16 != null) day16.setOnClickListener(v -> selectDayCapsule(16, day16, tvDay16Label, tvDay16Num));
        if (day17 != null) day17.setOnClickListener(v -> selectDayCapsule(17, day17, tvDay17Label, tvDay17Num));
        if (day18 != null) day18.setOnClickListener(v -> selectDayCapsule(18, day18, tvDay18Label, tvDay18Num));
        if (day19 != null) day19.setOnClickListener(v -> selectDayCapsule(19, day19, tvDay19Label, tvDay19Num));
    }

    private void selectDayCapsule(int dayNumber, LinearLayout selectedLayout, TextView tvLabel, TextView tvNum) {
        selectedDayNumber = dayNumber;

        // Bỏ chọn tất cả các capsule
        resetCapsuleStyle(day14, tvDay14Label, tvDay14Num);
        resetCapsuleStyle(day15, tvDay15Label, tvDay15Num);
        resetCapsuleStyle(day16, tvDay16Label, tvDay16Num);
        resetCapsuleStyle(day17, tvDay17Label, tvDay17Num);
        resetCapsuleStyle(day18, tvDay18Label, tvDay18Num);
        resetCapsuleStyle(day19, tvDay19Label, tvDay19Num);

        // Highlight capsule được bấm
        if (selectedLayout != null) {
            selectedLayout.setBackgroundResource(R.drawable.bg_pill_brown);
        }
        if (tvLabel != null) tvLabel.setTextColor(0xFFCCCCCC);
        if (tvNum != null) tvNum.setTextColor(0xFFFFFFFF);

        // Lọc và xem trước bản tối ưu cho ngày được chọn
        filterAndOptimizeForDay(dayNumber);
    }

    private void resetCapsuleStyle(LinearLayout layout, TextView tvLabel, TextView tvNum) {
        if (layout != null) layout.setBackgroundResource(0);
        if (tvLabel != null) tvLabel.setTextColor(0xFF888888);
        if (tvNum != null) tvNum.setTextColor(0xFF222222);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRealScheduleAndOptimize();
    }

    private void loadRealScheduleAndOptimize() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);

        if (currentUserId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem lịch AI", Toast.LENGTH_SHORT).show();
            return;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        apiService.getTasksByUser(currentUserId).enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allUserTasks.clear();
                    allUserTasks.addAll(response.body());

                    filterAndOptimizeForDay(selectedDayNumber);
                }
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                Toast.makeText(AiAutoScheduleActivity.this, "Lỗi tải lịch trình AI", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterAndOptimizeForDay(int dayNumber) {
        displayedScheduleTasks.clear();

        Calendar todayCal = Calendar.getInstance();
        int currentMonth = todayCal.get(Calendar.MONTH) + 1;
        boolean isTodaySelected = (dayNumber == todayCal.get(Calendar.DAY_OF_MONTH));

        int fixedCount = 0;
        int aiScheduledCount = 0;

        for (Task t : allUserTasks) {
            String timeStr = t.getTime();
            boolean isMatch = false;

            if (timeStr == null || timeStr.trim().isEmpty()) {
                if (isTodaySelected) isMatch = true;
            } else if (timeStr.contains("Hôm nay")) {
                if (isTodaySelected) isMatch = true;
            } else if (timeStr.contains(String.valueOf(dayNumber))) {
                isMatch = true;
            }

            if (isMatch) {
                String loc = t.getLocation() != null ? t.getLocation() : "";
                String title = t.getTitle() != null ? t.getTitle() : "";

                if (loc.contains("PMT") || loc.contains("H.A9") || title.contains("Tiết") || t.getProjectId() == 2) {
                    fixedCount++;
                } else {
                    aiScheduledCount++;
                    if (t.getTime() == null || !t.getTime().contains("🕒")) {
                        t.setTime("📅 Ngày " + dayNumber + "/" + currentMonth + " | 🕒 10:00 - 12:00 (Giờ trống)");
                    }
                }
                displayedScheduleTasks.add(t);
            }
        }

        if (timelineAdapter != null) {
            timelineAdapter.updateData(displayedScheduleTasks);
        }

        int totalEvents = displayedScheduleTasks.size();
        int efficiency = totalEvents > 0 ? Math.min(85 + totalEvents * 4, 98) : 80;

        if (tvEfficiencyPercent != null) {
            tvEfficiencyPercent.setText(efficiency + "% Hiệu suất");
        }

        if (tvOptimizedCount != null) {
            if (totalEvents > 0) {
                tvOptimizedCount.setText("Bản tối ưu ngày " + dayNumber + ": " + totalEvents + " sự kiện");
            } else {
                tvOptimizedCount.setText("Chưa có lịch trình ngày " + dayNumber);
            }
        }

        if (tvOptimizedSummary != null) {
            tvOptimizedSummary.setText("Gồm " + fixedCount + " lịch cố định • " + aiScheduledCount + " công việc AI đề xuất");
        }

        if (tvEventCount != null) {
            tvEventCount.setText("● " + totalEvents + " Sự kiện");
        }

        if (tvPeakFocus != null) {
            tvPeakFocus.setText("● Đỉnh năng lượng: 09:30 - 11:30");
        }
    }
}
