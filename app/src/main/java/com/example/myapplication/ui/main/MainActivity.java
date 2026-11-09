package com.example.myapplication.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import androidx.appcompat.app.AppCompatDelegate;
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
import com.example.myapplication.network.RetrofitClient;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final Pattern NON_DATE_CHARS = Pattern.compile("[^0-9/-]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Locale LOCALE_VN = new Locale("vi", "VN");

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

    private final SimpleDateFormat monthFormat = new SimpleDateFormat("'Tháng' MM", LOCALE_VN);
    private final SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", LOCALE_VN);
    private final SimpleDateFormat agendaFormat = new SimpleDateFormat("EEEE, dd 'Thg' MM, yyyy", LOCALE_VN);
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", LOCALE_VN);
    private final SimpleDateFormat numberFormat = new SimpleDateFormat("dd", LOCALE_VN);

    private TextView tvTimeZoneValue;
    private TextView tvAgendaDate;
    private TextView tvTasksToday;
    private TextView tvMonthYear;
    private TextView tvYearSubtitle;

    private ImageButton btnPrevMonth, btnNextMonth;
    private ImageView imgAvatar;
    private String lastLoadedAvatarUri = "";

    private RecyclerView rvHorizontalCalendar;
    private View layoutFullCalendar;
    private RecyclerView rvFullCalendarGrid;

    private CalendarAdapter calendarAdapter;
    private FullCalendarAdapter fullCalendarAdapter;
    private final List<String> daysInMonth = new ArrayList<>(42);
    private final List<CalendarDate> dateList = new ArrayList<>(31);
    private final Set<Integer> currentDaysWithEvents = new HashSet<>();
    private final Map<Integer, List<Integer>> currentDayTaskColors = new HashMap<>();
    private final Map<String, List<int[]>> parsedDatesCache = new HashMap<>();
    private int currentLeadingEmpty = 0;

    private RecyclerView rvTasks;
    private TaskAdapter taskAdapter;
    private final List<Task> allTaskList = new ArrayList<>();
    private final List<Task> displayedTaskList = new ArrayList<>();

    private View bottomSheet;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    private final Calendar currentCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences appPref = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isDarkMode = appPref.getBoolean("DARK_MODE_ENABLED", false);
        int targetNightMode = isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        if (AppCompatDelegate.getDefaultNightMode() != targetNightMode) {
            AppCompatDelegate.setDefaultNightMode(targetNightMode);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1. ÁNH XẠ VIEW
        tvTimeZoneValue = findViewById(R.id.tvTimeZoneValue);
        tvAgendaDate = findViewById(R.id.tvAgendaDate);
        tvTasksToday = findViewById(R.id.tvTasksToday);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvYearSubtitle = findViewById(R.id.tvYearSubtitle);

        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        imgAvatar = findViewById(R.id.imgAvatar);

        rvHorizontalCalendar = findViewById(R.id.rvHorizontalCalendar);
        layoutFullCalendar = findViewById(R.id.layoutFullCalendar);
        rvFullCalendarGrid = findViewById(R.id.rvFullCalendarGrid);

        bottomSheet = findViewById(R.id.layoutAgendaBottomSheet);
        rvTasks = findViewById(R.id.rvTasks);

        // 2. KHỞI TẠO DANH SÁCH TASK (Tối ưu RecyclerView cache)
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setItemViewCacheSize(10);
        taskAdapter = new TaskAdapter(displayedTaskList);
        rvTasks.setAdapter(taskAdapter);

        // 3. KHỞI TẠO LỊCH & THÔNG TIN HEADER
        setupHeaderAndCalendar();

        // 4. THIẾT LẬP BOTTOM SHEET (KÉO LÊN KÉO XUỐNG)
        setupBottomSheet();

        // 5. THIẾT LẬP BOTTOM NAV & CLICKS
        setupClickListeners();
    }

    private void setupHeaderAndCalendar() {
        rvFullCalendarGrid.setLayoutManager(new GridLayoutManager(this, 7));
        rvFullCalendarGrid.setHasFixedSize(true);
        rvFullCalendarGrid.setItemAnimator(null);
        rvFullCalendarGrid.setItemViewCacheSize(42);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvHorizontalCalendar.setLayoutManager(layoutManager);
        rvHorizontalCalendar.setHasFixedSize(true);
        rvHorizontalCalendar.setItemAnimator(null);
        rvHorizontalCalendar.setItemViewCacheSize(14);

        fullCalendarAdapter = new FullCalendarAdapter(daysInMonth, 0, this::onDateSelected);
        rvFullCalendarGrid.setAdapter(fullCalendarAdapter);

        calendarAdapter = new CalendarAdapter(dateList, 0, this::onDateSelected);
        rvHorizontalCalendar.setAdapter(calendarAdapter);

        setupTimeZoneHeader();

        if (btnPrevMonth != null) {
            btnPrevMonth.setOnClickListener(v -> {
                int day = currentCalendar.get(Calendar.DAY_OF_MONTH);
                currentCalendar.add(Calendar.MONTH, -1);
                int maxDays = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                currentCalendar.set(Calendar.DAY_OF_MONTH, Math.min(day, maxDays));
                refreshAllCalendarViews();
                filterTasksForSelectedDate();
            });
        }

        if (btnNextMonth != null) {
            btnNextMonth.setOnClickListener(v -> {
                int day = currentCalendar.get(Calendar.DAY_OF_MONTH);
                currentCalendar.add(Calendar.MONTH, 1);
                int maxDays = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                currentCalendar.set(Calendar.DAY_OF_MONTH, Math.min(day, maxDays));
                refreshAllCalendarViews();
                filterTasksForSelectedDate();
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
        if (timeStr == null || timeStr.isEmpty()) return Collections.emptyList();

        List<int[]> cached = parsedDatesCache.get(timeStr);
        if (cached != null) {
            return cached;
        }

        List<int[]> dates = new ArrayList<>(2);
        try {
            String cleanTime = NON_DATE_CHARS.matcher(timeStr).replaceAll(" ").trim();
            if (!cleanTime.isEmpty()) {
                String[] parts = WHITESPACE.split(cleanTime);
                for (String part : parts) {
                    int slashIdx = part.indexOf('/');
                    int dashIdx = part.indexOf('-');
                    if (slashIdx > 0 || dashIdx > 0) {
                        String sep = slashIdx > 0 ? "/" : "-";
                        String[] dateParts = part.split(sep);
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
            }
        } catch (Exception ignored) {}

        parsedDatesCache.put(timeStr, dates);
        return dates;
    }

    private void addTaskColorForDay(int day, Task task) {
        currentDaysWithEvents.add(day);
        List<Integer> colors = currentDayTaskColors.get(day);
        if (colors == null) {
            colors = new ArrayList<>(4);
            currentDayTaskColors.put(day, colors);
        }
        colors.add(TaskAdapter.getColorForPriority(task, colors.size()));
    }

    private void updateDaysWithEventsForCurrentCalendar() {
        currentDaysWithEvents.clear();
        currentDayTaskColors.clear();

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
                    addTaskColorForDay(realTodayDay, t);
                }
                continue;
            }

            List<int[]> dates = parseDatesFromTimeStr(timeStr, realTodayYear);
            if (!dates.isEmpty()) {
                for (int[] d : dates) {
                    int day = d[0];
                    int month = d[1];
                    int year = d[2];

                    if (month == viewedMonth && (year == viewedYear || year == 0)) {
                        addTaskColorForDay(day, t);
                    }
                }
            } else if (timeStr.contains("Hôm nay")) {
                if (viewedMonth == realTodayMonth && viewedYear == realTodayYear) {
                    addTaskColorForDay(realTodayDay, t);
                }
            }
        }
    }

    private void refreshAllCalendarViews() {
        updateDaysWithEventsForCurrentCalendar();

        // 1. Cập nhật Tiêu đề Tháng/Năm
        if (tvMonthYear != null) {
            tvMonthYear.setText(monthFormat.format(currentCalendar.getTime()));
        }
        if (tvYearSubtitle != null) {
            tvYearSubtitle.setText(yearFormat.format(currentCalendar.getTime()));
        }

        // 2. Cập nhật Tiêu đề Ngày ở Bottom Sheet
        if (tvAgendaDate != null) {
            tvAgendaDate.setText(agendaFormat.format(currentCalendar.getTime()));
        }

        // 3. Cập nhật Lịch Lưới (Full Grid Calendar) — Tái sử dụng Adapter không tạo mới
        Calendar cal = (Calendar) currentCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // Sunday=1, Monday=2...
        currentLeadingEmpty = (firstDayOfWeek + 5) % 7; // Căn chỉnh cột T2=0, T3=1... CN=6
        int daysInMonthCount = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        daysInMonth.clear();
        for (int i = 0; i < currentLeadingEmpty; i++) {
            daysInMonth.add("");
        }
        for (int i = 1; i <= daysInMonthCount; i++) {
            daysInMonth.add(String.valueOf(i));
        }

        int totalCells = daysInMonth.size();
        int targetSize = totalCells <= 35 ? 35 : ((totalCells + 6) / 7) * 7;
        while (daysInMonth.size() < targetSize) {
            daysInMonth.add("");
        }

        int currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH);
        int gridSelectedIndex = currentLeadingEmpty + currentDay - 1;

        if (fullCalendarAdapter != null) {
            fullCalendarAdapter.updateCalendarData(daysInMonth, gridSelectedIndex, currentDaysWithEvents, currentDayTaskColors);
        }

        // 4. Cập nhật Lịch Ngang (Horizontal Calendar) — Tái sử dụng Adapter không tạo mới
        dateList.clear();
        Calendar horizontalCal = (Calendar) currentCalendar.clone();
        horizontalCal.set(Calendar.DAY_OF_MONTH, 1);

        int horizontalSelectedIndex = currentDay - 1;

        for (int i = 1; i <= daysInMonthCount; i++) {
            horizontalCal.set(Calendar.DAY_OF_MONTH, i);
            String dayName = dayFormat.format(horizontalCal.getTime());
            String dayNum = numberFormat.format(horizontalCal.getTime());
            dateList.add(new CalendarDate(dayName, dayNum, i == currentDay));
        }

        if (calendarAdapter != null) {
            calendarAdapter.updateCalendarData(dateList, horizontalSelectedIndex, currentDaysWithEvents, currentDayTaskColors);
        }
        if (horizontalSelectedIndex >= 0 && horizontalSelectedIndex < dateList.size()) {
            rvHorizontalCalendar.scrollToPosition(horizontalSelectedIndex);
        }
    }

    private void onDateSelected(int dayNumber) {
        int maxDays = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (dayNumber < 1 || dayNumber > maxDays) return;

        currentCalendar.set(Calendar.DAY_OF_MONTH, dayNumber);

        if (tvAgendaDate != null) {
            tvAgendaDate.setText(agendaFormat.format(currentCalendar.getTime()));
        }

        // Chỉ cập nhật ô được chọn trên 2 lịch (2 ViewHolder) thay vì tạo mới toàn bộ Adapter
        int gridSelectedIndex = currentLeadingEmpty + dayNumber - 1;
        if (fullCalendarAdapter != null) {
            fullCalendarAdapter.setSelectedPosition(gridSelectedIndex);
        }
        int horizontalSelectedIndex = dayNumber - 1;
        if (calendarAdapter != null) {
            calendarAdapter.setSelectedPosition(horizontalSelectedIndex);
        }

        filterTasksForSelectedDate();
    }

    private void filterTasksForSelectedDate() {
        displayedTaskList.clear();

        int selectedDay = currentCalendar.get(Calendar.DAY_OF_MONTH);
        int selectedMonth = currentCalendar.get(Calendar.MONTH) + 1;
        int selectedYear = currentCalendar.get(Calendar.YEAR);

        Calendar todayCal = Calendar.getInstance();
        int realTodayYear = todayCal.get(Calendar.YEAR);
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

            List<int[]> dates = parseDatesFromTimeStr(timeStr, realTodayYear);
            if (!dates.isEmpty()) {
                for (int[] d : dates) {
                    int day = d[0];
                    int month = d[1];
                    int year = d[2];
                    if (day == selectedDay && month == selectedMonth && (year == selectedYear || year == 0)) {
                        isMatch = true;
                        break;
                    }
                }
            } else if (timeStr.contains("Hôm nay") && isSelectedDayToday) {
                isMatch = true;
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
        bottomSheetBehavior.setFitToContents(false);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        int pxPeek = (int) (110 * getResources().getDisplayMetrics().density);
        bottomSheetBehavior.setPeekHeight(pxPeek);
        int pxOffset = (int) (225 * getResources().getDisplayMetrics().density);
        bottomSheetBehavior.setExpandedOffset(pxOffset);

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
                    if (layoutFullCalendar != null) layoutFullCalendar.setVisibility(View.GONE);
                    if (rvHorizontalCalendar != null) {
                        rvHorizontalCalendar.setVisibility(View.VISIBLE);
                        rvHorizontalCalendar.setAlpha(1f);
                    }
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    if (rvHorizontalCalendar != null) rvHorizontalCalendar.setVisibility(View.GONE);
                    if (layoutFullCalendar != null) {
                        layoutFullCalendar.setVisibility(View.VISIBLE);
                        layoutFullCalendar.setAlpha(1f);
                    }
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (slideOffset > 0.2f) {
                    if (rvHorizontalCalendar != null) {
                        rvHorizontalCalendar.setVisibility(View.VISIBLE);
                        rvHorizontalCalendar.setAlpha(Math.min(1f, (slideOffset - 0.2f) * 1.25f));
                    }
                    if (layoutFullCalendar != null) {
                        layoutFullCalendar.setVisibility(View.GONE);
                    }
                } else {
                    if (layoutFullCalendar != null) {
                        layoutFullCalendar.setVisibility(View.VISIBLE);
                        layoutFullCalendar.setAlpha(Math.max(0f, 1f - slideOffset * 3f));
                    }
                    if (rvHorizontalCalendar != null) {
                        rvHorizontalCalendar.setVisibility(View.GONE);
                    }
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
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            });
        }

        LinearLayout navProjects = findViewById(R.id.navProjects);
        if (navProjects != null) {
            navProjects.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ProjectsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            });
        }

        LinearLayout navTimeline = findViewById(R.id.navTimeline);
        if (navTimeline != null) {
            navTimeline.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AiAutoScheduleActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHeaderAvatar();
        loadTasksFromServer();
    }

    private void updateHeaderAvatar() {
        if (imgAvatar == null) return;
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String savedAvatarUriStr = sharedPref.getString("USER_AVATAR_URI", "");
        if (!savedAvatarUriStr.isEmpty() && !savedAvatarUriStr.equals(lastLoadedAvatarUri)) {
            try {
                imgAvatar.setImageTintList(null);
                imgAvatar.setImageURI(Uri.parse(savedAvatarUriStr));
                lastLoadedAvatarUri = savedAvatarUriStr;
            } catch (Exception ignored) {}
        }
    }

    private void applyTasksToViews(List<Task> tasks) {
        allTaskList.clear();
        allTaskList.addAll(tasks);
        parsedDatesCache.clear();

        updateDaysWithEventsForCurrentCalendar();

        int currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH);
        int gridSelectedIndex = currentLeadingEmpty + currentDay - 1;
        if (fullCalendarAdapter != null) {
            fullCalendarAdapter.updateCalendarData(daysInMonth, gridSelectedIndex, currentDaysWithEvents, currentDayTaskColors);
        }
        if (calendarAdapter != null) {
            calendarAdapter.updateCalendarData(dateList, currentDay - 1, currentDaysWithEvents, currentDayTaskColors);
        }

        filterTasksForSelectedDate();
    }

    private void loadTasksFromServer() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);

        if (currentUserId == -1) return;

        // Hiển thị tức thì từ bộ nhớ đệm RAM (0ms) nếu đã có dữ liệu
        List<Task> cached = RetrofitClient.getCachedTasks();
        if (!cached.isEmpty() && allTaskList.isEmpty()) {
            applyTasksToViews(cached);
        }

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getTasksByUser(currentUserId).enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Task> tasks = response.body();
                    RetrofitClient.setCachedTasks(tasks);
                    applyTasksToViews(tasks);
                }
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                if (allTaskList.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Lỗi tải danh sách công việc!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
