package com.example.myapplication.ui.task;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.Task;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.ui.ai.AiTaskBreakdownActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProjectDetailActivity extends AppCompatActivity {

    private int taskId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvTitle = findViewById(R.id.tvTaskDetailTitle);
        TextView tvDesc = findViewById(R.id.tvTaskDetailDesc);
        TextView tvTime = findViewById(R.id.tvTaskDetailTime);
        TextView tvLocation = findViewById(R.id.tvTaskDetailLocation);
        TextView tvStatus = findViewById(R.id.tvTaskDetailStatus);

        TextView btnDeleteTask = findViewById(R.id.btnDeleteTask);
        TextView btnEditTask = findViewById(R.id.btnEditTask);
        TextView btnCompleteTask = findViewById(R.id.btnCompleteTask);
        TextView btnAddSubtask = findViewById(R.id.btnAddSubtask);

        Intent intent = getIntent();
        if (intent != null) {
            taskId = intent.getIntExtra("TASK_ID", -1);
            String title = intent.getStringExtra("TASK_TITLE");
            String desc = intent.getStringExtra("TASK_DESC");
            String time = intent.getStringExtra("TASK_TIME");
            String location = intent.getStringExtra("TASK_LOC");
            String status = intent.getStringExtra("TASK_STATUS");

            if (tvTitle != null) tvTitle.setText((title != null && !title.trim().isEmpty()) ? title : "Chưa có tiêu đề");
            if (tvDesc != null) tvDesc.setText((desc != null && !desc.trim().isEmpty()) ? desc : "Không có mô tả chi tiết.");
            if (tvTime != null) tvTime.setText((time != null && !time.trim().isEmpty()) ? time : "Chưa hẹn giờ");
            if (tvLocation != null) tvLocation.setText((location != null && !location.trim().isEmpty()) ? "📍 " + location : "📍 Không có địa điểm / Link họp");

            if (tvStatus != null) {
                if ("DONE".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status) || "HOÀN THÀNH".equalsIgnoreCase(status)) {
                    tvStatus.setText("● ĐÃ HOÀN THÀNH");
                    tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                } else {
                    tvStatus.setText("● ĐANG XỬ LÝ");
                    tvStatus.setTextColor(Color.parseColor("#E65100"));
                }
            }
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnDeleteTask != null) {
            btnDeleteTask.setOnClickListener(v -> {
                if (taskId != -1) {
                    new AlertDialog.Builder(ProjectDetailActivity.this)
                            .setTitle("Xác nhận xóa")
                            .setMessage("Bạn có chắc chắn muốn xóa công việc này không?")
                            .setPositiveButton("Xóa", (dialog, which) -> {
                                getApiService().deleteTask(taskId).enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> response) {
                                        if (response.isSuccessful()) {
                                            Toast.makeText(ProjectDetailActivity.this, "Đã xóa công việc!", Toast.LENGTH_SHORT).show();
                                            finish();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<Void> call, Throwable t) {
                                        Toast.makeText(ProjectDetailActivity.this, "Lỗi kết nối khi xóa!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                }
            });
        }

        if (btnEditTask != null) {
            btnEditTask.setOnClickListener(v -> {
                Intent editIntent = new Intent(ProjectDetailActivity.this, AddTaskActivity.class);
                editIntent.putExtra("TASK_ID", taskId);
                if (tvTitle != null) editIntent.putExtra("TASK_TITLE", tvTitle.getText().toString());
                if (tvDesc != null) editIntent.putExtra("TASK_DESC", tvDesc.getText().toString());
                if (tvTime != null) editIntent.putExtra("TASK_TIME", tvTime.getText().toString());
                if (tvLocation != null) editIntent.putExtra("TASK_LOC", tvLocation.getText().toString().replace("📍 ", ""));
                startActivity(editIntent);
            });
        }

        if (btnCompleteTask != null) {
            btnCompleteTask.setOnClickListener(v -> {
                if (taskId != -1) {
                    if (tvStatus != null) {
                        tvStatus.setText("● ĐÃ HOÀN THÀNH");
                        tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                    }
                    Toast.makeText(this, "Đã đánh dấu hoàn thành!", Toast.LENGTH_SHORT).show();

                    Task updatedTask = new Task();
                    updatedTask.setStatus("DONE");

                    getApiService().updateTask(taskId, updatedTask).enqueue(new Callback<Task>() {
                        @Override
                        public void onResponse(Call<Task> call, Response<Task> response) {}

                        @Override
                        public void onFailure(Call<Task> call, Throwable t) {}
                    });
                }
            });
        }

        if (btnAddSubtask != null) {
            btnAddSubtask.setOnClickListener(v -> {
                Intent aiIntent = new Intent(ProjectDetailActivity.this, AiTaskBreakdownActivity.class);
                startActivity(aiIntent);
            });
        }
    }

    private ApiService getApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(ApiService.class);
    }
}