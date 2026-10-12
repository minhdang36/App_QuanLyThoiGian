package com.example.myapplication; // Lưu ý: Giữ nguyên tên package của bạn nếu nó khác với dòng này

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// Import các class Adapter và Model
import com.example.myapplication.adapter.CalendarAdapter;
import com.example.myapplication.adapter.FullCalendarAdapter;
import com.example.myapplication.adapter.TaskAdapter;
import com.example.myapplication.model.CalendarDate;
import com.example.myapplication.model.Task;

// Import thư viện Retrofit
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    // 1. BIẾN CHO TASK LIST VÀ CÁC THÔNG TIN TEXTVIEW
    private TextView tvTimeZoneValue;
    private TextView tvAgendaDate;
    private TextView tvTasksToday; // BIẾN MỚI: Dành cho đếm số Task

    private RecyclerView rvTasks;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;

    // Biến cho Lịch ngang và Header
    private RecyclerView rvHorizontalCalendar;
    private View layoutFullCalendar;
    private CalendarAdapter calendarAdapter;
    private List<CalendarDate> dateList;
    private TextView tvMonthYear;

    // Biến cho Lịch lưới 35 ngày
    private RecyclerView rvFullCalendarGrid;
    private FullCalendarAdapter fullCalendarAdapter;

    // Biến cho Bottom Sheet
    private View bottomSheet;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // ==========================================
        // 2. ÁNH XẠ CÁC VIEW TỪ XML
        // ==========================================
        tvTimeZoneValue = findViewById(R.id.tvTimeZoneValue);
        tvAgendaDate = findViewById(R.id.tvAgendaDate);
        rvHorizontalCalendar = findViewById(R.id.rvHorizontalCalendar);
        layoutFullCalendar = findViewById(R.id.layoutFullCalendar);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        bottomSheet = findViewById(R.id.layoutAgendaBottomSheet);
        tvTasksToday = findViewById(R.id.tvTasksToday); // Ánh xạ TextView đếm task

        // ==========================================
        // 3. KHỞI TẠO DỮ LIỆU LỊCH NGANG
        // ==========================================
        Calendar calendar = Calendar.getInstance();
        Locale localeVN = new Locale("vi", "VN");

        // 3.1 Cập nhật Tháng/Năm trên Header
        SimpleDateFormat monthFormat = new SimpleDateFormat("'Tháng' MM 'năm' yyyy", localeVN);
        tvMonthYear.setText(monthFormat.format(calendar.getTime()));

        // 3.2 Cập nhật Múi giờ thực tế của điện thoại
        java.util.TimeZone tz = calendar.getTimeZone();
        int offsetHours = tz.getRawOffset() / 3600000;
        String offsetSign = offsetHours >= 0 ? "+" : "";
        tvTimeZoneValue.setText(tz.getID() + " (UTC" + offsetSign + offsetHours + ")");

        // 3.3 Cập nhật ngày ở Bảng xám Bottom Sheet
        SimpleDateFormat agendaFormat = new SimpleDateFormat("'●' EEEE, dd 'Thg' MM, yyyy", localeVN);
        tvAgendaDate.setText(agendaFormat.format(calendar.getTime()));

        dateList = new ArrayList<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", localeVN);
        SimpleDateFormat numberFormat = new SimpleDateFormat("dd", localeVN);

        for (int i = 0; i < 14; i++) {
            String day = dayFormat.format(calendar.getTime());
            String number = numberFormat.format(calendar.getTime());
            dateList.add(new CalendarDate(day, number, i == 0));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvHorizontalCalendar.setLayoutManager(layoutManager);
        calendarAdapter = new CalendarAdapter(dateList);
        rvHorizontalCalendar.setAdapter(calendarAdapter);

        // ==========================================
        // 4. THIẾT LẬP DANH SÁCH CÔNG VIỆC (TASK LIST)
        // ==========================================
        rvTasks = findViewById(R.id.rvTasks);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));

        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList);
        rvTasks.setAdapter(taskAdapter);

        // GỌI API ĐỂ LẤY DỮ LIỆU TỪ MYSQL
        loadTasksFromServer();

        // ==========================================
        // 5. KHỞI TẠO LỊCH LƯỚI ĐẦY ĐỦ (35 NGÀY)
        // ==========================================
        rvFullCalendarGrid = findViewById(R.id.rvFullCalendarGrid);
        rvFullCalendarGrid.setLayoutManager(new GridLayoutManager(this, 7));

        List<String> daysInMonth = new ArrayList<>();
        daysInMonth.add(""); // T2
        daysInMonth.add(""); // T3
        for (int i = 1; i <= 31; i++) {
            daysInMonth.add(String.valueOf(i));
        }
        while (daysInMonth.size() < 35) {
            daysInMonth.add("");
        }

        fullCalendarAdapter = new FullCalendarAdapter(daysInMonth);
        rvFullCalendarGrid.setAdapter(fullCalendarAdapter);

        // ==========================================
        // 6. SỰ KIỆN CLICK CHUYỂN TRANG
        // ==========================================
        // 6.1 Sự kiện cho nút Thêm (+) ở giữa
        com.google.android.material.floatingactionbutton.FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            startActivity(intent);
        });

        // 6.2 Sự kiện cho thẻ Projects
        LinearLayout navProjects = findViewById(R.id.navProjects);
        navProjects.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProjectsActivity.class);
            startActivity(intent);
        });

        // ==========================================
        // 7. THIẾT LẬP HIỆU ỨNG KÉO THẢ (BOTTOM SHEET)
        // ==========================================
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        layoutFullCalendar.setVisibility(View.VISIBLE);
        layoutFullCalendar.setAlpha(1f);

        rvHorizontalCalendar.setVisibility(View.INVISIBLE);
        rvHorizontalCalendar.setAlpha(0f);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    layoutFullCalendar.setVisibility(View.INVISIBLE);
                    rvHorizontalCalendar.setVisibility(View.VISIBLE);
                    rvHorizontalCalendar.setAlpha(1f);
                    tvMonthYear.setVisibility(View.VISIBLE);
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    rvHorizontalCalendar.setVisibility(View.INVISIBLE);
                    layoutFullCalendar.setVisibility(View.VISIBLE);
                    layoutFullCalendar.setAlpha(1f);
                    tvMonthYear.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (slideOffset >= 0 && slideOffset <= 1) {
                    if (rvHorizontalCalendar.getVisibility() != View.VISIBLE) {
                        rvHorizontalCalendar.setVisibility(View.VISIBLE);
                    }
                    if (layoutFullCalendar.getVisibility() != View.VISIBLE) {
                        layoutFullCalendar.setVisibility(View.VISIBLE);
                    }

                    rvHorizontalCalendar.setAlpha(slideOffset);

                    // Đã sửa lỗi cú pháp ở đây
                    layoutFullCalendar.setAlpha(1f - slideOffset);

                    tvMonthYear.setAlpha(slideOffset);
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tự động tải lại dữ liệu mỗi khi quay lại trang chủ
        loadTasksFromServer();
    }

    // ==========================================
    // HÀM GỌI API LẤY DANH SÁCH TASK TỪ SPRING BOOT (ĐÃ CẬP NHẬT)
    // ==========================================
    private void loadTasksFromServer() {
        // 1. Lấy ID người dùng
        android.content.SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);

        if (currentUserId == -1) return; // Nếu chưa đăng nhập thì dừng lại

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/") // Đảm bảo URL này khớp với backend
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        apiService.getTasksByUser(currentUserId).enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Task> serverTasks = response.body();

                    if (taskList != null) {
                        // 1. Làm mới dữ liệu
                        taskList.clear();
                        taskList.addAll(serverTasks);
                        taskAdapter.notifyDataSetChanged();

                        // 2. CẬP NHẬT SỐ LƯỢNG TASK LÊN GIAO DIỆN CHÍNH (DYNAMIC UPDATE)
                        int totalTasks = taskList.size();
                        if (tvTasksToday != null) { // Đảm bảo View đã được ánh xạ (chống lỗi null)
                            if (totalTasks <= 1) {
                                tvTasksToday.setText(totalTasks + " Task Today");
                            } else {
                                tvTasksToday.setText(totalTasks + " Tasks Today");
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lỗi tải danh sách công việc!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}