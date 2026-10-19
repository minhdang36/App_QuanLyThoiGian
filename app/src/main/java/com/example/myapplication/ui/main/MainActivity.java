package com.example.myapplication.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.CalendarAdapter;
import com.example.myapplication.adapter.FullCalendarAdapter;
import com.example.myapplication.adapter.TaskAdapter;
import com.example.myapplication.model.CalendarDate;
import com.example.myapplication.model.Task;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.ui.ai.AiAutoScheduleActivity;
import com.example.myapplication.ui.ai.ScanScheduleActivity;
import com.example.myapplication.ui.pomodoro.PomodoroActivity;
import com.example.myapplication.ui.project.ProjectsActivity;
import com.example.myapplication.ui.task.AddTaskActivity;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String[] TIMEZONE_OPTIONS = new String[]{
            "Asia/Ho_Chi_Minh (UTC+7) - Việt Nam",
            "Asia/Bangkok (UTC+7) - Thái Lan",
            "Asia/Tokyo (UTC+9) - Nhật Bản",
            "Asia/Seoul (UTC+9) - Hàn Quốc",
            "Asia/Singapore (UTC+8) - Singapore",
            "Asia/Shanghai (UTC+8) - Trung Quốc",
            "Europe/London (UTC+0) - Anh",
            "Europe/Paris (UTC+1) - Pháp",
            "America/New_York (UTC-5) - Mỹ (Đông)",
            "America/Los_Angeles (UTC-8) - Mỹ (Tây)",
            "America/Chicago (UTC-6) - Mỹ (Trung tâm)"
    };

    private static final String[] TIMEZONE_IDS = new String[]{
            "Asia/Ho_Chi_Minh",
            "Asia/Bangkok",
            "Asia/Tokyo",
            "Asia/Seoul",
            "Asia/Singapore",
            "Asia/Shanghai",
            "Europe/London",
            "Europe/Paris",
            "America/New_York",
            "America/Los_Angeles",
            "America/Chicago"
    };

    private TextView tvTimeZoneValue;
    private TextView tvAgendaDate;
    private TextView tvTasksToday;
    private TextView tvMonthYear;

    private ImageButton btnPrevMonth, btnNextMonth;
    private ImageView imgAvatar;

    private RecyclerView rvHorizontalCalendar;
    private View layoutFullCalendar;
    private RecyclerView rvFullCalendarGrid;

    private CalendarAdapter calendarAdapter;
    private FullCalendarAdapter fullCalendarAdapter;
    private final List<CalendarDate> dateList = new ArrayList<>();
    private final Set<Integer> currentDaysWithEvents = new HashSet<>();

    private RecyclerView rvTasks;
    private TaskAdapter taskAdapter;
    private final List<Task> allTaskList = new ArrayList<>();
    private final List<Task> displayedTaskList = new ArrayList<>();

    private View bottomSheet;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    private final Calendar currentCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1. ÁNH XẠ VIEW
        tvTimeZoneValue = findViewById(R.id.tvTimeZoneValue);
        tvAgendaDate = findViewById(R.id.tvAgendaDate);
        tvTasksToday = findViewById(R.id.tvTasksToday);
        tvMonthYear = findViewById(R.id.tvMonthYear);

        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        imgAvatar = findViewById(R.id.imgAvatar);

        rvHorizontalCalendar = findViewById(R.id.rvHorizontalCalendar);
        layoutFullCalendar = findViewById(R.id.layoutFullCalendar);
        rvFullCalendarGrid = findViewById(R.id.rvFullCalendarGrid);

        bottomSheet = findViewById(R.id.layoutAgendaBottomSheet);
        rvTasks = findViewById(R.id.rvTasks);

        // 2. KHỞI TẠO LỊCH & THÔNG TIN HEADER
        setupHeaderAndCalendar();

        // 3. KHỞI TẠO DANH SÁCH TASK
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(displayedTaskList);
        rvTasks.setAdapter(taskAdapter);

        // 4. THIẾT LẬP BOTTOM SHEET (KÉO LÊN KÉO XUỐNG)
        setupBottomSheet();

        // 5. THIẾT LẬP BOTTOM NAV & CLICKS
        setupClickListeners();

        // 6. TẢI DỮ LIỆU TỪ SERVER
        loadTasksFromServer();
    }

    private void setupHeaderAndCalendar() {
        // Cài đặt và lắng nghe sự kiện đổi Múi giờ
        setupTimeZoneHeader();

        // LayoutManager cho lịch grid (7 cột: T2 -> CN)
        rvFullCalendarGrid.setLayoutManager(new GridLayoutManager(this, 7));

        // LayoutManager cho lịch ngang
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvHorizontalCalendar.setLayoutManager(layoutManager);

        // Hiển thị dữ liệu lịch tháng hiện tại
        refreshAllCalendarViews();

        if (btnPrevMonth != null) {
            btnPrevMonth.setOnClickListener(v -> {
                int day = currentCalendar.get(Calendar.DAY_OF_MONTH);
                currentCalendar.add(Calendar.MONTH, -1);
                int maxDays = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                currentCalendar.set(Calendar.DAY_OF_MONTH, Math.min(day, maxDays));
                refreshAllCalendarViews();
            });
        }

        if (btnNextMonth != null) {
            btnNextMonth.setOnClickListener(v -> {
                int day = currentCalendar.get(Calendar.DAY_OF_MONTH);
                currentCalendar.add(Calendar.MONTH, 1);
                int maxDays = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                currentCalendar.set(Calendar.DAY_OF_MONTH, Math.min(day, maxDays));
                refreshAllCalendarViews();
            });
        }
    }

    private void setupTimeZoneHeader() {
        View layoutTimeZone = findViewById(R.id.layoutTimeZone);
        TextView tvTimeZoneLabel = findViewById(R.id.tvTimeZoneLabel);

        SharedPreferences sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE);
        String savedTz = sharedPref.getString("SELECTED_TIMEZONE", TimeZone.getDefault().getID());
        applyTimeZone(savedTz);

        View.OnClickListener openDialogListener = v -> showTimeZoneDialog();

        if (layoutTimeZone != null) layoutTimeZone.setOnClickListener(openDialogListener);
        if (tvTimeZoneLabel != null) tvTimeZoneLabel.setOnClickListener(openDialogListener);
        if (tvTimeZoneValue != null) tvTimeZoneValue.setOnClickListener(openDialogListener);
    }

    private void showTimeZoneDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Chọn Múi Giờ")
                .setItems(TIMEZONE_OPTIONS, (dialog, which) -> {
                    String selectedTzId = TIMEZONE_IDS[which];
                    SharedPreferences sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE);
                    sharedPref.edit().putString("SELECTED_TIMEZONE", selectedTzId).apply();
                    applyTimeZone(selectedTzId);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void applyTimeZone(String tzId) {
        TimeZone tz = TimeZone.getTimeZone(tzId);
        currentCalendar.setTimeZone(tz);

        int offsetHours = tz.getRawOffset() / 3600000;
        String offsetSign = offsetHours >= 0 ? "+" : "";
        if (tvTimeZoneValue != null) {
            tvTimeZoneValue.setText(tz.getID() + " (UTC" + offsetSign + offsetHours + ")");
        }

        refreshAllCalendarViews();
    }

    private List<int[]> parseDatesFromTimeStr(String timeStr, int realTodayYear) {
        List<int[]> dates = new ArrayList<>();
        if (timeStr == null || timeStr.trim().isEmpty()) return dates;

        try {
            String cleanTime = timeStr.replaceAll("[^0-9/-]", " ");
            String[] parts = cleanTime.trim().split("\\s+");

            for (String part : parts) {
                if (part.contains("/")) {
                    String[] dateParts = part.split("/");
                    if (dateParts.length >= 2) {
                        int p1 = Integer.parseInt(dateParts[0].trim());
                        int p2 = Integer.parseInt(dateParts[1].trim());
                        int p3 = (dateParts.length >= 3) ? Integer.parseInt(dateParts[2].trim()) : realTodayYear;

                        int day, month, year;
                        if (p1 > 1000) {
                            year = p1;
                            month = p2;
                            day = p3;
                        } else {
                            day = p1;
                            month = p2;
                            year = p3;
                        }
                        dates.add(new int[]{day, month, year});
                    }
                } else if (part.contains("-")) {
                    String[] dateParts = part.split("-");
                    if (dateParts.length >= 2) {
                        int p1 = Integer.parseInt(dateParts[0].trim());
                        int p2 = Integer.parseInt(dateParts[1].trim());
                        int p3 = (dateParts.length >= 3) ? Integer.parseInt(dateParts[2].trim()) : realTodayYear;

                        int day, month, year;
                        if (p1 > 1000) {
                            year = p1;
                            month = p2;
                            day = p3;
                        } else {
                            day = p1;
                            month = p2;
                            year = p3;
                        }
                        dates.add(new int[]{day, month, year});
                    }
                }
            }
        } catch (Exception ignored) {}

        return dates;
    }

    private void updateDaysWithEventsForCurrentCalendar() {
        currentDaysWithEvents.clear();

        int viewedMonth = currentCalendar.get(Calendar.MONTH) + 1;
        int viewedYear = currentCalendar.get(Calendar.YEAR);

        Calendar realTodayCal = Calendar.getInstance();
        int realTodayDay = realTodayCal.get(Calendar.DAY_OF_MONTH);
        int realTodayMonth = realTodayCal.get(Calendar.MONTH) + 1;
        int realTodayYear = realTodayCal.get(Calendar.YEAR);

        for (Task t : allTaskList) {
            String timeStr = t.getTime();
            if (timeStr == null || timeStr.trim().isEmpty()) {
                if (viewedMonth == realTodayMonth && viewedYear == realTodayYear) {
                    currentDaysWithEvents.add(realTodayDay);
                }
                continue;
            }

            if (timeStr.contains("Hôm nay")) {
                if (viewedMonth == realTodayMonth && viewedYear == realTodayYear) {
                    currentDaysWithEvents.add(realTodayDay);
                }
            }

            List<int[]> dates = parseDatesFromTimeStr(timeStr, realTodayYear);
            for (int[] d : dates) {
                int day = d[0];
                int month = d[1];
                int year = d[2];

                if (month == viewedMonth && year == viewedYear) {
                    currentDaysWithEvents.add(day);
                }
            }
        }
    }

    private void refreshAllCalendarViews() {
        Locale localeVN = new Locale("vi", "VN");

        // Cập nhật tập hợp các ngày có lịch/task thuộc đúng tháng & năm đang xem
        updateDaysWithEventsForCurrentCalendar();

        // 1. Cập nhật Tiêu đề Tháng/Năm
        SimpleDateFormat monthFormat = new SimpleDateFormat("'Tháng' MM 'năm' yyyy", localeVN);
        if (tvMonthYear != null) {
            tvMonthYear.setText(monthFormat.format(currentCalendar.getTime()));
        }

        // 2. Cập nhật Tiêu đề Ngày ở Bottom Sheet
        SimpleDateFormat agendaFormat = new SimpleDateFormat("'●' EEEE, dd 'Thg' MM, yyyy", localeVN);
        if (tvAgendaDate != null) {
            tvAgendaDate.setText(agendaFormat.format(currentCalendar.getTime()));
        }

        // 3. Cập nhật Lịch Lưới (Full Grid Calendar)
        Calendar cal = (Calendar) currentCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // Sunday=1, Monday=2...
        int leadingEmpty = (firstDayOfWeek + 5) % 7; // Căn chỉnh cột T2=0, T3=1... CN=6
        int daysInMonthCount = cal.getActualMaximum(Calendar.DAY_OF_MONTH); // 28, 29, 30 hoặc 31 ngày thực tế

        List<String> daysInMonth = new ArrayList<>();
        for (int i = 0; i < leadingEmpty; i++) {
            daysInMonth.add("");
        }
        for (int i = 1; i <= daysInMonthCount; i++) {
            daysInMonth.add(String.valueOf(i));
        }

        // Đảm bảo số ô là bội số của 7 (tối thiểu 35 ô)
        int totalCells = daysInMonth.size();
        int targetSize = totalCells <= 35 ? 35 : ((totalCells + 6) / 7) * 7;
        while (daysInMonth.size() < targetSize) {
            daysInMonth.add("");
        }

        int currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH);
        int gridSelectedIndex = leadingEmpty + currentDay - 1;

        fullCalendarAdapter = new FullCalendarAdapter(daysInMonth, gridSelectedIndex, dayNumber -> onDateSelected(dayNumber));
        fullCalendarAdapter.setDaysWithEvents(currentDaysWithEvents);
        rvFullCalendarGrid.setAdapter(fullCalendarAdapter);

        // 4. Cập nhật Lịch Ngang (Horizontal Calendar)
        dateList.clear();
        Calendar horizontalCal = (Calendar) currentCalendar.clone();
        horizontalCal.set(Calendar.DAY_OF_MONTH, 1);

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", localeVN);
        SimpleDateFormat numberFormat = new SimpleDateFormat("dd", localeVN);

        int horizontalSelectedIndex = currentDay - 1;

        for (int i = 1; i <= daysInMonthCount; i++) {
            horizontalCal.set(Calendar.DAY_OF_MONTH, i);
            String dayName = dayFormat.format(horizontalCal.getTime());
            String dayNum = numberFormat.format(horizontalCal.getTime());
            dateList.add(new CalendarDate(dayName, dayNum, i == currentDay));
        }

        calendarAdapter = new CalendarAdapter(dateList, horizontalSelectedIndex, dayNumber -> onDateSelected(dayNumber));
        calendarAdapter.setDaysWithEvents(currentDaysWithEvents);
        rvHorizontalCalendar.setAdapter(calendarAdapter);
        if (horizontalSelectedIndex >= 0 && horizontalSelectedIndex < dateList.size()) {
            rvHorizontalCalendar.scrollToPosition(horizontalSelectedIndex);
        }
        if (horizontalSelectedIndex >= 0 && horizontalSelectedIndex < dateList.size()) {
            rvHorizontalCalendar.scrollToPosition(horizontalSelectedIndex);
        }
    }

    private void onDateSelected(int dayNumber) {
        int maxDays = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (dayNumber < 1 || dayNumber > maxDays) return;

        currentCalendar.set(Calendar.DAY_OF_MONTH, dayNumber);

        Locale localeVN = new Locale("vi", "VN");
        SimpleDateFormat agendaFormat = new SimpleDateFormat("'●' EEEE, dd 'Thg' MM, yyyy", localeVN);
        if (tvAgendaDate != null) {
            tvAgendaDate.setText(agendaFormat.format(currentCalendar.getTime()));
        }

        // Đồng bộ vị trí chọn giữa 2 lịch
        refreshAllCalendarViews();

        // Lọc danh sách công việc chỉ thuộc về ngày được chọn
        filterTasksForSelectedDate();
    }

    private void filterTasksForSelectedDate() {
        displayedTaskList.clear();

        int selectedDay = currentCalendar.get(Calendar.DAY_OF_MONTH);
        int selectedMonth = currentCalendar.get(Calendar.MONTH) + 1;
        int selectedYear = currentCalendar.get(Calendar.YEAR);

        Calendar todayCal = Calendar.getInstance();
        boolean isSelectedDayToday = (selectedDay == todayCal.get(Calendar.DAY_OF_MONTH) &&
                selectedMonth == (todayCal.get(Calendar.MONTH) + 1));

        for (Task t : allTaskList) {
            String timeStr = t.getTime();
            if (timeStr == null || timeStr.trim().isEmpty()) {
                if (isSelectedDayToday) {
                    displayedTaskList.add(t);
                }
                continue;
            }

            boolean isMatch = false;

            if (timeStr.contains("Hôm nay")) {
                if (isSelectedDayToday) {
                    isMatch = true;
                }
            }

            List<int[]> dates = parseDatesFromTimeStr(timeStr, todayCal.get(Calendar.YEAR));
            for (int[] d : dates) {
                int day = d[0];
                int month = d[1];
                int year = d[2];
                if (day == selectedDay && month == selectedMonth && (year == selectedYear || year == 0)) {
                    isMatch = true;
                    break;
                }
            }

            if (isMatch) {
                displayedTaskList.add(t);
            }
        }

        taskAdapter.notifyDataSetChanged();

        int total = displayedTaskList.size();
        if (tvTasksToday != null) {
            tvTasksToday.setText(total + (total <= 1 ? " Task Today" : " Tasks Today"));
        }
    }

    private void setupBottomSheet() {
        if (bottomSheet == null) return;
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        if (layoutFullCalendar != null) {
            layoutFullCalendar.setVisibility(View.VISIBLE);
            layoutFullCalendar.setAlpha(1f);
        }
        if (rvHorizontalCalendar != null) {
            rvHorizontalCalendar.setVisibility(View.INVISIBLE);
            rvHorizontalCalendar.setAlpha(0f);
        }

        if (tvMonthYear != null) {
            tvMonthYear.setVisibility(View.VISIBLE);
            tvMonthYear.setAlpha(1f);
        }

        View dragHandle = findViewById(R.id.dragHandle);
        View.OnClickListener toggleSheet = v -> {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        };

        if (dragHandle != null) dragHandle.setOnClickListener(toggleSheet);
        if (tvAgendaDate != null) tvAgendaDate.setOnClickListener(toggleSheet);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    if (layoutFullCalendar != null) layoutFullCalendar.setVisibility(View.INVISIBLE);
                    if (rvHorizontalCalendar != null) {
                        rvHorizontalCalendar.setVisibility(View.VISIBLE);
                        rvHorizontalCalendar.setAlpha(1f);
                    }
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    if (rvHorizontalCalendar != null) rvHorizontalCalendar.setVisibility(View.INVISIBLE);
                    if (layoutFullCalendar != null) {
                        layoutFullCalendar.setVisibility(View.VISIBLE);
                        layoutFullCalendar.setAlpha(1f);
                    }
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (slideOffset >= 0 && slideOffset <= 1) {
                    if (rvHorizontalCalendar != null && rvHorizontalCalendar.getVisibility() != View.VISIBLE) {
                        rvHorizontalCalendar.setVisibility(View.VISIBLE);
                    }
                    if (layoutFullCalendar != null && layoutFullCalendar.getVisibility() != View.VISIBLE) {
                        layoutFullCalendar.setVisibility(View.VISIBLE);
                    }

                    if (rvHorizontalCalendar != null) rvHorizontalCalendar.setAlpha(slideOffset);
                    if (layoutFullCalendar != null) layoutFullCalendar.setAlpha(1f - slideOffset);
                }
            }
        });
    }

    private void setupClickListeners() {
        ImageView btnScanSchedule = findViewById(R.id.btnScanSchedule);
        if (btnScanSchedule != null) {
            btnScanSchedule.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ScanScheduleActivity.class);
                startActivity(intent);
            });
        }

        if (imgAvatar != null) {
            imgAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ProfileSettingsActivity.class);
                startActivity(intent);
            });
        }

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
                startActivity(intent);
            });
        }

        LinearLayout navFocus = findViewById(R.id.navFocus);
        if (navFocus != null) {
            navFocus.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, PomodoroActivity.class);
                startActivity(intent);
            });
        }

        LinearLayout navProjects = findViewById(R.id.navProjects);
        if (navProjects != null) {
            navProjects.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ProjectsActivity.class);
                startActivity(intent);
            });
        }

        LinearLayout navTimeline = findViewById(R.id.navTimeline);
        if (navTimeline != null) {
            navTimeline.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AiAutoScheduleActivity.class);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasksFromServer();
    }

    private void loadTasksFromServer() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);

        if (currentUserId == -1) return;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        apiService.getTasksByUser(currentUserId).enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allTaskList.clear();
                    allTaskList.addAll(response.body());

                    // Trích xuất các ngày có lịch/task thuộc đúng tháng/năm đang xem
                    updateDaysWithEventsForCurrentCalendar();

                    if (fullCalendarAdapter != null) {
                        fullCalendarAdapter.setDaysWithEvents(currentDaysWithEvents);
                    }
                    if (calendarAdapter != null) {
                        calendarAdapter.setDaysWithEvents(currentDaysWithEvents);
                    }

                    // Lọc hiển thị công việc đúng theo ngày đang được chọn
                    filterTasksForSelectedDate();
                }
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lỗi tải danh sách công việc!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
