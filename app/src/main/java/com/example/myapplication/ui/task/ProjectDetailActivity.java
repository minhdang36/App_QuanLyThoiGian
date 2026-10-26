package com.example.myapplication.ui.task;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.Project;
import com.example.myapplication.model.Task;
import com.example.myapplication.network.ApiService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProjectDetailActivity extends AppCompatActivity {

    private int taskId = -1;
    private LinearLayout layoutSubtasksContainer;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        ImageView btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTaskDetailTitle);
        TextView tvDesc = findViewById(R.id.tvTaskDetailDesc);
        TextView tvTime = findViewById(R.id.tvTaskDetailTime);
        TextView tvLocation = findViewById(R.id.tvTaskDetailLocation);
        TextView tvStatus = findViewById(R.id.tvTaskDetailStatus);

        TextView btnDeleteTask = findViewById(R.id.btnDeleteTask);
        TextView btnEditTask = findViewById(R.id.btnEditTask);
        TextView btnCompleteTask = findViewById(R.id.btnCompleteTask);
        TextView btnAddSubtask = findViewById(R.id.btnAddSubtask);
        layoutSubtasksContainer = findViewById(R.id.layoutSubtasksContainer);

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

            updatePriorityTag(intent.getStringExtra("TASK_PRIORITY"), intent.getStringExtra("TASK_TIME"));
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

        // Bắt đầu bấm giờ làm task bằng Chế độ Tập trung Pomodoro Focus
        TextView btnStartPomodoro = findViewById(R.id.btnStartPomodoro);
        if (btnStartPomodoro != null) {
            btnStartPomodoro.setOnClickListener(v -> {
                Intent pomodoroIntent = new Intent(ProjectDetailActivity.this, com.example.myapplication.ui.pomodoro.PomodoroActivity.class);
                if (tvTitle != null) {
                    pomodoroIntent.putExtra("ACTIVE_TASK_TITLE", tvTitle.getText().toString());
                }
                TextView tvTaskTime = findViewById(R.id.tvTaskDetailTime);
                if (tvTaskTime != null) {
                    pomodoroIntent.putExtra("TASK_TIME", tvTaskTime.getText().toString());
                }
                startActivity(pomodoroIntent);
            });
        }

        if (btnAddSubtask != null) {
            btnAddSubtask.setOnClickListener(v -> showAddSubtaskDialog());
        }

        renderSubtasksForCurrentTask();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRealTaskAndProjectData();
    }

    private void renderSubtasksForCurrentTask() {
        if (layoutSubtasksContainer == null) return;
        layoutSubtasksContainer.removeAllViews();

        String rawTitle = tvTitle != null ? tvTitle.getText().toString().trim() : "";
        if (rawTitle.isEmpty()) return;

        SharedPreferences pref = getSharedPreferences("TaskSubTasks", MODE_PRIVATE);
        Gson gson = new Gson();

        String cleanTitle = rawTitle;
        if (cleanTitle.startsWith("[") && cleanTitle.contains("]")) {
            cleanTitle = cleanTitle.substring(cleanTitle.indexOf("]") + 1).trim();
        }

        String json = pref.getString("SUBTASKS_" + cleanTitle.hashCode(), null);
        if (json == null) {
            json = pref.getString("SUBTASKS_" + rawTitle.hashCode(), null);
        }

        List<String> subTasks = new ArrayList<>();
        if (json != null && !json.trim().isEmpty()) {
            try {
                Type listType = new TypeToken<ArrayList<String>>() {}.getType();
                List<String> parsed = gson.fromJson(json, listType);
                if (parsed != null) subTasks.addAll(parsed);
            } catch (Exception ignored) {}
        }

        // Nếu chưa có subtasks người dùng chủ động tích chọn thêm vào, hiển thị hướng dẫn thân thiện
        if (subTasks.isEmpty()) {
            TextView tvEmptyHint = new TextView(this);
            tvEmptyHint.setText("💡 Chưa có nhiệm vụ phụ. Bạn có thể mở trang Chi tiết Giai đoạn, tích chọn các đầu việc và bấm nút 'Thêm vào Nhiệm vụ phụ' hoặc bấm '⊕ Thêm nhiệm vụ phụ' ở bên dưới.");
            tvEmptyHint.setTextColor(Color.parseColor("#9E9E9E"));
            tvEmptyHint.setTextSize(12);
            tvEmptyHint.setPadding(10, 10, 10, 10);
            layoutSubtasksContainer.addView(tvEmptyHint);
            return;
        }

        SharedPreferences checkPref = getSharedPreferences("SubTaskCheckedState", MODE_PRIVATE);

        final String finalCleanTitle = cleanTitle;

        // Hiển thị từng nhiệm vụ phụ đầy đủ tính năng: Tích hoàn thành, Sửa, Xóa
        for (int i = 0; i < subTasks.size(); i++) {
            final int itemIndex = i;
            String st = subTasks.get(i);
            String checkKey = "CHECK_" + finalCleanTitle.hashCode() + "_" + i;

            LinearLayout itemRow = new LinearLayout(this);
            itemRow.setOrientation(LinearLayout.HORIZONTAL);
            itemRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowLp.setMargins(0, 8, 0, 8);
            itemRow.setLayoutParams(rowLp);

            // CheckBox tích chọn hoàn thành
            CheckBox checkBox = new CheckBox(this);
            LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            );
            checkBox.setLayoutParams(cbLp);
            checkBox.setText(st);
            checkBox.setTextSize(13);
            checkBox.setButtonTintList(ColorStateList.valueOf(Color.parseColor("#004D40")));

            boolean isChecked = checkPref.getBoolean(checkKey, false);
            checkBox.setChecked(isChecked);
            updateCheckboxStyle(checkBox, isChecked);

            checkBox.setOnCheckedChangeListener((buttonView, isCheckedVal) -> {
                checkPref.edit().putBoolean(checkKey, isCheckedVal).apply();
                updateCheckboxStyle(checkBox, isCheckedVal);
            });

            itemRow.addView(checkBox);

            // Nút SỬA nhiệm vụ phụ
            ImageButton btnEditSub = new ImageButton(this);
            btnEditSub.setImageResource(android.R.drawable.ic_menu_edit);
            btnEditSub.setBackgroundResource(android.R.color.transparent);
            btnEditSub.setImageTintList(ColorStateList.valueOf(Color.parseColor("#888888")));
            LinearLayout.LayoutParams btnEditLp = new LinearLayout.LayoutParams(72, 72);
            btnEditLp.setMargins(8, 0, 0, 0);
            btnEditSub.setLayoutParams(btnEditLp);
            btnEditSub.setOnClickListener(v -> showEditSubtaskDialog(itemIndex, st, finalCleanTitle, subTasks));
            itemRow.addView(btnEditSub);

            // Nút XÓA nhiệm vụ phụ
            ImageButton btnDeleteSub = new ImageButton(this);
            btnDeleteSub.setImageResource(android.R.drawable.ic_menu_delete);
            btnDeleteSub.setBackgroundResource(android.R.color.transparent);
            btnDeleteSub.setImageTintList(ColorStateList.valueOf(Color.parseColor("#C62828")));
            LinearLayout.LayoutParams btnDeleteLp = new LinearLayout.LayoutParams(72, 72);
            btnDeleteLp.setMargins(4, 0, 0, 0);
            btnDeleteSub.setLayoutParams(btnDeleteLp);
            btnDeleteSub.setOnClickListener(v -> showDeleteSubtaskDialog(itemIndex, finalCleanTitle, subTasks));
            itemRow.addView(btnDeleteSub);

            layoutSubtasksContainer.addView(itemRow);
        }
    }

    private void updateCheckboxStyle(CheckBox checkBox, boolean isChecked) {
        if (isChecked) {
            checkBox.setPaintFlags(checkBox.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            checkBox.setTextColor(Color.parseColor("#888888"));
        } else {
            checkBox.setPaintFlags(checkBox.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            checkBox.setTextColor(Color.parseColor("#222222"));
        }
    }

    private void showEditSubtaskDialog(int index, String currentText, String cleanTitle, List<String> subTasks) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✏ Sửa nhiệm vụ phụ");

        final EditText edtInput = new EditText(this);
        edtInput.setText(currentText);
        edtInput.setPadding(40, 30, 40, 30);
        builder.setView(edtInput);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newText = edtInput.getText().toString().trim();
            if (!newText.isEmpty()) {
                subTasks.set(index, newText);
                saveSubtasksListToPref(cleanTitle, subTasks);
                renderSubtasksForCurrentTask();
                Toast.makeText(ProjectDetailActivity.this, "✔ Đã cập nhật nhiệm vụ phụ!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showDeleteSubtaskDialog(int index, String cleanTitle, List<String> subTasks) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa nhiệm vụ phụ này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    subTasks.remove(index);
                    saveSubtasksListToPref(cleanTitle, subTasks);
                    renderSubtasksForCurrentTask();
                    Toast.makeText(ProjectDetailActivity.this, "✔ Đã xóa nhiệm vụ phụ!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void saveSubtasksListToPref(String cleanTitle, List<String> subTasks) {
        SharedPreferences pref = getSharedPreferences("TaskSubTasks", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = gson.toJson(subTasks);

        String rawTitle = tvTitle != null ? tvTitle.getText().toString().trim() : "";
        pref.edit().putString("SUBTASKS_" + cleanTitle.hashCode(), json).apply();
        pref.edit().putString("SUBTASKS_" + rawTitle.hashCode(), json).apply();
    }

    private void showAddSubtaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⊕ Thêm nhiệm vụ phụ mới");

        final EditText edtInput = new EditText(this);
        edtInput.setHint("Nhập nội dung nhiệm vụ phụ...");
        edtInput.setPadding(40, 30, 40, 30);
        builder.setView(edtInput);

        builder.setPositiveButton("Thêm", (dialog, which) -> {
            String newSub = edtInput.getText().toString().trim();
            if (!newSub.isEmpty()) {
                String rawTitle = tvTitle != null ? tvTitle.getText().toString().trim() : "";
                String cleanTitle = rawTitle;
                if (cleanTitle.startsWith("[") && cleanTitle.contains("]")) {
                    cleanTitle = cleanTitle.substring(cleanTitle.indexOf("]") + 1).trim();
                }

                SharedPreferences pref = getSharedPreferences("TaskSubTasks", MODE_PRIVATE);
                Gson gson = new Gson();

                String json = pref.getString("SUBTASKS_" + cleanTitle.hashCode(), null);
                List<String> subTasks = new ArrayList<>();
                if (json != null) {
                    try {
                        Type listType = new TypeToken<ArrayList<String>>() {}.getType();
                        List<String> parsed = gson.fromJson(json, listType);
                        if (parsed != null) subTasks.addAll(parsed);
                    } catch (Exception ignored) {}
                }

                subTasks.add(newSub);
                saveSubtasksListToPref(cleanTitle, subTasks);

                renderSubtasksForCurrentTask();
                Toast.makeText(ProjectDetailActivity.this, "✔ Đã thêm nhiệm vụ phụ!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void loadRealTaskAndProjectData() {
        if (taskId == -1) return;

        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);
        if (currentUserId == -1) return;

        getApiService().getTasksByUser(currentUserId).enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Task currentTask = null;
                    for (Task t : response.body()) {
                        if (t.getId() == taskId) {
                            currentTask = t;
                            break;
                        }
                    }

                    if (currentTask != null) {
                        TextView tvTitle = findViewById(R.id.tvTaskDetailTitle);
                        TextView tvDesc = findViewById(R.id.tvTaskDetailDesc);
                        TextView tvTime = findViewById(R.id.tvTaskDetailTime);
                        TextView tvLocation = findViewById(R.id.tvTaskDetailLocation);
                        TextView tvStatus = findViewById(R.id.tvTaskDetailStatus);

                        if (tvTitle != null) tvTitle.setText((currentTask.getTitle() != null && !currentTask.getTitle().trim().isEmpty()) ? currentTask.getTitle() : "Chưa có tiêu đề");
                        if (tvDesc != null) tvDesc.setText((currentTask.getDescription() != null && !currentTask.getDescription().trim().isEmpty()) ? currentTask.getDescription() : "Không có mô tả chi tiết.");
                        if (tvTime != null) tvTime.setText((currentTask.getTime() != null && !currentTask.getTime().trim().isEmpty()) ? currentTask.getTime() : "Chưa hẹn giờ");
                        if (tvLocation != null) tvLocation.setText((currentTask.getLocation() != null && !currentTask.getLocation().trim().isEmpty()) ? "📍 " + currentTask.getLocation() : "📍 Không có địa điểm / Link họp");

                        if (tvStatus != null) {
                            String s = currentTask.getStatus();
                            if ("DONE".equalsIgnoreCase(s) || "COMPLETED".equalsIgnoreCase(s) || "HOÀN THÀNH".equalsIgnoreCase(s)) {
                                tvStatus.setText("● ĐÃ HOÀN THÀNH");
                                tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                            } else {
                                tvStatus.setText("● ĐANG XỬ LÝ");
                                tvStatus.setTextColor(Color.parseColor("#E65100"));
                            }
                        }

                        // Cập nhật mức độ ưu tiên thực tế
                        updatePriorityTag(currentTask.getPriority(), currentTask.getTime());

                        renderSubtasksForCurrentTask();

                        int realProjectId = currentTask.getProjectId();
                        fetchAndShowProjectName(currentUserId, realProjectId);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {}
        });
    }

    private void updatePriorityTag(String priority, String timeStr) {
        TextView tvPriorityTag = findViewById(R.id.tvPriorityTag);
        if (tvPriorityTag == null) return;

        String combined = (priority != null ? priority : "") + " " + (timeStr != null ? timeStr : "");
        String p = combined.toUpperCase();

        if (p.contains("ƯU 2") || p.contains("TRUNG BÌNH") || p.contains("MEDIUM")) {
            tvPriorityTag.setText("! ƯU TIÊN TRUNG BÌNH");
            tvPriorityTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF8E1")));
            tvPriorityTag.setTextColor(Color.parseColor("#F57F17"));
        } else if (p.contains("ƯU 3") || p.contains("THẤP") || p.contains("LOW")) {
            tvPriorityTag.setText("! ƯU TIÊN THẤP");
            tvPriorityTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            tvPriorityTag.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            tvPriorityTag.setText("! ƯU TIÊN CAO");
            tvPriorityTag.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FDECE8")));
            tvPriorityTag.setTextColor(Color.parseColor("#D84315"));
        }
    }

    private void fetchAndShowProjectName(int userId, int projectId) {
        TextView tvProjectTag = findViewById(R.id.tvProjectTag);
        if (tvProjectTag == null) return;

        getApiService().getProjectsByUser(userId).enqueue(new Callback<List<Project>>() {
            @Override
            public void onResponse(Call<List<Project>> call, Response<List<Project>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Project p : response.body()) {
                        if (p.getId() == projectId) {
                            tvProjectTag.setText("• " + p.getName());
                            return;
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Project>> call, Throwable t) {}
        });
    }

    private ApiService getApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(ApiService.class);
    }
}
