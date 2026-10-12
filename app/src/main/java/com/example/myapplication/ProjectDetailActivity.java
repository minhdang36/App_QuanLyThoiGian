package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.model.Task;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProjectDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        // =========================================
        // 1. ÁNH XẠ CÁC VIEW TỪ XML
        // =========================================
        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvTitle = findViewById(R.id.tvTaskDetailTitle);
        TextView tvDesc = findViewById(R.id.tvTaskDetailDesc);
        TextView tvTime = findViewById(R.id.tvTaskDetailTime);
        TextView tvLocation = findViewById(R.id.tvTaskDetailLocation);
        TextView tvStatus = findViewById(R.id.tvTaskDetailStatus);

        // Ánh xạ các nút chức năng mới
        TextView btnDeleteTask = findViewById(R.id.btnDeleteTask);
        TextView btnEditTask = findViewById(R.id.btnEditTask);
        TextView btnCompleteTask = findViewById(R.id.btnCompleteTask);
        TextView btnAddSubtask = findViewById(R.id.btnAddSubtask);

        // =========================================
        // 2. NHẬN DỮ LIỆU TỪ TRANG CHỦ & HIỂN THỊ
        // =========================================
        Intent intent = getIntent();
        int taskId = -1;

        if (intent != null) {
            taskId = intent.getIntExtra("TASK_ID", -1);
            String title = intent.getStringExtra("TASK_TITLE");
            String desc = intent.getStringExtra("TASK_DESC");
            String time = intent.getStringExtra("TASK_TIME");
            String location = intent.getStringExtra("TASK_LOC");
            String status = intent.getStringExtra("TASK_STATUS");

            Log.d("DEBUG_INTENT", "ID nhận được: " + taskId);

            // Đổ dữ liệu Tiêu đề & Mô tả
            if (title != null && !title.trim().isEmpty()) tvTitle.setText(title);
            else tvTitle.setText("Chưa có tiêu đề");

            if (desc != null && !desc.trim().isEmpty()) tvDesc.setText(desc);
            else tvDesc.setText("Không có mô tả chi tiết.");

            // Đổ dữ liệu Thời gian & Địa điểm
            if (time != null && !time.trim().isEmpty()) tvTime.setText(time);
            else tvTime.setText("Chưa hẹn giờ");

            if (location != null && !location.trim().isEmpty()) tvLocation.setText("📍 " + location);
            else tvLocation.setText("📍 Không có địa điểm / Link họp");

            // Việt hóa trạng thái
            if ("COMPLETED".equals(status) || "HOÀN THÀNH".equalsIgnoreCase(status)) {
                tvStatus.setText("● ĐÃ HOÀN THÀNH");
                tvStatus.setTextColor(Color.parseColor("#2E7D32")); // Màu xanh lá
            } else {
                tvStatus.setText("● ĐANG XỬ LÝ");
                tvStatus.setTextColor(Color.parseColor("#E65100")); // Màu cam
            }
        }

        final int finalTaskId = taskId; // Lưu lại ID để dùng trong các sự kiện Click

        // =========================================
        // 3. XỬ LÝ SỰ KIỆN CÁC NÚT BẤM
        // =========================================

        // Nút Back
        btnBack.setOnClickListener(v -> finish());

        // =========================================
        // NÚT XÓA CÔNG VIỆC (Thêm hộp thoại xác nhận)
        // =========================================
        btnDeleteTask.setOnClickListener(v -> {
            if (finalTaskId != -1) {
                new android.app.AlertDialog.Builder(ProjectDetailActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc muốn xóa công việc này không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            ApiService apiService = getApiService();
                            apiService.deleteTask(finalTaskId).enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    if (response.isSuccessful()) {
                                        Toast.makeText(ProjectDetailActivity.this, "Đã xóa công việc", Toast.LENGTH_SHORT).show();
                                        finish(); // Đóng trang chi tiết, quay về danh sách
                                    }
                                }
                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    Toast.makeText(ProjectDetailActivity.this, "Lỗi mạng khi xóa!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });

        // =========================================
        // NÚT SỬA CÔNG VIỆC (Gửi dữ liệu sang AddTaskActivity)
        // =========================================
        btnEditTask.setOnClickListener(v -> {
            Intent editIntent = new Intent(ProjectDetailActivity.this, AddTaskActivity.class);
            // Gửi dữ liệu hiện tại sang màn hình Sửa
            editIntent.putExtra("TASK_ID", finalTaskId);

            // Lấy dữ liệu từ giao diện đang hiển thị để truyền đi
            editIntent.putExtra("TASK_TITLE", tvTitle.getText().toString());

            String desc = tvDesc.getText().toString();
            if (desc.equals("Không có mô tả chi tiết.")) desc = "";
            editIntent.putExtra("TASK_DESC", desc);

            String time = tvTime.getText().toString();
            if (time.equals("Chưa hẹn giờ")) time = "";
            editIntent.putExtra("TASK_TIME", time);

            String loc = tvLocation.getText().toString().replace("📍 ", "");
            if (loc.equals("Không có địa điểm / Link họp")) loc = "";
            editIntent.putExtra("TASK_LOC", loc);

            startActivity(editIntent);
        });

        // NÚT HOÀN THÀNH
        btnCompleteTask.setOnClickListener(v -> {
            if (finalTaskId != -1) {
                // Đổi giao diện lập tức cho mượt
                tvStatus.setText("● ĐÃ HOÀN THÀNH");
                tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                Toast.makeText(this, "Đánh dấu hoàn thành!", Toast.LENGTH_SHORT).show();

                // Gửi thông tin cập nhật lên Spring Boot
                Task updatedTask = new Task();
                updatedTask.setStatus("COMPLETED");

                ApiService apiService = getApiService();
                apiService.updateTask(finalTaskId, updatedTask).enqueue(new Callback<Task>() {
                    @Override
                    public void onResponse(Call<Task> call, Response<Task> response) {
                        Log.d("API", "Cập nhật trạng thái thành công!");
                    }
                    @Override
                    public void onFailure(Call<Task> call, Throwable t) {
                        Toast.makeText(ProjectDetailActivity.this, "Lỗi cập nhật server", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // =========================================
        // NÚT SỬA CÔNG VIỆC (Chuyển trang)
        // =========================================
        btnEditTask.setOnClickListener(v -> {
            Intent editIntent = new Intent(ProjectDetailActivity.this, AddTaskActivity.class);
            // Gửi ID công việc sang màn hình Sửa
            editIntent.putExtra("TASK_ID", finalTaskId);

            // Lấy dữ liệu chữ đang hiển thị trên màn hình để truyền đi
            editIntent.putExtra("TASK_TITLE", tvTitle.getText().toString());

            String desc = tvDesc.getText().toString();
            if (desc.equals("Không có mô tả chi tiết.")) desc = "";
            editIntent.putExtra("TASK_DESC", desc);

            String time = tvTime.getText().toString();
            if (time.equals("Chưa hẹn giờ")) time = "";
            editIntent.putExtra("TASK_TIME", time);

            String loc = tvLocation.getText().toString().replace("📍 ", "");
            if (loc.equals("Không có địa điểm / Link họp")) loc = "";
            editIntent.putExtra("TASK_LOC", loc);

            startActivity(editIntent);
        });

        // =========================================
        // NÚT XÓA CÔNG VIỆC (Gọi API)
        // =========================================
        btnDeleteTask.setOnClickListener(v -> {
            if (finalTaskId != -1) {
                new android.app.AlertDialog.Builder(ProjectDetailActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc muốn xóa công việc này không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            ApiService apiService = getApiService();
                            apiService.deleteTask(finalTaskId).enqueue(new retrofit2.Callback<Void>() {
                                @Override
                                public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                                    if (response.isSuccessful()) {
                                        Toast.makeText(ProjectDetailActivity.this, "Đã xóa công việc", Toast.LENGTH_SHORT).show();
                                        finish(); // Đóng trang này lại
                                    } else {
                                        Toast.makeText(ProjectDetailActivity.this, "Xóa thất bại!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                                    Toast.makeText(ProjectDetailActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });
        // NÚT THÊM SUBTASK
        btnAddSubtask.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng thêm nhiệm vụ phụ đang phát triển", Toast.LENGTH_SHORT).show();
        });
    }

    // Hàm tạo kết nối API dùng chung
    private ApiService getApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(ApiService.class);
    }
}