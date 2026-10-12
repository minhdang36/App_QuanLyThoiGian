package com.example.myapplication;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.model.Project;
import com.example.myapplication.model.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AddTaskActivity extends AppCompatActivity {

    private EditText edtTaskTitle, edtLocation, edtDescription;
    private TextView tvSelectDate, tvSelectTime, tvCancel;
    private Button btnSave;
    private Spinner spinnerCategory;
    private List<Project> currentProjects;

    // Biến lưu trữ ID công việc. -1 nghĩa là thêm mới, khác -1 là đang sửa.
    private int taskId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        // 1. Ánh xạ các View từ file XML
        spinnerCategory = findViewById(R.id.spinnerCategory);
        edtTaskTitle = findViewById(R.id.edtTaskTitle);
        edtLocation = findViewById(R.id.edtLocation);
        edtDescription = findViewById(R.id.edtDescription);
        tvSelectDate = findViewById(R.id.tvSelectDate);
        tvSelectTime = findViewById(R.id.tvSelectTime);
        btnSave = findViewById(R.id.btnSave);
        tvCancel = findViewById(R.id.tvCancel);

        // 2. Thiết lập ngày giờ mặc định ban đầu
        Calendar today = Calendar.getInstance();
        int currentDay = today.get(Calendar.DAY_OF_MONTH);
        int currentMonth = today.get(Calendar.MONTH) + 1;
        int currentYear = today.get(Calendar.YEAR);
        String currentDateStr = "📅 " + currentDay + "/" + currentMonth + "/" + currentYear;
        tvSelectDate.setText(currentDateStr);

        int currentHour = today.get(Calendar.HOUR_OF_DAY);
        int currentMinute = today.get(Calendar.MINUTE);
        String currentTimeStr = String.format(Locale.getDefault(), "🕒 %02d:%02d", currentHour, currentMinute);
        tvSelectTime.setText(currentTimeStr);

        // ==========================================
        // 3. KIỂM TRA CHẾ ĐỘ SỬA CÔNG VIỆC
        // ==========================================
        taskId = getIntent().getIntExtra("TASK_ID", -1);
        if (taskId != -1) {
            // Đổ dữ liệu cũ lên giao diện
            edtTaskTitle.setText(getIntent().getStringExtra("TASK_TITLE"));
            edtDescription.setText(getIntent().getStringExtra("TASK_DESC"));
            edtLocation.setText(getIntent().getStringExtra("TASK_LOC"));

            // Xử lý tách ngày và giờ từ chuỗi "Ngày | Giờ"
            String taskTime = getIntent().getStringExtra("TASK_TIME");
            if (taskTime != null && taskTime.contains("|")) {
                String[] timeParts = taskTime.split("\\|");
                if (timeParts.length >= 2) {
                    tvSelectDate.setText(timeParts[0].trim());
                    tvSelectTime.setText(timeParts[1].trim());
                }
            } else if (taskTime != null && !taskTime.isEmpty()) {
                tvSelectDate.setText(taskTime); // Nếu format cũ không có dấu |
            }

            btnSave.setText("CẬP NHẬT CÔNG VIỆC");
        }

        // Tải danh mục dự án CHỈ CỦA NGƯỜI DÙNG ĐANG ĐĂNG NHẬP
        loadCategoriesForSpinner();

        tvCancel.setOnClickListener(v -> finish());

        // --- SỰ KIỆN CHỌN NGÀY ---
        tvSelectDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(AddTaskActivity.this,
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        String selectedDate = "📅 " + dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                        tvSelectDate.setText(selectedDate);
                    }, year, month, day);
            datePickerDialog.show();
        });

        // --- SỰ KIỆN CHỌN GIỜ ---
        tvSelectTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int pickerHour = calendar.get(Calendar.HOUR_OF_DAY);
            int pickerMinute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(AddTaskActivity.this,
                    (view, hourOfDay, minute1) -> {
                        String formattedTime = String.format(Locale.getDefault(), "🕒 %02d:%02d", hourOfDay, minute1);
                        tvSelectTime.setText(formattedTime);
                    }, pickerHour, pickerMinute, true);
            timePickerDialog.show();
        });

        // --- SỰ KIỆN: BẤM LƯU / CẬP NHẬT ---
        btnSave.setOnClickListener(v -> {
            String title = edtTaskTitle.getText().toString().trim();
            String location = edtLocation.getText().toString().trim();
            String description = edtDescription.getText().toString().trim();

            String date = tvSelectDate.getText().toString().trim();
            String timeStr = tvSelectTime.getText().toString().trim();
            String fullDateTime = date + " | " + timeStr;

            if (title.isEmpty()) {
                Toast.makeText(AddTaskActivity.this, "Vui lòng nhập tiêu đề!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentProjects == null || currentProjects.isEmpty()) {
                Toast.makeText(this, "Đang tải danh mục dự án...", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. "MÓC TÚI" LẤY THẺ ĐỊNH DANH (USER_ID)
            SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
            int currentUserId = sharedPref.getInt("USER_ID", -1);

            if (currentUserId == -1) {
                Toast.makeText(this, "Lỗi: Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedPosition = spinnerCategory.getSelectedItemPosition();
            int selectedProjectId = currentProjects.get(selectedPosition).getId();

            // 2. TẠO CÔNG VIỆC VÀ "DÁN TEM"
            Task taskData = new Task();
            taskData.setTitle(title);
            taskData.setLocation(location);
            taskData.setDescription(description);
            taskData.setTime(fullDateTime);
            taskData.setProjectId(selectedProjectId);
            taskData.setStatus("PENDING");

            // DÒNG QUAN TRỌNG NHẤT: Đóng dấu chủ sở hữu cho Task này
            taskData.setUserId(currentUserId);

            // 3. Gọi hàm xử lý lưu/sửa
            saveOrUpdateTaskInMySQL(taskData);
        });
    }

    private void loadCategoriesForSpinner() {
        // Lấy ID người dùng đang đăng nhập
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);

        if (currentUserId == -1) return;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);

        // GỌI getProjectsByUser(currentUserId) ĐỂ CHỈ LẤY DỰ ÁN CỦA ĐÚNG NGƯỜI NÀY
        apiService.getProjectsByUser(currentUserId).enqueue(new Callback<List<Project>>() {
            @Override
            public void onResponse(Call<List<Project>> call, Response<List<Project>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentProjects = response.body();
                    List<String> projectNames = new ArrayList<>();

                    for (Project p : currentProjects) {
                        projectNames.add(p.getName());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            AddTaskActivity.this,
                            android.R.layout.simple_spinner_dropdown_item,
                            projectNames
                    );
                    spinnerCategory.setAdapter(adapter);
                }
            }
            @Override
            public void onFailure(Call<List<Project>> call, Throwable t) {
                Toast.makeText(AddTaskActivity.this, "Lỗi tải danh mục!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==========================================
    // 4. HÀM GỌI API XỬ LÝ (THÊM HOẶC SỬA)
    // ==========================================
    private void saveOrUpdateTaskInMySQL(Task task) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<Task> call;

        // Phân nhánh logic: Nếu có taskId thì gọi PUT (Sửa), ngược lại gọi POST (Thêm)
        if (taskId != -1) {
            call = apiService.updateTask(taskId, task);
            btnSave.setText("Đang cập nhật...");
        } else {
            call = apiService.addTask(task);
            btnSave.setText("Đang lưu...");
        }

        btnSave.setEnabled(false);

        call.enqueue(new Callback<Task>() {
            @Override
            public void onResponse(Call<Task> call, Response<Task> response) {
                if (response.isSuccessful()) {
                    String message = (taskId != -1) ? "Đã cập nhật công việc!" : "Đã thêm công việc thành công!";
                    Toast.makeText(AddTaskActivity.this, message, Toast.LENGTH_SHORT).show();

                    // Để dữ liệu được làm mới hoàn toàn, dọn dẹp các Activity cũ và quay về MainActivity
                    Intent intent = new Intent(AddTaskActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    btnSave.setEnabled(true);
                    btnSave.setText(taskId != -1 ? "CẬP NHẬT CÔNG VIỆC" : "✔ Lưu Công Việc");
                    Toast.makeText(AddTaskActivity.this, "Thất bại. Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Task> call, Throwable t) {
                btnSave.setEnabled(true);
                btnSave.setText(taskId != -1 ? "CẬP NHẬT CÔNG VIỆC" : "✔ Lưu Công Việc");
                Toast.makeText(AddTaskActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_LONG).show();
            }
        });
    }
}