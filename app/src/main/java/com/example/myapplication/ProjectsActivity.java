package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.ProjectAdapter;
import com.example.myapplication.model.Project;
import com.example.myapplication.model.Task;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProjectsActivity extends AppCompatActivity {

    private RecyclerView rvProjects;
    private ProjectAdapter projectAdapter;
    private List<Project> projectList;

    private TextView tvTotalTasks;
    private TextView tvCompletionRate;
    private TextView tvActiveProjects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projects);

        // Ánh xạ các nút bấm
        ImageView imgAvatar = findViewById(R.id.imgAvatar);
        if (imgAvatar != null) {
            imgAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(ProjectsActivity.this, ProfileSettingsActivity.class);
                startActivity(intent);
            });
        }

        TextView btnAddProject = findViewById(R.id.btnAddProject);
        if (btnAddProject != null) {
            btnAddProject.setOnClickListener(v -> {
                Intent intent = new Intent(ProjectsActivity.this, AddProjectActivity.class);
                startActivity(intent);
            });
        }

        TextView tvCancelProjects = findViewById(R.id.tvCancelProjects);
        if (tvCancelProjects != null) {
            tvCancelProjects.setOnClickListener(v -> finish());
        }

        // Ánh xạ RecyclerView & TextView thống kê
        rvProjects = findViewById(R.id.rvProjects);
        rvProjects.setLayoutManager(new LinearLayoutManager(this));

        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompletionRate = findViewById(R.id.tvCompletionRate);
        tvActiveProjects = findViewById(R.id.tvActiveProjectsCount);

        // Khởi tạo Adapter
        projectList = new ArrayList<>();
        projectAdapter = new ProjectAdapter(projectList);
        rvProjects.setAdapter(projectAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load lại danh sách mỗi khi trang hiển thị
        fetchDataFromApi();
    }

    private void fetchDataFromApi() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);

        if (currentUserId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);

        // 1. GỌI API LẤY DANH SÁCH DỰ ÁN CỦA NGƯỜI DÙNG NÀY
        apiService.getProjectsByUser(currentUserId).enqueue(new Callback<List<Project>>() {
            @Override
            public void onResponse(Call<List<Project>> call, Response<List<Project>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    projectList.clear();
                    projectList.addAll(response.body());

                    // 2. NGAY SAU ĐÓ, GỌI TIẾP API LẤY TẤT CẢ CÔNG VIỆC CỦA NGƯỜI DÙNG NÀY
                    apiService.getTasksByUser(currentUserId).enqueue(new Callback<List<Task>>() {
                        @Override
                        public void onResponse(Call<List<Task>> taskCall, Response<List<Task>> taskResponse) {
                            if (taskResponse.isSuccessful() && taskResponse.body() != null) {
                                List<Task> allMyTasks = taskResponse.body();

                                // 3. GHÉP NỐI VÀ ĐẾM SỐ LƯỢNG CHO TỪNG DỰ ÁN
                                for (Project project : projectList) {
                                    int total = 0;
                                    int completed = 0;

                                    for (Task task : allMyTasks) {
                                        // Nếu Task có cùng projectId với Project đang xét
                                        if (task.getProjectId() == project.getId()) {
                                            total++;
                                            // Đếm số task đã hoàn thành (Giả sử status là COMPLETED hoặc DONE)
                                            if ("COMPLETED".equalsIgnoreCase(task.getStatus()) || "DONE".equalsIgnoreCase(task.getStatus())) {
                                                completed++;
                                            }
                                        }
                                    }

                                    // Cập nhật số đếm vào đối tượng Project
                                    project.setTotalTasks(total);
                                    project.setFinishedTasks(completed);
                                }
                            }

                            // 4. BẬT ĐÈN XANH: CẬP NHẬT GIAO DIỆN
                            projectAdapter.notifyDataSetChanged();
                            updateSummaryData(projectList);
                        }

                        @Override
                        public void onFailure(Call<List<Task>> call, Throwable t) {
                            // Nếu lỗi tải Task, vẫn hiển thị danh sách Project
                            projectAdapter.notifyDataSetChanged();
                            updateSummaryData(projectList);
                        }
                    });

                } else {
                    // Nếu lỗi hoặc rỗng, làm mới giao diện về 0
                    updateSummaryData(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<List<Project>> call, Throwable t) {
                Toast.makeText(ProjectsActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
                updateSummaryData(new ArrayList<>());
            }
        });
    }

    private void updateSummaryData(List<Project> list) {
        // Cập nhật giao diện về 0 nếu danh sách rỗng
        if (list == null || list.isEmpty()) {
            if (tvActiveProjects != null) tvActiveProjects.setText("0 Đang chạy");
            if (tvTotalTasks != null) tvTotalTasks.setText("0 Công\nviệc");
            if (tvCompletionRate != null) tvCompletionRate.setText("0% Tỷ lệ\nTB HOÀN THÀNH");
            return;
        }

        int totalTasks = 0;
        int totalCompletedTasks = 0;
        int activeProjects = list.size();

        for (Project project : list) {
            totalTasks += project.getTotalTasks();
            totalCompletedTasks += project.getFinishedTasks();
        }

        int averageCompletion = 0;
        if (totalTasks > 0) {
            averageCompletion = (totalCompletedTasks * 100) / totalTasks;
        }

        if (tvTotalTasks != null) {
            tvTotalTasks.setText(totalTasks + " Công\nviệc");
        }
        if (tvCompletionRate != null) {
            tvCompletionRate.setText(averageCompletion + "% Tỷ lệ\nTB HOÀN THÀNH");
        }
        if (tvActiveProjects != null) {
            tvActiveProjects.setText(activeProjects + " Đang chạy");
        }
    }}