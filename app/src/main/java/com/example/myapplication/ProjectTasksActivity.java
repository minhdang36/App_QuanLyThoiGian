package com.example.myapplication;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.TaskAdapter;
import com.example.myapplication.model.Task;

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
    private List<Task> projectTaskList;
    private int projectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_tasks);

        // Ánh xạ View
        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvProjectNameTitle = findViewById(R.id.tvProjectNameTitle);
        rvProjectTasks = findViewById(R.id.rvProjectTasks);

        btnBack.setOnClickListener(v -> finish());

        // Lấy thông tin dự án từ Intent (gửi từ ProjectAdapter)
        projectId = getIntent().getIntExtra("PROJECT_ID", -1);
        String projectName = getIntent().getStringExtra("PROJECT_NAME");

        if (projectName != null) {
            tvProjectNameTitle.setText(projectName);
        }

        // Cài đặt RecyclerView dùng chung TaskAdapter
        rvProjectTasks.setLayoutManager(new LinearLayoutManager(this));
        projectTaskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(projectTaskList);
        rvProjectTasks.setAdapter(taskAdapter);

        // Gọi API tải danh sách
        loadTasksForProject();
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
                    projectTaskList.clear();

                    // Lọc: Chỉ lấy những Task có projectId khớp với dự án đang xem
                    for (Task task : response.body()) {
                        if (task.getProjectId() == projectId) {
                            projectTaskList.add(task);
                        }
                    }
                    taskAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                Toast.makeText(ProjectTasksActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }
}