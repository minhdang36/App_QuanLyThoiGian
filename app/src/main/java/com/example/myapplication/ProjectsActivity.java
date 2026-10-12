package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.ProjectAdapter;
import com.example.myapplication.model.Project;

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

    // Khai báo các TextView hiển thị thống kê
    private TextView tvTotalTasks;
    private TextView tvCompletionRate;
    private TextView tvActiveProjects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projects);

        // Ánh xạ ảnh Avatar góc phải trên cùng (ID này đã có sẵn trong activity_projects.xml)
        android.widget.ImageView imgAvatar = findViewById(R.id.imgAvatar);
        if (imgAvatar != null) {
            imgAvatar.setOnClickListener(v -> {
                // Nhảy sang trang Profile (Cài đặt)
                Intent intent = new Intent(ProjectsActivity.this, ProfileSettingsActivity.class);
                startActivity(intent);
            });
        }

        // Nút Thêm Danh Mục Mới
        TextView btnAddProject = findViewById(R.id.btnAddProject);
        if (btnAddProject != null) {
            btnAddProject.setOnClickListener(v -> {
                Intent intent = new Intent(ProjectsActivity.this, AddProjectActivity.class);
                startActivity(intent);
            });
        }

        // Nút Hủy (Thoát trang)
        TextView tvCancelProjects = findViewById(R.id.tvCancelProjects);
        if (tvCancelProjects != null) {
            tvCancelProjects.setOnClickListener(v -> finish());
        }

        // Ánh xạ RecyclerView
        rvProjects = findViewById(R.id.rvProjects);
        rvProjects.setLayoutManager(new LinearLayoutManager(this));

        // Ánh xạ các TextView thống kê
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
        // Load lại danh sách từ server mỗi khi mở trang hoặc quay lại từ trang Thêm
        fetchDataFromApi();
    }

    private void fetchDataFromApi() {
        // 1. Lấy ID người dùng
        android.content.SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);

        if (currentUserId == -1) return;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);

        // 2. THAY VÌ GỌI getProjects(), HÃY GỌI getProjectsByUser(currentUserId)
        apiService.getProjectsByUser(currentUserId).enqueue(new Callback<List<Project>>() {
            @Override
            public void onResponse(Call<List<Project>> call, Response<List<Project>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    projectList.clear();
                    projectList.addAll(response.body());
                    projectAdapter.notifyDataSetChanged();

                    // Gọi hàm cập nhật thống kê (nếu bạn có)
                    updateSummaryData(projectList);
                }
            }

            @Override
            public void onFailure(Call<List<Project>> call, Throwable t) {
                Toast.makeText(ProjectsActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void loadProjectsFromApi() {
        // Lấy USER_ID đang đăng nhập
        android.content.SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);

        if (currentUserId == -1) {
            // Nếu chưa đăng nhập, đá văng ra màn hình Login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

// Khởi tạo Retrofit để gọi API
        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);        // GỌI API MỚI VỚI ID CỦA NGƯỜI DÙNG NÀY
        apiService.getProjectsByUser(currentUserId).enqueue(new retrofit2.Callback<List<Project>>() {
            @Override
            public void onResponse(retrofit2.Call<List<Project>> call, retrofit2.Response<List<Project>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    projectList.clear();
                    projectList.addAll(response.body());
                    projectAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(retrofit2.Call<List<Project>> call, Throwable t) {
                Toast.makeText(ProjectsActivity.this, "Lỗi tải dữ liệu!", Toast.LENGTH_SHORT).show();
            }
        });
    }
    // Hàm tính toán và hiển thị dữ liệu thật lên giao diện
    private void updateSummaryData(List<Project> list) {
        if (list == null || list.isEmpty()) {
            if (tvActiveProjects != null) tvActiveProjects.setText("0 Đang chạy");
            return;
        }

        int totalTasks = 0;
        int totalCompletedTasks = 0;
        int activeProjects = list.size(); // Số lượng dự án trả về từ API

        for (Project project : list) {
            totalTasks += project.getTotalTasks(); // Lấy tổng task của từng dự án
            totalCompletedTasks += project.getFinishedTasks(); // Lấy số task đã hoàn thành
        }

        int averageCompletion = 0;
        if (totalTasks > 0) {
            averageCompletion = (totalCompletedTasks * 100) / totalTasks;
        }

        // Cập nhật lên giao diện
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