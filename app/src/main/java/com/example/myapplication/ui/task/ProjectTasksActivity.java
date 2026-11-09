package com.example.myapplication.ui.task;

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
import com.example.myapplication.adapter.PhaseGroupAdapter;
import com.example.myapplication.model.PhaseGroup;
import com.example.myapplication.model.Task;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProjectTasksActivity extends AppCompatActivity {

    private RecyclerView rvProjectTasks;
    private PhaseGroupAdapter phaseAdapter;

    private final List<Task> fullTaskList = new ArrayList<>();
    private final List<PhaseGroup> phaseGroupList = new ArrayList<>();

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

        LinearLayout btnAiBreakdownProject = findViewById(R.id.btnAiBreakdownProject);
        if (btnAiBreakdownProject != null) {
            String pName = projectName != null ? projectName.trim() : "";
            boolean isDefaultProject = pName.contains("Công việc") ||
                                      pName.contains("Học tập") || pName.contains("Học Tập") ||
                                      pName.contains("Cá nhân") || pName.contains("Cá Nhân") ||
                                      pName.contains("Sức khỏe") || pName.contains("Thể thao") ||
                                      (projectId >= 1 && projectId <= 4);

            if (isDefaultProject) {
                btnAiBreakdownProject.setVisibility(View.GONE);
            } else {
                btnAiBreakdownProject.setVisibility(View.VISIBLE);
                btnAiBreakdownProject.setOnClickListener(v -> {
                    Intent aiIntent = new Intent(ProjectTasksActivity.this, com.example.myapplication.ui.ai.AiTaskBreakdownActivity.class);
                    aiIntent.putExtra("MAIN_TASK_TITLE", projectName != null ? projectName : "Dự án lớn");
                    aiIntent.putExtra("CREATED_PROJECT_ID", projectId);
                    startActivity(aiIntent);
                });
            }
        }

        phaseAdapter = new PhaseGroupAdapter(phaseGroupList);
        rvProjectTasks.setAdapter(phaseAdapter);

        setupSearch();
        setupFilterTabs();
    }

    @Override
    protected void onResume() {
        super.onResume();
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

        int inactiveText = androidx.core.content.ContextCompat.getColor(this, R.color.brand_primary);
        tabAll.setBackgroundResource(R.drawable.bg_pill_light);
        tabAll.setTextColor(inactiveText);
        tabPending.setBackgroundResource(R.drawable.bg_pill_light);
        tabPending.setTextColor(inactiveText);
        tabInProgress.setBackgroundResource(R.drawable.bg_pill_light);
        tabInProgress.setTextColor(inactiveText);

        selectedTab.setBackgroundResource(R.drawable.bg_pill_brown);
        selectedTab.setTextColor(0xFFFFFFFF);

        applyFilterAndSearch();
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

    private void applyFilterAndSearch() {
        String searchQuery = edtSearch != null ? edtSearch.getText().toString().toLowerCase().trim() : "";
        phaseGroupList.clear();

        Map<String, PhaseGroup> mapPhases = new LinkedHashMap<>();

        String projectName = getIntent().getStringExtra("PROJECT_NAME");
        String pName = projectName != null ? projectName.trim() : "";
        boolean isDefaultProject = pName.contains("Công việc") ||
                                  pName.contains("Học tập") || pName.contains("Học Tập") ||
                                  pName.contains("Cá nhân") || pName.contains("Cá Nhân") ||
                                  pName.contains("Sức khỏe") || pName.contains("Thể thao") ||
                                  (projectId >= 1 && projectId <= 4);

        Calendar todayCal = Calendar.getInstance();
        SimpleDateFormat sdfVi = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
        String todayFormatted = sdfVi.format(todayCal.getTime());

        for (Task task : fullTaskList) {
            if (isDefaultProject) {
                if (!isTaskForToday(task, todayCal)) {
                    continue;
                }
            }

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
                String rawTitle = task.getTitle() != null ? task.getTitle() : "Công việc";
                String phaseName;
                String cleanTitle = rawTitle;

                if (rawTitle.startsWith("[") && rawTitle.contains("]")) {
                    int endBracket = rawTitle.indexOf("]");
                    phaseName = "📂 " + rawTitle.substring(1, endBracket).trim();
                    cleanTitle = rawTitle.substring(endBracket + 1).trim();
                } else {
                    if (isDefaultProject) {
                        phaseName = "📅 Công việc hôm nay (" + todayFormatted + ")";
                    } else {
                        phaseName = "📂 Các công việc chung";
                    }
                }

                Task displayTask = new Task();
                displayTask.setId(task.getId());
                displayTask.setProjectId(task.getProjectId());
                displayTask.setStatus(task.getStatus());
                displayTask.setTime(task.getTime());
                displayTask.setTitle(cleanTitle);
                displayTask.setDescription(task.getDescription());
                displayTask.setLocation(task.getLocation());
                displayTask.setUserId(task.getUserId());
                displayTask.setPriority(task.getPriority());

                PhaseGroup group = mapPhases.get(phaseName);
                if (group == null) {
                    group = new PhaseGroup(phaseName);
                    group.setExpanded(true);
                    mapPhases.put(phaseName, group);
                }
                group.getSubTaskList().add(displayTask);
            }
        }

        phaseGroupList.addAll(mapPhases.values());
        if (phaseAdapter != null) {
            phaseAdapter.updateData(phaseGroupList);
        }
    }

    private void updateOverviewStats() {
        String projectName = getIntent().getStringExtra("PROJECT_NAME");
        String pName = projectName != null ? projectName.trim() : "";
        boolean isDefaultProject = pName.contains("Công việc") ||
                                  pName.contains("Học tập") || pName.contains("Học Tập") ||
                                  pName.contains("Cá nhân") || pName.contains("Cá Nhân") ||
                                  pName.contains("Sức khỏe") || pName.contains("Thể thao") ||
                                  (projectId >= 1 && projectId <= 4);

        Calendar todayCal = Calendar.getInstance();

        int total = 0;
        int pending = 0;
        int inProgress = 0;
        int done = 0;

        for (Task t : fullTaskList) {
            if (isDefaultProject && !isTaskForToday(t, todayCal)) {
                continue;
            }

            total++;
            String s = t.getStatus() != null ? t.getStatus().toUpperCase() : "PENDING";
            if ("DONE".equals(s) || "COMPLETED".equals(s) || "HOÀN THÀNH".equals(s)) {
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
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);

        if (currentUserId == -1) {
            fullTaskList.clear();
            updateOverviewStats();
            applyFilterAndSearch();
            return;
        }

        ApiService apiService = RetrofitClient.getApiService();

        List<Task> cached = RetrofitClient.getCachedTasks();
        if (!cached.isEmpty() && fullTaskList.isEmpty()) {
            for (Task task : cached) {
                if (projectId == -1 || task.getProjectId() == projectId) {
                    fullTaskList.add(task);
                }
            }
            updateOverviewStats();
            applyFilterAndSearch();
        }

        apiService.getTasksByUser(currentUserId).enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Task> body = response.body();
                    RetrofitClient.setCachedTasks(body);
                    fullTaskList.clear();

                    for (Task task : body) {
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
                if (fullTaskList.isEmpty()) {
                    Toast.makeText(ProjectTasksActivity.this, "Lỗi tải dữ liệu công việc", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
