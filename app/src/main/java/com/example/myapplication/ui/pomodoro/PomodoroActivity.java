package com.example.myapplication.ui.pomodoro;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import androidx.core.content.ContextCompat;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.Task;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.ui.ai.AiAutoScheduleActivity;
import com.example.myapplication.ui.main.MainActivity;
import com.example.myapplication.ui.project.ProjectsActivity;
import com.example.myapplication.ui.task.AddTaskActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PomodoroActivity extends AppCompatActivity {

    private TextView tabFocus, tabShortBreak, tabLongBreak;
    private TextView tvTimerDisplay, tvSessionCount, btnStartTimer;
    private ImageButton btnResetTimer, btnSoundToggle, btnSwapTask, btnSettings;
    private MaterialCardView cardActiveTask, cardSoundSelector;
    private TextView tvSoundTitle, tvSoundSubtitle;
    private ImageView btnPlayPauseSound;

    private TextView tvActiveTaskTitle, tvTotalFocusHours, tvGoalProgress, tvFocusLabel, tvGoalLabel;
    private ProgressBar progressTask;
    private CircularTimerView circularTimerView;

    private static final int MODE_FOCUS = 0;
    private static final int MODE_SHORT_BREAK = 1;
    private static final int MODE_LONG_BREAK = 2;
    private int currentTimerMode = MODE_FOCUS;

    private int totalFocusSecondsAcc = 0;
    private int totalShortBreakSecondsAcc = 0;
    private int totalLongBreakSecondsAcc = 0;

    private double shortBreakGoalHours = 0.5;
    private double longBreakGoalHours = 1.0;

    private TextView chipSound1, chipSound2, chipSound3, chipSound4, chipSound5, chipSound6;

    private LinearLayout navAgenda, navProjects, navTimeline;
    private FloatingActionButton fabAdd;

    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private long timeRemainingInMillis = 25 * 60 * 1000L;
    private long timerDurationInMillis = 25 * 60 * 1000L;

    private int currentSession = 1;
    private final int totalSessions = 4;

    private boolean isSoundPlaying = false;
    private AudioTrack audioTrack;
    private Thread audioThread;
    private int selectedSoundIndex = 0;

    private int activeTaskId = -1;
    private String activeTaskTitle = "";
    private boolean isIntentTaskProvided = false;
    private double goalHours = 2.0;
    private int totalFocusMinutesAcc = 0;

    private final List<Task> userTaskList = new ArrayList<>();

    private final String[] soundTitles = {
            "Rừng thông & Gió nhẹ",
            "Tiếng mưa rơi thư giãn",
            "Sóng biển rì rào",
            "Quán cà phê yên tĩnh",
            "Binaural Beats - 432Hz",
            "Tiếng ồn trắng (White Noise)"
    };

    private final String[] soundSubtitles = {
            "Tần số thiên nhiên - 432Hz",
            "Âm thanh hạt mưa xoa dịu",
            "Sóng biển nhịp nhàng 220Hz",
            "Không gian làm việc ấm cúng",
            "Sóng não tập trung chuyên sâu",
            "Loại bỏ tạp âm xung quanh"
    };

    private final double[] soundFrequencies = {432.0, 300.0, 220.0, 528.0, 432.0, 180.0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro);

        tabFocus = findViewById(R.id.tabFocus);
        tabShortBreak = findViewById(R.id.tabShortBreak);
        tabLongBreak = findViewById(R.id.tabLongBreak);

        tvTimerDisplay = findViewById(R.id.tvTimerDisplay);
        tvSessionCount = findViewById(R.id.tvSessionCount);
        circularTimerView = findViewById(R.id.circularTimerView);
        btnStartTimer = findViewById(R.id.btnStartTimer);
        btnResetTimer = findViewById(R.id.btnResetTimer);
        btnSoundToggle = findViewById(R.id.btnSoundToggle);
        btnSwapTask = findViewById(R.id.btnSwapTask);
        btnSettings = findViewById(R.id.btnSettings);

        if (btnSettings != null) {
            btnSettings.setVisibility(View.GONE);
        }

        cardActiveTask = findViewById(R.id.cardActiveTask);
        tvActiveTaskTitle = findViewById(R.id.tvActiveTaskTitle);
        tvTotalFocusHours = findViewById(R.id.tvTotalFocusHours);
        tvGoalProgress = findViewById(R.id.tvGoalProgress);
        tvFocusLabel = findViewById(R.id.tvFocusLabel);
        tvGoalLabel = findViewById(R.id.tvGoalLabel);
        progressTask = findViewById(R.id.progressTask);

        if (tvGoalProgress != null) {
            tvGoalProgress.setOnClickListener(v -> showEditGoalClockDialog());
        }

        chipSound1 = findViewById(R.id.chipSound1);
        chipSound2 = findViewById(R.id.chipSound2);
        chipSound3 = findViewById(R.id.chipSound3);
        chipSound4 = findViewById(R.id.chipSound4);
        chipSound5 = findViewById(R.id.chipSound5);
        chipSound6 = findViewById(R.id.chipSound6);

        cardSoundSelector = findViewById(R.id.cardSoundSelector);
        tvSoundTitle = findViewById(R.id.tvSoundTitle);
        tvSoundSubtitle = findViewById(R.id.tvSoundSubtitle);
        btnPlayPauseSound = findViewById(R.id.btnPlayPauseSound);

        navAgenda = findViewById(R.id.navAgenda);
        navProjects = findViewById(R.id.navProjects);
        navTimeline = findViewById(R.id.navTimeline);
        fabAdd = findViewById(R.id.fabAdd);

        Intent intent = getIntent();
        if (intent != null) {
            activeTaskId = intent.getIntExtra("ACTIVE_TASK_ID", -1);
            if (intent.hasExtra("ACTIVE_TASK_TITLE")) {
                String tTitle = intent.getStringExtra("ACTIVE_TASK_TITLE");
                if (tTitle != null && !tTitle.trim().isEmpty()) {
                    activeTaskTitle = tTitle;
                    isIntentTaskProvided = true;
                }
            }

            if (intent.hasExtra("TASK_TIME")) {
                String tTime = intent.getStringExtra("TASK_TIME");
                goalHours = parseHoursFromTimeStr(tTime);
            }
        }

        if (btnSwapTask != null) {
            btnSwapTask.setOnClickListener(v -> showTaskDetailAndSwitchDialog());
        }

        setupTabs();
        setupTimerControls();
        setupAmbientSoundControls();
        setupBottomNav();
    }

    private void showEditGoalClockDialog() {
        int initialHours = (int) goalHours;
        int initialMinutes = (int) Math.round((goalHours - initialHours) * 60);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                PomodoroActivity.this,
                (view, selectedHour, selectedMinute) -> {
                    double calculatedHours = selectedHour + (selectedMinute / 60.0);
                    if (calculatedHours > 0) {
                        goalHours = calculatedHours;

                        SharedPreferences pref = getSharedPreferences("PomodoroStats", MODE_PRIVATE);
                        pref.edit().putFloat("CUSTOM_GOAL_" + activeTaskTitle.hashCode(), (float) goalHours).apply();

                        updateTaskFocusStats();
                        Toast.makeText(this, "✔ Đã chọn mục tiêu: " + selectedHour + " giờ " + selectedMinute + " phút!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Vui lòng chọn thời gian lớn hơn 0!", Toast.LENGTH_SHORT).show();
                    }
                },
                initialHours, initialMinutes, true
        );
        timePickerDialog.setTitle("🕒 Chọn Thời Gian Mục Tiêu (Đồng Hồ)");
        timePickerDialog.show();
    }

    private void loadAccFocusMinutesForCurrentTask() {
        if (activeTaskTitle == null || activeTaskTitle.trim().isEmpty()) return;
        SharedPreferences pref = getSharedPreferences("PomodoroStats", MODE_PRIVATE);
        String taskHash = String.valueOf(activeTaskTitle.hashCode());

        totalFocusSecondsAcc = pref.getInt("ACC_SECONDS_" + taskHash, 0);
        totalShortBreakSecondsAcc = pref.getInt("ACC_SHORT_BREAK_SECONDS_" + taskHash, 0);
        totalLongBreakSecondsAcc = pref.getInt("ACC_LONG_BREAK_SECONDS_" + taskHash, 0);

        if (totalFocusSecondsAcc == 0) {
            int oldMins = pref.getInt("ACC_MINUTES_" + taskHash, 0);
            totalFocusSecondsAcc = oldMins * 60;
        }

        float savedGoal = pref.getFloat("CUSTOM_GOAL_" + taskHash, -1f);
        if (savedGoal > 0) {
            goalHours = savedGoal;
        }

        updateTaskFocusStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchUserTasksAndSingleGoal();
    }

    private void fetchUserTasksAndSingleGoal() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);
        if (currentUserId == -1) {
            if (cardActiveTask != null) cardActiveTask.setVisibility(View.GONE);
            return;
        }

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getTasksByUser(currentUserId).enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userTaskList.clear();
                    userTaskList.addAll(response.body());

                    Calendar todayCal = Calendar.getInstance();
                    int todayDayNum = todayCal.get(Calendar.DAY_OF_MONTH);

                    Task activeTodayTask = null;

                    if (isIntentTaskProvided && !activeTaskTitle.isEmpty()) {
                        for (Task t : userTaskList) {
                            if ((activeTaskId != -1 && t.getId() == activeTaskId) ||
                                (t.getTitle() != null && t.getTitle().trim().equalsIgnoreCase(activeTaskTitle.trim()))) {
                                activeTodayTask = t;
                                activeTaskId = t.getId();
                                break;
                            }
                        }
                    }

                    if (activeTodayTask == null) {
                        for (Task t : userTaskList) {
                            String timeStr = t.getTime();
                            if (timeStr == null || timeStr.contains("Hôm nay") || timeStr.contains(String.valueOf(todayDayNum))) {
                                activeTodayTask = t;
                                activeTaskId = t.getId();
                                activeTaskTitle = t.getTitle();
                                break;
                            }
                        }
                    }

                    if (activeTodayTask != null && activeTaskTitle != null && !activeTaskTitle.trim().isEmpty()) {
                        SharedPreferences pref = getSharedPreferences("PomodoroStats", MODE_PRIVATE);
                        float savedGoal = pref.getFloat("CUSTOM_GOAL_" + activeTaskTitle.hashCode(), -1f);

                        if (savedGoal > 0) {
                            goalHours = savedGoal;
                        } else {
                            double parsedGoal = parseHoursFromTimeStr(activeTodayTask.getTime());
                            if (parsedGoal > 0) goalHours = parsedGoal;
                        }

                        if (tvActiveTaskTitle != null) {
                            tvActiveTaskTitle.setText(activeTaskTitle);
                        }

                        if (cardActiveTask != null) {
                            cardActiveTask.setVisibility(View.VISIBLE);
                        }

                        loadAccFocusMinutesForCurrentTask();
                        if (activeTodayTask.getTotalFocusTimeMinutes() > totalFocusMinutesAcc) {
                            totalFocusMinutesAcc = activeTodayTask.getTotalFocusTimeMinutes();
                        }
                    } else {
                        if (cardActiveTask != null) {
                            cardActiveTask.setVisibility(View.GONE);
                        }
                    }

                    updateTaskFocusStats();
                } else {
                    if (cardActiveTask != null) cardActiveTask.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                if (cardActiveTask != null) cardActiveTask.setVisibility(View.GONE);
            }
        });
    }

    private boolean isTaskForToday(Task task, Calendar todayCal) {
        if (task == null || task.getTime() == null || task.getTime().trim().isEmpty()) {
            return false;
        }

        String taskTime = task.getTime().trim();

        int todayDay = todayCal.get(Calendar.DAY_OF_MONTH);
        int todayMonth = todayCal.get(Calendar.MONTH) + 1;
        int todayYear = todayCal.get(Calendar.YEAR);

        java.util.regex.Matcher matcherYmd = java.util.regex.Pattern.compile("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})").matcher(taskTime);
        if (matcherYmd.find()) {
            try {
                int y = Integer.parseInt(matcherYmd.group(1));
                int m = Integer.parseInt(matcherYmd.group(2));
                int d = Integer.parseInt(matcherYmd.group(3));
                return (d == todayDay && m == todayMonth && y == todayYear);
            } catch (Exception ignored) {}
        }

        java.util.regex.Matcher matcherDm = java.util.regex.Pattern.compile("(?:^|[^0-9])(\\d{1,2})[/-](\\d{1,2})(?:[^0-9]|$)").matcher(taskTime);
        if (matcherDm.find()) {
            try {
                int d = Integer.parseInt(matcherDm.group(1));
                int m = Integer.parseInt(matcherDm.group(2));
                return (d == todayDay && m == todayMonth);
            } catch (Exception ignored) {}
        }

        return taskTime.contains("Hôm nay") || taskTime.contains("hôm nay");
    }

    private void showTaskDetailAndSwitchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout dialogCard = new LinearLayout(this);
        dialogCard.setOrientation(LinearLayout.VERTICAL);
        dialogCard.setBackgroundResource(R.drawable.bg_rounded_24dp);
        dialogCard.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_bg)));
        dialogCard.setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("📋 Chi Tiết Công Việc Đang Tập Trung");
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        dialogCard.addView(tvTitle);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setPadding(0, dpToPx(12), 0, dpToPx(12));
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(layout);

        LinearLayout cardTitleBox = new LinearLayout(this);
        cardTitleBox.setOrientation(LinearLayout.VERTICAL);
        cardTitleBox.setBackgroundResource(R.drawable.bg_search_bar);
        cardTitleBox.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp1.setMargins(0, 0, 0, dpToPx(10));
        cardTitleBox.setLayoutParams(lp1);

        TextView tvSub1 = new TextView(this);
        tvSub1.setText("📌 TÊN ĐẦY ĐỦ CÔNG VIỆC");
        tvSub1.setTextSize(11);
        tvSub1.setTypeface(null, Typeface.BOLD);
        tvSub1.setTextColor(ContextCompat.getColor(this, R.color.brand_primary));

        TextView tvVal1 = new TextView(this);
        tvVal1.setText(activeTaskTitle != null && !activeTaskTitle.isEmpty() ? activeTaskTitle : "Chưa chọn công việc");
        tvVal1.setTextSize(14);
        tvVal1.setTypeface(null, Typeface.BOLD);
        tvVal1.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tvVal1.setPadding(0, dpToPx(4), 0, 0);

        cardTitleBox.addView(tvSub1);
        cardTitleBox.addView(tvVal1);
        layout.addView(cardTitleBox);

        LinearLayout cardGoalBox = new LinearLayout(this);
        cardGoalBox.setOrientation(LinearLayout.VERTICAL);
        cardGoalBox.setBackgroundResource(R.drawable.bg_search_bar);
        cardGoalBox.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.setMargins(0, 0, 0, dpToPx(12));
        cardGoalBox.setLayoutParams(lp2);

        TextView tvSub2 = new TextView(this);
        tvSub2.setText("⏱ MỤC TIÊU THỜI GIAN TASK NÀY");
        tvSub2.setTextSize(11);
        tvSub2.setTypeface(null, Typeface.BOLD);
        tvSub2.setTextColor(ContextCompat.getColor(this, R.color.brand_primary));

        TextView tvVal2 = new TextView(this);
        tvVal2.setText(String.format(Locale.getDefault(), "%.1f giờ tập trung", goalHours));
        tvVal2.setTextSize(14);
        tvVal2.setTypeface(null, Typeface.BOLD);
        tvVal2.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tvVal2.setPadding(0, dpToPx(4), 0, 0);

        cardGoalBox.addView(tvSub2);
        cardGoalBox.addView(tvVal2);
        layout.addView(cardGoalBox);

        builder.setView(dialogCard);
        AlertDialog dialog = builder.create();

        Calendar todayCal = Calendar.getInstance();
        List<Task> todayTaskList = new ArrayList<>();
        java.util.Set<String> seenTitles = new java.util.HashSet<>();

        for (Task t : userTaskList) {
            if (isTaskForToday(t, todayCal)) {
                String cleanTitle = t.getTitle() != null ? t.getTitle().trim() : "";
                if (!cleanTitle.isEmpty() && !seenTitles.contains(cleanTitle)) {
                    seenTitles.add(cleanTitle);
                    todayTaskList.add(t);
                }
            }
        }

        if (!todayTaskList.isEmpty()) {
            TextView tvSwitchHeader = new TextView(this);
            tvSwitchHeader.setText("🔄 ĐỔI SANG CÔNG VIỆC KHÁC TRONG NGÀY:");
            tvSwitchHeader.setTextSize(11);
            tvSwitchHeader.setTypeface(null, Typeface.BOLD);
            tvSwitchHeader.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvSwitchHeader.setPadding(0, dpToPx(4), 0, dpToPx(8));
            layout.addView(tvSwitchHeader);

            for (Task t : todayTaskList) {
                TextView item = new TextView(this);
                item.setText("• " + t.getTitle());
                item.setTextSize(13);
                item.setTypeface(null, Typeface.BOLD);
                item.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                item.setBackgroundResource(R.drawable.bg_search_bar);
                item.setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10));
                LinearLayout.LayoutParams lpItem = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lpItem.setMargins(0, 0, 0, dpToPx(6));
                item.setLayoutParams(lpItem);

                item.setOnClickListener(v -> {
                    activeTaskId = t.getId();
                    activeTaskTitle = t.getTitle();

                    SharedPreferences pref = getSharedPreferences("PomodoroStats", MODE_PRIVATE);
                    float savedGoal = pref.getFloat("CUSTOM_GOAL_" + activeTaskTitle.hashCode(), -1f);
                    if (savedGoal > 0) {
                        goalHours = savedGoal;
                    } else {
                        goalHours = parseHoursFromTimeStr(t.getTime());
                    }

                    if (tvActiveTaskTitle != null) tvActiveTaskTitle.setText(activeTaskTitle);
                    if (cardActiveTask != null) cardActiveTask.setVisibility(View.VISIBLE);

                    loadAccFocusMinutesForCurrentTask();
                    Toast.makeText(PomodoroActivity.this, "Đã chuyển sang: " + activeTaskTitle, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });

                layout.addView(item);
            }
        }

        dialogCard.addView(scrollView);

        TextView btnClose = new TextView(this);
        btnClose.setText("ĐÓNG");
        btnClose.setTextSize(13);
        btnClose.setTypeface(null, Typeface.BOLD);
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setTextColor(Color.WHITE);
        btnClose.setBackgroundResource(R.drawable.bg_pill_brown);
        LinearLayout.LayoutParams lpClose = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(48));
        btnClose.setLayoutParams(lpClose);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialogCard.addView(btnClose);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private double parseHoursFromTimeStr(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return 2.0;

        try {
            if (timeStr.contains("🕒")) {
                String clockPart = timeStr.substring(timeStr.indexOf("🕒") + 1);
                String clean = clockPart.replaceAll("[^0-9.]", " ").trim();
                String[] parts = clean.split("\\s+");
                for (String p : parts) {
                    if (!p.isEmpty()) {
                        double val = Double.parseDouble(p);
                        if (val > 0 && val <= 24) {
                            return val;
                        }
                    }
                }
            }

            String cleanDate = timeStr.replaceAll("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}", " ")
                                      .replaceAll("\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}", " ");
            String clean = cleanDate.replaceAll("[^0-9.]", " ").trim();
            String[] parts = clean.split("\\s+");
            for (String p : parts) {
                if (!p.isEmpty()) {
                    double val = Double.parseDouble(p);
                    if (val > 0 && val <= 24) {
                        return val;
                    }
                }
            }
        } catch (Exception ignored) {}

        return 2.0;
    }

    private void updateTaskFocusStats() {
        int currentSecs = totalFocusSecondsAcc;
        double currentGoal = goalHours;
        String labelFocus = "Tổng tập trung";
        String labelGoal = "🎯 Mục tiêu ngày";

        if (currentTimerMode == MODE_SHORT_BREAK) {
            currentSecs = totalShortBreakSecondsAcc;
            currentGoal = shortBreakGoalHours;
            labelFocus = "Tổng nghỉ ngắn";
            labelGoal = "☕ Mục tiêu nghỉ ngắn";
        } else if (currentTimerMode == MODE_LONG_BREAK) {
            currentSecs = totalLongBreakSecondsAcc;
            currentGoal = longBreakGoalHours;
            labelFocus = "Tổng nghỉ dài";
            labelGoal = "🌿 Mục tiêu nghỉ dài";
        }

        if (tvFocusLabel != null) tvFocusLabel.setText(labelFocus);
        if (tvGoalLabel != null) tvGoalLabel.setText(labelGoal);

        double totalHoursAcc = currentSecs / 3600.0;
        double remainingGoalHours = Math.max(0.0, currentGoal - totalHoursAcc);
        int percent = currentGoal > 0 ? (int) Math.round((totalHoursAcc / currentGoal) * 100) : 0;
        percent = Math.min(percent, 100);

        if (tvTotalFocusHours != null) {
            int hrs = currentSecs / 3600;
            int mins = (currentSecs % 3600) / 60;
            int secs = currentSecs % 60;

            if (hrs > 0) {
                tvTotalFocusHours.setText(String.format(Locale.getDefault(), "%dh %dm %ds", hrs, mins, secs));
            } else if (mins > 0) {
                tvTotalFocusHours.setText(String.format(Locale.getDefault(), "0h %dm %ds", mins, secs));
            } else {
                tvTotalFocusHours.setText(String.format(Locale.getDefault(), "0h 0m %ds", secs));
            }
        }

        if (tvGoalProgress != null) {
            if (remainingGoalHours > 0) {
                tvGoalProgress.setText(String.format(Locale.getDefault(), "%.1fh còn lại (%d%%)", remainingGoalHours, percent));
            } else {
                tvGoalProgress.setText(String.format(Locale.getDefault(), "✔ Hoàn thành (%d%%)", percent));
            }
        }

        if (progressTask != null) {
            progressTask.setProgress(percent);
        }
    }

    private void setupTabs() {
        tabFocus.setOnClickListener(v -> {
            currentTimerMode = MODE_FOCUS;
            setTimerMode(25, tabFocus);
        });
        tabShortBreak.setOnClickListener(v -> {
            currentTimerMode = MODE_SHORT_BREAK;
            setTimerMode(5, tabShortBreak);
        });
        tabLongBreak.setOnClickListener(v -> {
            currentTimerMode = MODE_LONG_BREAK;
            setTimerMode(15, tabLongBreak);
        });
    }

    private void setTimerMode(int minutes, TextView selectedTab) {
        if (isTimerRunning) {
            pauseTimer();
        }

        int inactiveTabColor = androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary);
        tabFocus.setBackground(null);
        tabFocus.setTextColor(inactiveTabColor);
        tabShortBreak.setBackground(null);
        tabShortBreak.setTextColor(inactiveTabColor);
        tabLongBreak.setBackground(null);
        tabLongBreak.setTextColor(inactiveTabColor);

        selectedTab.setBackgroundResource(R.drawable.bg_pill_brown);
        selectedTab.setTextColor(0xFFFFFFFF);

        timerDurationInMillis = minutes * 60 * 1000L;
        timeRemainingInMillis = timerDurationInMillis;
        updateTimerText();

        loadAccFocusMinutesForCurrentTask();
    }

    private void setupTimerControls() {
        btnStartTimer.setOnClickListener(v -> {
            if (isTimerRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnResetTimer.setOnClickListener(v -> resetTimer());

        View.OnClickListener editTimerClick = v -> showCustomTimerDurationDialog();
        if (tvTimerDisplay != null) tvTimerDisplay.setOnClickListener(editTimerClick);
        if (circularTimerView != null) circularTimerView.setOnClickListener(editTimerClick);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void showCustomTimerDurationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout dialogCard = new LinearLayout(this);
        dialogCard.setOrientation(LinearLayout.VERTICAL);
        dialogCard.setBackgroundResource(R.drawable.bg_rounded_24dp);
        dialogCard.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_bg)));
        dialogCard.setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⏱ Đặt Thời Gian Bấm Giờ (Phút)");
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        dialogCard.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("Chọn nhanh hoặc nhập số phút tập trung mong muốn:");
        tvSub.setTextSize(13);
        tvSub.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvSub.setPadding(0, dpToPx(6), 0, dpToPx(14));
        dialogCard.addView(tvSub);

        android.widget.HorizontalScrollView hsv = new android.widget.HorizontalScrollView(this);
        hsv.setPadding(0, 0, 0, dpToPx(14));
        hsv.setHorizontalScrollBarEnabled(false);

        LinearLayout layoutMinPresets = new LinearLayout(this);
        layoutMinPresets.setOrientation(LinearLayout.HORIZONTAL);

        int currentMinutes = (int) (timerDurationInMillis / 60000);
        if (currentMinutes <= 0) currentMinutes = 25;

        final EditText edtValue = new EditText(this);
        edtValue.setInputType(InputType.TYPE_CLASS_NUMBER);
        edtValue.setText(String.valueOf(currentMinutes));

        int[] minPresets = {15, 25, 30, 45, 60, 90, 120};
        for (int p : minPresets) {
            TextView btn = new TextView(this);
            btn.setText(p + " phút");
            btn.setTextSize(12);
            btn.setTypeface(null, Typeface.BOLD);
            btn.setTextColor(0xFFFFFFFF);
            btn.setBackgroundResource(R.drawable.bg_pill_brown);
            btn.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, dpToPx(8), 0);
            btn.setLayoutParams(lp);
            btn.setOnClickListener(v -> {
                edtValue.setText(String.valueOf(p));
                edtValue.setSelection(edtValue.getText().length());
            });
            layoutMinPresets.addView(btn);
        }
        hsv.addView(layoutMinPresets);
        dialogCard.addView(hsv);

        LinearLayout inputContainer = new LinearLayout(this);
        inputContainer.setOrientation(LinearLayout.HORIZONTAL);
        inputContainer.setGravity(Gravity.CENTER_VERTICAL);
        inputContainer.setBackgroundResource(R.drawable.bg_search_bar);
        inputContainer.setPadding(dpToPx(16), 0, dpToPx(16), 0);
        LinearLayout.LayoutParams lpInput = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(50)
        );
        lpInput.setMargins(0, 0, 0, dpToPx(16));
        inputContainer.setLayoutParams(lpInput);

        ImageView imgClock = new ImageView(this);
        imgClock.setImageResource(android.R.drawable.ic_menu_recent_history);
        imgClock.setColorFilter(ContextCompat.getColor(this, R.color.brand_primary));
        inputContainer.addView(imgClock, new LinearLayout.LayoutParams(dpToPx(20), dpToPx(20)));

        edtValue.setBackground(null);
        edtValue.setTextSize(16);
        edtValue.setTypeface(null, Typeface.BOLD);
        edtValue.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        edtValue.setHintTextColor(ContextCompat.getColor(this, R.color.text_muted));
        LinearLayout.LayoutParams lpEt = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        lpEt.setMargins(dpToPx(12), 0, 0, 0);
        inputContainer.addView(edtValue, lpEt);
        dialogCard.addView(inputContainer);

        builder.setView(dialogCard);
        AlertDialog dialog = builder.create();

        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setGravity(Gravity.CENTER_VERTICAL);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("HỦY");
        btnCancel.setTextSize(13);
        btnCancel.setTypeface(null, Typeface.BOLD);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        btnCancel.setBackgroundResource(R.drawable.bg_pill_light);
        btnCancel.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        TextView btnStart = new TextView(this);
        btnStart.setText("BẮT ĐẦU ĐẾM");
        btnStart.setTextSize(13);
        btnStart.setTypeface(null, Typeface.BOLD);
        btnStart.setGravity(Gravity.CENTER);
        btnStart.setTextColor(Color.WHITE);
        btnStart.setBackgroundResource(R.drawable.bg_pill_brown);
        btnStart.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        btnStart.setOnClickListener(v -> {
            String input = edtValue.getText().toString().trim();
            if (!input.isEmpty()) {
                try {
                    int min = Integer.parseInt(input);
                    if (min > 0) {
                        long durationMs = min * 60 * 1000L;
                        if (isTimerRunning) {
                            pauseTimer();
                        }
                        timerDurationInMillis = durationMs;
                        timeRemainingInMillis = durationMs;
                        updateTimerText();

                        Toast.makeText(this, "⏱ Đã thiết lập thời gian tập trung: " + min + " phút!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(this, "Vui lòng nhập số phút lớn hơn 0!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ignored) {}
            }
        });

        LinearLayout.LayoutParams lpCancel = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        lpCancel.setMargins(0, 0, dpToPx(8), 0);
        btnCancel.setLayoutParams(lpCancel);

        LinearLayout.LayoutParams lpStart = new LinearLayout.LayoutParams(0, dpToPx(48), 1.5f);
        lpStart.setMargins(dpToPx(8), 0, 0, 0);
        btnStart.setLayoutParams(lpStart);

        btnLayout.addView(btnCancel);
        btnLayout.addView(btnStart);
        dialogCard.addView(btnLayout);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void setupAmbientSoundControls() {
        if (chipSound1 != null) chipSound1.setOnClickListener(v -> selectSoundByIndex(0));
        if (chipSound2 != null) chipSound2.setOnClickListener(v -> selectSoundByIndex(1));
        if (chipSound3 != null) chipSound3.setOnClickListener(v -> selectSoundByIndex(2));
        if (chipSound4 != null) chipSound4.setOnClickListener(v -> selectSoundByIndex(3));
        if (chipSound5 != null) chipSound5.setOnClickListener(v -> selectSoundByIndex(4));
        if (chipSound6 != null) chipSound6.setOnClickListener(v -> selectSoundByIndex(5));

        View.OnClickListener togglePlayClick = v -> toggleSoundPlayback();

        if (btnPlayPauseSound != null) {
            btnPlayPauseSound.setOnClickListener(togglePlayClick);
        }
        if (btnSoundToggle != null) {
            btnSoundToggle.setOnClickListener(togglePlayClick);
        }

        if (cardSoundSelector != null) {
            cardSoundSelector.setOnClickListener(v -> showSoundSelectionDialog());
        }
    }

    private void selectSoundByIndex(int index) {
        selectedSoundIndex = index;

        int inactiveChipColor = androidx.core.content.ContextCompat.getColor(this, R.color.brand_primary);
        TextView[] soundChips = {chipSound1, chipSound2, chipSound3, chipSound4, chipSound5, chipSound6};
        for (int i = 0; i < soundChips.length; i++) {
            if (soundChips[i] != null) {
                if (i == index) {
                    soundChips[i].setBackgroundResource(R.drawable.bg_pill_brown);
                    soundChips[i].setTextColor(0xFFFFFFFF);
                } else {
                    soundChips[i].setBackgroundResource(R.drawable.bg_pill_light);
                    soundChips[i].setTextColor(inactiveChipColor);
                }
            }
        }

        if (tvSoundTitle != null) tvSoundTitle.setText("Âm thanh nền: " + soundTitles[selectedSoundIndex]);
        if (tvSoundSubtitle != null) tvSoundSubtitle.setText(soundSubtitles[selectedSoundIndex]);

        if (!isSoundPlaying) {
            playAmbientAudio(soundFrequencies[selectedSoundIndex]);
            isSoundPlaying = true;
            if (btnPlayPauseSound != null) btnPlayPauseSound.setImageResource(android.R.drawable.ic_media_pause);
            Toast.makeText(this, "🔊 Đang phát: " + soundTitles[selectedSoundIndex], Toast.LENGTH_SHORT).show();
        } else {
            stopAmbientAudio();
            playAmbientAudio(soundFrequencies[selectedSoundIndex]);
            Toast.makeText(this, "🔊 Đã đổi sang: " + soundTitles[selectedSoundIndex], Toast.LENGTH_SHORT).show();
        }
    }

    private void showSoundSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎧 Chọn âm thanh nền thư giãn (6 loại)");
        builder.setSingleChoiceItems(soundTitles, selectedSoundIndex, (dialog, which) -> {
            selectSoundByIndex(which);
            dialog.dismiss();
        });
        builder.setNegativeButton("Đóng", null);
        builder.show();
    }

    private void toggleSoundPlayback() {
        if (isSoundPlaying) {
            stopAmbientAudio();
            isSoundPlaying = false;
            if (btnPlayPauseSound != null) btnPlayPauseSound.setImageResource(android.R.drawable.ic_media_play);
            Toast.makeText(this, "Đã tạm dừng âm thanh nền", Toast.LENGTH_SHORT).show();
        } else {
            playAmbientAudio(soundFrequencies[selectedSoundIndex]);
            isSoundPlaying = true;
            if (btnPlayPauseSound != null) btnPlayPauseSound.setImageResource(android.R.drawable.ic_media_pause);
            Toast.makeText(this, "🔊 Đang phát: " + soundTitles[selectedSoundIndex], Toast.LENGTH_SHORT).show();
        }
    }

    private void playAmbientAudio(double frequency) {
        stopAmbientAudio();

        audioThread = new Thread(() -> {
            int sampleRate = 44100;
            int numSamples = sampleRate;
            double[] sample = new double[numSamples];
            byte[] generatedSnd = new byte[2 * numSamples];

            for (int i = 0; i < numSamples; ++i) {
                sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / frequency));
            }

            int idx = 0;
            for (double dVal : sample) {
                short val = (short) ((dVal * 32767) * 0.15);
                generatedSnd[idx++] = (byte) (val & 0x00ff);
                generatedSnd[idx++] = (byte) ((val & 0xff00) >> 8);
            }

            int bufferSize = AudioTrack.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

            try {
                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, bufferSize,
                        AudioTrack.MODE_STREAM);

                audioTrack.play();

                while (isSoundPlaying && !Thread.currentThread().isInterrupted()) {
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        audioThread.start();
    }

    private void stopAmbientAudio() {
        if (audioTrack != null) {
            try {
                audioTrack.stop();
                audioTrack.release();
            } catch (Exception ignored) {}
            audioTrack = null;
        }

        if (audioThread != null) {
            audioThread.interrupt();
            audioThread = null;
        }
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeRemainingInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemainingInMillis = millisUntilFinished;
                updateTimerText();

                if (activeTaskTitle != null && !activeTaskTitle.isEmpty()) {
                    SharedPreferences pref = getSharedPreferences("PomodoroStats", MODE_PRIVATE);
                    String taskHash = String.valueOf(activeTaskTitle.hashCode());

                    if (currentTimerMode == MODE_FOCUS) {
                        totalFocusSecondsAcc++;
                        pref.edit().putInt("ACC_SECONDS_" + taskHash, totalFocusSecondsAcc).apply();
                    } else if (currentTimerMode == MODE_SHORT_BREAK) {
                        totalShortBreakSecondsAcc++;
                        pref.edit().putInt("ACC_SHORT_BREAK_SECONDS_" + taskHash, totalShortBreakSecondsAcc).apply();
                    } else if (currentTimerMode == MODE_LONG_BREAK) {
                        totalLongBreakSecondsAcc++;
                        pref.edit().putInt("ACC_LONG_BREAK_SECONDS_" + taskHash, totalLongBreakSecondsAcc).apply();
                    }

                    updateTaskFocusStats();
                }
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                btnStartTimer.setText("▶ Bắt đầu");

                int sessionMinutesCompleted = (int) (timerDurationInMillis / 60000);
                if (sessionMinutesCompleted <= 0) sessionMinutesCompleted = 25;

                if (activeTaskId > 0) {
                    RetrofitClient.getApiService().completePomodoro(activeTaskId, sessionMinutesCompleted).enqueue(new Callback<Task>() {
                        @Override
                        public void onResponse(Call<Task> call, Response<Task> response) {}

                        @Override
                        public void onFailure(Call<Task> call, Throwable t) {}
                    });
                }

                updateTaskFocusStats();

                Toast.makeText(PomodoroActivity.this, "🎉 Hoàn thành phiên tập trung! Đã đồng bộ giờ tập trung.", Toast.LENGTH_LONG).show();

                if (currentSession < totalSessions) {
                    currentSession++;
                    tvSessionCount.setText("● Phiên " + currentSession + " / " + totalSessions);
                }
            }
        }.start();

        isTimerRunning = true;
        btnStartTimer.setText("⏸ Tạm dừng");
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        btnStartTimer.setText("▶ Bắt đầu");
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        timeRemainingInMillis = timerDurationInMillis;
        updateTimerText();
        btnStartTimer.setText("▶ Bắt đầu");
    }

    private void updateTimerText() {
        int minutes = (int) (timeRemainingInMillis / 1000) / 60;
        int seconds = (int) (timeRemainingInMillis / 1000) % 60;
        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvTimerDisplay.setText(timeFormatted);

        if (circularTimerView != null && timerDurationInMillis > 0) {
            float ratio = (float) ((double) timeRemainingInMillis / timerDurationInMillis);
            circularTimerView.setProgressRatio(ratio);
        }
    }

    private void setupBottomNav() {
        if (navAgenda != null) {
            navAgenda.setOnClickListener(v -> {
                Intent intent = new Intent(PomodoroActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(PomodoroActivity.this, AddTaskActivity.class);
                startActivity(intent);
            });
        }

        if (navProjects != null) {
            navProjects.setOnClickListener(v -> {
                Intent intent = new Intent(PomodoroActivity.this, ProjectsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            });
        }

        if (navTimeline != null) {
            navTimeline.setOnClickListener(v -> {
                Intent intent = new Intent(PomodoroActivity.this, AiAutoScheduleActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        stopAmbientAudio();
        super.onDestroy();
    }
}
