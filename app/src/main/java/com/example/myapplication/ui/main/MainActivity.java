package com.example.myapplication.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.TaskAdapter;
import com.example.myapplication.model.Task;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.ui.ai.AiAutoScheduleActivity;
import com.example.myapplication.ui.pomodoro.PomodoroActivity;
import com.example.myapplication.ui.project.ProjectsActivity;
import com.example.myapplication.ui.task.AddTaskActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private TextView tvTasksToday, tvCompletionPercent, tvPendingCount, tvInProgressCount, tvDoneCount;
    private TextView tabAll, tabPending, tabInProgress;
    private EditText edtSearch;

    private RecyclerView rvTasks;
    private TaskAdapter taskAdapter;
    private List<Task> fullTaskList = new ArrayList<>();
    private List<Task> displayedTaskList = new ArrayList<>();

    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ImageView imgAvatar = findViewById(R.id.imgAvatar);
        if (imgAvatar != null) {
            imgAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ProfileSettingsActivity.class);
                startActivity(intent);
            });
        }

        tvTasksToday = findViewById(R.id.tvTasksToday);
        tvCompletionPercent = findViewById(R.id.tvCompletionPercent);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvInProgressCount = findViewById(R.id.tvInProgressCount);
        tvDoneCount = findViewById(R.id.tvDoneCount);

        edtSearch = findViewById(R.id.edtSearch);
        tabAll = findViewById(R.id.tabAll);
        tabPending = findViewById(R.id.tabPending);
        tabInProgress = findViewById(R.id.tabInProgress);

        rvTasks = findViewById(R.id.rvTasks);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(displayedTaskList);
        rvTasks.setAdapter(taskAdapter);

        setupSearch();
        setupFilterTabs();
        setupBottomNav();

        loadTasksFromServer();
    }

    private void setupSearch() {
        if (edtSearch == null) return;
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilterAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilterTabs() {
        if (tabAll == null) return;
        tabAll.setOnClickListener(v -> selectFilterTab("ALL", tabAll));
        tabPending.setOnClickListener(v -> selectFilterTab("PENDING", tabPending));
        tabInProgress.setOnClickListener(v -> selectFilterTab("IN_PROGRESS", tabInProgress));
    }

    private void selectFilterTab(String filterMode, TextView selectedTab) {
        currentFilter = filterMode;

        tabAll.setBackgroundResource(R.drawable.bg_pill_light);
        tabAll.setTextColor(0xFF555555);
        tabPending.setBackgroundResource(R.drawable.bg_pill_light);
        tabPending.setTextColor(0xFF555555);
        tabInProgress.setBackgroundResource(R.drawable.bg_pill_light);
        tabInProgress.setTextColor(0xFF555555);

        selectedTab.setBackgroundResource(R.drawable.bg_pill_brown);
        selectedTab.setTextColor(0xFFFFFFFF);

        applyFilterAndSearch();
    }

    private void applyFilterAndSearch() {
        String searchQuery = edtSearch != null ? edtSearch.getText().toString().toLowerCase().trim() : "";
        displayedTaskList.clear();

        for (Task task : fullTaskList) {
            boolean matchesSearch = searchQuery.isEmpty() ||
                    (task.getTitle() != null && task.getTitle().toLowerCase().contains(searchQuery)) ||
                    (task.getDescription() != null && task.getDescription().toLowerCase().contains(searchQuery));

            boolean matchesFilter = false;
            String status = task.getStatus() != null ? task.getStatus().toUpperCase() : "PENDING";

            if ("ALL".equals(currentFilter)) {
                matchesFilter = true;
            } else if ("PENDING".equals(currentFilter) && ("PENDING".equals(status) || "CHỜ XỬ LÝ".contains(status))) {
                matchesFilter = true;
            } else if ("IN_PROGRESS".equals(currentFilter) && ("IN_PROGRESS".equals(status) || "DOING".equals(status))) {
                matchesFilter = true;
            }

            if (matchesSearch && matchesFilter) {
                displayedTaskList.add(task);
            }
        }

        taskAdapter.notifyDataSetChanged();
    }

    private void updateOverviewStats() {
        int total = fullTaskList.size();
        int pending = 0;
        int inProgress = 0;
        int done = 0;

        for (Task t : fullTaskList) {
            String s = t.getStatus() != null ? t.getStatus().toUpperCase() : "PENDING";
            if ("DONE".equals(s) || "COMPLETED".equals(s)) {
                done++;
            } else if ("IN_PROGRESS".equals(s) || "DOING".equals(s)) {
                inProgress++;
            } else {
                pending++;
            }
        }

        if (tvTasksToday != null) {
            tvTasksToday.setText(total + " công việc");
        }

        int percent = total > 0 ? (done * 100 / total) : 0;
        if (tvCompletionPercent != null) {
            tvCompletionPercent.setText(percent + "% Hoàn thành");
        }

        if (tvPendingCount != null) {
            int pPct = total > 0 ? (pending * 100 / total) : 0;
            tvPendingCount.setText(pending + " mục • " + pPct + "%");
        }

        if (tvInProgressCount != null) {
            int ipPct = total > 0 ? (inProgress * 100 / total) : 0;
            tvInProgressCount.setText(inProgress + " mục • " + ipPct + "%");
        }

        if (tvDoneCount != null) {
            int dPct = total > 0 ? (done * 100 / total) : 0;
            tvDoneCount.setText(done + " mục • " + dPct + "%");
        }

        if (tabAll != null) tabAll.setText("Tất cả " + total);
        if (tabPending != null) tabPending.setText("● Chờ xử lý " + pending);
        if (tabInProgress != null) tabInProgress.setText("● Đang làm " + inProgress);
    }

    private void setupBottomNav() {
        View layoutAppTitle = findViewById(R.id.layoutAppTitle);
        if (layoutAppTitle != null) {
            layoutAppTitle.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AiAutoScheduleActivity.class);
                startActivity(intent);
            });
        }

        View btnFilterTop = findViewById(R.id.btnFilterTop);
        if (btnFilterTop != null) {
            btnFilterTop.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AiAutoScheduleActivity.class);
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

        LinearLayout navAgenda = findViewById(R.id.navAgenda);
        if (navAgenda != null) {
            navAgenda.setOnClickListener(v -> {
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
                    fullTaskList.clear();
                    fullTaskList.addAll(response.body());

                    updateOverviewStats();
                    applyFilterAndSearch();
                }
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lỗi tải danh sách công việc!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}