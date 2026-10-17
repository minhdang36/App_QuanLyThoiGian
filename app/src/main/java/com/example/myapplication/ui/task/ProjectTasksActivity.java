package com.example.myapplication.ui.task;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.TaskAdapter;
import com.example.myapplication.model.Task;
import com.example.myapplication.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProjectTasksActivity extends AppCompatActivity {

    private RecyclerView rvProjectTasks;
    private TaskAdapter taskAdapter;
    private List<Task> fullTaskList = new ArrayList<>();
    private List<Task> displayedTaskList = new ArrayList<>();

    private TextView tvTasksCount, tvCompletionPercent, tvPendingCount, tvInProgressCount, tvDoneCount;
    private TextView tabAll, tabPending, tabInProgress;
    private EditText edtSearch;

    private int projectId = -1;
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_tasks);

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvProjectNameTitle = findViewById(R.id.tvProjectNameTitle);
        tvTasksCount = findViewById(R.id.tvTasksCount);
        tvCompletionPercent = findViewById(R.id.tvCompletionPercent);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvInProgressCount = findViewById(R.id.tvInProgressCount);
        tvDoneCount = findViewById(R.id.tvDoneCount);

        edtSearch = findViewById(R.id.edtSearch);
        tabAll = findViewById(R.id.tabAll);
        tabPending = findViewById(R.id.tabPending);
        tabInProgress = findViewById(R.id.tabInProgress);

        rvProjectTasks = findViewById(R.id.rvProjectTasks);
        rvProjectTasks.setLayoutManager(new LinearLayoutManager(this));

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        projectId = getIntent().getIntExtra("PROJECT_ID", -1);
        String projectName = getIntent().getStringExtra("PROJECT_NAME");
        if (projectName != null && !projectName.isEmpty() && tvProjectNameTitle != null) {
            tvProjectNameTitle.setText(projectName);
        }

        taskAdapter = new TaskAdapter(displayedTaskList);
        rvProjectTasks.setAdapter(taskAdapter);

        setupSearch();
        setupFilterTabs();

        loadTasksForProject();
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

        if (tvTasksCount != null) {
            tvTasksCount.setText(total + " công việc");
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

    private void loadTasksForProject() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        apiService.getAllTasks().enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullTaskList.clear();

                    for (Task task : response.body()) {
                        if (projectId == -1 || task.getProjectId() == projectId) {
                            fullTaskList.add(task);
                        }
                    }

                    updateOverviewStats();
                    applyFilterAndSearch();
                }
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                Toast.makeText(ProjectTasksActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }
}