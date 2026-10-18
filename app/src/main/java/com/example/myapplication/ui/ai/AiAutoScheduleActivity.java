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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.ScheduleTimelineAdapter;
import com.example.myapplication.model.Task;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.ui.main.ProfileSettingsActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

    private int selectedDayNumber = -1;

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
            timelineAdapter.setOnTaskStatusChangeListener(this::recalculateEfficiencyOnly);
            rvTimeline.setAdapter(timelineAdapter);

            // Tích hợp Kéo & Thả (Drag & Drop) để xếp lại vị trí các task thoải mái
            ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    int fromPosition = viewHolder.getBindingAdapterPosition();
                    int toPosition = target.getBindingAdapterPosition();

                    if (fromPosition != RecyclerView.NO_POSITION && toPosition != RecyclerView.NO_POSITION) {
                        timelineAdapter.onItemMove(fromPosition, toPosition);
                        return true;
                    }
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
            };

            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
            itemTouchHelper.attachToRecyclerView(rvTimeline);
        }

        if (imgAvatar != null) {
            imgAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(AiAutoScheduleActivity.this, ProfileSettingsActivity.class);
                startActivity(intent);
            });
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        setupDaysStrip();

        if (btnReOptimize != null) {
            btnReOptimize.setOnClickListener(v -> {
                filterAndOptimizeForDay(selectedDayNumber);
                Toast.makeText(this, "⚡ AI đã sắp xếp Ưu tiên 1 ➔ Ưu tiên 2 ➔ Ưu tiên 3 cho ngày " + selectedDayNumber + "!", Toast.LENGTH_SHORT).show();
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

        Calendar realCal = Calendar.getInstance();
        int realTodayDayNum = realCal.get(Calendar.DAY_OF_MONTH);
        selectedDayNumber = realTodayDayNum;

        Calendar weekCal = (Calendar) realCal.clone();
        weekCal.add(Calendar.DAY_OF_MONTH, -2);

        SimpleDateFormat labelFormat = new SimpleDateFormat("EEE", new Locale("vi", "VN"));

        LinearLayout[] layouts = new LinearLayout[]{day14, day15, day16, day17, day18, day19};
        TextView[] labels = new TextView[]{tvDay14Label, tvDay15Label, tvDay16Label, tvDay17Label, tvDay18Label, tvDay19Label};
        TextView[] nums = new TextView[]{tvDay14Num, tvDay15Num, tvDay16Num, tvDay17Num, tvDay18Num, tvDay19Num};

        for (int i = 0; i < 6; i++) {
            int dNum = weekCal.get(Calendar.DAY_OF_MONTH);
            String dLabel = (dNum == realTodayDayNum) ? "H.NAY" : labelFormat.format(weekCal.getTime()).toUpperCase();

            if (labels[i] != null) labels[i].setText(dLabel);
            if (nums[i] != null) nums[i].setText(String.valueOf(dNum));

            final int clickDay = dNum;
            final LinearLayout curLayout = layouts[i];
            final TextView curLabel = labels[i];
            final TextView curNum = nums[i];

            if (curLayout != null) {
                curLayout.setOnClickListener(v -> selectDayCapsule(clickDay, curLayout, curLabel, curNum));
            }

            if (dNum == realTodayDayNum) {
                selectDayCapsule(dNum, curLayout, curLabel, curNum);
            }

            weekCal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void selectDayCapsule(int dayNumber, LinearLayout selectedLayout, TextView tvLabel, TextView tvNum) {
        selectedDayNumber = dayNumber;

        resetCapsuleStyle(day14, tvDay14Label, tvDay14Num);
        resetCapsuleStyle(day15, tvDay15Label, tvDay15Num);
        resetCapsuleStyle(day16, tvDay16Label, tvDay16Num);
        resetCapsuleStyle(day17, tvDay17Label, tvDay17Num);
        resetCapsuleStyle(day18, tvDay18Label, tvDay18Num);
        resetCapsuleStyle(day19, tvDay19Label, tvDay19Num);

        if (selectedLayout != null) {
            selectedLayout.setBackgroundResource(R.drawable.bg_pill_brown);
        }
        if (tvLabel != null) tvLabel.setTextColor(0xFFCCCCCC);
        if (tvNum != null) tvNum.setTextColor(0xFFFFFFFF);

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

        // AI SẮP XẾP ƯU TIÊN 1 ➔ ƯU TIÊN 2 ➔ ƯU TIÊN 3
        Collections.sort(displayedScheduleTasks, (t1, t2) -> {
            int p1 = getPriorityLevel(t1);
            int p2 = getPriorityLevel(t2);
            return Integer.compare(p1, p2);
        });

        if (timelineAdapter != null) {
            timelineAdapter.updateData(displayedScheduleTasks);
        }

        recalculateEfficiencyOnly();

        if (tvOptimizedCount != null) {
            int totalEvents = displayedScheduleTasks.size();
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
            tvEventCount.setText("● " + displayedScheduleTasks.size() + " Sự kiện");
        }

        if (tvPeakFocus != null) {
            tvPeakFocus.setText("● Đỉnh năng lượng: 09:30 - 11:30");
        }
    }

    private int getPriorityLevel(Task t) {
        String combined = (t.getPriority() != null ? t.getPriority() : "") + " " + (t.getTime() != null ? t.getTime() : "");
        String p = combined.toUpperCase();
        if (p.contains("ƯU 1") || p.contains("CAO") || p.contains("HIGH")) return 1;
        if (p.contains("ƯU 2") || p.contains("TRUNG BÌNH") || p.contains("MEDIUM")) return 2;
        if (p.contains("ƯU 3") || p.contains("THẤP") || p.contains("LOW")) return 3;
        return 2;
    }

    private void recalculateEfficiencyOnly() {
        int totalEvents = displayedScheduleTasks.size();
        int doneCount = 0;

        for (Task t : displayedScheduleTasks) {
            String s = t.getStatus();
            if ("DONE".equalsIgnoreCase(s) || "COMPLETED".equalsIgnoreCase(s) || "HOÀN THÀNH".equalsIgnoreCase(s)) {
                doneCount++;
            }
        }

        int efficiency = totalEvents > 0 ? (int) Math.round(((double) doneCount / totalEvents) * 100) : 100;

        if (tvEfficiencyPercent != null) {
            tvEfficiencyPercent.setText(efficiency + "% Hiệu suất (" + doneCount + "/" + totalEvents + " Hoàn thành)");
        }
    }
}
