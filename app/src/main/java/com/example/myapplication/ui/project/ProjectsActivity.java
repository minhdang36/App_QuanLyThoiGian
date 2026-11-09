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
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.ui.main.ProfileSettingsActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

        android.widget.LinearLayout btnAiBreakdownMain = findViewById(R.id.btnAiBreakdownMain);
        if (btnAiBreakdownMain != null) {
            btnAiBreakdownMain.setOnClickListener(v -> {
                Intent aiIntent = new Intent(ProjectsActivity.this, com.example.myapplication.ui.ai.AiTaskBreakdownActivity.class);
                aiIntent.putExtra("MAIN_TASK_TITLE", "Dự án lớn của tôi");
                startActivity(aiIntent);
            });
        }

        TextView tvCancelProjects = findViewById(R.id.tvCancelProjects);
        if (tvCancelProjects != null) {
            tvCancelProjects.setOnClickListener(v -> finish());
        }

        rvProjects = findViewById(R.id.rvProjects);
        rvProjects.setLayoutManager(new LinearLayoutManager(this));
        rvProjects.setItemViewCacheSize(10);

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

    private void computeProjectTaskStats(List<Project> projects, List<Task> tasks) {
        if (projects == null || tasks == null) return;
        Calendar todayCal = Calendar.getInstance();

        for (Project p : projects) {
            String pName = p.getName() != null ? p.getName().trim() : "";
            boolean isDefaultProject = pName.contains("Công việc") ||
                                      pName.contains("Học tập") || pName.contains("Học Tập") ||
                                      pName.contains("Cá nhân") || pName.contains("Cá Nhân") ||
                                      pName.contains("Sức khỏe") || pName.contains("Thể thao") ||
                                      (p.getId() >= 1 && p.getId() <= 4);

            int total = 0;
            int finished = 0;

            for (Task t : tasks) {
                if (t.getProjectId() == p.getId()) {
                    if (isDefaultProject && !isTaskForToday(t, todayCal)) {
                        continue;
                    }

                    total++;
                    String status = t.getStatus() != null ? t.getStatus().toUpperCase() : "";
                    if ("DONE".equals(status) || "COMPLETED".equals(status) || "HOÀN THÀNH".equals(status)) {
                        finished++;
                    }
                }
            }
            p.setTotalTasks(total);
            p.setFinishedTasks(finished);
        }
    }

    private void fetchDataFromApi() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);

        if (currentUserId == -1) return;

        // 1. Hiển thị ngay lập tức từ bộ nhớ đệm RAM nếu có
        List<Project> cachedProjects = RetrofitClient.getCachedProjects();
        List<Task> cachedTasks = RetrofitClient.getCachedTasks();
        if (!cachedProjects.isEmpty() && projectList.isEmpty()) {
            projectList.clear();
            projectList.addAll(cachedProjects);
            computeProjectTaskStats(projectList, cachedTasks);
            projectAdapter.notifyDataSetChanged();
            updateSummaryData(projectList);
        }

        ApiService apiService = RetrofitClient.getApiService();

        apiService.getProjectsByUser(currentUserId).enqueue(new Callback<List<Project>>() {
            @Override
            public void onResponse(Call<List<Project>> call, Response<List<Project>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    projectList.clear();
                    projectList.addAll(response.body());
                    RetrofitClient.setCachedProjects(projectList);

                    // Nếu cache task vừa được tải gần đây (< 5 giây), dùng ngay không cần đợi vòng mạng thứ 2
                    if (RetrofitClient.isTasksCacheFresh(5000)) {
                        computeProjectTaskStats(projectList, RetrofitClient.getCachedTasks());
                        projectAdapter.notifyDataSetChanged();
                        updateSummaryData(projectList);
                        return;
                    }

                    apiService.getTasksByUser(currentUserId).enqueue(new Callback<List<Task>>() {
                        @Override
                        public void onResponse(Call<List<Task>> taskCall, Response<List<Task>> taskResponse) {
                            if (taskResponse.isSuccessful() && taskResponse.body() != null) {
                                List<Task> userTasks = taskResponse.body();
                                RetrofitClient.setCachedTasks(userTasks);
                                computeProjectTaskStats(projectList, userTasks);
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
                if (projectList.isEmpty()) {
                    Toast.makeText(ProjectsActivity.this, "Lỗi tải dữ liệu dự án", Toast.LENGTH_SHORT).show();
                }
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
