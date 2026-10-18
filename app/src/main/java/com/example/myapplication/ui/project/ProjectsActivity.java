package com.example.myapplication.ui.project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.ProjectAdapter;
import com.example.myapplication.model.Project;
import com.example.myapplication.model.Task;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.ui.main.ProfileSettingsActivity;

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

        rvProjects = findViewById(R.id.rvProjects);
        rvProjects.setLayoutManager(new LinearLayoutManager(this));

        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompletionRate = findViewById(R.id.tvCompletionRate);
        tvActiveProjects = findViewById(R.id.tvActiveProjectsCount);

        projectList = new ArrayList<>();
        projectAdapter = new ProjectAdapter(projectList);
        rvProjects.setAdapter(projectAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchDataFromApi();
    }

    private void fetchDataFromApi() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);

        if (currentUserId == -1) return;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);

        apiService.getProjectsByUser(currentUserId).enqueue(new Callback<List<Project>>() {
            @Override
            public void onResponse(Call<List<Project>> call, Response<List<Project>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    projectList.clear();
                    projectList.addAll(response.body());

                    // Tải danh sách công việc của user để tự động tính tổng số task và số task đã hoàn thành cho từng dự án
                    apiService.getTasksByUser(currentUserId).enqueue(new Callback<List<Task>>() {
                        @Override
                        public void onResponse(Call<List<Task>> taskCall, Response<List<Task>> taskResponse) {
                            if (taskResponse.isSuccessful() && taskResponse.body() != null) {
                                List<Task> userTasks = taskResponse.body();
                                for (Project p : projectList) {
                                    int total = 0;
                                    int finished = 0;
                                    for (Task t : userTasks) {
                                        if (t.getProjectId() == p.getId()) {
                                            total++;
                                            String status = t.getStatus() != null ? t.getStatus().toUpperCase() : "";
                                            if ("DONE".equals(status) || "COMPLETED".equals(status)) {
                                                finished++;
                                            }
                                        }
                                    }
                                    p.setTotalTasks(total);
                                    p.setFinishedTasks(finished);
                                }
                            }
                            projectAdapter.notifyDataSetChanged();
                            updateSummaryData(projectList);
                        }

                        @Override
                        public void onFailure(Call<List<Task>> taskCall, Throwable t) {
                            projectAdapter.notifyDataSetChanged();
                            updateSummaryData(projectList);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<List<Project>> call, Throwable t) {
                Toast.makeText(ProjectsActivity.this, "Lỗi tải dữ liệu dự án", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSummaryData(List<Project> list) {
        if (list == null || list.isEmpty()) {
            if (tvActiveProjects != null) tvActiveProjects.setText("0 Đang chạy");
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
    }
}